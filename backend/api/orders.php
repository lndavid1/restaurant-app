<?php
error_reporting(0);
ini_set('display_errors', 0);

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Access-Control-Allow-Methods: POST, GET, PUT, OPTIONS");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit(); }

include_once '../config/db.php';
include_once '../libs/jwt.php';

$database = new Database();
$db = $database->getConnection();

// Lấy Header Authorization một cách an toàn nhất
$all_headers = getallheaders();
$auth_header = "";
foreach ($all_headers as $name => $value) {
    if (strtolower($name) === 'authorization') {
        $auth_header = $value;
        break;
    }
}
$jwt = $auth_header ? str_replace('Bearer ', '', $auth_header) : null;
$user = JWT::decode($jwt);

if (!$user) {
    http_response_code(401);
    echo json_encode(["message" => "Hết phiên đăng nhập. Vui lòng đăng nhập lại."]);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];

switch($method) {
    case 'POST':
        $input = file_get_contents("php://input");
        $data = json_decode($input);
        if (!$data || empty($data->table_id)) {
            http_response_code(400);
            echo json_encode(["message" => "Dữ liệu không hợp lệ."]);
            exit();
        }

        $db->beginTransaction();
        try {
            if (empty($data->items)) {
                // Khách hàng vừa mới chọn bàn (Chưa order món nào)
                // -> Chỉ khoá bàn (đổi màu bàn), KHÔNG tạo đơn rỗng đẩy xuống bếp!
                $stmt = $db->prepare("UPDATE restaurant_tables SET status = 'occupied' WHERE id = ?");
                $stmt->execute([(int)$data->table_id]);
                
                $db->commit();
                echo json_encode(["message" => "Đã giữ bàn thành công!"]);
            } else {
                // 1. Tạo đơn hàng (Có món)
                $stmt = $db->prepare("INSERT INTO orders (user_id, table_id, total_amount, order_status) VALUES (?, ?, ?, 'pending')");
                $stmt->execute([$user->id, (int)$data->table_id, $data->total_amount]);
                $order_id = $db->lastInsertId();

                // 2. Lưu chi tiết món
                foreach ($data->items as $item) {
                    $stmt = $db->prepare("INSERT INTO order_details (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)");
                    $stmt->execute([$order_id, (int)$item->product_id, (int)$item->quantity, $item->price]);
                }

                // 3. ĐỔI MÀU BÀN SANG ĐỎ (Occupied) NGAY LẬP TỨC
                $stmt = $db->prepare("UPDATE restaurant_tables SET status = 'occupied' WHERE id = ?");
                $stmt->execute([(int)$data->table_id]);

                $db->commit();
                echo json_encode(["message" => "Đã gửi đơn hàng tới bếp!"]);
            }
        } catch (Exception $e) {
            $db->rollBack();
            http_response_code(500);
            echo json_encode(["message" => "Lỗi server: " . $e->getMessage()]);
        }
        break;

    case 'GET':
        $query = "SELECT o.*, u.full_name as employee_name, t.table_number
                  FROM orders o
                  JOIN users u ON o.user_id = u.id
                  LEFT JOIN restaurant_tables t ON o.table_id = t.id
                  ORDER BY o.created_at DESC";
        $stmt = $db->prepare($query);
        $stmt->execute();
        $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);

        foreach ($orders as &$order) {
            $order['id'] = (int)$order['id'];
            $order['table_id'] = $order['table_id'] ? (int)$order['table_id'] : null;
            $stmtDetails = $db->prepare("SELECT od.quantity, p.name FROM order_details od JOIN products p ON od.product_id = p.id WHERE od.order_id = ?");
            $stmtDetails->execute([$order['id']]);
            $order['items_detail'] = $stmtDetails->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode($orders);
        break;

    case 'PUT':
        $input = file_get_contents("php://input");
        $data = json_decode($input);
        if ($user->role != 'admin' || empty($data->order_id) || empty($data->status)) {
            http_response_code(403);
            echo json_encode(["message" => "Không có quyền hoặc dữ liệu sai."]);
            exit();
        }

        $stmt = $db->prepare("UPDATE orders SET order_status = ? WHERE id = ?");
        if ($stmt->execute([$data->status, (int)$data->order_id])) {
            echo json_encode(["message" => "Đã cập nhật trạng thái đơn hàng!"]);
        } else {
            http_response_code(500);
            echo json_encode(["message" => "Lỗi cập nhật SQL."]);
        }
        break;
}
?>
