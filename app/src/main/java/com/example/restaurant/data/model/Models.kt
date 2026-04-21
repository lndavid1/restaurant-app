package com.example.restaurant.data.model

import com.google.firebase.firestore.PropertyName

data class LoginRequest(val email: String = "", val password: String = "")
data class LoginResponse(val message: String = "", val jwt: String? = null, val role: String? = null)

data class RegisterRequest(
    val email: String = "",
    val password: String = "",
    val full_name: String = "",
    val phone: String = "",
    val address: String = ""
)

data class Category(
    val id: Int = 0,
    val name: String = "",
    val image_url: String? = null
)

// Định lượng nguyên liệu cho 1 phần ăn
data class RecipeItem(
    val ingredient_id: String = "",     // Document ID trong Firestore (tránh lỗi rename)
    val quantity: Double = 0.0,          // Lượng cần cho 1 phần
    val unit: String = "gram",
    val waste_percent: Double = 0.0      // % hao hụt: 0.1 = 10% → thực tế = quantity*(1+waste)
)

// Kết quả AI Scan từ ảnh menu
data class ScannedMenuItem(
    val name: String = "",
    val price: Long = 0,                 // price=0 nếu AI không đọc được → highlight cam trong UI
    val category: String = "khac",
    val description: String = "",
    var isSelected: Boolean = true,
    var isPossibleDuplicate: Boolean = false, // true nếu trùng tên với món đã có
    var recipe: List<RecipeItem>? = emptyList(), // AI suggested recipe
    val search_keyword: String = "",        // Từ khoá tiếng Anh do AI dịch để get ảnh Pexels
    var image_url: String? = null           // URL ảnh tự động fetch từ Pexels qua Proxy
)

// Kết quả AI Scan từ hóa đơn nguyên liệu
data class ScannedIngredientItem(
    val name: String = "",
    val unit: String = "gram",
    val stock: Double = 0.0,
    var isSelected: Boolean = true,
    var isPossibleDuplicate: Boolean = false
)

@com.google.firebase.firestore.IgnoreExtraProperties
data class Product(
    val id: Int = 0,
    val category_id: Int? = null,
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val image_url: String? = null,
    @get:PropertyName("is_available") @set:PropertyName("is_available")
    @get:JvmName("getIsAvailable") @set:JvmName("setIsAvailable")
    var is_available: Int = 1,
    val category_name: String? = null,
    val ingredients: String? = null,
    @get:PropertyName("is_featured") @set:PropertyName("is_featured") var is_featured: Boolean = false,
    val recipe: List<RecipeItem>? = emptyList()   // Công thức định lượng nguyên liệu
)

data class RestaurantTable(
    val id: Int = 0,
    val table_number: String = "",
    val capacity: Int = 4,
    val status: String = "available",
    val needs_service: Boolean = false
)

data class OrderRequest(
    val table_id: Int = 0,
    val total_amount: Double = 0.0,
    val items: List<OrderItemRequest> = emptyList()
)

data class OrderItemRequest(
    val product_id: Int = 0,
    val quantity: Int = 1,
    val price: Double = 0.0
)

data class OrderItemDetail(
    val product_id: Int = 0,                        // ID để tra recipe chính xác (0 = order cũ)
    val name: String = "",                          // Tên hiển thị trên màn hình bếp
    val quantity: Int = 1,
    val price: Double = 0.0,                        // Đơn giá tại thời điểm đặt hàng (dùng cho hóa đơn)
    val recipe_snapshot: List<RecipeItem>? = null   // Snapshot recipe tại thời điểm đặt hàng
)

data class Order(
    val id: Int = 0,
    val user_id: String = "",
    val table_id: Int? = null,
    val order_type: String = "dine_in",
    val total_amount: Double = 0.0,
    val payment_status: String = "unpaid",
    val order_status: String = "pending",
    val created_at: String = "",
    val employee_name: String? = null,
    val table_number: String? = null,
    val items_detail: List<OrderItemDetail>? = null,
    val vnpay_qr_url: String? = null,
    val payos_order_code: Long? = null   // Mã đơn PayOS để tracking
)

data class Recommendation(val name: String = "", val reason: String = "")

data class OrderStatusUpdateRequest(val order_id: Int = 0, val status: String = "")
data class CheckoutRequest(val table_id: Int = 0)
data class DailyRevenue(
    val date: String = "",
    val revenue: Double = 0.0,
    val order_count: Int = 0,
    val last_updated: Long = 0L
)
