<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, GET");

include_once '../config/db.php';
include_once '../libs/jwt.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

switch($method) {
    case 'POST':
        $data = json_decode(file_get_contents("php://input"));
        $headers = getallheaders();
        $jwt = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;
        $user = JWT::decode($jwt);

        if (!$user) {
            http_response_code(401);
            echo json_encode(["message" => "Unauthorized"]);
            break;
        }

        $query = "INSERT INTO reservations (user_id, table_id, reservation_time, num_people, status) VALUES (?, ?, ?, ?, 'pending')";
        $stmt = $db->prepare($query);
        if ($stmt->execute([$user->id, $data->table_id, $data->reservation_time, $data->num_people])) {
            echo json_encode(["message" => "Reservation created."]);
        }
        break;

    case 'GET':
        $query = "SELECT r.*, u.full_name, t.table_number FROM reservations r JOIN users u ON r.user_id = u.id JOIN restaurant_tables t ON r.table_id = t.id";
        $stmt = $db->prepare($query);
        $stmt->execute();
        $reservations = $stmt->fetchAll(PDO::FETCH_ASSOC);
        echo json_encode($reservations);
        break;
}
?>
