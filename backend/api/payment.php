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

$database = new Database();
$db = $database->getConnection();

// Lấy Token bảo mật an toàn
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

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->table_id)) {
    // 1. Tìm đơn hàng VNĐ chưa thanh toán
    $stmt = $db->prepare("SELECT id, total_amount FROM orders WHERE table_id = ? AND payment_status = 'unpaid' AND order_status != 'cancelled' LIMIT 1");
    $stmt->execute([(int)$data->table_id]);
    $order = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$order) {
        echo json_encode(["message" => "Không tìm thấy hóa đơn cần thanh toán."]);
        exit();
    }

    if ($data->method == 'cash') {
        $db->beginTransaction();
        try {
            $db->prepare("UPDATE orders SET payment_status = 'paid', order_status = 'completed' WHERE id = ?")->execute([$order['id']]);
            $db->prepare("UPDATE restaurant_tables SET status = 'available' WHERE id = ?")->execute([(int)$data->table_id]);
            $db->commit();
            echo json_encode(["message" => "Thanh toán VNĐ thành công!", "status" => "success"]);
        } catch (Exception $e) {
            $db->rollBack();
            http_response_code(500);
            echo json_encode(["message" => "Lỗi server."]);
        }
    } else {
        // --- CẤU HÌNH VNPAY SANDBOX CHUẨN ---
        $vnp_TmnCode = trim("2QXG2YQ8");
        $vnp_HashSecret = trim("ONLREBTMUPURMREBWZBIXFOCBSNAGSST");
        $vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        $vnp_Returnurl = "http://10.0.2.2/backend/api/vnpay_return.php";

        // VNPAY yêu cầu Số tiền * 100 và phải là số NGUYÊN (Không dấu chấm)
        // Hệ thống dùng VNĐ trực tiếp, nên ta nhân với 100.
        $vnp_Amount = (int)round($order['total_amount'] * 100);

        $vnp_TxnRef = $order['id'] . "_" . date("His");
        $vnp_IpAddr = '127.0.0.1'; // Fix IPv6 loopback issue

        $inputData = array(
            "vnp_Version" => "2.1.0",
            "vnp_TmnCode" => $vnp_TmnCode,
            "vnp_Amount" => $vnp_Amount,
            "vnp_Command" => "pay",
            "vnp_CreateDate" => date('YmdHis'),
            "vnp_CurrCode" => "VND",
            "vnp_IpAddr" => $vnp_IpAddr,
            "vnp_Locale" => "vn",
            "vnp_OrderInfo" => "Thanh toan hoa don nha hang",
            "vnp_OrderType" => "billpayment",
            "vnp_ReturnUrl" => $vnp_Returnurl,
            "vnp_TxnRef" => $vnp_TxnRef
        );

        ksort($inputData);
        $query = "";
        $i = 0;
        $hashdata = "";
        foreach ($inputData as $key => $value) {
            if ($i == 1) {
                $hashdata .= '&' . urlencode($key) . "=" . urlencode($value);
            } else {
                $hashdata .= urlencode($key) . "=" . urlencode($value);
                $i = 1;
            }
            $query .= urlencode($key) . "=" . urlencode($value) . '&';
        }

        $vnp_Url .= "?" . $query;
        $vnpSecureHash = hash_hmac('sha512', $hashdata, $vnp_HashSecret);
        $vnp_Url .= 'vnp_SecureHash=' . $vnpSecureHash;

        echo json_encode(["payment_url" => $vnp_Url, "status" => "vnpay_redirect"]);
    }
}
?>
