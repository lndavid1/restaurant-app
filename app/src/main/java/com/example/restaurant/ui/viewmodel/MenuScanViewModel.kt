package com.example.restaurant.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.Product
import com.example.restaurant.data.model.Ingredient
import com.example.restaurant.data.model.ScannedMenuItem
import com.example.restaurant.data.model.Category
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withPermit

class MenuScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // =====================================================
    // STATE
    // =====================================================
    sealed class ScanState {
        object Idle : ScanState()
        data class Loading(val message: String) : ScanState()
        data class Success(val items: List<ScannedMenuItem>) : ScanState()
        data class Error(val message: String) : ScanState()
    }



    // =====================================================
    // TRIM JSON — tránh crash khi Gemini thêm text thừa
    // =====================================================
    private fun extractJsonArray(raw: String): String {
        var count = 0
        var start = -1

        raw.forEachIndexed { i, c ->
            if (c == '[') {
                if (count == 0) start = i
                count++
            } else if (c == ']') {
                count--
                if (count == 0 && start != -1) {
                    return raw.substring(start, i + 1)
                }
            }
        }
        return "[]"
    }

    private fun trimToJson(raw: String): String {
        return extractJsonArray(raw)
    }

    // =====================================================
    // HEURISTIC MATCHING
    // =====================================================
    private fun levenshteinScore(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }

    private fun isSimilar(name1: String, name2: String): Boolean {
        val n1 = name1.lowercase().trim()
        val n2 = name2.lowercase().trim()
        if (n1 == n2) return true
        if (n1.contains(n2) || n2.contains(n1)) return true
        
        val genericWords = listOf("món", "chiên", "nướng", "xào", "luộc", "hấp", "tươi", "nước", "thịt", "cá", "phần", "suất", "size")
        var norm1 = n1
        var norm2 = n2
        genericWords.forEach { 
            norm1 = norm1.replace(it, "").trim()
            norm2 = norm2.replace(it, "").trim()
        }
        if (norm1.isNotBlank() && norm2.isNotBlank()) {
            if (norm1 == norm2) return true
            if (norm1.contains(norm2) || norm2.contains(norm1)) return true
            val distance = levenshteinScore(norm1, norm2)
            val maxLength = maxOf(norm1.length, norm2.length)
            if (distance <= 2 && maxLength > 4) return true
        }
        return false
    }

    // =====================================================
    // PARSE & VALIDATE — xử lý field thiếu, category sai
    // =====================================================
    private fun parseAndValidate(raw: String): List<ScannedMenuItem> {
        return try {
            val json = trimToJson(raw)
            val list = Gson().fromJson(json, Array<ScannedMenuItem>::class.java).toList()
            list.map { item ->
                // Clamp quantity & trust the unit AI picked (which should match inventory)
                val safeRecipe = item.recipe?.map { r ->
                    r.copy(
                        quantity = r.quantity.coerceIn(0.0, 9999.0),
                        unit = r.unit?.lowercase()?.trim() ?: "gram"
                    )
                } ?: emptyList()
                item.copy(
                    name = item.name?.ifBlank { "(Chưa xác định)" } ?: "(Chưa xác định)",
                    // price=0 → highlight cam để Admin biết cần điền
                    price = if (item.price < 0) 0L else item.price,
                    category = item.category?.trim()?.ifBlank { "Khác" } ?: "Khác",
                    search_keyword = item.search_keyword ?: "",
                    description = item.description ?: "",
                    recipe = safeRecipe
                )
            }.filter { it.name.isNotBlank() && it.name != "(Chưa xác định)" }
        } catch (e: Exception) {
            android.util.Log.e("MenuScan", "Parse lỗi: ${e.message}")
            emptyList()
        }
    }

    // =====================================================
    // RESIZE ẢNH — tránh request fail do ảnh quá lớn
    // =====================================================
    fun resizeBitmapForAI(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxDim = 1024  // 1024px đủ để Gemini đọc text menu
            val scale = minOf(
                maxDim.toFloat() / originalBitmap.width,
                maxDim.toFloat() / originalBitmap.height,
                1.0f   // Không phóng to
            )
            return if (scale < 1.0f) {
                val newW = (originalBitmap.width * scale).toInt()
                val newH = (originalBitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
                originalBitmap.recycle()
                scaled
            } else originalBitmap
        } catch (e: Exception) {
            android.util.Log.e("MenuScan", "Resize lỗi: ${e.message}")
            null
        }
    }

    // =====================================================
    // IMAGE FETCHING & CACHING (PEXELS API)
    // =====================================================
    private val imageCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val fetchSemaphore = kotlinx.coroutines.sync.Semaphore(5)

    object ApiConfig {
        // Tạm thời bật false để bạn TEST được ngay lập tức với Key Pexels đã cấp.
        // Ngay khi deploy xong PexelsWorkerProxy.js, đổi thành true và dán URL vào PROXY_BASE_URL!
        const val USE_PROXY = false
        const val PROXY_BASE_URL = "https://your-worker-url.workers.dev/?q="
        const val PEXELS_DIRECT_KEY = "6vkF29CZbGVIcAez1u4z8Cop3oPe8K3CPTGUw6t6hF9ciyhIs0U6MdT9"
    }

    private suspend fun fetchImageForKeyword(keyword: String, category: String): String? {
        if (keyword.isBlank()) return null
        
        // 1. Kiểm tra Cache
        val normalizedKey = keyword.lowercase().trim()
        if (imageCache.containsKey(normalizedKey)) {
            val cached = imageCache[normalizedKey]
            if (cached != "FALLBACK") return cached
            return null // Nếu lần trước cache fallback thì giờ thử fallback lại nhánh phụ
        }

        return fetchSemaphore.withPermit {
            try {
                // Thử tải bằng Keyword chính
                val url1 = fetchFromNetwork(normalizedKey)
                if (url1 != null) {
                    imageCache[normalizedKey] = url1
                    return@withPermit url1
                }

                // Luồng Retry (Fallback 1): Thử chỉ với Category tiếng Anh rộng hơn (nếu tìm món k ra)
                val broadCategory = when {
                    category.contains("nước", true) || category.contains("uống", true) -> "drink"
                    category.contains("cơm", true) -> "rice dish"
                    category.contains("bún", true) || category.contains("phở", true) || category.contains("mì", true) -> "asian noodles"
                    category.contains("lẩu", true) -> "hotpot"
                    category.contains("kèm", true) || category.contains("snack", true) -> "snack food"
                    else -> "asian food" // Nhánh mặc định
                }
                
                val url2 = fetchFromNetwork(broadCategory)
                if (url2 != null) {
                    imageCache[normalizedKey] = url2 // Lưu cho keyword này luôn để tốn 1 lần retry thôi
                    return@withPermit url2
                }

                // Không tìm được gì cả -> Đánh dấu FALLBACK để khỏi tra mạng lần sau
                imageCache[normalizedKey] = "FALLBACK"
                null
            } catch (e: Exception) {
                android.util.Log.e("MenuScan", "Lỗi get ảnh Pexels: ${e.message}")
                null
            }
        }
    }

    private suspend fun fetchFromNetwork(query: String): String? {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encodeQ = java.net.URLEncoder.encode(query, "UTF-8")
                val targetUrl = if (ApiConfig.USE_PROXY) {
                    "${ApiConfig.PROXY_BASE_URL}$encodeQ"
                } else {
                    "https://api.pexels.com/v1/search?query=$encodeQ&per_page=1&orientation=square"
                }

                val conn = java.net.URL(targetUrl).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                if (!ApiConfig.USE_PROXY) {
                    conn.setRequestProperty("Authorization", ApiConfig.PEXELS_DIRECT_KEY)
                }
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val stream = conn.inputStream
                    val responseStr = stream.bufferedReader().use { it.readText() }
                    
                    if (ApiConfig.USE_PROXY) {
                        // Cấu trúc từ Worker trả về: { imageUrl: "..." }
                        val json = org.json.JSONObject(responseStr)
                        if (json.has("imageUrl") && !json.isNull("imageUrl")) {
                            return@withContext json.getString("imageUrl")
                        }
                    } else {
                        // Cấu trúc chuẩn Pexels: { photos: [ { src: { medium: "..." } } ] }
                        val json = org.json.JSONObject(responseStr)
                        val photos = json.getJSONArray("photos")
                        if (photos.length() > 0) {
                            val src = photos.getJSONObject(0).getJSONObject("src")
                            return@withContext src.optString("medium", src.optString("large"))
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    // =====================================================
    // SCAN MENU — Core function
    // =====================================================
    fun scanMenuImage(uri: Uri, existingProducts: List<Product>, existingCategories: List<Category>, existingIngredients: List<Ingredient>, context: Context) {
        viewModelScope.launch {
            _scanState.value = ScanState.Loading("Đang chuẩn bị ảnh... 🖼️")
            var loadingJob: kotlinx.coroutines.Job? = null
            try {
                val bitmap = resizeBitmapForAI(uri, context)
                    ?: throw Exception("Không thể đọc ảnh. Vui lòng thử lại.")
                
                loadingJob = launch {
                    val messages = listOf(
                        "Đang gửi ảnh lên AI... ☁️",
                        "Đang đọc thực đơn... 🧠",
                        "Sắp xong rồi, chờ chút nhé... ⏳"
                    )
                    var i = 0
                    while(true) {
                        _scanState.value = ScanState.Loading(messages[i % messages.size])
                        delay(2000)
                        i++
                    }
                }

                val khoString = existingIngredients.joinToString("\n") { "ID: ${it.id} | Tên: ${it.name} | ĐVT: ${it.unit}" }
                val categoryString = existingCategories.joinToString("\n") { "- ${it.name}" }

                val model = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
                    modelName = "gemini-2.5-flash",
                    generationConfig = generationConfig { temperature = 0.1f }
                )

                val prompt = """
Bạn là AI chuyên phân tích thực đơn nhà hàng.
Phân tích ảnh thực đơn này và trả về CHỈ một mảng JSON, không giải thích.
Bắt đầu bằng [ và kết thúc bằng ]

Cùng với đó, suy đoán công thức (recipe) và PHẢI TỰ DỊCH tên món ăn sang CỤM TỪ TIẾNG ANH (search_keyword) ngắn gọn, chuẩn xác thực thể để dùng làm từ khoá tìm kiếm kho ảnh (VD: "beef noodle soup", "fried spring rolls").

Danh mục hiện có:
$categoryString

Danh sách Kho:
$khoString

Format bắt buộc:
[
  {
    "name": "Tên món ăn tiếng Việt",
    "search_keyword": "từ khoá tiếng anh",
    "price": 75000,
    "category": "Tên danh mục",
    "description": "Mô tả ngắn",
    "recipe": [
       { "ingredient_id": "ID", "quantity": 1.0, "unit": "gram", "waste_percent": 0.0 }
    ]
  }
]

Quy tắc:
- name: tên đầy đủ tiếng Việt.
- search_keyword: Tiếng Anh ngắn gọn mô tả thực thể (VD: "Pork ribs", "Orange juice").
- price: VNĐ. Nếu không có -> 0.
- category: Dùng danh mục có sẵn hoặc tạo mới ngắn gọn.
- recipe: CỰC KỲ QUAN TRỌNG. Chỉ được dùng thông tin từ Danh sách Kho. KHÔNG tự chế ingredient_id không có trong kho. Nếu không có cái nào phù hợp, để rỗng []. Đơn vị `unit` PHẢI GIỮ NGUYÊN giống hệt `unit` của nguyên liệu trong kho. Tuyệt đối không tự ý đổi đơn vị. Tự suy tính `quantity` sao cho phù hợp với đơn vị gốc đó (VD: kho tính bằng 'lít' mà 1 ly cần 100ml thì quantity là 0.1).
                """.trimIndent()

                val response = withTimeout(120000) {
                    model.generateContent(content { image(bitmap); text(prompt) })
                }

                val raw = response.text ?: "[]"
                android.util.Log.d("MenuScan", "Raw Gemini response: $raw")
                val parsed = parseAndValidate(raw)
                val deduplicated = parsed.distinctBy { it.name.lowercase().trim() }

                if (deduplicated.isEmpty()) {
                    _scanState.value = ScanState.Error("Không tìm thấy món ăn nào. Thử ảnh rõ hơn.")
                    return@launch
                }

                // Cập nhật text UI
                loadingJob.cancel()
                val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
                
                loadingJob = launch {
                    while(true) {
                        _scanState.value = ScanState.Loading("Đang fetch ảnh tự động... 🖼️ (${completedCount.get()}/${deduplicated.size})")
                        delay(500)  // 500ms đủ mượt, tránh spam UI thread
                    }
                }

                // B5: Tải ảnh song song
                val finalItems = deduplicated.map { item ->
                    async {
                        val isDup = existingProducts.any { isSimilar(it.name ?: "", item.name) }
                        val imgUrl = fetchImageForKeyword(item.search_keyword, item.category)
                        val result = item.copy(isPossibleDuplicate = isDup, image_url = imgUrl)
                        completedCount.incrementAndGet()
                        result
                    }
                }.awaitAll()

                _scanState.value = ScanState.Success(finalItems)

            } catch (e: Exception) {
                android.util.Log.e("MenuScan", "Lỗi scan: ${e.message}")
                _scanState.value = ScanState.Error("Lỗi hệ thống: ${e.message ?: "Không rõ"}")
            } finally {
                loadingJob?.cancel()
            }
        }
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
    }
}
