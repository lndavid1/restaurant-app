<?php
// File này chỉ hứng redirect từ PayOS sau khi thanh toán thành công
// Việc thực sự cập nhật DB được thực hiện AN TOÀN qua Webhook (payos_webhook.php)
// Trang này chỉ hiển thị thông báo thân thiện cho người dùng
header("Content-Type: text/html; charset=UTF-8");
$orderCode = $_GET['orderCode'] ?? 'N/A';
$status    = $_GET['status']    ?? '';
?>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Thanh toán thành công</title>
<style>
  body { font-family: sans-serif; text-align: center; padding-top: 60px; background: #f0faf0; }
  .card { background: white; max-width: 380px; margin: auto; padding: 40px 24px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
  .icon { font-size: 64px; }
  h2 { color: #2e7d32; margin: 16px 0 8px; }
  p  { color: #666; font-size: 14px; }
  .code { font-weight: bold; color: #333; font-size: 18px; margin: 12px 0; }
</style>
</head>
<body>
  <div class="card">
    <div class="icon">✅</div>
    <h2>Thanh toán thành công!</h2>
    <div class="code">Mã đơn: #<?php echo htmlspecialchars($orderCode); ?></div>
    <p>Giao dịch của bạn đã được ghi nhận.<br>Bạn có thể đóng trang này và quay lại ứng dụng.</p>
  </div>
</body>
</html>
