import os
import re

replacements = {
    r'T\?ng b\?n': 'Tổng bàn',
    r'Tr\?ng': 'Trống',
    r'\?ang d\?ng': 'Đang dùng',
    r'T\?ng quan': 'Tổng quan',
    r'Qu\?n l\? b\?n': 'Quản lý bàn',
    r'Th\?c don': 'Thực đơn',
    r'Th\?ng k\?': 'Thống kê',
    r'\? Admin Dashboard': '⚙ Admin Dashboard',
    r'H\?y b\?': 'Hủy bỏ',
    r'H\?y': 'Hủy',
    r'C\?p nh\?t': 'Cập nhật',
    r'Th\?m m\?n': 'Thêm món',
    r'X\?c nh\?n x\?a': 'Xác nhận xóa',
    r'Chi ti\?t h\?a don': 'Chi tiết hóa đơn',
    r'T\?ng ti\?n': 'Tổng tiền',
    r'S\? lu\?ng': 'Số lượng',
    r'Th\?nh ti\?n': 'Thành tiền',
    r'X\?c Nh\?n Thanh To\?n': 'Xác Nhận Thanh Toán',
    r'\?\?ng b\? d\? li\?u t\? h\?a don': 'Đồng bộ dữ liệu từ hóa đơn',
    r'Chua c\? l\?ch s\? doanh thu': 'Chưa có lịch sử doanh thu',
    r'D\? li\?u s\? xu\?t hi\?n sau khi d\?ng b\? ho\?c c\? don thanh to\?n': 'Dữ liệu sẽ xuất hiện sau khi đồng bộ hoặc có đơn thanh toán',
    r'\?\?ng b\?': 'Đồng bộ',
    r'L\?ch s\? doanh thu': 'Lịch sử doanh thu',
    r'th\?t b\?i': 'thất bại',
    r'Doanh thu h\?m nay': 'Doanh thu hôm nay',
    r'\?on h\?ng d\? thanh to\?n': 'Đơn hàng đã thanh toán',
    r'T\?ng s\? ti\?n': 'Tổng số tiền',
    r'Danh m\?c': 'Danh mục',
    r'VD: Th\?t b\?, h\?nh t\?y, gia v\?': 'VD: Thịt bò, hành tây, gia vị',
    r'Th\?nh ph\?n m\?n an': 'Thành phần món ăn',
    r'Gi\? ti\?n': 'Giá tiền',
    r'VD: Ph\? b\?': 'VD: Phở bò',
    r'Upload \?nh m\?i n\?u ngu\?i d\?ng d\? ch\?n': 'Upload ảnh mới nếu người dùng đã chọn',
    r'X\?c nh\?n thanh to\?n h\?a don': 'Xác nhận thanh toán hóa đơn',
    r'kh\?ng th\? ho\?n t\?c': 'không thể hoàn tác',
    r'B\?n c\? ch\?c mu\?n x\?a': 'Bạn có chắc muốn xóa',
    r'Kh\?ng c\? don gi\? t\?ng m\?n': 'Không có đơn giá từng món',
    r'hi\?n th\? tr\?ng ho\?c t\?nh b\?nh qu\?n': 'hiển thị trống hoặc tính bình quân',
    r'N\?t thanh to\?n - ch\? hi\?n th\? khi chua thanh to\?n': 'Nút thanh toán - chỉ hiển thị khi chưa thanh toán',
    r'Nh\?n ng\?y \(ch\? l\?y 2 s\? cu\?i c\?a ng\?y\)': 'Nhãn ngày (chỉ lấy 2 số cuối của ngày)',
    r'Thanh bi\?u d\?': 'Thanh biểu đồ',
    r'Doanh thu th\?ng': 'Doanh thu tháng',
    r'T\?ng h\?a don h\?m nay \(k\? c\? chua thanh to\?n\)': 'Tổng hóa đơn hôm nay (kể cả chưa thanh toán)',
    r't\?ng h\?p t\? history \(ch\?nh x\?c hon l\? filter orders\)': 'tổng hợp từ history (chính xác hơn là filter orders)',
    r'Tr\?ng': 'Trống',
    
    # customer dashboard
    r'B\?n mu\?n an g\? h\?m nay\?': 'Bạn muốn ăn gì hôm nay?',
    r'Kh\?ch h\?ng': 'Khách hàng',
    r'B\?n c\?a t\?i & Th\?ng b\?o': 'Bàn của tôi & Thông báo',
    r'Chua c\? \?`\?n \?n n\?o \?`ang \?`\?c n\?u': 'Chưa có đồ ăn nào đang được nấu',
    r'Mang V\?': 'Mang Về',
    r'Mang v\?': 'Mang về',
    r'BA\?n c\?a tA\?i & ThA\?ng bA\?o': 'Bàn của tôi & Thông báo',
    
    # kitchen
    r'\?ang ch\? bi\?n m\?n an': 'đang chế biến món ăn',
    r'B\?p \?`A\? nh\?-n, \?`ang ch\? bi\?n mA3n \?n!': 'Bếp đã nhận, đang chế biến món ăn!',
    r'MA3n \?n \?`A\? s\?n sA\?ng\. Xin m\??i dA1ng!': 'Món ăn đã sẵn sàng. Xin mời dùng!',
    r'\?`\?i B\?p xA\?c nh\?-n': 'Đợi Bếp xác nhận',

    # other AdminDashboardScreen corrupted texts I can see
    r'T\?ng h\?a don': 'Tổng hóa đơn',
    r'Bi\?u d\? 7 ng\?y g\?n nh\?t': 'Biểu đồ 7 ngày gần nhất',
    r'Ti\?u d\? l\?ch s\?': 'Tiêu đề lịch sử',
    r'L\?ch s\? t\?ng ng\?y': 'Lịch sử từng ngày',
    r'X\?c nh\?n': 'Xác nhận',
    r'Ti\?u d\? b\?ng': 'Tiêu đề bảng',
    
    # Special character fallback fixes
    r'\? Admin': '⚙ Admin',
    r'b\?n': 'bàn',
}

file_paths = [
    r'd:\prj\app\src\main\java\com\example\restaurant\ui\screens\AdminDashboardScreen.kt',
    r'd:\prj\app\src\main\java\com\example\restaurant\ui\screens\TableMapScreen.kt',
    r'd:\prj\app\src\main\java\com\example\restaurant\ui\screens\KitchenDashboardScreen.kt',
    r'd:\prj\app\src\main\java\com\example\restaurant\ui\screens\CustomerDashboardScreen.kt',
    r'd:\prj\app\src\main\java\com\example\restaurant\ui\screens\CartScreen.kt'
]

for fp in file_paths:
    if os.path.exists(fp):
        with open(fp, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        for p, r in replacements.items():
            content = re.sub(p, r, content)
            
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(content)

print('Auto-replaced text in Kotlin files.')
