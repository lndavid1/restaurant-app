package com.example.restaurant.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.*
import com.example.restaurant.data.repository.RestaurantRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class StockStatus { OK, LOW_STOCK, OUT_OF_STOCK, NO_RECIPE }

class RestaurantViewModel : ViewModel() {
    private val repository = RestaurantRepository()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _tables = MutableStateFlow<List<RestaurantTable>>(emptyList())
    val tables: StateFlow<List<RestaurantTable>> = _tables

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _dailyRevenueHistory = MutableStateFlow<List<DailyRevenue>>(emptyList())
    val dailyRevenueHistory: StateFlow<List<DailyRevenue>> = _dailyRevenueHistory

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients

    private val _cartItems = MutableStateFlow<List<Pair<Product, Int>>>(emptyList())
    val cartItems: StateFlow<List<Pair<Product, Int>>> = _cartItems

    // Toast/Snackbar messages từ background operations (vd: PayOS polling)
    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    // ============================================================
    // SSOT Derived State: Pre-computed stock map — reactive, O(1) lookup trong UI
    // flowOn(Default): chạy trên background thread — KHÔNG block main thread
    // Đây là fix cho crash "Channel unrecoverably broken" (ANR)
    // ============================================================
    val productStockStatusMap: StateFlow<Map<Int, StockStatus>> = combine(
        _products, _ingredients
    ) { products, ingredients ->
        if (ingredients.isEmpty()) return@combine emptyMap()
        val ingMap = ingredients.associateBy { it.id.toString() }
        products.associate { product -> product.id to calculateStockStatus(product, ingMap) }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)  // ← critical: off main thread
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Upload ảnh lên Firebase Storage, trả về URL tải về hoặc null nếu lỗi.
     * Path: product_images/<timestamp>_<filename>
     */
    suspend fun uploadProductImage(imageUri: Uri): String? {
        return try {
            _isUploading.value = true
            val storage = FirebaseStorage.getInstance()
            val fileName = "${System.currentTimeMillis()}_${imageUri.lastPathSegment ?: "img"}"
            val ref = storage.reference.child("product_images/$fileName")
            ref.putFile(imageUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            _toastMessage.emit("Lỗi upload ảnh: ${e.message}")
            null
        } finally {
            _isUploading.value = false
        }
    }

    fun addToCart(product: Product) {
        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val existing = currentItems[index]
            currentItems[index] = existing.first to (existing.second + 1)
        } else {
            currentItems.add(product to 1)
        }
        _cartItems.value = currentItems
    }

    fun removeFromCart(product: Product) {
        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val existing = currentItems[index]
            if (existing.second > 1) {
                currentItems[index] = existing.first to (existing.second - 1)
            } else {
                currentItems.removeAt(index)
            }
        }
        _cartItems.value = currentItems
    }

    fun clearCart() { _cartItems.value = emptyList() }

    fun fetchCategories() {
        viewModelScope.launch {
            try { _categories.value = repository.getCategories() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addCategory(name: String, onCategoryAdded: (Category) -> Unit) {
        viewModelScope.launch {
            val newCat = repository.addCategory(name)
            if (newCat != null) {
                _toastMessage.emit("Đã thêm danh mục mới")
                fetchCategories()
                onCategoryAdded(newCat)
            } else {
                _toastMessage.emit("Lỗi: Không thể thêm danh mục")
            }
        }
    }

    fun fetchProducts(categoryId: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try { _products.value = repository.getProducts(categoryId) } catch (e: Exception) { e.printStackTrace() } finally { _isLoading.value = false }
        }
    }

    private var isObservingProducts = false
    private var productsJob: kotlinx.coroutines.Job? = null

    /** SSOT: Khởi động observer sản phẩm 1 lần duy nhất cho toàn app */
    private fun startObservingProducts() {
        if (isObservingProducts && productsJob?.isActive == true) return
        productsJob?.cancel()
        isObservingProducts = true
        productsJob = viewModelScope.launch {
            repository.observeProducts()
                .distinctUntilChanged()
                .flowOn(kotlinx.coroutines.Dispatchers.Default)  // parse objects off main thread
                .collect { list -> _products.value = list }
        }
    }

    private var isObserving = false
    private var currentObservingToken = ""
    private val observingJobs = mutableListOf<kotlinx.coroutines.Job>()

    val knownPaidIds = mutableSetOf<Int>()
    val knownPendingIds = mutableSetOf<Int>()
    val knownReqIds = mutableSetOf<Int>()
    val knownCallingIds = mutableSetOf<Int>()
    val knownCompletedIds = mutableSetOf<Int>()

    fun startObservingData(token: String) {
        // Khởi động SSOT products observer nếu chưa chạy
        startObservingProducts()

        // Tránh restart observers nếu token không đổi và đang chạy bình thường
        if (isObserving && token == currentObservingToken && observingJobs.any { it.isActive }) {
            return
        }

        observingJobs.forEach { it.cancel() }
        observingJobs.clear()

        isObserving = true
        currentObservingToken = token

        observingJobs.add(viewModelScope.launch {
            var isFirstEmitTables = true
            repository.observeTables(token)
                .distinctUntilChanged()
                .flowOn(kotlinx.coroutines.Dispatchers.Default)  // Firestore parse off main thread
                .collect { list ->
                    if (isFirstEmitTables) {
                        knownCallingIds.addAll(list.filter { it.needs_service }.map { it.id })
                        isFirstEmitTables = false
                    }
                    _tables.value = list
                }
        })
        observingJobs.add(viewModelScope.launch {
            var isFirstEmitOrders = true
            repository.observeOrders(token)
                .distinctUntilChanged()
                .flowOn(kotlinx.coroutines.Dispatchers.Default)  // Firestore parse off main thread
                .collect { list ->
                    if (isFirstEmitOrders) {
                        knownPaidIds.addAll(list.filter { it.payment_status == "paid" }.map { it.id })
                        knownPendingIds.addAll(list.filter { it.order_status == "pending" }.map { it.id })
                        knownReqIds.addAll(list.filter { it.payment_status == "requested" || it.payment_status == "cash_requested" || it.payment_status == "online_requested" }.map { it.id })
                        knownCompletedIds.addAll(list.filter { it.order_status == "completed" }.map { it.id })
                        isFirstEmitOrders = false
                    }
                    _orders.value = list
                }
        })
        // Quan sát dữ liệu doanh thu theo ngày realtime
        observingJobs.add(viewModelScope.launch {
            repository.observeDailyRevenue()
                .distinctUntilChanged()
                .flowOn(kotlinx.coroutines.Dispatchers.Default)  // Firestore parse off main thread
                .collect { list ->
                    _dailyRevenueHistory.value = list
                }
        })
    }

    /**
     * fetchTables/fetchOrders: Đảm bảo observers đang chạy với token đúng.
     * Nếu token thay đổi → restart. Nếu đang chạy → không làm gì.
     */
    fun fetchTables(token: String) {
        if (token.isNotBlank()) startObservingData(token)
    }
    fun fetchOrders(token: String) {
        if (token.isNotBlank()) startObservingData(token)
    }

    /** Gọi khi logout để reset state và cho phép observe lại khi login mới */
    fun clearAllData() {
        isObserving = false
        currentObservingToken = ""
        observingJobs.forEach { it.cancel() }
        observingJobs.clear()

        // Reset SSOT products observer
        isObservingProducts = false
        productsJob?.cancel()
        productsJob = null

        isObservingInventory = false
        inventoryJob?.cancel()
        inventoryJob = null

        stopPayOSPolling()

        _cartItems.value = emptyList()
        _products.value = emptyList()
        _tables.value = emptyList()
        _orders.value = emptyList()
        _ingredients.value = emptyList()
    }

    fun toggleProductFeatured(productId: Int, isFeatured: Boolean) {
        viewModelScope.launch {
            if (repository.toggleProductFeatured(productId, isFeatured)) {
                _toastMessage.emit(if (isFeatured) "Đã thêm vào Món nổi bật" else "Đã gỡ khỏi Món nổi bật")
                // SSOT observer tự cập nhật — không cần gọi fetchProducts()
            } else {
                _toastMessage.emit("Lỗi: Không thể cập nhật trạng thái món ăn")
            }
        }
    }

    // ============================================================
    // Private helper: tính stock status cho 1 sản phẩm
    // Dùng trong combine() — không bao giờ gọi trực tiếp trong UI
    // ============================================================
    private fun calculateStockStatus(product: Product, ingMap: Map<String, Ingredient>): StockStatus {
        val recipe = product.recipe
        if (recipe.isNullOrEmpty()) return StockStatus.NO_RECIPE
        if (ingMap.isEmpty()) return StockStatus.NO_RECIPE

        var lowestPortion = Double.MAX_VALUE
        var anyMatched = false

        for (item in recipe) {
            val ingredient = ingMap[item.ingredient_id.toString()] ?: continue
            val normalizedStock = normalizeQuantity(ingredient.stock, ingredient.unit)
            val normalizedNeeded = normalizeQuantity(item.quantity * (1.0 + item.waste_percent), item.unit)
            if (normalizedNeeded > 0) {
                lowestPortion = minOf(lowestPortion, normalizedStock / normalizedNeeded)
                anyMatched = true
            }
        }

        if (!anyMatched) return StockStatus.NO_RECIPE
        return when {
            lowestPortion <= 0 -> StockStatus.OUT_OF_STOCK
            lowestPortion < 2  -> StockStatus.LOW_STOCK
            else               -> StockStatus.OK
        }
    }

    private fun normalizeQuantity(amount: Double, unit: String): Double {
        val lowerUnit = unit.lowercase().trim()
        return when (lowerUnit) {
            "kg", "kilogram", "kilograms" -> amount * 1000.0
            "l", "lit", "liter", "liters", "lít" -> amount * 1000.0
            "g", "gram", "grams", "gr" -> amount
            "ml", "milliliter", "milliliters" -> amount
            else -> amount // Các đơn vị không quy đổi được (quả, con, bó, lon), giữ nguyên
        }
    }

    /** Giữ lại cho backward compat — UI nên dùng productStockStatusMap thay thế.
     * Tính trạng thái tồn kho của món theo công thức recipe. */
    fun getProductStockStatus(product: Product): StockStatus {
        val ingMap = _ingredients.value.associateBy { it.id.toString() }
        return calculateStockStatus(product, ingMap)
    }
    fun addProduct(token: String, product: Product) {
        viewModelScope.launch {
            if (repository.addProduct(product)) {
                _toastMessage.emit("Đã thêm món mới")
                // SSOT observer tự cập nhật — không cần fetchProducts()
            }
        }
    }
    fun updateProduct(token: String, product: Product) {
        viewModelScope.launch {
            if (repository.updateProduct(product)) {
                _toastMessage.emit("Đã cập nhật món ăn")
                // SSOT observer tự cập nhật
            }
        }
    }
    fun deleteProduct(token: String, id: Int) {
        viewModelScope.launch {
            if (repository.deleteProduct(id)) {
                _toastMessage.emit("Đã xóa món ăn")
                // SSOT observer tự cập nhật
            }
        }
    }
    fun clearAllProducts(token: String) {
        viewModelScope.launch {
            if (repository.clearAllProducts()) {
                _toastMessage.emit("Đã xóa toàn bộ thực đơn")
                // SSOT observer tự cập nhật
            } else {
                _toastMessage.emit("Lỗi: Không thể xóa thực đơn")
            }
        }
    }

    fun addTable(table: RestaurantTable) {
        viewModelScope.launch {
            if (repository.addTable(table)) {
                _toastMessage.emit("Đã thêm bàn mới")
                // Observers Firestore tự cập nhật — không cần restart
            }
        }
    }
    fun updateTableAdmin(table: RestaurantTable) {
        viewModelScope.launch {
            if (repository.updateTable(table)) {
                _toastMessage.emit("Đã cập nhật bàn")
                // Observers Firestore tự cập nhật — không cần restart
            }
        }
    }
    fun deleteTableAdmin(id: Int) {
        viewModelScope.launch {
            if (repository.deleteTable(id)) {
                _toastMessage.emit("Đã xóa bàn")
                // Observers Firestore tự cập nhật — không cần restart
            }
        }
    }

    private var isObservingInventory = false
    private var inventoryJob: kotlinx.coroutines.Job? = null
    
    fun fetchInventory() {
        // Tránh tạo nhiều inventory observers
        if (isObservingInventory && inventoryJob?.isActive == true) return
        inventoryJob?.cancel()
        isObservingInventory = true
        inventoryJob = viewModelScope.launch {
            repository.observeInventory()
                .distinctUntilChanged()
                .flowOn(kotlinx.coroutines.Dispatchers.Default)  // Firestore parse off main thread
                .collect { list ->
                    _ingredients.value = list
                }
        }
    }
    fun addIngredient(item: Ingredient) {
        viewModelScope.launch {
            if (repository.addIngredient(item)) {
                _toastMessage.emit("Đã thêm nguyên liệu")
            }
        }
    }
    fun updateIngredient(item: Ingredient) {
        viewModelScope.launch {
            if (repository.updateIngredient(item)) {
                _toastMessage.emit("Đã cập nhật")
            }
        }
    }
    fun deleteIngredient(id: Int) {
        viewModelScope.launch {
            if (repository.deleteIngredient(id)) {
                _toastMessage.emit("Đã xóa nguyên liệu")
            }
        }
    }

    fun clearAllIngredients() {
        viewModelScope.launch {
            val deleted = repository.clearAllIngredients()
            if (deleted >= 0) {
                _toastMessage.emit("Đã xóa toàn bộ $deleted nguyên liệu trong kho")
            } else {
                _toastMessage.emit("Lỗi khi xóa kho nguyên liệu")
            }
        }
    }

    fun clearCompletedOrders(token: String) {
        viewModelScope.launch {
            val deleted = repository.deleteCompletedOrders()
            if (deleted >= 0) {
                _toastMessage.emit("Đã xóa $deleted đơn đã phục vụ")
                fetchOrders(token)
            } else {
                _toastMessage.emit("Lỗi khi xóa đơn hàng")
            }
        }
    }

    fun placeOrder(token: String, tableId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val items = _cartItems.value.map { OrderItemRequest(it.first.id, it.second, it.first.price) }
                val total = items.sumOf { it.price * it.quantity }
                val success = repository.createOrder(token, OrderRequest(tableId, total, items))
                if (success) {
                    clearCart()
                    onSuccess()
                    _toastMessage.emit("Đã gửi đơn hàng tới bếp!")
                    fetchTables(token)
                } else {
                    _toastMessage.emit("Lỗi kết nối đặt hàng")
                }
            } catch (e: Exception) { _toastMessage.emit("Lỗi kết nối đặt hàng") }
        }
    }

    fun claimTable(token: String, tableId: Int) {
        if (tableId == 0) return
        viewModelScope.launch {
            repository.createOrder(token, OrderRequest(tableId, 0.0, emptyList()))
        }
    }

    fun releaseTableIfEmpty(token: String, tableId: Int) {
        if (tableId == 0) return
        viewModelScope.launch {
            repository.releaseTableIfEmpty(token, tableId)
        }
    }

    fun updateOrderStatus(token: String, orderId: Int, status: String) {
        viewModelScope.launch {
            if (repository.updateOrderStatus(token, orderId, status)) {
                _toastMessage.emit("Cập nhật trạng thái thành công")
                fetchOrders(token)
            }
        }
    }

    fun checkoutTable(token: String, tableId: Int) {
        viewModelScope.launch {
            if (repository.checkoutTable(token, CheckoutRequest(tableId))) {
                _toastMessage.emit("Thanh toán thành công. Bàn đã trống!")
                fetchTables(token)
                fetchOrders(token)
            } else {
                _toastMessage.emit("Lỗi khi thanh toán")
            }
        }
    }

    fun callStaff(tableId: Int) {
        viewModelScope.launch {
            if (repository.callStaff(tableId)) {
                _toastMessage.emit("Đã gửi thông báo gọi nhân viên!")
            } else {
                _toastMessage.emit("Lỗi gọi nhân viên")
            }
        }
    }

    fun clearStaffCall(tableId: Int) {
        viewModelScope.launch {
            repository.clearStaffCall(tableId)
        }
    }

    fun updateOrderItems(orderId: Int, newItems: List<OrderItemDetail>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (repository.updateOrderItems(orderId, newItems)) {
                _toastMessage.emit("Đã cập nhật đơn hàng thành công!")
                onSuccess()
            } else {
                _toastMessage.emit("Lỗi khi cập nhật đơn hàng")
            }
        }
    }

    fun requestPayment(orderId: Int) {
        viewModelScope.launch {
            if (repository.requestPayment(orderId)) {
                _toastMessage.emit("Đã gọi nhân viên thanh toán!")
            } else {
                _toastMessage.emit("Lỗi gọi thanh toán")
            }
        }
    }

    fun approvePaymentRequest(orderId: Int) {
        viewModelScope.launch {
            if (repository.approvePaymentRequest(orderId)) {
                _toastMessage.emit("Đã cho phép khách chọn phương thức thanh toán!")
            } else {
                _toastMessage.emit("Lỗi xử lý yêu cầu thanh toán")
            }
        }
    }

    fun requestCashPayment(orderId: Int) {
        viewModelScope.launch {
            if (repository.requestCashPayment(orderId)) {
                _toastMessage.emit("Đã thông báo thanh toán tiền mặt!")
            } else {
                _toastMessage.emit("Lỗi thông báo tiền mặt")
            }
        }
    }

    fun requestOnlinePayment(orderId: Int) {
        viewModelScope.launch {
            if (repository.requestOnlinePayment(orderId)) {
                _toastMessage.emit("Đã mở cổng thanh toán. Hãy báo nhân viên khi thanh toán xong!")
            } else {
                _toastMessage.emit("Lỗi mở cổng thanh toán")
            }
        }
    }

    fun checkoutOrder(token: String, orderId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (repository.checkoutOrder(token, orderId)) {
                _toastMessage.emit("Thanh toán thành công!")
                onSuccess()
            } else {
                _toastMessage.emit("Lỗi khi thanh toán")
            }
        }
    }

    // Ghi URL VNPAY lên Firestore để màn hình khách hàng bắt và hiện QR Code
    fun saveVnpayUrlToOrder(orderId: Int, url: String) {
        viewModelScope.launch {
            repository.saveVnpayUrl(orderId, url)
        }
    }

    // Xoá URL VNPAY sau khi thanh toán xong
    fun clearVnpayUrl(orderId: Int) {
        viewModelScope.launch {
            repository.clearVnpayUrl(orderId)
        }
    }

    /**
     * Tạo link thanh toán PayOS và trả về checkoutUrl + orderCode qua callback.
     * @param onSuccess(checkoutUrl, orderCode) – mở trình duyệt mặc định với URL + bắt đầu polling
     * @param onError(message)                  – hiện Snackbar
     */
    fun createPayOSPayment(
        token: String,
        orderId: Int,
        onSuccess: (String, Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repository.createPayOSPayment(token, orderId)
                if (response != null) {
                    val url       = response["checkout_url"] as? String
                    val orderCode = (response["order_code"] as? Number)?.toLong()
                    if (!url.isNullOrEmpty() && orderCode != null && orderCode > 0L) {
                        onSuccess(url, orderCode)
                    } else {
                        onError("Không nhận được link thanh toán từ PayOS")
                    }
                } else {
                    onError("Lỗi kết nối tới máy chủ")
                }
            } catch (e: Exception) {
                onError("Lỗi: ${e.message}")
            }
        }
    }

    private var pollingJob: kotlinx.coroutines.Job? = null

    /**
     * Bắt đầu polling Worker /status mỗi 3 giây.
     * Khi Worker xác nhận paid → Repository tự update Firestore → Firestore listener
     * trong ViewModel sẽ phát lại trạng thái mới về UI ngay lập tức (realtime).
     */
    fun startPayOSPolling(token: String, orderId: Int, orderCode: Long, tableId: Int, amount: Double) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.pollPayOSStatus(
                orderId   = orderId,
                orderCode = orderCode,
                tableId   = tableId,
                amount    = amount,
                onPaid    = {
                    _toastMessage.emit("✅ Thanh toán thành công! Cảm ơn bạn.")
                },
                onError   = { msg ->
                    launch { _toastMessage.emit(msg) }
                }
            )
        }
    }

    fun stopPayOSPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun fetchDailyRevenueHistory() {
        // Luôn fetch để đảm bảo lấy đủ lịch sử, observer sẽ cập nhật realtime sau đó
        viewModelScope.launch {
            val result = repository.getDailyRevenueHistory()
            if (result.isNotEmpty() || _dailyRevenueHistory.value.isEmpty()) {
                _dailyRevenueHistory.value = result
            }
        }
    }

    fun syncDailyRevenue() {
        viewModelScope.launch {
            repository.syncDailyRevenueFromOrders()
            fetchDailyRevenueHistory()
            _toastMessage.emit("Đã đồng bộ doanh thu với danh sách đơn hàng!")
        }
    }

}
