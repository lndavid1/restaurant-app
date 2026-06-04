# BÁO CÁO PHÂN TÍCH VÀ HƯỚNG DẪN TÍCH HỢP MÃ OTP ĐỔI MẬT KHẨU

## 1. Mục đích và Quy trình Tổng quan
Chức năng OTP (One-Time Password) được sử dụng để xác thực danh tính người dùng khi họ có nhu cầu đổi mật khẩu. Để đảm bảo tính bảo mật và tuân thủ kiến trúc Serverless của ứng dụng (dùng Firebase), quy trình OTP được thiết kế đặc thù với sự hỗ trợ của **Cloudflare Worker** (để gửi email) và cơ chế **Hashing (băm)** để không lộ mã OTP trên Database.

Quy trình tổng quan:
1. Người dùng yêu cầu đổi mật khẩu -> App sinh mã OTP ngẫu nhiên.
2. Mã OTP được băm (SHA-256) và lưu lên Firebase Firestore cùng với thời gian hết hạn.
3. App gửi mã OTP gốc (chưa băm) qua Cloudflare Worker để Worker gửi email tới người dùng.
4. Người dùng nhập mã từ Email vào App -> App băm mã nhập vào và so sánh với Firestore.
5. Nếu khớp, đổi mật khẩu qua Firebase Auth.

---

## 2. Kiến trúc Bảo mật của Hệ thống OTP

Hệ thống được thiết kế với các tiêu chuẩn bảo mật khắt khe:
- **Không lưu mã OTP dạng rõ (Plain-text):** Mã OTP lưu trên Firestore luôn được băm qua thuật toán **SHA-256**. Ngay cả Admin vào xem Database cũng không thể biết được mã OTP đang gửi cho khách là gì.
- **Bảo mật thông tin gửi Email (SMTP):** Không hardcode tài khoản Email/Mật khẩu SMTP vào trong code Android. Việc gửi thư được giao phó cho **Cloudflare Worker** làm proxy.
- **Chống Spam (Rate Limiting):** Chỉ cho phép gửi lại mã OTP sau ít nhất **60 giây**.
- **Chống Brute-force (Mò mã):** Nếu người dùng nhập sai quá **5 lần**, tài khoản (tính theo email đó) sẽ bị khóa chức năng OTP trong **5 phút**.
- **Thời hạn (TTL - Time to live):** Mỗi mã OTP chỉ có hiệu lực trong đúng **3 phút**.

---

## 3. Hướng dẫn Tích hợp Code vào Ứng dụng

### Bước 3.1. Thiết lập Cloudflare Worker (Dịch vụ gửi Email)
Tương tự như PayOS, việc gửi Email cần có một Proxy Server để giấu mật khẩu SMTP.

1. Đăng nhập Cloudflare Dashboard, tạo một Worker mới.
2. Cấu hình các biến môi trường (Environment Variables) trong Worker:
   - `SMTP_HOST`, `SMTP_PORT`
   - `SMTP_USER`, `SMTP_PASS` (Ví dụ tài khoản SendGrid hoặc Gmail App Password).
3. Viết mã nguồn cho Worker nhận Request `POST /send-otp` (chứa `email` và `otp_code`), sau đó dùng thư viện Node.js (ví dụ `nodemailer` hoặc gọi API Resend/SendGrid) để gửi thư đi.
4. Deploy Worker và lấy đường dẫn URL. Trên App Android, tạo Retrofit Interface (`CloudflareClient.kt`) để trỏ tới URL này.

### Bước 3.2. Sinh OTP và mã hóa (Băm SHA-256)
Tại Android App, sử dụng file `SecurityUtils.kt` để quản lý việc tạo và mã hoá:

1. **Hàm sinh mã ngẫu nhiên (6 chữ số):**
   ```kotlin
   fun generateOTP(): String {
       val otp = (100000..999999).random()
       return otp.toString()
   }
   ```
2. **Hàm băm SHA-256:**
   ```kotlin
   fun hashSHA256(input: String): String {
       val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
       return bytes.joinToString("") { "%02x".format(it) }
   }
   ```

### Bước 3.3. Xử lý logic yêu cầu Gửi OTP (`requestPasswordChangeOTP`)
Viết logic trong lớp `FirebaseAuthRepository.kt`:

1. **Kiểm tra Spam & Chặn Brute-force:**
   Đọc document `otp_recovery/{email}` từ Firestore để kiểm tra:
   - Khách có đang bị khoá (`lockUntil`) do nhập sai 5 lần không?
   - Khách có gửi OTP quá nhanh (`lastSentAt` < 60 giây) không?
2. **Tạo và lưu OTP lên Firestore:**
   ```kotlin
   val plainOtp = SecurityUtils.generateOTP()
   val hashedOtp = SecurityUtils.hashSHA256(plainOtp)
   val now = System.currentTimeMillis()

   val otpData = mapOf(
       "hashedOtp" to hashedOtp,
       "expiresAt" to now + 3 * 60 * 1000, // Hết hạn sau 3 phút
       "attempts" to 0,
       "lockUntil" to 0L,
       "lastSentAt" to now
   )
   firestore.collection("otp_recovery").document(email).set(otpData).await()
   ```
3. **Gọi Cloudflare Worker để gửi Email:**
   ```kotlin
   val mailResp = CloudflareClient.instance.sendEmailOTP(EmailOtpRequest(email, plainOtp))
   if (!mailResp.isSuccessful) {
       // Nếu gửi mail lỗi (VD: sai địa chỉ), rollback bằng cách xoá OTP trên Firestore
       firestore.collection("otp_recovery").document(email).delete().await()
   }
   ```

### Bước 3.4. Xác thực OTP và Đổi mật khẩu (`verifyOTPAndChangePassword`)
Khi người dùng nhập 6 số từ email vào App, ta thực hiện:

1. **Lấy dữ liệu từ Firestore:**
   ```kotlin
   val otpDoc = firestore.collection("otp_recovery").document(email).get().await()
   ```
2. **Kiểm tra tính hợp lệ:**
   - Document có tồn tại không?
   - Đã quá thời gian `lockUntil` chưa?
   - Thời điểm hiện tại có vượt quá `expiresAt` (3 phút) không?
3. **Kiểm tra mã băm:**
   ```kotlin
   val hashedOtp = otpDoc.getString("hashedOtp") ?: ""
   val inputHash = SecurityUtils.hashSHA256(inputOtp) // Băm mã khách nhập

   if (inputHash != hashedOtp) {
       // Xử lý khi sai OTP
       var attempts = otpDoc.getLong("attempts")?.toInt() ?: 0
       attempts++
       if (attempts >= 5) {
           // Khoá 5 phút
           otpRef.update(mapOf("attempts" to attempts, "lockUntil" to now + 5 * 60 * 1000)).await()
       } else {
           // Cập nhật số lần thử
           otpRef.update("attempts", attempts).await()
       }
       return Result.failure(Exception("Mã OTP không đúng!"))
   }
   ```
4. **Đổi mật khẩu nếu thành công:**
   ```kotlin
   otpRef.delete().await() // Xoá OTP đã dùng
   auth.currentUser?.updatePassword(newPass)?.await() // Cập nhật mật khẩu Firebase Auth
   ```

---

## 4. Đánh giá Ưu và Nhược điểm

- **Ưu điểm vượt trội:**
  - **Tuyệt đối an toàn:** Không ai (kể cả Admin có quyền vào Firebase) có thể biết được OTP của khách vì đã bị băm 1 chiều (SHA-256).
  - **Bảo mật SMTP:** Ứng dụng Mobile Android không chứa mật khẩu email, tránh bị decompile lấy cắp tài khoản hòm thư.
  - **Tự động phòng thủ:** Cơ chế chống Spam (60 giây) và chống Brute-force (Sai 5 lần khoá 5 phút) tự động chạy mà không cần Backend quản lý State.

- **Lưu ý / Nhược điểm:**
  - Vẫn yêu cầu người dùng phải đang có "phiên đăng nhập hiện tại" (`currentUser`) chưa hết hạn quá lâu (trong Firebase Auth) mới đổi được mật khẩu. Nếu phiên quá cũ, Firebase Auth sẽ ném lỗi `CREDENTIAL_TOO_OLD_LOGIN_AGAIN`, lúc này App sẽ phải thông báo người dùng đăng xuất và đăng nhập lại.
