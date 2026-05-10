# BÁO CÁO PHÂN TÍCH LUỒNG XỬ LÝ HỆ THỐNG
## Sơ Đồ Tuần Tự (Sequence Diagram) — Các Chức Năng Nghiệp Vụ Cốt Lõi

> **Dự án:** Hệ thống Quản lý Nhà Hàng (Android - Jetpack Compose + Firebase)  
> **Mục đích:** Phân tích luồng tương tác giữa các thành phần cho 7 chức năng nghiệp vụ quan trọng nhất.

---

## 1. Đổi Mật Khẩu Qua OTP Email

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Người dùng
    participant App as 📱 Android App
    participant Repo as 🔐 AuthRepository
    participant FS as 🔥 Firestore DB
    participant Mail as 📧 Email Service

    User->>App: Nhập email, nhấn "Gửi mã OTP"
    App->>FS: Kiểm tra tài khoản tồn tại?
    FS-->>App: Tài khoản hợp lệ ✓

    App->>Repo: generateOTP(email)
    Note over Repo: Sinh 6 số ngẫu nhiên<br/>Hash SHA-256(otp)
    Repo->>FS: Lưu {otp_hash, expired_at: now+3min, attempts: 0}
    Repo->>Mail: Gửi email OTP tới người dùng
    Mail-->>User: 📧 Email chứa mã 6 số

    User->>App: Nhập mã OTP nhận được

    App->>Repo: verifyOTP(email, input)
    Repo->>FS: Đọc document otp_verifications/{email}

    alt OTP hết hạn (now > expired_at)
        Repo-->>App: ❌ Lỗi: Mã đã hết hạn
        App-->>User: Thông báo lỗi, cho gửi lại
    else Sai mã (attempts >= 5)
        Repo->>FS: set locked_until = now + 5min
        Repo-->>App: ❌ Tài khoản bị khóa 5 phút
        App-->>User: Hiển thị countdown
    else OTP hợp lệ ✓
        Repo-->>App: ✅ Xác minh thành công
        App-->>User: Mở form nhập mật khẩu mới
        User->>App: Nhập mật khẩu mới
        App->>Repo: updatePassword(newPassword)
        Repo->>Repo: FirebaseAuth.updatePassword()
        Repo->>FS: Xóa document OTP (dọn dẹp)
        Repo-->>App: ✅ Thành công
        App-->>User: Đổi mật khẩu thành công!
    end
```

### 1.1. Thành phần tham gia

| Thành phần | Vai trò |
|---|---|
| **Người dùng** | Nhập email, mã OTP, mật khẩu mới |
| **Android App** | Giao diện `ChangePasswordDialog`, xử lý input |
| **FirebaseAuthRepository** | Logic gửi OTP, xác minh, đổi mật khẩu |
| **Firestore Database** | Lưu `{otp_hash, expired_at, attempts, locked_until}` |
| **Email Service** | Gửi email chứa mã OTP 6 số |

### 1.2. Cơ chế bảo mật nổi bật
- **SHA-256 hashing**: không thể reverse-engineer mã OTP từ database
- **Rate limiting**: chờ 60 giây giữa các lần gửi lại; khóa 5 phút sau 5 lần sai liên tiếp
- **TTL 3 phút**: OTP tự vô hiệu hóa sau thời gian ngắn

---

## 2. Scan Menu Thực Đơn Bằng AI

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 👨‍💼 Admin
    participant App as 📱 Android App
    participant VM as 🧠 MenuScanViewModel
    participant Gemini as 🤖 Gemini 2.5 Flash
    participant Pexels as 🖼️ Pexels API
    participant Storage as ☁️ Firebase Storage
    participant FS as 🔥 Firestore DB

    Admin->>App: Nhấn "Scan Menu", chọn ảnh thực đơn giấy
    App->>VM: scanMenuImage(uri, products, categories)
    VM->>VM: Encode ảnh → base64

    VM->>Gemini: POST ảnh + prompt phân tích thực đơn
    Note over Gemini: Phân tích từng dòng thực đơn<br/>Nhận dạng tên, giá, danh mục<br/>Dịch tên sang tiếng Anh (keyword)
    Gemini-->>VM: JSON [{name, price, category, description, recipe[], englishKeyword}]

    VM->>VM: So sánh với products hiện có → đánh dấu trùng tên
    loop Với mỗi món đã nhận dạng
        VM->>Pexels: GET /search?query={englishKeyword}
        Pexels-->>VM: URL ảnh minh họa
    end

    VM-->>App: Kết quả scan (list + trạng thái trùng/mới)
    App-->>Admin: Hiển thị MenuScanResultScreen<br/>🟠 Highlight giá=0 | ⚠️ Cảnh báo trùng

    Admin->>App: Xem xét, sửa giá/tên, bỏ tick món không muốn
    Admin->>App: Nhấn "Xác nhận"

    loop Với mỗi món được chọn
        App->>Storage: Upload ảnh → nhận download URL
    end
    App->>FS: Batch write tất cả món vào collection "products"
    FS-->>App: ✅ Thành công
    App-->>Admin: Tự động reload thực đơn
```

### 2.1. Điểm kỹ thuật đặc biệt
- Gemini tự dịch tên món sang tiếng Anh để tìm kiếm ảnh Pexels phù hợp
- Phát hiện trùng tên tránh thêm món bị duplicate vào menu
- **Batch write** đảm bảo tính nhất quán (all-or-nothing cho từng lần xác nhận)

---

## 3. Scan Hóa Đơn Nhập Kho Nguyên Liệu Bằng AI

```mermaid
sequenceDiagram
    autonumber
    actor Staff as 👨‍🍳 Bếp / Admin
    participant App as 📱 Android App
    participant VM as 🧠 IngredientScanViewModel
    participant Gemini as 🤖 Gemini AI API
    participant FS as 🔥 Firestore DB

    Staff->>App: Chụp / chọn ảnh hóa đơn nhập kho
    App->>VM: scanIngredientImage(uri, currentIngredients)
    VM->>VM: Encode ảnh base64

    VM->>Gemini: POST ảnh + prompt nhận dạng hóa đơn
    Note over Gemini: Phân tích từng dòng hóa đơn<br/>Nhận dạng tên, số lượng, đơn vị
    Gemini-->>VM: JSON [{name, quantity, unit}]

    VM->>VM: So sánh tên với kho hiện tại (case-insensitive)
    Note over VM: ✅ Không trùng → nguyên liệu mới (xanh)<br/>🟠 Trùng tên → sẽ cộng thêm (cam)

    VM-->>App: Kết quả phân loại (mới / trùng)
    App-->>Staff: Hiển thị IngredientScanResultScreen<br/>với checkbox và màu phân loại

    Staff->>App: Chỉnh sửa số lượng nếu cần
    Staff->>App: Bỏ tick những dòng không muốn nhập
    Staff->>App: Nhấn "Xác nhận nhập kho"

    loop Với mỗi mục được chọn
        alt Nguyên liệu mới
            App->>FS: addDoc("ingredients", {name, unit, stock: quantity})
        else Nguyên liệu đã có (trùng tên)
            App->>FS: updateDoc(id, {stock: FieldValue.increment(quantity)})
            Note over FS: FieldValue.increment → atomic,<br/>không cần đọc-sửa-ghi thủ công
        end
    end
    FS-->>App: ✅ Ghi thành công
    App->>FS: Fetch lại danh sách nguyên liệu
    App-->>Staff: Hiển thị tồn kho đã cập nhật
```

### 3.1. Điểm kỹ thuật đặc biệt
- `FieldValue.increment()` đảm bảo cập nhật nguyên liệu hiện có là **atomic** (tránh lost update khi đồng thời)
- Người dùng có thể điều chỉnh số lượng trước khi nhập → độ linh hoạt cao

---

## 4. Thanh Toán Qua Cổng PayOS

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 👤 Khách hàng
    actor Staff as 👨‍💼 Nhân viên
    participant AppS as 📱 App Nhân viên
    participant VM as ⚙️ RestaurantViewModel
    participant PayOS as 💳 PayOS API
    participant FS as 🔥 Firestore DB
    participant AppC as 📱 App Khách hàng

    Customer->>AppC: Nhấn "Yêu cầu thanh toán"
    AppC->>FS: updateDoc(order, {payment_status: "requested"})
    FS-->>AppS: 🔔 Snapshot Listener báo yêu cầu mới
    AppS-->>Staff: Âm thanh + badge đỏ trên tab Thanh toán

    Staff->>AppS: Chọn đơn → nhấn "Thanh toán PayOS"
    AppS->>VM: createPayOSPayment(orderId, amount)
    VM->>PayOS: POST /v2/payment-requests {orderCode, amount, returnUrl}
    PayOS-->>VM: {checkoutUrl, qrCode, orderCode}

    VM->>FS: updateDoc(order, {payos_order_code: orderCode})
    FS-->>AppC: 🔔 Snapshot Listener → nhận payos_order_code

    AppC-->>Customer: Hiển thị nút "Thanh toán PayOS"
    Customer->>AppC: Nhấn nút → mở WebView checkoutUrl
    Customer->>PayOS: Quét QR / thanh toán trên trang PayOS

    par Webhook (chính)
        PayOS->>FS: Webhook callback → payment_status = "paid"
    and Polling (dự phòng)
        loop Mỗi 3 giây
            AppC->>PayOS: GET /v2/payment-requests/{orderCode}
            PayOS-->>AppC: Trạng thái giao dịch
        end
    end

    FS-->>AppC: payment_status = "paid" ✅
    AppC->>VM: stopPolling()
    VM->>FS: checkoutTable() → giải phóng bàn, cộng điểm
    AppC-->>Customer: 🎉 Thanh toán thành công!
```

### 4.1. Cơ chế dự phòng (Polling)
> Webhook phụ thuộc vào server → nếu bị trễ/thất bại, **polling mỗi 3 giây đóng vai trò fallback** đảm bảo trải nghiệm người dùng không bị gián đoạn.

---

## 5. AI Chatbot Tư Vấn Món Ăn

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Người dùng
    participant Screen as 💬 ChatbotScreen
    participant VM as 🧠 ChatViewModel
    participant Gemini as 🤖 Gemini 2.5 Flash
    participant FS as 🔥 Firestore DB

    User->>Screen: Nhấn FAB logo → mở Chatbot
    Screen->>VM: init()
    VM->>FS: Load lịch sử chat ai_chat_history/{uid}
    FS-->>VM: Danh sách tin nhắn cũ
    VM-->>Screen: Hiển thị lịch sử chat

    User->>Screen: Gõ câu hỏi (VD: "Gợi ý món không cay dưới 100k")
    Screen->>VM: sendMessage(userText)

    VM->>VM: Lấy products từ StateFlow (cache sẵn, không fetch lại)
    VM->>VM: Build system prompt động:<br/>"Bạn là trợ lý nhà hàng...<br/>Menu hiện có: [Phở Bò-65k, Cơm Sườn-55k, ...]"

    VM->>Gemini: POST [systemPrompt + history + userMessage] (streaming)
    Note over Gemini: Phân tích yêu cầu<br/>Đối chiếu với menu thực tế<br/>Gợi ý món phù hợp

    loop Streaming response
        Gemini-->>VM: Chunk text (từng phần)
        VM-->>Screen: Cập nhật UI realtime (hiển thị từng chữ)
    end

    alt AI gợi ý món cụ thể
        VM->>VM: Parse JSON [{productId, name, price, reason}]
        VM-->>Screen: Render card món ăn (ảnh, tên, giá, nút "Thêm vào giỏ")
    end

    VM->>FS: Lưu {user_message, ai_response} vào Firestore
    
    opt Người dùng nhấn "Thêm vào giỏ"
        Screen->>Screen: Navigate → CartScreen với món đã chọn
    end
```

### 5.1. Điểm kỹ thuật đặc biệt
- **System prompt dynamic**: AI được nạp toàn bộ menu thực tế → tư vấn chính xác giá và tên
- **Streaming response**: trải nghiệm tự nhiên, không phải đợi AI "nghĩ xong" mới hiện kết quả
- **Persistent history**: giữ ngữ cảnh cuộc trò chuyện qua Firestore, kể cả sau khi tắt app

---

## 6. Đặt Bàn Trước

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 👤 Khách hàng
    participant Screen as 📋 ReservationScreen
    participant VM as ⚙️ ReservationViewModel
    participant FS as 🔥 Firestore DB
    actor Staff as 👨‍💼 Admin / Nhân viên

    Customer->>Screen: Mở màn hình "Đặt bàn trước"
    Screen->>VM: init(token)
    VM->>FS: Query reservations có status=pending/confirmed của user
    FS-->>VM: Danh sách lịch đặt hiện tại

    alt Đang có lịch chưa dùng (pending/confirmed)
        VM-->>Screen: ❌ Khóa form
        Screen-->>Customer: "Bạn đang có lịch đặt chưa hoàn thành.<br/>Vui lòng hủy trước khi đặt mới."
    else Không có lịch pending
        Screen-->>Customer: Hiển thị form đặt bàn

        Note over Customer,Screen: Tên + SĐT tự điền từ profile Firestore
        Customer->>Screen: Chọn ngày (DatePicker, mặc định: ngày mai)
        Customer->>Screen: Chọn giờ (TimePicker, mặc định: 18:00)
        Customer->>Screen: Nhập số người (stepper +/-, tối đa 20)
        Customer->>Screen: Nhập ghi chú (tùy chọn: sinh nhật, dị ứng...)
        Customer->>Screen: Nhấn "Xác nhận đặt bàn"

        Screen->>VM: Validation (tên, SĐT, ngày bắt buộc)
        alt Thiếu thông tin bắt buộc
            VM-->>Screen: ❌ Thông báo lỗi validation
        else Hợp lệ
            VM->>FS: addDoc("reservations", {user_id, name, phone, date, time, guest_count, note, status: "pending"})
            FS-->>VM: ✅ Document ID
            VM-->>Screen: Thành công → navigate back

            FS-->>Staff: 🔔 Snapshot Listener báo lịch mới
            Staff->>FS: Xem ReservationManagementScreen

            alt Duyệt lịch
                Staff->>FS: updateDoc(status: "confirmed")
                FS-->>Customer: 🔔 Realtime update → "Đã xác nhận"
            else Từ chối
                Staff->>FS: updateDoc(status: "cancelled")
                FS-->>Customer: 🔔 Realtime update → "Đã bị hủy"
            end
        end
    end
```

---

## 7. Xử Lý Đơn Hàng End-to-End

```mermaid
sequenceDiagram
    autonumber
    actor CustStaff as 👤 Khách / Nhân viên
    participant App as 📱 Android App
    participant VM as ⚙️ RestaurantViewModel
    participant FS as 🔥 Firestore DB
    participant Kitchen as 👨‍🍳 Bếp App
    participant Stock as 📦 Kho Nguyên Liệu

    CustStaff->>App: Chọn bàn → duyệt thực đơn → thêm vào Cart
    CustStaff->>App: Nhấn "Gửi đơn lên bếp"
    App->>VM: submitOrder(tableId, cartItems)
    VM->>FS: addDoc("orders", {table_id, items_detail+recipe_snapshot,<br/>total_amount, order_status:"pending", payment_status:"unpaid"})
    Note over FS: recipe_snapshot: bản sao công thức<br/>tại thời điểm đặt → lịch sử bất biến

    FS-->>Kitchen: 🔔 Snapshot Listener → đơn mới xuất hiện
    Kitchen->>Kitchen: 🔊 playNewOrderSound()
    Kitchen-->>Kitchen: Badge vàng "CHỜ DUYỆT"

    Kitchen->>FS: updateDoc(order_status: "processing")
    Kitchen-->>Kitchen: Badge xanh dương "ĐANG NẤU"
    Note over Kitchen: Bếp chế biến món...

    Kitchen->>FS: updateDoc(order_status: "completed")

    FS->>VM: Trigger: order completed → deductIngredients()
    Note over VM,Stock: ── FIRESTORE TRANSACTION (Atomic) ──
    VM->>FS: runTransaction { <br/>  đọc recipe_snapshot từng món<br/>  tính tổng nguyên liệu cần<br/>  stock -= needed (tất cả trong 1 commit)<br/>}
    FS-->>Stock: Cập nhật tồn kho nguyên liệu ✅
    Note over FS: Nếu conflict → auto retry<br/>Đảm bảo tính nguyên tử

    FS-->>App: 🔔 Đơn completed → thông báo nhân viên
    CustStaff->>App: Chọn phương thức thanh toán

    alt Tiền mặt
        CustStaff->>VM: checkoutTable(tableId)
    else VNPAY / PayOS
        CustStaff->>VM: createPaymentQR(orderId, amount)
        Note over VM: Xem luồng chi tiết ở mục 4
    end

    VM->>FS: checkoutTable() → batch update:
    Note over VM,FS: payment_status = "paid"<br/>loyaltyPoints += total × 1%<br/>daily_revenue += total (FieldValue.increment)<br/>table.status = "available"

    FS-->>App: 🔔 Snapshot Listener → tất cả thiết bị cập nhật đồng thời
    App-->>CustStaff: 🎉 Thanh toán hoàn tất, bàn đã trống
```

### 7.1. Cơ chế kỹ thuật then chốt

| Cơ chế | Mục đích |
|---|---|
| `recipe_snapshot` nhúng vào Order | Bảo toàn lịch sử — công thức thay đổi không ảnh hưởng đơn cũ |
| Firestore Transaction (trừ kho) | Tránh race condition khi nhiều đơn hoàn thành cùng lúc |
| `FieldValue.increment` | Cập nhật doanh thu/điểm atomic — không cần đọc-sửa-ghi thủ công |
| Snapshot Listener | Realtime sync < 1 giây trên mọi thiết bị, không cần polling |
| StateFlow SSOT | Cache products dùng chung toàn app, giảm số lần đọc Firestore |

---

## 8. Tổng Quan Kiến Trúc Hệ Thống

```mermaid
graph TB
    subgraph App["📱 Android App"]
        UI["Screen (UI)<br/>Compose"]
        VM["ViewModel<br/>StateFlow SSOT"]
        Repo["Repository<br/>Firebase SDK"]
        UI --> VM --> Repo
    end

    subgraph Firebase["🔥 Firebase Platform"]
        Auth["Firebase Auth<br/>Đăng nhập / OTP"]
        Firestore["Firestore DB<br/>Realtime SSOT"]
        Storage["Firebase Storage<br/>Ảnh / Avatar"]
        FCM["FCM<br/>Push Notification"]
    end

    subgraph External["🌐 API Bên Ngoài"]
        Gemini["🤖 Gemini 2.5 Flash<br/>Chatbot / Scan AI"]
        PayOS["💳 PayOS API<br/>Thanh toán QR"]
        VNPAY["💳 VNPAY API<br/>Thanh toán QR"]
        Pexels["🖼️ Pexels API<br/>Ảnh món ăn"]
    end

    Repo --> Auth
    Repo --> Firestore
    Repo --> Storage
    Repo --> FCM
    Repo --> Gemini
    Repo --> PayOS
    Repo --> VNPAY
    Repo --> Pexels

    Firestore -->|"Snapshot Listener<br/>< 1 giây"| VM

    style App fill:#FFF3E0,stroke:#E65100
    style Firebase fill:#FFF8E1,stroke:#F57F17
    style External fill:#E8F5E9,stroke:#2E7D32
```

---

*Báo cáo phân tích luồng xử lý — Hệ thống Quản lý Nhà Hàng*
