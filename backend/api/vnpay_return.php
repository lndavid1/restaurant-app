<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/db.php';

$vnp_HashSecret = "YOUR_HASH_SECRET"; // Sandbox Hash Secret
$vnp_SecureHash = $_GET['vnp_SecureHash'];
$inputData = array();
foreach ($_GET as $key => $value) {
    if (substr($key, 0, 4) == "vnp_") {
        $inputData[$key] = $value;
    }
}

unset($inputData['vnp_SecureHash']);
ksort($inputData);
$i = 0;
$hashData = "";
foreach ($inputData as $key => $value) {
    if ($i == 1) {
        $hashData .= '&' . urlencode($key) . "=" . urlencode($value);
    } else {
        $hashData .= urlencode($key) . "=" . urlencode($value);
        $i = 1;
    }
}

$secureHash = hash_hmac('sha512', $hashData, $vnp_HashSecret);

if ($secureHash == $vnp_SecureHash) {
    if ($_GET['vnp_ResponseCode'] == '00') {
        $database = new Database();
        $db = $database->getConnection();

        $order_id = $_GET['vnp_TxnRef'];
        $query = "UPDATE orders SET payment_status = 'paid' WHERE id = ?";
        $stmt = $db->prepare($query);
        $stmt->execute([$order_id]);

        echo "<h1>Payment Success</h1><p>Order ID: $order_id</p>";
    } else {
        echo "<h1>Payment Failed</h1>";
    }
} else {
    echo "<h1>Invalid Signature</h1>";
}
?>
