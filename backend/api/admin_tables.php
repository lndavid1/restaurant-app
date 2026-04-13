<?php
error_reporting(0);
ini_set('display_errors', 0);

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Authorization, Admin-Role, X-Requested-With");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit(); }

include_once '../config/db.php';

// Auth bypass verify
$headers = getallheaders();
$isAdmin = isset($headers['Admin-Role']) && $headers['Admin-Role'] === 'true';

// Employees also need Table access to GET tables and checkout.
// For simplicity, we allow standard GET requests, but block unauthorized mutations (except checkout handled uniquely).
// Actually, let's just let it be fully accessible for Employee & Admin via Android checking UI.

$database = new Database();
$db = $database->getConnection();
$action = isset($_GET['action']) ? $_GET['action'] : '';
$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'POST' && $action == 'checkout') {
        $input = file_get_contents("php://input");
        $data = json_decode($input);

        if (!$data || empty($data->table_id)) {
            http_response_code(400);
            echo json_encode(["message" => "Thiếu mã bàn."]);
            exit();
        }

        $db->beginTransaction();
        $table_id = (int)$data->table_id;

        // 1. Giải phóng
        $stmt = $db->prepare("UPDATE restaurant_tables SET status = 'available' WHERE id = ?");
        $stmt->execute([$table_id]);

        // 2. Chốt bill
        $stmt = $db->prepare("UPDATE orders SET order_status = 'completed', payment_status = 'paid' WHERE table_id = ? AND payment_status = 'unpaid'");
        $stmt->execute([$table_id]);

        $db->commit();
        echo json_encode(["message" => "Thanh toán thành công!", "status" => "success"]);
        exit();
    }

    if ($method === 'GET') {
        $query = "SELECT id, table_number, capacity, status FROM restaurant_tables ORDER BY id ASC";
        $stmt = $db->prepare($query);
        $stmt->execute();
        $tables = $stmt->fetchAll(PDO::FETCH_ASSOC);
        foreach($tables as &$t) {
            $t['id'] = (int)$t['id'];
            $t['capacity'] = (int)$t['capacity'];
        }
        echo json_encode($tables);
        exit();
    }

    // API dưới đây yêu cầu Admin
    if (!$isAdmin) {
        http_response_code(403);
        echo json_encode(["message" => "Lỗi phân quyền. Yêu cầu Admin."]);
        exit();
    }

    if ($method === 'POST') {
        $input = file_get_contents("php://input");
        $data = json_decode($input);
        if (empty($data->table_number)) throw new Exception("Thiếu tên bàn");
        $stmt = $db->prepare("INSERT INTO restaurant_tables (table_number, capacity, status) VALUES (?, ?, ?)");
        $stmt->execute([$data->table_number, $data->capacity ?? 4, $data->status ?? 'available']);
        echo json_encode(["message" => "Đã thêm bàn"]);
        
    } elseif ($method === 'PUT') {
        $input = file_get_contents("php://input");
        $data = json_decode($input);
        if (empty($data->id)) throw new Exception("Thiếu mã bàn");
        $stmt = $db->prepare("UPDATE restaurant_tables SET table_number=?, capacity=?, status=? WHERE id=?");
        $stmt->execute([$data->table_number, $data->capacity, $data->status, $data->id]);
        echo json_encode(["message" => "Đã cập nhật bàn"]);
        
    } elseif ($method === 'DELETE') {
        $id = $_GET['id'] ?? null;
        if (!$id) throw new Exception("Thiếu mã bàn");
        $stmt = $db->prepare("DELETE FROM restaurant_tables WHERE id=?");
        $stmt->execute([$id]);
        echo json_encode(["message" => "Đã xóa bàn"]);
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["message" => "Lỗi server: " . $e->getMessage()]);
}
?>
