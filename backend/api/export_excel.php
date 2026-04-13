<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: text/csv; charset=utf-8");
header("Content-Disposition: attachment; filename=report_orders.csv");

include_once '../config/db.php';

$database = new Database();
$db = $database->getConnection();

$output = fopen("php://output", "w");
fputcsv($output, array('ID', 'User ID', 'Type', 'Amount', 'Payment Status', 'Order Status', 'Date'));

$query = "SELECT id, user_id, order_type, total_amount, payment_status, order_status, created_at FROM orders";
$stmt = $db->prepare($query);
$stmt->execute();

while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    fputcsv($output, $row);
}

fclose($output);
?>
