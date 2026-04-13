<?php
// Tắt mọi thông báo lỗi hiển thị ra ngoài để tránh làm hỏng cấu trúc JSON
error_reporting(0);
ini_set('display_errors', 0);

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once '../config/db.php';
include_once '../libs/jwt.php';

$database = new Database();
$db = $database->getConnection();

if (!$db) {
    http_response_code(500);
    echo json_encode(["message" => "Database connection failed."]);
    exit();
}

$input = file_get_contents("php://input");
$data = json_decode($input);

$action = isset($_GET['action']) ? $_GET['action'] : '';

if ($action == 'register') {
    if (!empty($data->email) && !empty($data->password) && !empty($data->full_name)) {
        $checkQuery = "SELECT id FROM users WHERE email = ?";
        $checkStmt = $db->prepare($checkQuery);
        $checkStmt->execute([$data->email]);
        if ($checkStmt->rowCount() > 0) {
            http_response_code(400);
            echo json_encode(["message" => "Email already exists."]);
            exit();
        }

        $query = "INSERT INTO users (role_id, full_name, email, password, phone, address) VALUES (?, ?, ?, ?, ?, ?)";
        $stmt = $db->prepare($query);
        $password_hash = password_hash($data->password, PASSWORD_BCRYPT);
        if ($stmt->execute([2, $data->full_name, $data->email, $password_hash, $data->phone, $data->address])) {
            echo json_encode(["message" => "User was registered."]);
        } else {
            http_response_code(500);
            echo json_encode(["message" => "Unable to register user."]);
        }
    } else {
        http_response_code(400);
        echo json_encode(["message" => "Incomplete data."]);
    }
} else if ($action == 'login') {
    if (!empty($data->email) && !empty($data->password)) {
        $query = "SELECT u.id, u.full_name, u.password, r.role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.email = ? LIMIT 1";
        $stmt = $db->prepare($query);
        $stmt->execute([$data->email]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($user && password_verify($data->password, $user['password'])) {
            $token = JWT::encode([
                "id" => (int)$user['id'],
                "full_name" => $user['full_name'],
                "role" => $user['role_name'],
                "exp" => time() + (3600 * 24) // 1 day expiration
            ]);

            echo json_encode([
                "message" => "Successful login.",
                "jwt" => $token,
                "role" => $user['role_name']
            ]);
        } else {
            http_response_code(401);
            echo json_encode(["message" => "Login failed. Invalid email or password."]);
        }
    } else {
        http_response_code(400);
        echo json_encode(["message" => "Incomplete data."]);
    }
} else {
    http_response_code(400);
    echo json_encode(["message" => "Invalid action."]);
}
?>
