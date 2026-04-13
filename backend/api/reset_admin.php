<?php
// Bật hiển thị lỗi tối đa để chẩn đoán
error_reporting(E_ALL);
ini_set('display_errors', 1);

include_once '../config/db.php';

$database = new Database();
$db = $database->getConnection();

echo "<body style='font-family: sans-serif; line-height: 1.6; padding: 20px;'>";
echo "<div style='max-width: 800px; margin: 0 auto; border: 1px solid #ccc; padding: 20px; border-radius: 10px; background: #f9f9f9;'>";
echo "<h2 style='color: #2196F3; text-align: center;'>HỆ THỐNG KIỂM TRA & KHỞI TẠO TÀI KHOẢN</h2>";

if(!$db) {
    die("<p style='color:red;'>❌ LỖI: Không thể kết nối Database. Hãy kiểm tra file config/db.php và XAMPP!</p>");
}

try {
    echo "<h4>1. Dọn dẹp dữ liệu cũ (Xóa triệt để các bảng)...</h4>";
    $db->exec("SET FOREIGN_KEY_CHECKS = 0;");
    $db->exec("DROP TABLE IF EXISTS users, roles, categories, products, restaurant_tables, orders, order_details, reservations, invoices, notifications;");
    $db->exec("SET FOREIGN_KEY_CHECKS = 1;");
    echo "<p style='color:green;'>✅ Đã làm sạch database.</p>";

    echo "<h4>2. Khởi tạo lại cấu trúc bảng từ SQL...</h4>";
    $sqlFile = '../database.sql';
    if (!file_exists($sqlFile)) {
        throw new Exception("Không tìm thấy file database.sql tại: " . realpath($sqlFile));
    }

    $sql = file_get_contents($sqlFile);
    // Tách các lệnh SQL theo dấu chấm phẩy
    $queries = explode(';', $sql);
    foreach ($queries as $q) {
        $q = trim($q);
        if (!empty($q)) {
            $db->exec($q);
        }
    }
    echo "<p style='color:green;'>✅ Đã tạo lại toàn bộ cấu trúc bảng chuẩn.</p>";

    echo "<h4>3. Tạo tài khoản đăng nhập (Ghi đè dữ liệu mẫu)...</h4>";

    // QUAN TRỌNG: Xóa các user này nếu lỡ database.sql có chèn sẵn để tránh lỗi Duplicate entry
    $db->exec("DELETE FROM users WHERE email IN ('admin@restaurant.com', 'nhanvien@restaurant.com')");

    $password = "admin123";
    $hash = password_hash($password, PASSWORD_BCRYPT);

    // Thêm Admin (Role ID 1)
    $stmtAdmin = $db->prepare("INSERT INTO users (role_id, full_name, email, password, phone, address) VALUES (1, 'Admin Hệ Thống', 'admin@restaurant.com', ?, '0912345678', 'Trung Tâm')");
    $stmtAdmin->execute([$hash]);
    echo "<p style='color:green;'>✅ Đã tạo Admin thành công!</p>";

    // Thêm Nhân viên (Role ID 3)
    $stmtEmp = $db->prepare("INSERT INTO users (role_id, full_name, email, password, phone, address) VALUES (3, 'Nhân Viên Phục Vụ', 'nhanvien@restaurant.com', ?, '0888999000', 'Sảnh Chính')");
    $stmtEmp->execute([$hash]);
    echo "<p style='color:green;'>✅ Đã tạo Nhân viên thành công!</p>";

    echo "<div style='background: #e3f2fd; padding: 15px; border-radius: 5px; margin-top: 20px;'>";
    echo "<h3 style='margin-top:0;'>🚀 THÔNG TIN ĐĂNG NHẬP:</h3>";
    echo "🔑 Mật khẩu chung: <b style='color:red; font-size: 20px;'>admin123</b><br><br>";
    echo "🔹 <b>ADMIN:</b> admin@restaurant.com<br>";
    echo "🔹 <b>NHÂN VIÊN:</b> nhanvien@restaurant.com";
    echo "</div>";

    echo "<p style='text-align:center; margin-top:30px;'><a href='#' onclick='window.location.reload()' style='background:#2196F3; color:white; padding:10px 20px; text-decoration:none; border-radius:5px;'>Thử Lại Nếu Cần</a></p>";

} catch (Exception $e) {
    echo "<p style='color:red;'>❌ LỖI HỆ THỐNG: " . $e->getMessage() . "</p>";
}

echo "</div></body>";
?>
