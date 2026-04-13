<?php
header("Content-Type: text/html; charset=UTF-8");
$orderCode = $_GET['orderCode'] ?? 'N/A';
?>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Đã huỷ thanh toán</title>
<style>
  body { font-family: sans-serif; text-align: center; padding-top: 60px; background: #fff8f0; }
  .card { background: white; max-width: 380px; margin: auto; padding: 40px 24px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
  .icon { font-size: 64px; }
  h2 { color: #c62828; margin: 16px 0 8px; }
  p  { color: #666; font-size: 14px; }
</style>
</head>
<body>
  <div class="card">
    <div class="icon">❌</div>
    <h2>Đã huỷ thanh toán</h2>
    <p>Bạn đã huỷ giao dịch #<?php echo htmlspecialchars($orderCode); ?>.<br>Đóng trang này và thử lại khi cần.</p>
  </div>
</body>
</html>
