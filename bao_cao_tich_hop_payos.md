# BÁO CÁO PHÂN TÍCH VÀ HƯỚNG DẪN THIẾT LẬP PAYOS

## 1. Mục đích và Quy trình Tổng quan
PayOS là một giải pháp thanh toán qua mã QR động (VietQR). Trong kiến trúc Serverless (sử dụng Firebase làm Database) của dự án này, quy trình tích hợp PayOS được thiết kế thông qua **Cloudflare Worker** để bảo mật API Key và dùng cơ chế **Polling (hỏi vòng)** trên thiết bị di động thay cho Webhook truyền thống.

Quy trình thiết lập gồm 2 phần chính:
1. Thiết lập tài khoản trên giao diện Web PayOS.
2. Tích hợp mã nguồn: Triển khai proxy trên Cloudflare Worker và lập trình logic trên Android App.

---

## 2. Hướng dẫn thiết lập trên Web PayOS

### Bước 2.1. Đăng ký và Xác thực tài khoản
1. Truy cập [https://my.payos.vn](https://my.payos.vn) và đăng ký tài khoản mới.
2. Xác thực địa chỉ email và hoàn thành các bước định danh cá nhân/doanh nghiệp (eKYC) theo yêu cầu của hệ thống để mở khóa tính năng.

### Bước 2.2. Thêm Tài khoản Ngân hàng
1. Từ menu chính, điều hướng đến mục **Tài khoản ngân hàng**.
2. Chọn "Thêm tài khoản", nhập đúng số tài khoản và chọn ngân hàng thụ hưởng.
3. PayOS sẽ tự động chuyển một khoản tiền nhỏ (vài nghìn đồng) vào tài khoản của bạn. Mở app ngân hàng, sao chép mã xác thực trong nội dung chuyển khoản và điền lên web PayOS để hoàn tất liên kết.

### Bước 2.3. Tạo Kênh thanh toán / Dự án (Project)
1. Về trang chủ, tạo một **Kênh thanh toán** mới.
2. Chọn tài khoản ngân hàng đã được xác nhận ở Bước 2.2 để gắn vào kênh thanh toán này. Mọi khoản thu từ dự án này sẽ chảy thẳng về tài khoản đó.

### Bước 2.4. Lấy API Keys
1. Truy cập vào mục **Cài đặt** của kênh thanh toán vừa tạo.
2. Sao chép 3 tham số mật: `Client ID`, `API Key`, và `Checksum Key`.
*(Lưu ý: Bỏ qua phần cấu hình Webhook URL trên web, vì kiến trúc của hệ thống này sẽ sử dụng cơ chế Polling).*

---

## 3. Hướng dẫn Tích hợp Code vào Hệ thống

Thay vì dựng Backend Node.js phức tạp, dự án sử dụng mô hình tối ưu chi phí: **Android App <-> Cloudflare Worker <-> PayOS**.

### Bước 3.1. Viết và triển khai Cloudflare Worker (Proxy Server)
Mục đích của bước này là đưa 3 Key bảo mật lên môi trường Cloudflare, tuyệt đối không hardcode trong app Android để tránh bị hacker dịch ngược file APK.

1. Đăng nhập vào Cloudflare Dashboard, vào mục **Workers & Pages** -> Tạo một Worker mới.
2. Trong phần Settings (Cài đặt) của Worker, cấu hình các **Biến môi trường (Environment Variables)**:
   - `PAYOS_CLIENT_ID`: (Dán Client ID vào đây)
   - `PAYOS_API_KEY`: (Dán API Key vào đây)
   - `PAYOS_CHECKSUM_KEY`: (Dán Checksum Key vào đây)
3. Lập trình mã nguồn JavaScript cho Worker xử lý 2 endpoint chính:
   - **`POST /create`**: Nhận `order_id` và `amount` từ Android gửi lên. Sử dụng Checksum Key để sinh chữ ký HMAC-SHA256 hợp lệ. Gọi API của PayOS để tạo đơn, lấy `checkoutUrl` và `orderCode` trả ngược về cho Android.
   - **`GET /status?order_code=...`**: Nhận `order_code` từ Android, gọi sang API của PayOS để kiểm tra xem khách hàng đã chuyển tiền chưa (trả về JSON `{ "paid": true }` nếu đã thanh toán).
4. Bấm **Deploy** để public Worker. Bạn sẽ nhận được một URL (Ví dụ: `https://payos-payment.vmc0886165119.workers.dev/`).

### Bước 3.2. Cấu hình kết nối API trên Android (Dùng Retrofit)
Trên ứng dụng Android (Kotlin), tiến hành cài đặt kết nối đến đường dẫn của Cloudflare Worker.

1. **Khai báo Model dữ liệu** (trong file `PayOSWorkerService.kt`):
   ```kotlin
   data class PayOSCreateRequest(val order_id: Int, val amount: Long)
   data class PayOSCreateResponse(val checkout_url: String?, val order_code: Long?)
   data class PayOSStatusResponse(val paid: Boolean)
   ```
2. **Khai báo Retrofit Interface:**
   ```kotlin
   interface PayOSWorkerService {
       @POST("create")
       suspend fun createPayment(@Body request: PayOSCreateRequest): Response<PayOSCreateResponse>
       
       @GET("status")
       suspend fun checkStatus(@Query("order_code") orderCode: Long): Response<PayOSStatusResponse>
   }
   ```
3. **Tạo Singleton Client** (trong `PayOSWorkerClient.kt`), thiết lập `BASE_URL` trỏ vào đường dẫn Cloudflare Worker vừa lấy ở Bước 3.1.

### Bước 3.3. Gọi API Tạo Link Thanh toán và Sinh QR
Thực hiện trong logic của `RestaurantRepository.kt` và ViewModel khi khách hàng bấm thanh toán:

1. Lấy tổng tiền thanh toán (`total_amount`) của đơn hàng từ Firebase Firestore.
2. Dùng Retrofit gọi API `createPayment` lên Cloudflare Worker.
3. Khi nhận được kết quả (chứa `checkout_url` và `order_code`), lập tức **lưu trữ `order_code` vào Firestore** để theo dõi:
   ```kotlin
   firestore.collection("orders").document(orderId.toString())
       .update("payos_order_code", code).await()
   ```
4. Trên giao diện UI (`CustomerDashboardScreen`), mở `checkout_url` ra (có thể dùng Intent bật trình duyệt hoặc WebView) để khách hàng thấy mã VietQR và tiến hành quét app ngân hàng.

### Bước 3.4. Cấu hình cơ chế Polling để chốt đơn (Thay thế Webhook)
Ngay lúc màn hình QR hiện lên, App cần liên tục kiểm tra xem khách đã chuyển khoản hay chưa. 

1. **Viết hàm Polling ngầm (Vòng lặp delay):**
   ```kotlin
   suspend fun pollPayOSStatus(orderId: Int, orderCode: Long, amount: Double, onPaid: () -> Unit) {
       val startTime = System.currentTimeMillis()
       val timeoutMs = 300000L // Timeout tự huỷ sau 5 phút nếu khách không chuyển
       
       while (System.currentTimeMillis() - startTime < timeoutMs) {
           // 1. Gọi GET /status lên Worker
           val resp = PayOSWorkerClient.instance.checkStatus(orderCode)
           
           // 2. Nếu PayOS xác nhận đã nhận tiền (paid == true)
           if (resp.isSuccessful && resp.body()?.paid == true) {
               
               // 3. App TỰ cập nhật Firestore thành công
               firestore.collection("orders").document(orderId.toString())
                   .update("payment_status", "paid", "order_status", "completed").await()
               
               // 4. Xử lý các logic nghiệp vụ liên quan:
               // - Chuyển trạng thái bàn thành "Trống" (available)
               // - Cộng điểm Loyalty cho user
               // - Cộng dồn doanh thu vào Daily Revenue
               
               onPaid() // Gọi callback để đóng UI quét mã
               return
           }
           
           // 5. Nếu chưa thanh toán, nghỉ 3 giây rồi hỏi lại (Polling)
           delay(3000L) 
       }
   }
   ```
2. **Kích hoạt ngầm:** Khi màn hình QR vừa mở, gọi hàm `pollPayOSStatus` bên trong một Coroutine (`viewModelScope.launch`). 
3. **Hoàn tất:** Khi hàm gọi callback `onPaid()`, giao diện tự động đóng trang web quét mã, chuyển hướng về lịch sử đơn hàng và hiển thị dòng chữ "Thanh toán PayOS thành công".

---

## 4. Đánh giá Kiến trúc Hệ thống

- **Ưu điểm vượt trội:** 
  - Tối ưu 100% chi phí. Gói miễn phí của Cloudflare Worker dư sức cân tải cho nhà hàng mà không cần phải thuê server vật lý/VPS để làm backend hứng Webhook.
  - Phù hợp hoàn toàn với triết lý Serverless của Firebase.
- **Vấn đề cần lưu ý:** 
  - Vì sử dụng Polling chạy trên điện thoại khách hàng, nên nếu khách hàng tắt ngóm App đột ngột ngay khi vừa chuyển khoản xong (hoặc điện thoại mất mạng), vòng lặp Polling sẽ bị ngắt. Mặc dù tài khoản ngân hàng của nhà hàng đã nhận tiền, nhưng dữ liệu trên Firebase vẫn chưa kịp đổi thành `"paid"`. 
  - **Hướng khắc phục trên App:** Nên cho phép Admin hoặc Nhân viên có nút bấm "Kiểm tra lại giao dịch PayOS" trên đơn hàng để họ tự gọi lại API `/status` và chốt thủ công khi phát hiện khách đã rời đi mà mạng bị lỗi.
