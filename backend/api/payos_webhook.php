<?php
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/db.php';

// ─── CẤU HÌNH PAYOS ─────────────────────────────────────
// ─── CẤU HÌNH PAYOS ─────────────────────────────────────
define('PAYOS_CHECKSUM_KEY', '2799eab820b20561d83819aa8599a6b663b76375648d4f0b9ae995e47bfaf901');
$firebaseProjectId = "android-app-4ba50";
$firebaseApiKey = "AIzaSyANyV3YLPanlM8ogZE96BuHud1FYrgYfrQ";
// ─────────────────────────────────────────────────────────

// Hàm update Firestore
function updateFirestore(string $projectId, string $apiKey, string $collection, string $docId, array $fields) {
    if (empty($docId)) return;
    $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/{$collection}/{$docId}?";
    
    $updateMask = [];
    $formattedFields = [];
    foreach ($fields as $key => $val) {
        $updateMask[] = "updateMask.fieldPaths={$key}";
        if (is_int($val)) {
            $formattedFields[$key] = ["integerValue" => $val];
        } else {
            $formattedFields[$key] = ["stringValue" => $val];
        }
    }
    
    $url .= implode('&', $updateMask);
    // Thêm Auth Key để Firebase cho phép ghi dữ liệu
    $url .= "&key={$apiKey}";
    
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PATCH");
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(["fields" => $formattedFields]));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_exec($ch);
    curl_close($ch);
}

// === BƯỚC 1: Đọc raw body (PayOS gửi JSON) ============
$rawBody = file_get_contents("php://input");
$data    = json_decode($rawBody, true);

if (!$data) {
    http_response_code(400);
    echo json_encode(["error" => "Invalid JSON body"]);
    exit();
}

// === BƯỚC 2: VERIFY SIGNATURE (CỰC QUAN TRỌNG) ========
$webhookData = $data['data'] ?? [];

$signFields = [
    'accountNumber', 'amount', 'description', 'orderCode',
    'reference', 'transactionDateTime', 'virtualAccountName',
    'virtualAccountNumber'
];

$signParts = [];
foreach ($signFields as $field) {
    if (array_key_exists($field, $webhookData)) {
        $signParts[] = $field . '=' . $webhookData[$field];
    }
}
$signString = implode('&', $signParts);

$computedSignature = hash_hmac('sha256', $signString, PAYOS_CHECKSUM_KEY);
$receivedSignature = $data['signature'] ?? '';

if (!hash_equals($computedSignature, $receivedSignature)) {
    http_response_code(400);
    echo json_encode(["error" => "Invalid signature"]);
    exit();
}

// === BƯỚC 3: Xử lý sự kiện ============================
$code      = $data['code']              ?? '';
$orderCode = $webhookData['orderCode']  ?? 0;
$amount    = $webhookData['amount']     ?? 0;

if ($code !== '00') {
    echo json_encode(["success" => true]);
    exit();
}

// === BƯỚC 4: Cập nhật CSDL ============================
$database = new Database();
$db       = $database->getConnection();

$stmt = $db->prepare("SELECT id, total_amount, table_id FROM orders WHERE payos_order_code = ? LIMIT 1");
$stmt->execute([(int)$orderCode]);
$order = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$order) {
    http_response_code(404);
    echo json_encode(["error" => "Order not found"]);
    exit();
}

$orderId = (string)$order['id'];
$tableId = (string)$order['table_id'];

$db->beginTransaction();
try {
    // 1. Update MySQL
    $db->prepare("UPDATE orders SET payment_status = 'paid', order_status = 'completed' WHERE id = ?")->execute([$orderId]);
    if (!empty($tableId) && $tableId != "0") {
        $db->prepare("UPDATE restaurant_tables SET status = 'available' WHERE id = ?")->execute([$tableId]);
    }
    $db->commit();

    // 2. Update Firestore TỰ ĐỘNG để bắn chuông
    updateFirestore($firebaseProjectId, $firebaseApiKey, "orders", $orderId, [
        "payment_status" => "paid",
        "order_status" => "completed"
    ]);

    if (!empty($tableId) && $tableId != "0") {
        updateFirestore($firebaseProjectId, $firebaseApiKey, "restaurant_tables", $tableId, [
            "status" => "available"
        ]);
    }

    echo json_encode(["success" => true]);

} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode(["error" => "Database error"]);
}
?>
