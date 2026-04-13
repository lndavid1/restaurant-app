<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/db.php';
include_once '../libs/jwt.php';

$database = new Database();
$db = $database->getConnection();

$headers = getallheaders();
$jwt = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;
$user = JWT::decode($jwt);

if (!$user || $user->role != 'admin') {
    http_response_code(403);
    echo json_encode(["message" => "Access denied."]);
    exit();
}

// 1. Total Revenue
$query = "SELECT SUM(total_amount) as total FROM orders WHERE payment_status = 'paid'";
$stmt = $db->prepare($query);
$stmt->execute();
$revenue = $stmt->fetch(PDO::FETCH_ASSOC);

// 2. Orders count
$query = "SELECT COUNT(*) as count FROM orders";
$stmt = $db->prepare($query);
$stmt->execute();
$orders_count = $stmt->fetch(PDO::FETCH_ASSOC);

// 3. Revenue by day (last 7 days)
$query = "SELECT DATE(created_at) as date, SUM(total_amount) as amount FROM orders WHERE payment_status = 'paid' AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY DATE(created_at)";
$stmt = $db->prepare($query);
$stmt->execute();
$daily_revenue = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo json_encode([
    "total_revenue" => $revenue['total'] ?? 0,
    "total_orders" => $orders_count['count'] ?? 0,
    "daily_revenue" => $daily_revenue
]);
?>
