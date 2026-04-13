<?php
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

// ─── CẤU HÌNH PAYOS ─────────────────────────────────────
// *** THAY BẰNG 3 KEY THẬT CỦA BẠN TỪ DASHBOARD PAYOS ***
define('PAYOS_CLIENT_ID', '0c136e8f-fa57-45dc-90d3-a4210fee504f');
define('PAYOS_API_KEY', '53867de1-a9af-4e50-9077-7d32f1039457');
define('PAYOS_CHECKSUM_KEY', '2799eab820b20561d83819aa8599a6b663b76375648d4f0b9ae995e47bfaf901');
define('PAYOS_API_URL', 'https://api-merchant.payos.vn/v2/payment-requests');
// Sử dụng Deep-link để App Android tự động mở lại sau khi khách hàng thanh toán xong
define('PAYOS_RETURN_URL', 'restaurantapp://payment/success');
define('PAYOS_CANCEL_URL', 'restaurantapp://payment/cancel');
// ─────────────────────────────────────────────────────────

// === 1. Xác thực JWT ================================
$auth_header = "";
foreach (getallheaders() as $name => $value) {
    if (strtolower($name) === 'authorization') {
        $auth_header = $value;
        break;
    }
}
$jwt = $auth_header ? str_replace('Bearer ', '', $auth_header) : null;
$user = JWT::decode($jwt);

if (!$user) {
    http_response_code(401);
    echo json_encode(["message" => "Hết phiên đăng nhập"]);
    exit();
}

// === 2. Đọc body =====================================
$data = json_decode(file_get_contents("php://input"));
if (empty($data->order_id)) {
    http_response_code(400);
    echo json_encode(["message" => "Thiếu order_id"]);
    exit();
}

$orderId = (int) $data->order_id;

// === 3. Lấy thông tin đơn hàng từ DB =================
$database = new Database();
$db = $database->getConnection();

$stmt = $db->prepare("SELECT id, total_amount, user_id FROM orders WHERE id = ? AND payment_status = 'unpaid' LIMIT 1");
$stmt->execute([$orderId]);
$order = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$order) {
    http_response_code(404);
    echo json_encode(["message" => "Không tìm thấy đơn hàng hoặc đã thanh toán."]);
    exit();
}

// === 4. Tạo dữ liệu thanh toán =======================
// PayOS yêu cầu amount là VNĐ nguyên – KHÔNG nhân 100
$amount = (int) round($order['total_amount']);
// orderCode duy nhất (int53), fix lỗi trùng trong cùng 1 phút
$orderCode = (int) (time() . rand(100, 999));
// Description không dấu, không ký tự đặc biệt (Max 25 ký tự)
$description = "Thanh toan don " . $orderId;

// === 5. Tạo SIGNATURE (HMAC-SHA256) ==================
// Xây dựng dictionary, sắp xếp theo alphabe và nối với '&' (Không url_encode)
$signParams = [
    "amount" => $amount,
    "cancelUrl" => PAYOS_CANCEL_URL,
    "description" => $description,
    "orderCode" => $orderCode,
    "returnUrl" => PAYOS_RETURN_URL
];
ksort($signParams);

$signParts = [];
foreach ($signParams as $k => $v) {
    $signParts[] = "{$k}={$v}";
}
$signData = implode('&', $signParts);

$signature = hash_hmac('sha256', $signData, PAYOS_CHECKSUM_KEY);

// === 6. Gọi PayOS API ================================
$payload = [
    "orderCode" => $orderCode,
    "amount" => $amount,
    "description" => $description,
    "returnUrl" => PAYOS_RETURN_URL,
    "cancelUrl" => PAYOS_CANCEL_URL,
    "signature" => $signature,
    "items" => [
        [
            "name" => "Hoa don nha hang #" . $orderId,
            "quantity" => 1,
            "price" => $amount
        ]
    ]
];

$ch = curl_init(PAYOS_API_URL);
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_HTTPHEADER => [
        "Content-Type: application/json",
        "x-client-id: " . PAYOS_CLIENT_ID,
        "x-api-key: " . PAYOS_API_KEY,
    ],
    CURLOPT_TIMEOUT => 15,
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

if ($curlError) {
    http_response_code(500);
    echo json_encode(["message" => "Lỗi kết nối PayOS: " . $curlError]);
    exit();
}

$result = json_decode($response, true);

// PayOS trả về code = "00" nếu thành công
if (isset($result['code']) && $result['code'] === '00') {
    $checkoutUrl = $result['data']['checkoutUrl'] ?? null;
    $payosOrderCode = $result['data']['orderCode'] ?? $orderCode;

    // Validate checkoutUrl để chống crash frontend nếu rỗng
    if (empty($checkoutUrl)) {
        http_response_code(502);
        echo json_encode(["message" => "Lỗi cấu hình: Thu ngân chưa chèn đúng API Key hoặc API Key hết hạn (PayOS không cấp link)", "raw" => $result]);
        exit();
    }

    // Lưu payos_order_code vào DB để đối soát webhook sau
    $db->prepare("UPDATE orders SET payos_order_code = ? WHERE id = ?")
        ->execute([$payosOrderCode, $orderId]);

    echo json_encode([
        "status" => "payos_redirect",
        "checkout_url" => $checkoutUrl,
        "order_code" => $payosOrderCode
    ]);
} else {
    $msg = $result['desc'] ?? 'Lỗi không xác định từ PayOS';
    http_response_code(502);
    echo json_encode(["message" => "PayOS lỗi: " . $msg, "raw" => $result]);
}
?>