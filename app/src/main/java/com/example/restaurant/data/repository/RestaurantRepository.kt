package com.example.restaurant.data.repository

import com.example.restaurant.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RestaurantRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = firestore.collection("categories").get().await()
            var list = snapshot.toObjects(Category::class.java)
            if (list.isEmpty()) {
                val defaultCategories = listOf(
                    Category(1, "Món chính"),
                    Category(2, "Đồ ăn vặt"),
                    Category(3, "Đồ uống")
                )
                for (cat in defaultCategories) {
                    firestore.collection("categories").document(cat.id.toString()).set(cat).await()
                }
                list = defaultCategories
            }
            list.sortedBy { it.id }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getProducts(categoryId: Int? = null): List<Product> {
        return try {
            val query = if (categoryId != null) {
                firestore.collection("products").whereEqualTo("category_id", categoryId).get().await()
            } else {
                firestore.collection("products").get().await()
            }
            query.toObjects(Product::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTables(token: String): List<RestaurantTable> {
        return try {
            val snapshot = firestore.collection("restaurant_tables").get().await()
            snapshot.toObjects(RestaurantTable::class.java).sortedBy { it.id }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getOrders(token: String): List<Order> {
        return try {
            val snapshot = firestore.collection("orders")
                .orderBy("id", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(300)
                .get().await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) { emptyList() }
    }

    fun observeTables(token: String): Flow<List<RestaurantTable>> = callbackFlow {
        val listenerRegistration = firestore.collection("restaurant_tables")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(RestaurantTable::class.java).sortedBy { it.id }
                    trySend(list)
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun observeOrders(token: String): Flow<List<Order>> = callbackFlow {
        val listenerRegistration = firestore.collection("orders")
            .orderBy("id", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Order::class.java)
                    trySend(list)
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun createOrder(token: String, request: OrderRequest): Boolean {
        return try {
            val productsDb = getProducts()
            // Preload map: O(1) lookup thay vì find() N lần
            val productMap = productsDb.associateBy { it.id }

            if (request.items.isEmpty()) {
                if (request.table_id != 0) {
                    firestore.collection("restaurant_tables").document(request.table_id.toString())
                        .update("status", "occupied").await()
                }
                return true
            }

            if (request.table_id != 0) {
                val existingOrders = firestore.collection("orders")
                    .whereEqualTo("table_id", request.table_id)
                    .whereEqualTo("payment_status", "unpaid")
                    .get().await()

                if (!existingOrders.isEmpty) {
                    val existingDoc = existingOrders.documents[0]
                    val existingOrder = existingDoc.toObject(Order::class.java)

                    if (existingOrder != null) {
                        val currentItems = existingOrder.items_detail ?: emptyList()
                        val newItems = request.items.map { reqItem ->
                            val product = productMap[reqItem.product_id]
                            OrderItemDetail(
                                product_id = reqItem.product_id,
                                name = product?.name ?: "Sản phẩm #${reqItem.product_id}",
                                quantity = reqItem.quantity,
                                recipe_snapshot = product?.recipe  // Snapshot tại thời điểm đặt
                            )
                        }

                        // Gộp theo product_id (có id) hoặc name (order cũ)
                        val mergedById = mutableMapOf<Int, OrderItemDetail>()
                        val mergedByName = mutableMapOf<String, OrderItemDetail>()
                        (currentItems + newItems).forEach { item ->
                            if (item.product_id != 0) {
                                val ex = mergedById[item.product_id]
                                mergedById[item.product_id] = ex?.copy(quantity = ex.quantity + item.quantity) ?: item
                            } else {
                                val ex = mergedByName[item.name]
                                mergedByName[item.name] = ex?.copy(quantity = ex.quantity + item.quantity) ?: item
                            }
                        }
                        val finalItems = mergedById.values.toList() + mergedByName.values.toList()

                        val newTotal = existingOrder.total_amount + request.total_amount
                        firestore.collection("orders").document(existingDoc.id)
                            .update(mapOf(
                                "items_detail" to finalItems,
                                "total_amount" to newTotal,
                                "order_status" to "pending"
                            )).await()
                        return true
                    }
                }
            }

            // Tạo hóa đơn mới
            val newId = kotlin.math.abs(Random.nextInt())
            val details = request.items.map { reqItem ->
                val product = productMap[reqItem.product_id]
                OrderItemDetail(
                    product_id = reqItem.product_id,
                    name = product?.name ?: "Sản phẩm #${reqItem.product_id}",
                    quantity = reqItem.quantity,
                    recipe_snapshot = product?.recipe  // Snapshot tại thời điểm đặt
                )
            }

            val order = Order(
                id = newId,
                user_id = token,
                table_id = request.table_id,
                order_type = if (request.table_id == 0) "takeaway" else "dine_in",
                total_amount = request.total_amount,
                payment_status = "unpaid",
                order_status = "pending",
                created_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                table_number = if (request.table_id == 0) "Mang về" else "Bàn ${request.table_id}",
                items_detail = details
            )
            firestore.collection("orders").document(newId.toString()).set(order).await()

            if (request.table_id != 0) {
                firestore.collection("restaurant_tables").document(request.table_id.toString())
                    .update("status", "occupied").await()
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateOrderStatus(token: String, orderId: Int, status: String): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("order_status", status).await()
            // Tự động trừ kho khi bếp đánh dấu hoàn thành
            if (status == "completed") {
                deductIngredientsForOrder(orderId)
            }
            true
        } catch (e: Exception) { false }
    }

    /** Trừ kho nguyên liệu cho một đơn hàng đã hoàn thành.
     * Sách: Preload all products 1 lần, ưu tiên recipe_snapshot, Firestore Transaction.
     */
    private suspend fun deductIngredientsForOrder(orderId: Int) {
        try {
            val orderDoc = firestore.collection("orders")
                .document(orderId.toString()).get().await()

            @Suppress("UNCHECKED_CAST")
            val itemsRaw = orderDoc.get("items_detail") as? List<Map<String, Any>> ?: return

            // Preload tất cả products 1 lần (1 Firestore read thay vì N reads)
            val allProducts = firestore.collection("products").get().await()
                .toObjects(Product::class.java)
            val productMap = allProducts.associateBy { it.id }

            // Gom tất cả ingredient cần trừ (aggregate toàn bộ món trong đơn)
            val deductMap = mutableMapOf<String, Double>() // ingredient_id -> tổng lượng cần trừ

            for (item in itemsRaw) {
                val productId = (item["product_id"] as? Number)?.toInt() ?: 0
                if (productId == 0) continue  // Order cũ không có product_id
                val orderedQty = (item["quantity"] as? Number)?.toInt() ?: 1

                // Ưu tiên recipe_snapshot (tại thời điểm đặt), fallback sang recipe hiện tại
                @Suppress("UNCHECKED_CAST")
                val snapshotRaw = item["recipe_snapshot"] as? List<Map<String, Any>>
                val recipeToUse: List<Map<String, Any>> = snapshotRaw ?: run {
                    val product = productMap[productId] ?: return@run null
                    product.recipe?.map { r ->
                        mapOf<String, Any>(
                            "ingredient_id" to r.ingredient_id,
                            "quantity" to r.quantity,
                            "waste_percent" to r.waste_percent
                        )
                    }
                } ?: continue

                for (recipeItem in recipeToUse) {
                    val ingId = recipeItem["ingredient_id"] as? String ?: continue
                    if (ingId.isBlank()) continue
                    val qty = (recipeItem["quantity"] as? Number)?.toDouble() ?: continue
                    val waste = (recipeItem["waste_percent"] as? Number)?.toDouble() ?: 0.0
                    val actualQty = qty * (1.0 + waste) * orderedQty
                    deductMap[ingId] = (deductMap[ingId] ?: 0.0) + actualQty
                }
            }

            if (deductMap.isEmpty()) return

            // Firestore Transaction: đọc VÀ ghi trong 1 lần atomic, tránh race condition
            firestore.runTransaction { transaction ->
                // Phải read TRONG transaction block
                val snapshots = deductMap.keys.associateWith { ingId ->
                    transaction.get(firestore.collection("ingredients").document(ingId))
                }
                for ((ingId, amount) in deductMap) {
                    val snap = snapshots[ingId] ?: continue
                    if (!snap.exists()) continue
                    val current = snap.getDouble("stock") ?: 0.0
                    // Fix Double precision: làm tròn 3 chữ số thập phân
                    val rounded = Math.round((current - amount) * 1000.0) / 1000.0
                    if (rounded < 0) {
                        android.util.Log.w("Inventory", "⚠️ Kho âm: id=$ingId stock=$rounded")
                    }
                    transaction.update(snap.reference, "stock", rounded)
                }
            }.await()

            android.util.Log.d("Inventory", "✅ Trừ kho thành công đơn #$orderId: $deductMap")
        } catch (e: Exception) {
            android.util.Log.e("Inventory", "Lỗi trừ kho đơn #$orderId: ${e.message}")
        }
    }

    suspend fun requestPayment(orderId: Int): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("payment_status", "requested").await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun approvePaymentRequest(orderId: Int): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("payment_status", "payment_approved").await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun requestCashPayment(orderId: Int): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("payment_status", "cash_requested").await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun requestOnlinePayment(orderId: Int): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("payment_status", "online_requested").await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun checkoutOrder(token: String, orderId: Int): Boolean {
        return try {
            val orderDoc = firestore.collection("orders").document(orderId.toString()).get().await()
            val tableId = orderDoc.getLong("table_id")?.toInt() ?: 0

            // Nếu có bàn thật (không phải mang về), giải phóng bàn
            if (tableId != 0) {
                firestore.collection("restaurant_tables").document(tableId.toString())
                    .update(mapOf("status" to "available", "needs_service" to false)).await()
            }

            val currentStatus = orderDoc.getString("payment_status") ?: ""
            if (currentStatus != "paid") {
                firestore.collection("orders").document(orderId.toString())
                    .update("order_status", "completed", "payment_status", "paid").await()
                
                // Cập nhật thống kê ngày dựa theo ngày tạo hóa đơn
                val amount = orderDoc.getDouble("total_amount") ?: 0.0
                val createdAt = orderDoc.getString("created_at") ?: ""
                val dateStr = if (createdAt.length >= 10) createdAt.substring(0, 10) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                updateDailyRevenue(dateStr, amount, 1)
            }
            
            true
        } catch (e: Exception) { false }
    }

    suspend fun checkoutTable(token: String, request: CheckoutRequest): Boolean {
        return try {
            firestore.collection("restaurant_tables").document(request.table_id.toString())
                .update(mapOf("status" to "available", "needs_service" to false)).await()
            
            val snapshot = firestore.collection("orders")
                .whereEqualTo("table_id", request.table_id)
                .get().await()

            val ordersByDate = mutableMapOf<String, Pair<Double, Int>>()
            for (doc in snapshot.documents) {
                val payStatus = doc.getString("payment_status") ?: ""
                if (payStatus in listOf("unpaid", "requested", "cash_requested", "payment_approved", "online_requested")) {
                    val orderAmount = doc.getDouble("total_amount") ?: 0.0
                    val createdAt = doc.getString("created_at") ?: ""
                    val dateStr = if (createdAt.length >= 10) createdAt.substring(0, 10) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    
                    val current = ordersByDate[dateStr] ?: Pair(0.0, 0)
                    ordersByDate[dateStr] = Pair(current.first + orderAmount, current.second + 1)

                    firestore.collection("orders").document(doc.id)
                        .update("order_status", "completed", "payment_status", "paid").await()
                }
            }

            for ((dateStr, pair) in ordersByDate) {
                updateDailyRevenue(dateStr, pair.first, pair.second)
            }

            true
        } catch (e: Exception) { false }
    }

    suspend fun releaseTableIfEmpty(token: String, tableId: Int): Boolean {
        return try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("table_id", tableId)
                .whereEqualTo("payment_status", "unpaid")
                .get().await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                val amount = doc.getDouble("total_amount") ?: 0.0
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items_detail") as? List<*>
                val hasNoItems = items.isNullOrEmpty()

                // Giải phóng nếu order rỗng (không có món và tổng = 0)
                if (hasNoItems && amount == 0.0) {
                    firestore.collection("orders").document(doc.id).delete().await()
                    firestore.collection("restaurant_tables").document(tableId.toString())
                        .update("status", "available").await()
                    return true
                }
            } else {
                // Không có order nào ở bàn này → đảm bảo bàn về trạng thái available
                firestore.collection("restaurant_tables").document(tableId.toString())
                    .update("status", "available").await()
                return true
            }
            false
        } catch (e: Exception) { false }
    }

    private suspend fun updateDailyRevenue(dateStr: String, amount: Double, orderCount: Int = 1) {
        try {
            val ref = firestore.collection("daily_revenue").document(dateStr)

            val data = mapOf(
                "date" to dateStr,
                "revenue" to FieldValue.increment(amount),
                "order_count" to FieldValue.increment(orderCount.toLong()),
                "last_updated" to System.currentTimeMillis()
            )

            ref.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncDailyRevenueFromOrders() {
        try {
            // 1. Fetch all orders
            val snapshot = firestore.collection("orders").get().await()
            val orders = snapshot.toObjects(Order::class.java)

            // 2. Aggregate by date (only paid orders)
            val aggregated = orders.filter { it.payment_status == "paid" }
                .groupBy { 
                    val dateStr = it.created_at
                    if (dateStr.length >= 10) dateStr.substring(0, 10) else "" 
                }
                .filterKeys { it.isNotEmpty() }

            // 3. Clear existing daily revenue
            val oldRevenues = firestore.collection("daily_revenue").get().await()
            for (doc in oldRevenues.documents) {
                firestore.collection("daily_revenue").document(doc.id).delete().await()
            }

            // 4. Insert accurate aggregated data
            for ((date, dailyOrders) in aggregated) {
                val revenue = dailyOrders.sumOf { it.total_amount }
                val count = dailyOrders.size
                val data = mapOf(
                    "date" to date,
                    "revenue" to revenue,
                    "order_count" to count,
                    "last_updated" to System.currentTimeMillis()
                )
                firestore.collection("daily_revenue").document(date).set(data).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getDailyRevenueHistory(): List<DailyRevenue> {
        return try {
            // Lấy TOÀN BỘ collection rồi lọc in-memory
            // (whereGreaterThanOrEqualTo trên document ID không hoạt động ổn định trong Firestore)
            val snapshot = firestore.collection("daily_revenue").get().await()

            android.util.Log.d("Revenue", "Fetched total ${snapshot.size()} daily_revenue documents")

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
            val cutoffDate = sdf.format(cal.time)

            snapshot.documents.mapNotNull { doc ->
                try {
                    val dateStr = doc.getString("date") ?: doc.id
                    android.util.Log.d("Revenue", "  Doc: ${doc.id} | date=$dateStr | revenue=${doc.getDouble("revenue")} | count=${doc.getLong("order_count")}")
                    DailyRevenue(
                        date = dateStr,
                        revenue = doc.getDouble("revenue") ?: 0.0,
                        order_count = (doc.getLong("order_count") ?: doc.getDouble("order_count")?.toLong() ?: 0L).toInt(),
                        last_updated = doc.getLong("last_updated") ?: 0L
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Revenue", "Error parsing doc ${doc.id}: ${e.message}")
                    null
                }
            }
            .filter { it.date >= cutoffDate }  // Lọc 30 ngày gần nhất in-memory
            .sortedBy { it.date }
        } catch (e: Exception) {
            android.util.Log.e("Revenue", "getDailyRevenueHistory FAILED: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun observeDailyRevenue(): Flow<List<DailyRevenue>> = callbackFlow {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Lấy toàn bộ collection, filter in-memory — tránh giới hạn query trên document ID
        val listenerRegistration = firestore.collection("daily_revenue")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("Revenue", "Observer error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                    val cutoffDate = sdf.format(cal.time)

                    android.util.Log.d("Revenue", "Observer: ${snapshot.size()} docs total, cutoff=$cutoffDate")

                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dateStr = doc.getString("date") ?: doc.id
                            DailyRevenue(
                                date = dateStr,
                                revenue = doc.getDouble("revenue") ?: 0.0,
                                order_count = (doc.getLong("order_count") ?: doc.getDouble("order_count")?.toLong() ?: 0L).toInt(),
                                last_updated = doc.getLong("last_updated") ?: 0L
                            )
                        } catch (e: Exception) { null }
                    }
                    .filter { it.date >= cutoffDate }
                    .sortedBy { it.date }

                    android.util.Log.d("Revenue", "Observer: ${list.size} docs after filter")
                    trySend(list)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    suspend fun getAIRecommendations(token: String): List<Recommendation> = emptyList()

    suspend fun addCategory(categoryName: String): Category? {
        return try {
            val newId = kotlin.math.abs(Random.nextInt())
            val category = Category(id = newId, name = categoryName)
            firestore.collection("categories").document(newId.toString()).set(category).await()
            category
        } catch (e: Exception) { null }
    }

    // ADMIN CRUD PRODUCTS
    suspend fun addProduct(product: Product): Boolean {
        return try {
            val newId = kotlin.math.abs(Random.nextInt())
            firestore.collection("products").document(newId.toString()).set(product.copy(id = newId)).await()
            true
        } catch (e: Exception) { false }
    }
    suspend fun updateProduct(product: Product): Boolean {
        return try {
            var snapshot = firestore.collection("products").whereEqualTo("id", product.id).get().await()
            if (snapshot.isEmpty) {
                snapshot = firestore.collection("products").whereEqualTo("id", product.id.toString()).get().await()
            }
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                firestore.collection("products").document(docId).set(product).await()
                true
            } else {
                false
            }
        } catch (e: Exception) { false }
    }
    suspend fun deleteProduct(id: Int): Boolean {
        return try {
            var snapshot = firestore.collection("products").whereEqualTo("id", id).get().await()
            if (snapshot.isEmpty) {
                snapshot = firestore.collection("products").whereEqualTo("id", id.toString()).get().await()
            }
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                firestore.collection("products").document(docId).delete().await()
                true
            } else {
                false
            }
        } catch (e: Exception) { false }
    }
    
    suspend fun clearAllProducts(): Boolean {
        return try {
            val snapshot = firestore.collection("products").get().await()
            for (doc in snapshot.documents) {
                firestore.collection("products").document(doc.id).delete().await()
            }
            true
        } catch (e: Exception) { false }
    }


    suspend fun toggleProductFeatured(productId: Int, isFeatured: Boolean): Boolean {
        return try {
            var snapshot = firestore.collection("products").whereEqualTo("id", productId).get().await()
            if (snapshot.isEmpty) {
                snapshot = firestore.collection("products").whereEqualTo("id", productId.toString()).get().await()
            }
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                firestore.collection("products").document(docId).update("is_featured", isFeatured).await()
                true
            } else {
                false
            }
        } catch (e: Exception) { false }
    }
    // ADMIN CRUD TABLES
    suspend fun addTable(table: RestaurantTable): Boolean {
        return try {
            val newId = kotlin.math.abs(Random.nextInt())
            firestore.collection("restaurant_tables").document(newId.toString()).set(table.copy(id = newId)).await()
            true
        } catch (e: Exception) { false }
    }
    suspend fun updateTable(table: RestaurantTable): Boolean {
        return try {
            firestore.collection("restaurant_tables").document(table.id.toString()).set(table).await()
            true
        } catch (e: Exception) { false }
    }
    suspend fun deleteTable(id: Int): Boolean {
        return try {
            firestore.collection("restaurant_tables").document(id.toString()).delete().await()
            true
        } catch (e: Exception) { false }
    }

    // ADMIN CRUD INVENTORY
    suspend fun getInventory(): List<Ingredient> {
        return try {
            val snapshot = firestore.collection("ingredients").get().await()
            snapshot.toObjects(Ingredient::class.java)
        } catch (e: Exception) { emptyList() }
    }
    fun observeInventory(): Flow<List<Ingredient>> = callbackFlow {
        val listenerRegistration = firestore.collection("ingredients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Ingredient::class.java)
                    trySend(list)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
    suspend fun addIngredient(item: Ingredient): Boolean {
        return try {
            val newId = kotlin.math.abs(Random.nextInt())
            firestore.collection("ingredients").document(newId.toString()).set(item.copy(id = newId)).await()
            true
        } catch (e: Exception) { false }
    }
    suspend fun updateIngredient(item: Ingredient): Boolean {
        return try {
            firestore.collection("ingredients").document(item.id.toString()).set(item).await()
            true
        } catch (e: Exception) { false }
    }
    suspend fun deleteIngredient(id: Int): Boolean {
        return try {
            firestore.collection("ingredients").document(id.toString()).delete().await()
            true
        } catch (e: Exception) { false }
    }
    suspend fun clearAllIngredients(): Int {
        return try {
            val snapshot = firestore.collection("ingredients").get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
            snapshot.size()
        } catch (e: Exception) { -1 }
    }

    suspend fun deleteCompletedOrders(): Int {
        return try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("order_status", "completed")
                .get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
            snapshot.size()
        } catch (e: Exception) { -1 }
    }

    // Lưu URL thanh toán VNPAY lên Firestore để màn hình khách hàng tự hiển thị QR
    suspend fun saveVnpayUrl(orderId: Int, url: String): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("vnpay_qr_url", url)
                .await()
            true
        } catch (e: Exception) { false }
    }

    // Xoá URL VNPAY khỏi order sau khi khách đã thanh toán
    suspend fun clearVnpayUrl(orderId: Int): Boolean {
        return try {
            firestore.collection("orders").document(orderId.toString())
                .update("vnpay_qr_url", null)
                .await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun callStaff(tableId: Int): Boolean {
        if (tableId == 0) return false
        return try {
            firestore.collection("restaurant_tables").document(tableId.toString())
                .update("needs_service", true).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun clearStaffCall(tableId: Int): Boolean {
        if (tableId == 0) return false
        return try {
            firestore.collection("restaurant_tables").document(tableId.toString())
                .update("needs_service", false).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateOrderItems(orderId: Int, newItems: List<OrderItemDetail>): Boolean {
        return try {
            // Lấy danh sách sản phẩm để nội suy giá tiền gốc theo tên
            val productsDb = getProducts()
            var newTotal = 0.0

            val mergedMap = mutableMapOf<String, Int>()
            for (item in newItems) {
                if (item.quantity > 0) {
                    mergedMap[item.name] = (mergedMap[item.name] ?: 0) + item.quantity
                }
            }

            val finalItems = mergedMap.map { entry ->
                val pPrice = productsDb.find { it.name == entry.key }?.price ?: 0.0
                newTotal += (pPrice * entry.value)
                OrderItemDetail(name = entry.key, quantity = entry.value)
            }

            firestore.collection("orders").document(orderId.toString())
                .update(
                    mapOf(
                        "items_detail" to finalItems,
                        "total_amount" to newTotal
                    )
                ).await()
            true
        } catch (e: Exception) { false }
    }

    /**
     * Tạo link thanh toán PayOS qua Cloudflare Worker.
     * Worker ký HMAC-SHA256 và gọi PayOS API. Không cần Firebase Admin.
     * App nhận checkoutUrl + orderCode, tự lưu orderCode vào Firestore.
     */
    suspend fun createPayOSPayment(token: String, orderId: Int): Map<String, Any>? {
        return try {
            // Lấy số tiền từ Firestore bằng Firebase SDK (đã có sẵn, không cần Admin)
            val orderDoc = firestore.collection("orders").document(orderId.toString()).get().await()
            val amount = (orderDoc.getDouble("total_amount") ?: 0.0).toLong()
            if (amount <= 0) return null

            // Gọi Cloudflare Worker /create
            val response = com.example.restaurant.data.remote.PayOSWorkerClient.instance
                .createPayment(
                    com.example.restaurant.data.remote.PayOSCreateRequest(
                        order_id = orderId,
                        amount   = amount
                    )
                )

            if (!response.isSuccessful) return null
            val body = response.body() ?: return null
            if (body.checkout_url.isNullOrEmpty()) return null

            // Lưu payos_order_code vào Firestore bằng Firebase SDK thông thường
            body.order_code?.let { code ->
                firestore.collection("orders").document(orderId.toString())
                    .update("payos_order_code", code).await()
            }

            mapOf(
                "checkout_url" to (body.checkout_url as Any),
                "order_code"   to (body.order_code   as? Any ?: 0L)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Polling: hỏi Worker /status?order_code=xxx mỗi [intervalMs] ms.
     * Khi Worker xác nhận paid (từ KV do webhook ghi), tự cập nhật Firestore.
     * @param onPaid  callback sau khi Firestore update thành công
     * @param onError callback nếu lỗi hoặc timeout
     */
    suspend fun pollPayOSStatus(
        orderId: Int,
        orderCode: Long,
        tableId: Int,
        amount: Double,
        intervalMs: Long = 3000L,
        timeoutMs: Long  = 300000L,
        onPaid: suspend () -> Unit,
        onError: (String) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val resp = com.example.restaurant.data.remote.PayOSWorkerClient.instance
                    .checkStatus(orderCode)

                if (resp.isSuccessful && resp.body()?.paid == true) {
                    firestore.collection("orders").document(orderId.toString())
                        .update(
                            mapOf(
                                "payment_status" to "paid",
                                "order_status"   to "completed"
                            )
                        ).await()

                    if (tableId != 0) {
                        firestore.collection("restaurant_tables").document(tableId.toString())
                            .update(mapOf("status" to "available", "needs_service" to false)).await()
                    }

                    // Cập nhật thống kê ngày
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    updateDailyRevenue(dateStr, amount, 1)

                    onPaid()
                    return
                }
            } catch (e: Exception) {
                // Bỏ qua lỗi mạng tạm thời
            }
            kotlinx.coroutines.delay(intervalMs)
        }
        onError("Hết thời gian chờ xác nhận thanh toán (5 phút)")
    }
}
