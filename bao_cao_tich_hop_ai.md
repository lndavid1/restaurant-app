# BÁO CÁO PHÂN TÍCH VÀ HƯỚNG DẪN TÍCH HỢP TRÍ TUỆ NHÂN TẠO (AI)

## 1. Mục đích và Quy trình Tổng quan
Dự án này sử dụng mô hình **Gemini 2.5 Flash** thông qua nền tảng **Firebase Vertex AI** để mang lại khả năng xử lý ngôn ngữ tự nhiên và thị giác máy tính (Vision) cho 4 chức năng cốt lõi:
1. **Admin Analytics:** Đóng vai trò Giám đốc vận hành (COO) phân tích doanh thu và đề xuất chiến lược.
2. **Chatbot Gen Z:** Trợ lý ảo tư vấn, tự động thêm món vào giỏ và chốt đơn gửi bếp.
3. **Quét Hóa Đơn Nhập Kho (Ingredient Scan):** Đọc ảnh hóa đơn giấy và bóc tách tự động nguyên liệu (Tên, số lượng, đơn vị).
4. **Quét Thực Đơn & Tự động tạo Công thức (Menu Scan):** Đọc ảnh Menu của nhà hàng, bóc tách món ăn, giá tiền, tự động map (nối) với nguyên liệu trong kho để ra công thức, và dịch sang tiếng Anh để tự động tìm ảnh minh hoạ.

---

## 2. Hướng dẫn Thiết lập Hạ tầng Firebase Vertex AI

Không cần tạo server Python hay Node.js riêng biệt để chạy AI, hệ thống gọi trực tiếp từ Android thông qua SDK của Firebase:

1. Trong **Firebase Console**, nâng cấp project lên gói **Blaze** (Pay-as-you-go).
2. Truy cập mục **Vertex AI**, kích hoạt API `Firebase Vertex AI API`.
3. Khai báo thư viện vào file `build.gradle.kts` (app):
   ```kotlin
   implementation("com.google.firebase:firebase-ai")
   ```

---

## 3. Hướng dẫn Tích hợp Code cho từng chức năng

Dưới đây là chi tiết mã nguồn và cách Prompt Engineering (Kỹ nghệ cấu trúc lệnh) cho từng chức năng đang chạy trong hệ thống.

### Bước 3.1. Phân tích Dữ liệu Kinh doanh (Admin Analytics)
*(File: `AdminAnalyticsViewModel.kt`)*

Mục đích: Phân tích 7 ngày doanh thu gần nhất và các món bán chạy/chậm để đưa ra chiến lược JSON.

1. **Khởi tạo Model:** Ép kiểu trả về là JSON và giảm Temperature để AI không sáng tạo bay bổng làm hỏng cấu trúc.
   ```kotlin
   val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
       modelName = "gemini-2.5-flash",
       systemInstruction = content {
           text("""
               Bạn là Giám đốc Vận hành (COO) chuyên nghiệp.
               BẮT BUỘC TRẢ VỀ DUY NHẤT MỘT PHẢN HỒI JSON HỢP LỆ VỚI CẤU TRÚC SAU:
               {
                 "summary": ["Doanh thu bình quân...", "Ngày cao nhất..."],
                 "patterns": ["Thứ 7 khách tăng vọt", "Ngày rớt khách..."],
                 "actions": ["Tăng tồn kho món A...", "Chạy Flash Sale món B..."]
               }
               Khẩu quyết: "Nói nhanh, nói gắt, nói trúng".
           """.trimIndent())
       },
       generationConfig = generationConfig {
           temperature = 0.2f
           responseMimeType = "application/json"
       }
   )
   ```
2. **Nạp Dữ liệu (Context):** Đưa số liệu doanh thu vào text để AI phân tích qua hàm `generateContent`.

### Bước 3.2. Trợ lý ảo AI Gen Z chốt đơn (Chatbot)
*(File: `ChatViewModel.kt`)*

Mục đích: Tư vấn bằng ngôn ngữ Gen Z, đặc biệt là **tự động phân tích ý định (Intent) của người dùng** để nhúng mã thao tác ẩn (Tags).

1. **Thiết lập System Prompt:** 
   ```kotlin
   val systemContext = """
       Bạn là trợ lý ảo ẩm thực mang đậm phong cách Gen Z (slay, keo lỳ...).
       TỰ ĐỘNG THÊM GIỎ HÀNG: Khi khách đồng ý thêm món, BẮT BUỘC chèn đánh dấu ẩn với cú pháp: [ADD_TO_CART: ID_món_ăn]
       GỬI ĐƠN CHO BẾP: Khi khách gọi chốt đơn, HÃY chèn đánh dấu ẩn: [PLACE_ORDER]
   """.trimIndent()
   ```
2. **Cơ chế Cửa sổ trượt (Sliding Window):** Để tránh hết Token, chỉ lấy 4 tin nhắn gần nhất nạp vào lịch sử (History) trước khi gọi `startChat(historyContent)`.
3. **Cơ chế bóc tách lệnh (Regex Parsing):** Sau khi AI trả lời, App sẽ quét tìm các Tag ẩn để thực thi lệnh trên thiết bị, sau đó xóa các Tag này đi trước khi hiển thị text cho khách:
   ```kotlin
   val cartTagRegex = Regex("\\[ADD_TO_CART:\\s*(\\d+)\\]")
   val matches = cartTagRegex.findAll(rawResponse)
   for (match in matches) {
       val productId = match.groupValues[1].toInt()
       _addToCartEvents.emit(productId) // Gọi lệnh thêm vào giỏ UI
   }
   // Xóa tag ẩn: rawResponse.replace(cartTagRegex, "")
   ```

### Bước 3.3. AI Quét Hóa đơn nhập kho (Vision)
*(File: `IngredientScanViewModel.kt`)*

Mục đích: Đưa ảnh hoá đơn chụp bằng camera vào AI để chuyển thành danh sách nguyên liệu.

1. **Tối ưu Ảnh:** Cần scale nhỏ ảnh (`resizeBitmapForAI` xuống max 1024px) trước khi gửi để chống văng App (OOM) và giảm chi phí Token mạng.
2. **Gọi Gemini Vision:**
   ```kotlin
   val prompt = """
       Phân tích ảnh hóa đơn này và trả về mảng JSON: 
       [{"name": "...", "unit": "...", "stock": 0.0}]
   """.trimIndent()

   val response = model.generateContent(
       content {
           image(bitmap) // Đính kèm ảnh
           text(prompt)
       }
   )
   ```
3. **Thuật toán Hậu kiểm (Levenshtein):** Dùng thuật toán khoảng cách Levenshtein để so sánh tên nguyên liệu AI quét được với kho hiện có, tự động bắt cặp các từ gần giống (VD: "Thịt heo" và "Thịt heo xay") để chống trùng lặp.

### Bước 3.4. AI Quét Thực đơn & Đoán công thức
*(File: `MenuScanViewModel.kt`)*

Mục đích: Chụp ảnh bìa Menu, AI tự tạo món ăn, tự đoán công thức dựa trên kho hiện có, và tự tìm ảnh trên mạng (Pexels).

1. **System Prompt siêu phức tạp:** Đưa toàn bộ danh sách `khoString` hiện tại vào prompt để bắt AI phải sử dụng đúng ID nguyên liệu đang có.
   ```kotlin
   val prompt = """
       Phân tích ảnh thực đơn và trả về mảng JSON. Suy đoán công thức và TỰ DỊCH tên món ăn sang tiếng Anh (search_keyword).
       Danh sách Kho: $khoString
       Quy tắc CỰC KỲ QUAN TRỌNG: Chỉ được dùng ingredient_id có trong kho.
   """.trimIndent()
   ```
2. **Logic Luồng Chạy:**
   - AI trả về danh sách `[ { "name": "Bò bít tết", "search_keyword": "beefsteak", "recipe": [...] } ]`
   - Dùng `search_keyword` (beefsteak) gọi sang API của Pexels để tải tự động hình ảnh bò bít tết gắn vào món ăn. App tự động hiển thị Loading cho từng tiến trình.

---

## 4. Các Chiến lược Tối ưu Đã Áp dụng

1. **Token Optimization (Giảm chi phí):**
   - Chỉ nạp 4 tin nhắn gần nhất vào Chatbot thay vì toàn bộ lịch sử trò chuyện.
   - Resize toàn bộ ảnh xuống max 1024px trước khi gửi qua API Vision.
2. **Bảo mật và Trải nghiệm:**
   - Dùng Timeout 120 giây `withTimeout(120000)` để chặn treo UI khi mạng yếu.
   - Regex xóa triệt để mã lệnh (VD `[ADD_TO_CART]`) giúp người dùng không nhìn thấy các mã code robot kỳ quặc.
3. **Data Integrity (Tính toàn vẹn dữ liệu):**
   - Đặt `temperature = 0.1` đến `0.2` cho các Model quét hoá đơn/phân tích để ép AI trả về JSON chuẩn, không sáng tạo text thừa (tránh lỗi Parse JSON `Gson()`).
   - Kết hợp thuật toán `Levenshtein` ở client (Mobile) để làm sạch dữ liệu lần 2, không phó thác 100% độ chính xác cho AI.
