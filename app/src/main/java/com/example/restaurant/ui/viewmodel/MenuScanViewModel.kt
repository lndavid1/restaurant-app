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
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream

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
                        unit = r.unit.lowercase().trim()
                    )
                } ?: emptyList()
                item.copy(
                    name = item.name.ifBlank { "(Chưa xác định)" },
                    // price=0 → highlight cam để Admin biết cần điền
                    price = if (item.price < 0) 0L else item.price,
                    category = item.category.trim().ifBlank { "Khác" },
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
    // SCAN MENU — Core function
    // =====================================================
    fun scanMenuImage(uri: Uri, existingProducts: List<Product>, existingCategories: List<Category>, existingIngredients: List<Ingredient>, context: Context) {
        viewModelScope.launch {
            _scanState.value = ScanState.Loading("Đang chuẩn bị ảnh... 🖼️")
            var loadingJob: kotlinx.coroutines.Job? = null
            try {
                // B1: Resize ảnh trước khi gửi Gemini
                val bitmap = resizeBitmapForAI(uri, context)
                    ?: throw Exception("Không thể đọc ảnh. Vui lòng thử lại.")
                
                // Dynamic feedback
                loadingJob = launch {
                    val messages = listOf(
                        "Đang gửi ảnh lên mây... ☁️",
                        "AI đang đọc thực đơn... 🧠",
                        "Đang bóc tách data... 🔍",
                        "Sắp xong rồi, chờ chút nhé... ⏳"
                    )
                    var i = 0
                    while(true) {
                        _scanState.value = ScanState.Loading(messages[i % messages.size])
                        delay(2500)
                        i++
                    }
                }

                // Tạo chuỗi danh sách Kho nguyên liệu và Danh mục
                val khoString = existingIngredients.joinToString("\n") { "ID: ${it.id} | Tên: ${it.name} | ĐVT: ${it.unit}" }
                val categoryString = existingCategories.joinToString("\n") { "- ${it.name}" }

                // B2: Gọi Gemini Vision với prompt chuẩn
                val model = Firebase.vertexAI.generativeModel(
                    modelName = "gemini-2.5-flash",
                    generationConfig = generationConfig {
                        temperature = 0.1f  // Nhiệt độ thấp → output ổn định, ít sáng tạo
                    }
                )

                val prompt = """
Bạn là AI chuyên phân tích thực đơn nhà hàng.
Phân tích ảnh thực đơn (menu) này và trả về CHỈ một mảng JSON, không giải thích, không markdown, không code block.
Bắt đầu bằng ký tự [ và kết thúc bằng ]

Cùng với đó, suy đoán một công thức mặc định (định lượng nguyên liệu - recipe) cho mỗi món ăn, tính cho MỘT PHẦN nhỏ nhất. TRÍCH XUẤT nguyên liệu TỪ danh sách trong Kho được cung cấp dưới đây. 
Việc đối chiếu nguyên liệu KHÔNG được chỉ khớp chữ (VD không nhầm thịt bò và viên bò) mà phải ĐÚNG THEO NGHĨA SEMANTIC.

Danh mục món ăn hiện có:
$categoryString

Danh sách Kho:
$khoString

Format bát buộc:
[
  {
    "name": "Tên món ăn",
    "price": 75000,
    "category": "Tên danh mục",
    "description": "Mô tả ngắn nếu có",
    "recipe": [
       { "ingredient_id": "Mã ID nguyên liệu dạng chuỗi lấy từ danh sách Kho", "quantity": 150.0, "unit": "gram", "waste_percent": 0.0 }
    ]
  }
]

Quy tắc:
- name: tên món đầy đủ
- price: số nguyên VND. Nếu không đọc được → 0
- Nhiều mức giá (S/M/L) → chỉ lấy size nhỏ nhất
- category: PHÂN LOẠI MÓN ĂN VÀO MỘT TRONG CÁC DANH MỤC HIỆN CÓ BẰNG CÁCH TRẢ VỀ CHÍNH XÁC TÊN DANH MỤC ĐÓ. NẾU MÓN ĂN KHÔNG PHÙ HỢP VỚI BẤT KỲ DANH MỤC NÀO TRONG DANH SÁCH TRÊN, HÃY TỰ TẠO MỘT TÊN DANH MỤC MỚI NGẮN GỌN (VD: "Món nướng", "Hải sản") và trả về tên đó.
- recipe: mảng công thức. Chỉ được dùng thông tin từ Danh sách Kho. KHÔNG tự chế ingredient_id không có trong kho. Nếu trùng hợp không có, để rỗng []. Đơn vị `unit` PHẢI GIỮ NGUYÊN giống hệt `unit` của nguyên liệu trong kho (nếu kho là 'kg' thì unit là 'kg', nếu kho là 'lít' thì unit là 'lít', nếu kho là 'hộp' thì unit là 'hộp'). TUYỆT ĐỐI không tự ý đổi đơn vị. Tự suy tính `quantity` sao cho phù hợp với đơn vị gốc đó (VD: kho tính bằng 'lít' mà 1 ly cần 100ml thì quantity là 0.1).                """.trimIndent()

                val response = withTimeout(120000) {
                    model.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                }

                val raw = response.text ?: "[]"
                android.util.Log.d("MenuScan", "Raw Gemini response: $raw")

                // B3: Parse + validate
                val parsed = parseAndValidate(raw)

                // B4: Dedup trong kết quả scan
                val deduplicated = parsed.distinctBy { it.name.lowercase().trim() }

                // B5: Đánh dấu trùng với món đã có trong DB
                val withDupFlag = deduplicated.map { item ->
                    val isDup = existingProducts.any { isSimilar(it.name, item.name) }
                    item.copy(isPossibleDuplicate = isDup)
                }

                if (withDupFlag.isEmpty()) {
                    _scanState.value = ScanState.Error("Không tìm thấy món ăn nào trong ảnh. Thử ảnh rõ hơn.")
                } else {
                    _scanState.value = ScanState.Success(withDupFlag)
                }

            } catch (e: Exception) {
                android.util.Log.e("MenuScan", "Lỗi scan: ${e.message}")
                _scanState.value = ScanState.Error("Lỗi AI: ${e.message ?: "Không rõ"}")
            } finally {
                loadingJob?.cancel()
            }
        }
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
    }
}
