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
        case 'POST': // THÊM MỚI
            $input = file_get_contents("php://input");
            $data = json_decode($input);
            if (empty($data->name) || empty($data->price)) throw new Exception("Thiếu thông tin món ăn.");

            $query = "INSERT INTO products (category_id, name, description, price, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?)";
            $stmt = $db->prepare($query);
            $stmt->execute([
                $data->category_id ?? 1,
                $data->name,
                $data->description ?? "",
                $data->price,
                $data->image_url ?? "",
                1
            ]);
            echo json_encode(["message" => "Thêm món ăn thành công!"]);
            break;

        case 'PUT': // CẬP NHẬT
            $input = file_get_contents("php://input");
            $data = json_decode($input);
            if (empty($data->id)) throw new Exception("Thiếu ID món ăn cần sửa.");

            $query = "UPDATE products SET category_id=?, name=?, description=?, price=?, image_url=?, is_available=? WHERE id=?";
            $stmt = $db->prepare($query);
            $stmt->execute([
                $data->category_id,
                $data->name,
                $data->description,
                $data->price,
                $data->image_url,
                $data->is_available ?? 1,
                $data->id
            ]);
            echo json_encode(["message" => "Cập nhật thành công!"]);
            break;

        case 'DELETE': // XÓA
            $id = isset($_GET['id']) ? $_GET['id'] : null;
            if (!$id) throw new Exception("Thiếu ID món ăn cần xóa.");

            $query = "DELETE FROM products WHERE id = ?";
            $stmt = $db->prepare($query);
            $stmt->execute([$id]);
            echo json_encode(["message" => "Đã xóa món ăn."]);
            break;
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["message" => "Lỗi: " . $e->getMessage()]);
}
?>
