# CHƯƠNG III: PHÂN TÍCH CƠ SỞ DỮ LIỆU

## 3.1. Giới thiệu và Lý do Lựa chọn Firebase

### 3.1.1. Tổng quan về Firebase

Firebase là một nền tảng phát triển ứng dụng (Backend-as-a-Service - BaaS) do Google cung cấp, tích hợp nhiều dịch vụ hỗ trợ xây dựng ứng dụng di động và web hiện đại. Trong đề tài này, hệ thống quản lý nhà hàng sử dụng bốn dịch vụ Firebase chính:

| Dịch vụ | Vai trò trong hệ thống |
|---|---|
| **Firebase Authentication** | Xác thực người dùng (đăng nhập / đăng ký) |
| **Cloud Firestore** | Cơ sở dữ liệu NoSQL lưu trữ toàn bộ dữ liệu nghiệp vụ |
| **Firebase Storage** | Lưu trữ ảnh sản phẩm, ảnh đại diện người dùng |
| **Firebase App Check** | Bảo vệ API, ngăn chặn truy cập trái phép |

### 3.1.2. Lý do Lựa chọn Firebase thay vì Hệ quản trị CSDL Quan hệ Truyền thống

Một điểm khác biệt cốt lõi trong quá trình phát triển dự án là lựa chọn **Firebase Firestore** (cơ sở dữ liệu NoSQL hướng tài liệu - Document-Oriented) thay thế hoàn toàn cho hướng tiếp cận SQL truyền thống (MySQL, PostgreSQL...). Quyết định này xuất phát từ các phân tích kỹ thuật sau:

#### a) Không cần tạo bảng thủ công (Schema-less / Schema-flexible)

Với hệ quản trị CSDL quan hệ truyền thống (RDBMS), trước khi lưu bất kỳ dữ liệu nào, người phát triển phải thực hiện bước thiết lập schema (cấu trúc bảng) rất tỉ mỉ:

```sql
-- Ví dụ SQL truyền thống - phải khai báo từng cột, từng kiểu dữ liệu
CREATE TABLE users (
    uid VARCHAR(128) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    role ENUM('admin','kitchen','employee','customer') DEFAULT 'customer',
    created_at BIGINT,
    loyalty_points INT DEFAULT 0
);

-- Còn phải thêm constraint, index, foreign key...
ALTER TABLE orders ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(uid);
ALTER TABLE order_items ADD CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id);
```

Với Firebase Firestore, không cần bước này. Khi ứng dụng ghi dữ liệu lần đầu, Firestore tự động tạo Collection (tương đương bảng) và Document (tương đương hàng):

```kotlin
// Kotlin - Firestore tự tạo collection "users" và document theo uid
val userData = hashMapOf(
    "email" to email,
    "fullName" to fullName,
    "phone" to phone,
    "role" to "customer",
    "createdAt" to System.currentTimeMillis(),
    "loyaltyPoints" to 0
)
firestore.collection("users").document(user.uid).set(userData).await()
```

Không có câu lệnh DDL (CREATE TABLE, ALTER TABLE), không có migration script khi thêm trường mới — chỉ cần thêm trường vào HashMap là xong.

#### b) Không cần Server riêng — Zero Infrastructure

Với MySQL/PostgreSQL, nhóm phát triển phải:
- Thuê hoặc cấu hình một máy chủ (VPS/Cloud VM)
- Cài đặt và cấu hình hệ quản trị CSDL
- Viết toàn bộ REST API backend (Node.js, Spring Boot, Laravel...) để ứng dụng Android kết nối

Với Firebase, SDK được nhúng trực tiếp vào ứng dụng Android. Ứng dụng giao tiếp trực tiếp với Firestore qua SDK mà không cần tầng API trung gian, giảm đáng kể chi phí và thời gian phát triển.

#### c) Realtime Listener — Cập nhật dữ liệu tức thời

Một tính năng đặc biệt quan trọng với ứng dụng nhà hàng là khả năng cập nhật theo thời gian thực. Khi nhân viên bếp cập nhật trạng thái đơn hàng, màn hình của nhân viên phục vụ và khách hàng phải phản ánh ngay lập tức. Firebase Firestore hỗ trợ điều này qua `addSnapshotListener`:

```kotlin
// Lắng nghe thay đổi đơn hàng — cập nhật ngay không cần polling
fun observeOrders(): Flow<List<Order>> = callbackFlow {
    val listener = firestore.collection("orders")
        .orderBy("id", Query.Direction.DESCENDING)
        .limit(100)
        .addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val list = snapshot.toObjects(Order::class.java)
                trySend(list)
            }
        }
    awaitClose { listener.remove() }
}
```

Với MySQL, chức năng này phải được hiện thực bằng polling định kỳ (gọi API mỗi vài giây) hoặc WebSocket — phức tạp hơn nhiều.

#### d) Tích hợp liền mạch với Hệ sinh thái Google

Firebase Authentication, Firestore, Storage và Firebase AI (Gemini) đều hoạt động trong cùng một project, dùng chung cấu hình xác thực và bảo mật. Không cần quản lý nhiều credential hay cấu hình kết nối cho từng dịch vụ riêng biệt.

---

## 3.2. Các Bước Thiết Lập Firebase

### Bước 1: Tạo Project Firebase

Truy cập [https://console.firebase.google.com](https://console.firebase.google.com), tạo project mới với tên `android-app-4ba50`. Đây là project ID được ghi nhận trong file cấu hình:

```json
{
  "project_info": {
    "project_number": "685757991468",
    "project_id": "android-app-4ba50",
    "storage_bucket": "android-app-4ba50.firebasestorage.app"
  }
}
```

### Bước 2: Đăng ký Ứng dụng Android

Trong Firebase Console, thêm ứng dụng Android với **Package Name**: `com.example.restaurant`. Firebase tạo file `google-services.json` và yêu cầu đặt file này vào thư mục `app/` của dự án. File này chứa toàn bộ thông tin kết nối, API key, và cấu hình OAuth.

### Bước 3: Tích hợp Firebase SDK vào Gradle

Khai báo plugin Google Services trong `build.gradle.kts` cấp project:

```kotlin
// settings.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.4.x" apply false
}
```

Trong `app/build.gradle.kts`, khai báo tất cả các thư viện Firebase thông qua **Firebase BoM** (Bill of Materials) — cơ chế giúp đồng bộ phiên bản tự động, tránh xung đột dependency:

```kotlin
// Firebase BOM đảm bảo tất cả thư viện dùng cùng một phiên bản tương thích
implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
implementation("com.google.firebase:firebase-analytics")
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")
implementation("com.google.firebase:firebase-storage")
implementation("com.google.firebase:firebase-ai")
implementation("com.google.firebase:firebase-messaging")
implementation("com.google.firebase:firebase-appcheck-playintegrity")
```

### Bước 4: Khởi tạo Firebase trong Ứng dụng

Firebase được khởi tạo trong `MainApplication.kt` — lớp Application chạy đầu tiên khi ứng dụng khởi động:

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Firebase — đọc cấu hình từ google-services.json
        FirebaseApp.initializeApp(this)

        // Cài App Check để bảo vệ API
        if (BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
```

### Bước 5: Kết nối và Sử dụng trong Repository

Mỗi Repository lấy instance Firestore thông qua singleton pattern — Firebase SDK đảm bảo chỉ có một kết nối duy nhất được duy trì trong suốt vòng đời ứng dụng:

```kotlin
class RestaurantRepository {
    // getInstance() trả về singleton — không tạo kết nối mới mỗi lần gọi
    private val firestore = FirebaseFirestore.getInstance()
}

class FirebaseAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
}
```

### Bước 6: Cấu hình Security Rules trên Firebase Console

Firestore Security Rules kiểm soát quyền đọc/ghi trực tiếp trên Console mà không cần viết middleware:

```javascript
// Ví dụ Security Rule: chỉ admin mới được đọc tất cả users
match /users/{userId} {
    allow read: if request.auth != null &&
                   (request.auth.uid == userId ||
                    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin');
    allow write: if request.auth.uid == userId;
}
```

---

## 3.3. Thiết kế Cơ sở Dữ liệu — Mô hình Document NoSQL

### 3.3.1. Tổng quan Cấu trúc Collections

Hệ thống sử dụng 9 Collections chính trong Firestore, tương ứng với các thực thể nghiệp vụ:

```
firestore-root/
├── users/                  # Người dùng & phân quyền
├── categories/             # Danh mục món ăn
├── products/               # Thực đơn (món ăn)
├── restaurant_tables/      # Bàn ăn
├── orders/                 # Đơn hàng (nhúng items_detail)
├── ingredients/            # Nguyên liệu kho
├── daily_revenue/          # Doanh thu tổng hợp theo ngày
├── vouchers/               # Mã giảm giá
└── otp_recovery/           # OTP đổi mật khẩu (TTL ngắn)
```

### 3.3.2. Collection `users` — Quản lý Người dùng & Phân quyền

Mỗi Document trong collection `users` dùng **Firebase Auth UID** làm Document ID, đảm bảo liên kết 1-1 không thể trùng lặp giữa tài khoản xác thực và hồ sơ dữ liệu:

```
users/{uid}
├── email:         "admin@restaurant.com"
├── fullName:      "Nguyễn Văn A"
├── phone:         "0901234567"
├── address:       "123 Nguyễn Trãi, TP.HCM"
├── role:          "admin" | "kitchen" | "employee" | "customer"
├── createdAt:     1715000000000   (Unix timestamp ms)
├── loyaltyPoints: 150             (điểm tích lũy)
├── avatarUrl:     "https://storage.firebase..."   (nullable)
└── liked_products: [101, 205, 307]  (danh sách ID món yêu thích)
```

**Luồng đăng nhập — kết hợp Auth + Firestore:**

```kotlin
suspend fun login(email: String, password: String): Result<Pair<String, String>> {
    // Bước 1: Xác thực danh tính qua Firebase Auth
    val authResult = auth.signInWithEmailAndPassword(email, password).await()
    val user = authResult.user

    // Bước 2: Lấy thông tin phân quyền từ Firestore
    val document = firestore.collection("users").document(user.uid).get().await()
    val role = document.getString("role") ?: "customer"

    return Result.success(Pair(user.uid, role))
}
```

### 3.3.3. Collection `products` — Thực đơn với Công thức Nhúng

Điểm đặc biệt trong thiết kế: mỗi sản phẩm nhúng trực tiếp công thức nguyên liệu (`recipe`) dưới dạng mảng con thay vì tạo bảng quan hệ riêng:

```
products/{productId}
├── id:           101
├── category_id:  2
├── name:         "Phở bò tái"
├── description:  "Phở truyền thống..."
├── price:        75000.0
├── image_url:    "https://storage.firebase..."
├── is_available: 1
├── is_featured:  true
└── recipe: [                        ← Nhúng trực tiếp, không cần JOIN
      {
        ingredient_id: "abc123",
        quantity: 0.2,               ← 0.2 kg thịt bò
        unit: "kg",
        waste_percent: 0.05          ← 5% hao hụt
      },
      {
        ingredient_id: "def456",
        quantity: 1.5,               ← 1.5 lít nước dùng
        unit: "lit",
        waste_percent: 0.0
      }
    ]
```

### 3.3.4. Collection `orders` — Đơn hàng với Items Nhúng

Đây là collection trung tâm của nghiệp vụ. Thay vì tách `ORDER_ITEMS` ra bảng riêng như SQL, Firestore cho phép nhúng (`items_detail`) trực tiếp vào document đơn hàng:

```
orders/{orderId}
├── id:              1847291034
├── user_id:         "firebase_uid_abc"     ← FK tới users
├── table_id:        3                      ← FK tới restaurant_tables
├── order_type:      "dine_in" | "takeaway"
├── total_amount:    185000.0
├── discount_amount: 20000.0
├── points_used:     100
├── voucher_code:    "SUMMER20"
├── payment_status:  "unpaid" | "requested" | "cash_requested" | "paid"
├── order_status:    "pending" | "processing" | "completed"
├── created_at:      "2024-05-08 18:30:00"
├── table_number:    "Bàn 3"
├── vnpay_qr_url:    null
├── payos_order_code: null
└── items_detail: [
      {
        product_id:       101,
        name:             "Phở bò tái",
        quantity:         2,
        price:            75000.0,           ← Giá tại thời điểm đặt (bất biến)
        recipe_snapshot: [...]               ← Snapshot công thức lúc đặt
      },
      {
        product_id:       305,
        name:             "Trà đào cam sả",
        quantity:         1,
        price:            45000.0,
        recipe_snapshot: [...]
      }
    ]
```

**Lý do lưu `price` và `recipe_snapshot` tại thời điểm đặt:**

- **`price`**: Nếu nhà hàng thay đổi giá trong `products` sau này, các hóa đơn cũ vẫn phản ánh đúng giá gốc — bảo toàn tính toàn vẹn báo cáo doanh thu.
- **`recipe_snapshot`**: Khi bếp hoàn thành đơn và hệ thống trừ kho, dùng chính xác công thức tại thời điểm khách đặt, không bị ảnh hưởng nếu Admin sửa công thức sau đó.

### 3.3.5. Collection `daily_revenue` — Bảng Tổng hợp Doanh thu

Thay vì tính tổng doanh thu bằng cách quét toàn bộ lịch sử đơn hàng mỗi khi mở Dashboard, hệ thống duy trì một collection tổng hợp sẵn:

```
daily_revenue/{yyyy-mm-dd}
├── date:         "2024-05-08"
├── revenue:      3850000.0      ← Tổng doanh thu trong ngày
├── order_count:  42             ← Số đơn hoàn thành
└── last_updated: 1715188800000
```

Mỗi khi một đơn hàng được thanh toán, hàm `updateDailyRevenue()` dùng `FieldValue.increment()` để cộng dồn nguyên tử (atomic increment) mà không cần đọc giá trị hiện tại:

```kotlin
private suspend fun updateDailyRevenue(dateStr: String, amount: Double, orderCount: Int) {
    val ref = firestore.collection("daily_revenue").document(dateStr)
    val data = mapOf(
        "date" to dateStr,
        "revenue" to FieldValue.increment(amount),       // Cộng nguyên tử
        "order_count" to FieldValue.increment(orderCount.toLong()),
        "last_updated" to System.currentTimeMillis()
    )
    ref.set(data, SetOptions.merge()).await()  // Tạo mới nếu chưa có, cộng nếu đã có
}
```

### 3.3.6. Collection `otp_recovery` — Bảo mật Đổi Mật khẩu

Collection này lưu trữ mã OTP (One-Time Password) có thời hạn ngắn phục vụ chức năng đổi mật khẩu an toàn:

```
otp_recovery/{email}
├── hashedOtp:  "sha256_hash_of_otp"   ← Không lưu OTP gốc — bảo mật SHA-256
├── expiresAt:  1715188980000          ← Hết hạn sau 3 phút
├── attempts:   0                      ← Số lần nhập sai
├── lockUntil:  0                      ← Khóa 5 phút nếu sai 5 lần
└── lastSentAt: 1715188800000          ← Chống spam: chờ 60s mới gửi lại
```

---

## 3.4. Cơ chế Realtime và Quản lý Kết nối

### 3.4.1. Single Source of Truth (SSOT) với StateFlow

Để tránh gọi Firestore nhiều lần cho cùng một dữ liệu, hệ thống duy trì một bộ nhớ cache trong bộ nhớ ứng dụng:

```kotlin
class RestaurantRepository {
    // Cache sản phẩm dùng chung toàn ứng dụng — chỉ 1 Snapshot Listener duy nhất
    private val _productsCache = MutableStateFlow<List<Product>>(emptyList())
    val productsCache: StateFlow<List<Product>> = _productsCache

    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listener = firestore.collection("products")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(Product::class.java) ?: emptyList()
                _productsCache.value = list   // Cập nhật cache SSOT
                trySend(list)
            }
        awaitClose { listener.remove() }      // Dọn dẹp khi không còn sử dụng
    }
}
```

Khi cần tạo đơn hàng, thay vì gọi Firestore để lấy thông tin sản phẩm, hệ thống dùng cache:

```kotlin
suspend fun createOrder(...) {
    // Zero Firestore read — dùng cache đã có
    val productMap = _productsCache.value.associateBy { it.id }
    // ... xử lý tiếp
}
```

### 3.4.2. Firestore Transaction — Đảm bảo Tính Toàn vẹn Dữ liệu

Khi trừ kho nguyên liệu sau khi hoàn thành đơn, hệ thống dùng Transaction để đảm bảo tính ACID (nguyên tử), tránh race condition khi nhiều đơn hàng hoàn thành đồng thời:

```kotlin
// Transaction: đọc VÀ ghi trong 1 lần — atomic, tránh race condition
firestore.runTransaction { transaction ->
    val snapshots = deductMap.keys.associateWith { ingId ->
        transaction.get(firestore.collection("ingredients").document(ingId))
    }
    for ((ingId, amount) in deductMap) {
        val snap = snapshots[ingId] ?: continue
        val current = snap.getDouble("stock") ?: 0.0
        val rounded = Math.round((current - amount) * 1000.0) / 1000.0
        transaction.update(snap.reference, "stock", rounded)
    }
}.await()
```

---

## 3.5. So sánh Firebase Firestore với SQL Truyền thống

| Tiêu chí | MySQL / PostgreSQL | Firebase Firestore |
|---|---|---|
| **Thiết lập ban đầu** | Tạo schema, migration scripts | Không cần — tự động khi ghi dữ liệu |
| **Server** | Cần server riêng (VPS, VM) | Không cần — Google quản lý hạ tầng |
| **Backend API** | Phải tự viết (Node/Spring/Laravel) | SDK nhúng trực tiếp vào app |
| **Cập nhật realtime** | Polling hoặc WebSocket tự xây | `addSnapshotListener` tích hợp sẵn |
| **Mở rộng schema** | `ALTER TABLE` + migration | Thêm field vào code, ghi ngay |
| **Quan hệ bảng** | JOIN nhiều bảng | Nhúng (embed) hoặc tham chiếu theo ID |
| **Xác thực người dùng** | Tự xây Auth + JWT | Firebase Auth tích hợp sẵn |
| **Bảo mật** | Middleware + role-based logic | Security Rules trực tiếp trên Console |
| **Chi phí vận hành** | Thuê server, DBA | Free tier rộng rãi, tự scale |
| **Offline support** | Không | Cache tự động trên thiết bị |

---

## 3.6. Sơ đồ ERD Tổng quát

Mặc dù Firestore là NoSQL, quan hệ giữa các thực thể vẫn được thiết kế rõ ràng theo nguyên tắc chuẩn hóa, sử dụng ID tham chiếu thay vì Foreign Key:

```
USERS ──────────────────── ORDERS ──────────── ORDER_ITEMS_DETAIL (nhúng)
  │ uid (PK)                │ id (PK)                │ product_id (ref)
  │                         │ user_id (ref uid)       │ name
  │                         │ table_id (ref)          │ quantity
  │                         │ payment_status          │ price
  └── AI_CHAT_HISTORY       │ order_status            └── recipe_snapshot (nhúng)
        user_id (ref uid)   └── items_detail (nhúng)

CATEGORIES ──── PRODUCTS ──────── INGREDIENTS
  id (PK)         id (PK)           id (PK)
  name            category_id (ref) name
                  price             stock
                  recipe (nhúng)    min_quantity

RESTAURANT_TABLES          DAILY_REVENUE        VOUCHERS
  id (PK)                   date (PK)            code (PK)
  status                    revenue              discount_amount
  capacity                  order_count          valid_until
```

---

## 3.7. Kết luận

Firebase Firestore đã chứng minh là lựa chọn phù hợp và hiệu quả cho hệ thống quản lý nhà hàng với các ưu điểm nổi bật:

1. **Rút ngắn thời gian phát triển**: Không cần thiết lập server, không cần viết backend API trung gian — nhóm tập trung hoàn toàn vào logic nghiệp vụ và giao diện người dùng.

2. **Realtime sẵn có**: Tính năng cập nhật theo thời gian thực (trạng thái bàn, trạng thái đơn hàng) được hiện thực chỉ với vài dòng code thay vì phải xây dựng WebSocket server.

3. **Bảo mật tích hợp**: Firebase Authentication + App Check + Security Rules tạo thành lớp bảo vệ đa tầng mà không cần lập trình thêm.

4. **Linh hoạt Schema**: Khi yêu cầu nghiệp vụ thay đổi (ví dụ: thêm trường `payos_order_code`, `recipe_snapshot`), chỉ cần cập nhật data class Kotlin — không cần chạy migration CSDL.

5. **Tích hợp AI**: Firebase AI (Gemini) hoạt động trong cùng project, cùng xác thực — cho phép tích hợp tư vấn thông minh và scan thực đơn bằng AI một cách liền mạch.

Hệ thống hiện đang vận hành ổn định với project ID `android-app-4ba50` trên Google Cloud, sử dụng Firebase BOM phiên bản `34.0.0` đảm bảo tất cả thư viện tương thích và đồng bộ phiên bản.
