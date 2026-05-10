# DANH SÁCH TOÀN BỘ CHỨC NĂNG ỨNG DỤNG QUẢN LÝ NHÀ HÀNG

> Được trích xuất trực tiếp từ source code. Phân loại từ nhỏ đến lớn theo từng vai trò.

---

## I. CHỨC NĂNG CHUNG (Mọi vai trò)

### 1. Xác thực & Tài khoản
- Hiển thị màn hình Splash (logo + kiểm tra trạng thái đăng nhập tự động)
- Đăng ký tài khoản mới (email, mật khẩu, họ tên, SĐT, địa chỉ)
- Đăng nhập bằng email & mật khẩu (Firebase Auth)
- Tự động điều hướng đến Dashboard đúng theo `role` sau đăng nhập
- Đăng xuất khỏi ứng dụng
- Yêu cầu gửi OTP về email để đổi mật khẩu
- Xác minh mã OTP (mã hóa SHA-256, hết hạn 3 phút)
- Chống spam OTP: chờ 60s mới gửi lại, khóa 5 phút sau 5 lần nhập sai
- Đổi mật khẩu mới sau xác minh OTP thành công
- Chỉnh sửa thông tin hồ sơ cá nhân (họ tên, SĐT, địa chỉ)
- Tải lên ảnh đại diện (lưu Firebase Storage, hiển thị avatar tròn)
- Xem điểm tích lũy Loyalty hiện tại

### 2. Giao diện chung
- Navigation pill nổi (floating bottom nav) với animation spring khi chuyển tab
- Hiệu ứng premium background gradient toàn app
- Toast / Snackbar thông báo kết quả thao tác
- Floating Action Button mở AI Chatbot (nhân viên & khách hàng)
- Bật/tắt âm thanh thông báo toàn app (SoundManager toggle)

---

## II. KHÁCH HÀNG (CustomerDashboardScreen)

### Tab Home
- Hiển thị lời chào cá nhân hoá theo tên (lấy từ Firestore realtime)
- Hiển thị avatar tròn (fallback icon nếu chưa có ảnh)
- Hiển thị badge thông báo chưa đọc (số đỏ trên chuông)
- Tìm kiếm món ăn realtime theo tên (lọc ngay khi gõ)
- Carousel banner tự động cuộn mỗi 3.5s:
  - Hiển thị banner voucher cá nhân hoá (màu theo tier: đỏ/vàng/tím)
  - Hiển thị ảnh món ăn nổi bật từ menu
  - Dot indicator hiển thị vị trí trang hiện tại
- 4 nút hành động nhanh: Tại bàn / Đặt chỗ / Bàn của tôi / Mang về
- Chặn đặt bàn mới nếu đang có hóa đơn chưa thanh toán (dialog cảnh báo)
- Danh sách "Top Món Nổi Bật" (cuộn ngang, lọc `is_featured = true`)
- Nút "Xem thêm" mở danh sách đầy đủ món nổi bật
- Danh sách "Bán Chạy Tháng Này" (tính từ đơn hàng tháng hiện tại, ngưỡng ≥15 lượt)
- Xem chi tiết món ăn (ảnh, tên, giá, mô tả) qua dialog popup
- Thêm/bỏ yêu thích món ăn (lưu `liked_products` vào Firestore)

### Tab Thông báo (Đơn hàng của tôi)
- Xem đơn hàng hiện tại đang chờ xử lý (realtime Firestore listener)
- Xem trạng thái từng đơn: Chờ bếp / Đang nấu / Hoàn thành
- Gọi thêm món vào đơn hiện tại
- Xem tổng tiền hóa đơn hiện tại
- Yêu cầu thanh toán từ màn hình khách
- Áp dụng mã voucher khi thanh toán (kiểm tra hạn dùng, tier, giá trị tối thiểu)
- Áp dụng điểm tích lũy để giảm giá (quy đổi tỉ lệ)
- Hiển thị QR thanh toán VNPAY khi nhân viên tạo (tự động nhận qua Firestore)
- Hiển thị QR thanh toán PayOS khi nhân viên tạo
- Mở WebView trang thanh toán PayOS
- Polling kiểm tra trạng thái thanh toán PayOS
- Nhận thông báo khi đơn được thanh toán thành công

### Tab Giới thiệu
- Hiển thị bản đồ Google Maps vị trí nhà hàng (nhúng WebView)
- Tự động giải phóng WebView khi chuyển tab (tránh memory leak)

### Tab Cài đặt
- Chỉnh sửa thông tin cá nhân
- Tải lên ảnh đại diện
- Đổi mật khẩu qua OTP Email
- Xem lịch sử đơn hàng đã thanh toán
- Xem lịch sử đặt bàn
- Đăng xuất

### Chọn bàn & Đặt món
- Xem sơ đồ bàn (lưới 2 cột, màu trạng thái: xanh/đỏ/vàng)
- Chọn bàn trống để ngồi
- Xem thực đơn theo danh mục (tab ngang)
- Tìm kiếm món trong thực đơn
- Thêm món vào giỏ hàng (Cart - state nội bộ)
- Điều chỉnh số lượng từng món trong giỏ
- Xem tổng tiền giỏ hàng theo thời gian thực
- Gửi đơn lên bếp (tạo/cập nhật Order trên Firestore)
- Chọn "Mang về" (takeaway, không gắn bàn)
- Xem CartScreen đầy đủ với danh sách món, số lượng, tổng tiền

### Đặt bàn trước (ReservationScreen)
- Nhập tên người đặt, số điện thoại
- Chọn ngày đến bằng DatePicker
- Chọn giờ đến bằng TimePicker (mặc định ngày mai 18:00)
- Chọn số lượng khách (tăng/giảm, tối đa 20)
- Nhập ghi chú đặc biệt (dị ứng, sinh nhật, ghế trẻ em...)
- Xác nhận tạo đặt bàn (gửi lên Firestore)
- Khóa tính năng nếu đang có lịch chưa hoàn thành
- Xem lịch sử đặt bàn của bản thân
- Hủy lịch đặt bàn

### AI Chatbot (ChatbotScreen)
- Chat với trợ lý AI Gemini 2.5 Flash
- AI được nạp ngữ cảnh thực đơn thực tế (tên, giá, mô tả)
- AI tư vấn món phù hợp theo yêu cầu, sở thích, ngân sách
- Lưu toàn bộ lịch sử chat vào Firestore (`ai_chat_history`)
- Hiển thị danh sách món AI gợi ý kèm nút thêm vào giỏ nhanh

### Lịch sử đơn hàng (OrderHistoryScreen)
- Danh sách đơn đã thanh toán (lọc `payment_status = paid`)
- Sắp xếp mới nhất trước
- Xem chi tiết từng đơn: tên món, số lượng, giá, tổng tiền, ngày đặt

---

## III. NHÂN VIÊN PHỤC VỤ (TableMapScreen - Employee)

### Tab Sơ đồ bàn
- Xem tổng quan KPI: Trống / Đang dùng / Gọi PV / Yêu cầu TT (realtime)
- Hiển thị lưới bàn 2 cột với màu gradient theo trạng thái
- Badge nhấp nháy đỏ khi bàn đang gọi phục vụ (animation blink)
- Badge xanh dương khi bàn yêu cầu thanh toán
- Nhận âm thanh cảnh báo khi có bàn gọi phục vụ mới
- Nhận âm thanh khi có yêu cầu thanh toán mới
- Nhận Toast thông báo tức thì khi sự kiện mới xảy ra
- Bật/tắt âm thanh thông báo
- Refresh thủ công danh sách bàn và đơn hàng
- Nhấn vào bàn đang dùng → mở dialog thao tác:
  - Xem mã hóa đơn và tổng tiền
  - Gọi thêm món cho bàn
  - Tạo QR thanh toán VNPAY (gửi URL lên Firestore → khách nhận tự động)
  - Thanh toán tiền mặt (Checkout ngay)
  - Tắt chuông báo phục vụ sau khi ra bàn
- Nhấn vào bàn đang gọi phục vụ → mở dialog đặc biệt (tên bàn to, màu đỏ nổi bật)
- Xác nhận đã phục vụ xong (xóa cờ `needs_service`)
- Nhận thông báo + âm thanh khi đơn hàng được thanh toán thành công qua cloud
- Tự động giải phóng bàn khi phát hiện đơn đã paid (realtime Firestore)

### Tab Thanh toán (EmployeePaymentTab)
- Danh sách tất cả đơn đang chờ thanh toán (requested/unpaid/cash_requested)
- Xem chi tiết hóa đơn từng đơn
- Tạo QR VNPAY cho đơn cụ thể
- Duyệt thanh toán tiền mặt (Checkout)
- Badge số đỏ trên tab hiển thị số đơn chờ xử lý

### Tab Đặt bàn (ReservationManagement - tích hợp trong Employee)
- Xem danh sách đặt bàn đang chờ duyệt
- Xác nhận / Từ chối lịch đặt bàn

---

## IV. NHÂN VIÊN BẾP (KitchenDashboardScreen)

### Tab Đơn hàng
- Xem KPI realtime: Chờ / Đang nấu / Xong (counter tự cập nhật)
- Nghe âm thanh khi có đơn hàng mới vào (phân biệt đơn mới chưa xử lý)
- Bật/tắt âm thanh thông báo
- Danh sách đơn đang hoạt động (lọc bỏ completed/cancelled)
- Chuyển sang tab "Lịch sử" xem đơn đã hoàn thành
- Xem chi tiết từng đơn: mã đơn, tên bàn, danh sách món, số lượng, tổng tiền
- Badge trạng thái màu: vàng (CHỜ DUYỆT) / xanh dương (ĐANG NẤU) / xanh lá (ĐÃ XONG)
- Nhấn "Duyệt đơn" → chuyển `pending` → `processing`
- Nhấn "Hoàn thành" → chuyển `processing` → `completed`
- Tự động trừ kho nguyên liệu khi đơn completed (Firestore Transaction, nguyên tử)
  - Đọc `recipe_snapshot` từ mỗi món trong đơn
  - Tính tổng lượng từng nguyên liệu (có tính % hao hụt)
  - Trừ kho tất cả trong 1 transaction (tránh race condition)
  - Cảnh báo log khi kho âm
- Xóa tất cả đơn đã hoàn thành (có dialog xác nhận)

### Tab Nguyên liệu
- Xem danh sách toàn bộ nguyên liệu kho
- Tìm kiếm nguyên liệu theo tên (realtime filter)
- Cảnh báo đỏ (icon Warning + viền đỏ) khi `stock < 5`
- Thêm nguyên liệu mới vào kho (tên, đơn vị, số lượng)
- Sửa thông tin / số lượng tồn kho nguyên liệu (tên bị lock khi sửa)
- Xóa nguyên liệu khỏi kho (có dialog xác nhận)
- Scan ảnh hóa đơn nhập kho bằng AI (Gemini):
  - Trích xuất tên, số lượng, đơn vị tự động
  - Phát hiện nguyên liệu trùng tên
  - Màn hình xem kết quả, chọn/bỏ chọn từng mục
  - Xác nhận nhập hàng loạt vào Firestore

---

## V. ADMIN (AdminDashboardScreen)

### Tab Tổng quan (Home)
- Xem tóm tắt: Tổng bàn / Bàn trống / Bàn đang dùng (realtime)
- Lưới bàn 2 cột tổng quan toàn nhà hàng

### Tab Quản lý bàn
- Xem tổng số bàn + danh sách lưới 2 cột
- Màu trạng thái bàn (dải màu bên trái card)
- Thêm bàn mới (nhập tên, sức chứa)
- Sửa thông tin bàn (tên, sức chứa)
- Xóa bàn (có dialog xác nhận, cảnh báo không thể hoàn tác)

### Tab Thực đơn
- Xem thống kê: Tổng món / Tổng danh mục / Số món nổi bật
- Tìm kiếm món theo tên
- Lọc món theo danh mục
- Thêm món mới (tên, giá, danh mục, mô tả, ảnh, công thức nguyên liệu)
- Sửa thông tin món ăn
- Xóa món ăn (dialog xác nhận)
- Xóa toàn bộ thực đơn (cảnh báo CAPS LOCK nguy hiểm)
- Ẩn/hiện món ăn (`is_available` toggle, không cần xóa)
- Đánh dấu / bỏ đánh dấu món nổi bật (`is_featured`)
- Kiểm tra trạng thái tồn kho cho từng món (đủ nguyên liệu / thiếu)
- Scan ảnh thực đơn giấy bằng AI (Gemini):
  - AI trích xuất tên món, giá, danh mục, mô tả
  - AI gợi ý công thức nguyên liệu tự động
  - Phát hiện món trùng tên với menu hiện tại
  - Highlight cam món giá = 0 (AI không đọc được giá)
  - Fetch ảnh tự động từ Pexels qua proxy (từ keyword tiếng Anh AI dịch)
  - Màn hình xem/sửa kết quả, chọn/bỏ chọn từng món
  - Thêm hàng loạt vào Firestore
- Quản lý danh mục:
  - Thêm danh mục mới
  - Xóa danh mục

### Tab Thống kê (AdminStatsView)
- Biểu đồ cột doanh thu 30 ngày gần nhất (Canvas tự vẽ)
- Biểu đồ đường xu hướng
- KPI tổng: Tổng doanh thu / Số đơn / Doanh thu trung bình/đơn
- Lọc 7 ngày / 30 ngày
- Realtime observer: biểu đồ tự cập nhật khi có giao dịch mới
- Đồng bộ lại doanh thu từ toàn bộ đơn lịch sử (syncDailyRevenue)
- Xem danh sách hóa đơn hôm nay
- Xem chi tiết từng hóa đơn (AdminInvoiceDetailScreen):
  - Danh sách món, số lượng, giá
  - Tổng tiền, giảm giá, điểm đã dùng, mã voucher
  - Trạng thái thanh toán
  - Nút Duyệt thanh toán tiền mặt
  - Nút Checkout (giải phóng bàn, cộng điểm loyalty, cập nhật daily_revenue)
- Phân tích AI doanh thu (Gemini):
  - Gửi dữ liệu 30 ngày cho Gemini AI
  - Nhận báo cáo phân tích xu hướng, gợi ý kinh doanh bằng ngôn ngữ tự nhiên
  - Hiển thị kết quả dạng streaming text
- Xuất báo cáo CSV:
  - Dữ liệu doanh thu theo ngày
  - Mã hóa UTF-8 BOM (mở đúng tiếng Việt trên Excel)
  - Lưu vào bộ nhớ thiết bị + share dialog

### Tab Kho nguyên liệu (AdminIngredientInventory)
- Xem toàn bộ nguyên liệu (giống Kitchen nhưng từ góc Admin)
- Tìm kiếm nguyên liệu
- Thêm / Sửa / Xóa nguyên liệu
- Xóa toàn bộ nguyên liệu (có cảnh báo)
- Cảnh báo tồn kho thấp (highlight đỏ)
- Scan hóa đơn nhập kho bằng AI (chức năng giống Kitchen)

### Tab Khuyến mãi (VoucherManagementScreen)
- Xem tổng số voucher / số đang chạy
- Danh sách voucher với trạng thái: Đang chạy / Hết hạn / Hết lượt
- Tạo voucher mới:
  - Mã voucher (tự động uppercase, xóa khoảng trắng)
  - Loại giảm: Tiền cố định hoặc Phần trăm (%)
  - Mức giảm, giảm tối đa (chỉ cho % voucher)
  - Giá trị đơn tối thiểu
  - Giới hạn số lượt dùng
  - Phân hạng áp dụng: Tất cả / Hạng Vàng (≥1000đ) / Hạng Kim Cương (≥5000đ)
  - Ngày hết hạn (DatePicker)
- Sửa voucher hiện có
- Xóa voucher (dialog xác nhận)

### Tab Đặt bàn (ReservationManagementScreen)
- Xem danh sách tất cả đặt bàn (lọc theo trạng thái)
- Xác nhận lịch đặt bàn (pending → confirmed)
- Từ chối / Hủy lịch đặt bàn
- Xem thông tin chi tiết: tên, SĐT, ngày, giờ, số người, ghi chú

### Quản lý nhân viên (tích hợp trong Admin Home)
- Xem danh sách toàn bộ tài khoản trong hệ thống
- Tạo tài khoản nhân viên mới (email, mật khẩu, họ tên, vai trò)
  - Dùng Secondary FirebaseApp để không bị đăng xuất Admin hiện tại
- Thay đổi vai trò tài khoản (customer/employee/kitchen/admin)
- Sửa họ tên và vai trò
- Xóa tài khoản khỏi Firestore (document)

### Giao bàn cho khách
- Xem danh sách khách hàng đã đăng ký
- Chọn khách → chọn bàn trống → giao bàn
- Tự động tạo Order trống và chuyển trạng thái bàn sang `occupied`

---

## VI. HỆ THỐNG NỀN (Backend Logic)

### Realtime & Cache
- Firestore Snapshot Listener trên `orders`, `tables`, `products`, `ingredients`, `daily_revenue`
- SSOT ProductsCache (StateFlow) - toàn app dùng chung, không fetch lại
- Tự động cập nhật mọi thiết bị khi dữ liệu thay đổi (không cần refresh)

### Nghiệp vụ tự động
- Tự động trừ kho khi bếp hoàn thành đơn (Firestore Transaction)
- Tự động cộng điểm loyalty khi thanh toán (1% giá trị đơn)
- Tự động cập nhật `daily_revenue` khi checkout (FieldValue.increment nguyên tử)
- Tự động giải phóng bàn khi thanh toán xong
- Tự động phát âm thanh phân loại theo sự kiện (đơn mới / gọi PV / thanh toán)
- Tự động đóng dialog QR phía nhân viên khi khách thanh toán xong

### Bảo mật
- Firebase App Check (Play Integrity cho production, Debug token cho development)
- OTP SHA-256 hash (không lưu plaintext)
- Rate limiting OTP: 60s gửi lại, 5 lần sai → khóa 5 phút
- Security Rules Firestore (phân quyền đọc/ghi theo role)

### Thông báo
- Firebase Cloud Messaging (FCM) nhận push notification
- Notification khi có đơn hàng mới, yêu cầu thanh toán
- Lưu notification vào Firestore, đọc lại khi mở app

### Thanh toán
- Tích hợp cổng VNPAY (tạo URL QR, lắng nghe callback)
- Tích hợp cổng PayOS (tạo order code, polling trạng thái)
- Lưu `vnpay_qr_url` / `payos_order_code` vào Order
- Khách nhận URL tự động qua Firestore realtime, không cần nhân viên thao tác thêm

### Hình ảnh & Storage
- Upload ảnh đại diện người dùng lên Firebase Storage (`avatars/{uid}.jpg`)
- Upload ảnh món ăn lên Firebase Storage
- Cache ảnh local (Coil): 25% RAM + 100MB disk cache
- Tự động crossfade khi load ảnh

---

## VII. TỔNG KẾT SỐ LƯỢNG

| Nhóm | Số chức năng ước tính |
|---|---|
| Chung (Auth + UI) | ~15 |
| Khách hàng | ~35 |
| Nhân viên phục vụ | ~20 |
| Nhân viên bếp | ~18 |
| Admin | ~45 |
| Hệ thống nền | ~20 |
| **Tổng cộng** | **~153 chức năng** |
