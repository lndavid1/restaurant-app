<?php
error_reporting(E_ALL);
ini_set('display_errors', 0); // Giữ sạch JSON

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Authorization, Admin-Role, X-Requested-With");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit(); }

include_once '../config/db.php';

// Bypass JWT: We check the internal Admin-Role header assigned by Android app Retrofit client
$headers = getallheaders();
$isAdmin = isset($headers['Admin-Role']) && $headers['Admin-Role'] === 'true';

if (!$isAdmin) {
    http_response_code(403);
    echo json_encode(["message" => "Truy cập bị từ chối. Chỉ Admin mới có quyền này."]);
    exit();
}

$database = new Database();
$db = $database->getConnection();
$method = $_SERVER['REQUEST_METHOD'];

try {
    switch($method) {
        case 'GET':
            $query = "SELECT id, name, unit, stock, updated_at FROM ingredients ORDER BY id ASC";
            $stmt = $db->prepare($query);
            $stmt->execute();
            $items = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach($items as &$item) {
                $item['id'] = (int)$item['id'];
                $item['stock'] = (float)$item['stock'];
            }
            echo json_encode($items);
            break;

        case 'POST': // Thêm nguyên liệu
            $input = file_get_contents("php://input");
            $data = json_decode($input);
            if (empty($data->name) || empty($data->unit)) throw new Exception("Thiếu Tên hoặc Đơn vị tính.");
            
            $query = "INSERT INTO ingredients (name, unit, stock) VALUES (?, ?, ?)";
            $stmt = $db->prepare($query);
            $stmt->execute([$data->name, $data->unit, $data->stock ?? 0]);
            echo json_encode(["message" => "Thêm nguyên liệu thành công!"]);
            break;

        case 'PUT': // Cập nhật nguyên liệu
            $input = file_get_contents("php://input");
            $data = json_decode($input);
            if (empty($data->id)) throw new Exception("Thiếu ID cần cập nhật.");

            $query = "UPDATE ingredients SET name=?, unit=?, stock=? WHERE id=?";
            $stmt = $db->prepare($query);
            $stmt->execute([$data->name, $data->unit, $data->stock, $data->id]);
            echo json_encode(["message" => "Cập nhật thành công!"]);
            break;

        case 'DELETE': // Xóa
            $id = isset($_GET['id']) ? $_GET['id'] : null;
            if (!$id) throw new Exception("Thiếu ID cần xóa.");

            $query = "DELETE FROM ingredients WHERE id = ?";
            $stmt = $db->prepare($query);
            $stmt->execute([$id]);
            echo json_encode(["message" => "Đã xóa nguyên liệu."]);
            break;
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["message" => "Lỗi: " . $e->getMessage()]);
}
?>
