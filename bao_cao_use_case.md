# CHƯƠNG II: PHÂN TÍCH YÊU CẦU — SƠ ĐỒ USE CASE

Chương này trình bày chi tiết các yêu cầu chức năng của hệ thống quản lý nhà hàng thông qua sơ đồ Use Case theo chuẩn UML, phân tách theo từng nhóm actor (tác nhân). Hệ thống phân quyền thành **4 vai trò** chính: Admin, Nhân viên bếp, Nhân viên phục vụ và Khách hàng, mỗi vai trò có màn hình và chức năng riêng biệt.

---

## 2.1. Tổng quan Phân quyền Hệ thống

| Vai trò | Trường `role` | Màn hình chính | Mô tả |
|---|---|---|---|
| **Admin** | `admin` | AdminDashboardScreen | Quản lý toàn bộ hệ thống |
| **Nhân viên bếp** | `kitchen` | KitchenDashboardScreen | Xử lý đơn hàng & quản lý kho |
| **Nhân viên phục vụ** | `employee` | EmployeeDashboardScreen | Phục vụ bàn & thanh toán |
| **Khách hàng** | `customer` | CustomerDashboardScreen | Đặt món & trải nghiệm |

Sau khi đăng nhập, ứng dụng đọc trường `role` từ Firestore collection `users` và điều hướng người dùng đến đúng màn hình Dashboard tương ứng.

---

## 2.2. Use Case — Admin (Quản lý)

![Biểu đồ Use Case Admin](C:\Users\Admin\.gemini\antigravity\brain\640ce5c8-b200-4329-8107-7bf715f3ae46\usecase_admin_1778245986546.png)

### 2.2.1. Danh sách Use Case

| Mã UC | Tên Use Case | Mô tả chi tiết |
|---|---|---|
| UC-A01 | Đăng nhập hệ thống | Admin đăng nhập bằng email/mật khẩu. Firebase Auth xác thực, Firestore kiểm tra `role = admin`. |
| UC-A02 | Quản lý tài khoản nhân viên | Xem danh sách toàn bộ người dùng, tạo tài khoản mới (dùng Secondary FirebaseApp để không bị đăng xuất), xóa tài khoản. |
| UC-A03 | Phân quyền vai trò | Thay đổi `role` của tài khoản (customer / employee / kitchen / admin) trực tiếp trên Firestore. |
| UC-A04 | Quản lý thực đơn | CRUD đầy đủ: Thêm, sửa, xóa món ăn. Ẩn/hiện món (`is_available`). Đánh dấu món nổi bật (`is_featured`). |
| UC-A05 | Scan thực đơn bằng AI | Chụp ảnh thực đơn giấy → Gemini AI phân tích, trích xuất tên món, giá, danh mục và tự động tạo recipe nguyên liệu. |
| UC-A06 | Quản lý danh mục | Thêm/xóa danh mục (Món chính, Đồ uống, Tráng miệng...). Danh mục liên kết với sản phẩm qua `category_id`. |
| UC-A07 | Quản lý bàn ăn | Thêm/sửa/xóa bàn. Xem trạng thái từng bàn theo realtime (available / occupied / reserved). |
| UC-A08 | Giao bàn cho khách | Chọn khách hàng từ danh sách → assign bàn → tự động tạo Order trống và chuyển bàn sang `occupied`. |
| UC-A09 | Quản lý kho nguyên liệu | Xem tồn kho tất cả nguyên liệu. Thêm/sửa/xóa từng mục. Cảnh báo khi `stock < min_quantity`. |
| UC-A10 | Xem thống kê doanh thu | Biểu đồ cột/đường 30 ngày gần nhất, tổng KPI (tổng doanh thu, số đơn, trung bình/đơn). |
| UC-A11 | Xuất báo cáo CSV | Xuất file `.csv` doanh thu theo ngày, mã hóa UTF-8 BOM để mở đúng trên Excel tiếng Việt. |
| UC-A12 | Phân tích AI doanh thu | Gửi dữ liệu doanh thu 30 ngày cho Gemini AI → nhận báo cáo phân tích xu hướng, gợi ý kinh doanh bằng ngôn ngữ tự nhiên. |
| UC-A13 | Duyệt yêu cầu thanh toán | Xem các đơn đang chờ thanh toán (`payment_status = requested`), phê duyệt hoặc từ chối. |
| UC-A14 | Checkout & Giải phóng bàn | Xác nhận thanh toán → cập nhật `payment_status = paid`, cộng điểm loyalty cho khách, cập nhật `daily_revenue`, giải phóng bàn. |
| UC-A15 | Quản lý mã voucher | Tạo voucher giảm giá (theo số tiền hoặc phần trăm), đặt điều kiện tối thiểu, hạn sử dụng, giới hạn lượt dùng. |

### 2.2.2. Luồng nghiệp vụ chính của Admin

```
Đăng nhập → AdminDashboard
    ├── Tab Thống kê: Biểu đồ KPI + AI phân tích + Xuất CSV
    ├── Tab Thực đơn: CRUD sản phẩm + Scan AI + Quản lý danh mục
    ├── Tab Bàn: Sơ đồ bàn realtime + Giao bàn cho khách
    ├── Tab Nhân viên: Danh sách user + Tạo/Phân quyền/Xóa
    ├── Tab Kho: Tồn kho nguyên liệu + Cảnh báo hết hàng
    └── Tab Đơn hàng: Duyệt thanh toán + Checkout bàn
```

---

## 2.3. Use Case — Nhân Viên Bếp (Kitchen)

![Biểu đồ Use Case Nhân Viên Bếp](C:\Users\Admin\.gemini\antigravity\brain\640ce5c8-b200-4329-8107-7bf715f3ae46\usecase_kitchen_1778246001177.png)

### 2.3.1. Danh sách Use Case

| Mã UC | Tên Use Case | Mô tả chi tiết |
|---|---|---|
| UC-K01 | Đăng nhập hệ thống | Đăng nhập bằng tài khoản `role = kitchen`. Chuyển thẳng đến KitchenDashboard. |
| UC-K02 | Xem danh sách đơn hàng realtime | Firestore Snapshot Listener cập nhật tức thì khi có đơn mới hoặc đơn cũ thay đổi trạng thái. Hiển thị riêng các đơn `pending` và `processing`. |
| UC-K03 | Xem chi tiết đơn hàng | Xem từng món trong đơn: tên món, số lượng, ghi chú. Xem tên bàn hoặc "Mang về". |
| UC-K04 | Duyệt đơn — Bắt đầu chế biến | Nhấn "Duyệt" → `order_status` chuyển từ `pending` sang `processing`. Toàn bộ thiết bị khác cập nhật realtime. |
| UC-K05 | Đánh dấu hoàn thành | Nhấn "Hoàn thành" → `order_status` chuyển sang `completed`. Hệ thống tự động kích hoạt **trừ kho** nguyên liệu theo công thức recipe. |
| UC-K06 | Trừ kho tự động | Khi đơn completed: đọc `recipe_snapshot` từ mỗi `OrderItemDetail`, tính tổng nguyên liệu cần trừ, chạy Firestore Transaction để trừ kho nguyên tử tránh race condition. |
| UC-K07 | Xem kho nguyên liệu | Danh sách nguyên liệu với số lượng tồn, đơn vị, mức tối thiểu. Highlight đỏ khi `stock < min_quantity`. |
| UC-K08 | Nhận cảnh báo tồn kho thấp | Observeflow `ingredients` realtime — khi bất kỳ nguyên liệu nào dưới ngưỡng, hiển thị cảnh báo trực quan ngay màn hình bếp. |
| UC-K09 | Thêm nguyên liệu mới | Nhập tên, đơn vị, số lượng, mức tối thiểu cảnh báo → lưu vào collection `ingredients`. |
| UC-K10 | Cập nhật số lượng tồn kho | Chỉnh sửa `stock` sau khi nhập hàng thực tế. |
| UC-K11 | Xóa nguyên liệu | Xóa mục nguyên liệu đã ngừng sử dụng. |
| UC-K12 | Scan hóa đơn nhập kho bằng AI | Chụp ảnh hóa đơn nhập hàng → Gemini AI trích xuất danh sách nguyên liệu, số lượng, đơn vị → nhân viên bếp xác nhận và cập nhật hàng loạt vào kho. |

### 2.3.2. Luồng xử lý đơn tại bếp

```
Đơn hàng mới tạo (order_status = "pending")
    ↓ [Nhân viên bếp nhấn "Duyệt"]
order_status = "processing"  (đang chế biến)
    ↓ [Nhân viên bếp nhấn "Hoàn thành"]
order_status = "completed"
    ↓ [Tự động]
Trừ kho nguyên liệu theo recipe_snapshot (Firestore Transaction)
```

---

## 2.4. Use Case — Nhân Viên Phục Vụ (Employee)

![Biểu đồ Use Case Nhân Viên Phục Vụ](C:\Users\Admin\.gemini\antigravity\brain\640ce5c8-b200-4329-8107-7bf715f3ae46\usecase_employee_1778246031637.png)

### 2.4.1. Danh sách Use Case

| Mã UC | Tên Use Case | Mô tả chi tiết |
|---|---|---|
| UC-E01 | Đăng nhập hệ thống | Đăng nhập bằng tài khoản `role = employee`. Chuyển đến EmployeeDashboard. |
| UC-E02 | Xem danh sách bàn ăn | Hiển thị sơ đồ bàn với màu trạng thái: xanh (available), đỏ (occupied), vàng (reserved). Cập nhật realtime. |
| UC-E03 | Giao bàn & nhận khách | Chọn bàn trống → chọn khách hàng đã đăng ký → hệ thống tạo Order trống và chuyển bàn sang `occupied`. |
| UC-E04 | Xem thực đơn & tìm kiếm | Duyệt thực đơn theo danh mục, tìm kiếm theo tên món, lọc theo `is_available`. |
| UC-E05 | Gọi món cho khách | Chọn món → thêm vào giỏ hàng (Cart) → nhập số lượng → gửi đơn lên bếp. |
| UC-E06 | Gửi đơn lên bếp | Tạo hoặc cập nhật Order trên Firestore → bếp nhận ngay qua Snapshot Listener. |
| UC-E07 | Xem trạng thái đơn realtime | Theo dõi `order_status` của từng bàn theo thời gian thực: pending / processing / completed. |
| UC-E08 | Yêu cầu thanh toán | Nhấn "Yêu cầu thanh toán" → `payment_status = requested`. Admin/màn hình thanh toán nhận được thông báo. |
| UC-E09 | Xử lý thanh toán tiền mặt | Nhận tiền mặt từ khách → xác nhận → `payment_status = cash_requested` → chờ Admin duyệt hoặc tự Checkout. |
| UC-E10 | Tạo QR thanh toán VNPAY | Tạo link thanh toán VNPAY → lưu `vnpay_qr_url` vào Order → màn hình khách hàng hiển thị QR tự động. |
| UC-E11 | Tạo QR thanh toán PayOS | Tương tự VNPAY, tích hợp PayOS làm cổng thanh toán thay thế. `payos_order_code` lưu mã để tracking. |
| UC-E12 | Checkout & Giải phóng bàn | Xác nhận tất cả đơn đã thanh toán → cập nhật `paid`, cộng điểm loyalty, cập nhật doanh thu ngày, đặt bàn về `available`. |
| UC-E13 | Báo cần hỗ trợ bàn | Bật cờ `needs_service = true` trên bàn để gọi hỗ trợ. Admin/nhân viên khác nhìn thấy icon cảnh báo trên sơ đồ bàn. |

### 2.4.2. Luồng phục vụ tiêu chuẩn

```
Khách vào → Giao bàn (tạo Order trống)
    ↓
Gọi món → Gửi đơn lên bếp
    ↓
Theo dõi trạng thái đơn (pending → processing → completed)
    ↓
Khách yêu cầu thanh toán → Chọn phương thức
    ├── Tiền mặt: cash_requested → Admin duyệt → paid
    └── QR Code: tạo link VNPAY/PayOS → khách quét → xác nhận → paid
    ↓
Checkout → Giải phóng bàn (available)
```

---

## 2.5. Use Case — Khách Hàng (Customer)

![Biểu đồ Use Case Khách Hàng](C:\Users\Admin\.gemini\antigravity\brain\640ce5c8-b200-4329-8107-7bf715f3ae46\usecase_customer_1778246048048.png)

### 2.5.1. Danh sách Use Case

| Mã UC | Tên Use Case | Mô tả chi tiết |
|---|---|---|
| UC-C01 | Đăng ký tài khoản | Nhập email, mật khẩu, họ tên, SĐT, địa chỉ. Firebase Auth tạo account, Firestore tạo document `users/{uid}` với `role = customer`. |
| UC-C02 | Đăng nhập | Xác thực bằng email/mật khẩu. Token Firebase Auth được sử dụng cho mọi thao tác sau. |
| UC-C03 | Xem thực đơn & tìm kiếm | Duyệt theo danh mục (tab ngang), tìm kiếm realtime theo tên. Chỉ hiển thị món `is_available = 1`. |
| UC-C04 | Xem chi tiết món ăn | Xem ảnh, mô tả, giá, đánh giá sao, bình luận của khách khác. Nút "Yêu thích" (toggle `liked_products`). |
| UC-C05 | Thêm món vào giỏ hàng | Chọn số lượng → thêm vào Cart state (lưu trong bộ nhớ, không cần Firestore). |
| UC-C06 | Đặt món | Từ giỏ hàng → xem tóm tắt đơn → xác nhận đặt. Hệ thống tạo/cập nhật Order trên Firestore. |
| UC-C07 | Chọn bàn / Mang về | Khi đặt món, chọn bàn đang ngồi hoặc chọn "Mang về" (`table_id = 0`, `order_type = takeaway`). |
| UC-C08 | Tư vấn AI Chatbot (Gemini) | Chat với trợ lý AI được nạp ngữ cảnh thực đơn thực tế. AI gợi ý món phù hợp sở thích, dietary, ngân sách. Lịch sử chat lưu vào `ai_chat_history`. |
| UC-C09 | Xem trạng thái đơn realtime | Màn hình hiển thị trạng thái đơn hiện tại: "Đang chờ bếp duyệt" / "Đang chế biến" / "Hoàn thành". |
| UC-C10 | Yêu cầu thanh toán | Nhấn "Thanh toán" → chọn phương thức: tiền mặt hoặc QR. Áp dụng voucher / điểm tích lũy trước khi xác nhận. |
| UC-C11 | Dùng mã voucher | Nhập mã voucher → hệ thống kiểm tra hạn dùng, giá trị đơn tối thiểu, tier khách hàng → tự động giảm giá. |
| UC-C12 | Dùng điểm loyalty | Quy đổi điểm tích lũy thành tiền giảm giá (ví dụ: 100 điểm = 10,000đ). Trừ `loyaltyPoints` trên Firestore. |
| UC-C13 | Quét QR VNPAY/PayOS | Khi nhân viên tạo link thanh toán, màn hình khách tự động nhận và hiển thị QR để quét qua app ngân hàng. |
| UC-C14 | Tích lũy điểm loyalty | Sau khi thanh toán thành công, hệ thống tự động cộng điểm (tỷ lệ 1% giá trị đơn) vào `loyaltyPoints`. |
| UC-C15 | Xem lịch sử đơn hàng | Danh sách các đơn đã hoàn thành (`payment_status = paid`), sắp xếp mới nhất trước. |
| UC-C16 | Chỉnh sửa hồ sơ cá nhân | Cập nhật họ tên, SĐT, địa chỉ. Tải lên ảnh đại diện (lưu vào Firebase Storage tại `avatars/{uid}.jpg`). |
| UC-C17 | Đổi mật khẩu qua OTP Email | Yêu cầu OTP → hệ thống gửi mã 6 số qua email (qua Cloudflare Worker) → nhập OTP → đặt mật khẩu mới. OTP mã hóa SHA-256, hết hạn sau 3 phút, khóa 5 phút sau 5 lần sai. |

### 2.5.2. Cơ chế Loyalty Points & Voucher

**Tích lũy điểm:**
- Mỗi đơn hàng thanh toán thành công → cộng điểm vào tài khoản khách.
- Điểm được lưu trực tiếp tại trường `loyaltyPoints` trong document `users/{uid}`.

**Sử dụng điểm và voucher:**
```
Tổng tiền đơn hàng
    - Giảm từ voucher (percent hoặc fixed, có max_discount)
    - Giảm từ điểm loyalty (points_used × tỉ lệ quy đổi)
    = Số tiền thực tế thanh toán
```

---

## 2.6. Bảng Tổng Hợp Phân Quyền Chức Năng

| Chức năng | Admin | Nhân viên bếp | Nhân viên phục vụ | Khách hàng |
|---|:---:|:---:|:---:|:---:|
| Đăng nhập / Đăng ký | ✅ | ✅ | ✅ | ✅ |
| Xem thực đơn | ✅ | — | ✅ | ✅ |
| Quản lý thực đơn (CRUD) | ✅ | — | — | — |
| Scan thực đơn AI | ✅ | — | — | — |
| Gọi món / Đặt hàng | — | — | ✅ | ✅ |
| Tư vấn AI Chatbot | — | — | — | ✅ |
| Duyệt đơn bếp | — | ✅ | — | — |
| Xem đơn realtime | ✅ | ✅ | ✅ | ✅ |
| Quản lý kho nguyên liệu | ✅ | ✅ | — | — |
| Scan hóa đơn nhập kho AI | — | ✅ | — | — |
| Quản lý bàn ăn | ✅ | — | ✅ | — |
| Thanh toán / Checkout | ✅ | — | ✅ | ✅ (yêu cầu) |
| Voucher & Loyalty Points | ✅ (quản lý) | — | — | ✅ (sử dụng) |
| Thống kê doanh thu | ✅ | — | — | — |
| Phân tích AI doanh thu | ✅ | — | — | — |
| Quản lý tài khoản nhân viên | ✅ | — | — | — |
| Chỉnh sửa hồ sơ cá nhân | ✅ | ✅ | ✅ | ✅ |
| Đổi mật khẩu OTP | ✅ | ✅ | ✅ | ✅ |

---

## 2.7. Kết luận

Hệ thống quản lý nhà hàng được thiết kế với **kiến trúc phân quyền chặt chẽ 4 cấp**, mỗi actor được trao đúng quyền hạn cần thiết để thực hiện công việc mà không thể can thiệp vào phạm vi của nhau. Cụ thể:

- **Admin** nắm quyền điều phối toàn hệ thống — từ cấu hình thực đơn, nhân sự đến phân tích kinh doanh bằng AI.
- **Nhân viên bếp** tập trung hoàn toàn vào luồng chế biến và quản lý nguyên liệu — được hỗ trợ bởi AI scan hóa đơn nhập kho và cơ chế tự động trừ kho khi hoàn thành đơn.
- **Nhân viên phục vụ** điều phối linh hoạt giữa sơ đồ bàn và quy trình thanh toán — hỗ trợ cả tiền mặt lẫn QR Code (VNPAY/PayOS).
- **Khách hàng** được trải nghiệm đặt món hiện đại với AI tư vấn (Gemini), hệ thống điểm tích lũy và thanh toán không tiếp xúc.

Toàn bộ luồng dữ liệu giữa 4 actor vận hành qua **Firebase Firestore Realtime Listener** — đảm bảo mọi thay đổi trạng thái (đơn hàng, bàn, kho) được phản ánh tức thì trên tất cả thiết bị mà không cần refresh thủ công.
