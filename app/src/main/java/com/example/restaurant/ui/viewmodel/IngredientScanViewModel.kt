package com.example.restaurant.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.Ingredient
import com.example.restaurant.data.model.ScannedIngredientItem
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

class IngredientScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // =====================================================
    // STATE
    // =====================================================
    sealed class ScanState {
        object Idle : ScanState()
        data class Loading(val message: String) : ScanState()
        data class Success(val items: List<ScannedIngredientItem>) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    // =====================================================
    // TRIM JSON
    // =====================================================
    private fun trimToJson(raw: String): String {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        return if (start != -1 && end != -1 && end > start) raw.substring(start, end + 1)
        else "[]"
    }

    // =====================================================
    // PARSE & VALIDATE
    // =====================================================
    private fun parseAndValidate(raw: String): List<ScannedIngredientItem> {
        return try {
            val json = trimToJson(raw)
            val list = Gson().fromJson(json, Array<ScannedIngredientItem>::class.java).toList()
            list.map { item ->
                item.copy(
                    name = item.name.ifBlank { "(Chưa xác định)" },
                    stock = if (item.stock < 0) 0.0 else item.stock,
                    unit = item.unit.ifBlank { "gram" }
                )
            }.filter { it.name.isNotBlank() && it.name != "(Chưa xác định)" }
        } catch (e: Exception) {
            android.util.Log.e("IngredientScan", "Parse lỗi: ${e.message}")
            emptyList()
        }
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
        
        // Normalize: remove generic words
        val genericWords = listOf("tươi", "đông lạnh", "hộp", "gói", "thịt", "cá", "rau", "củ", "quả", "đồ", "nước")
        var norm1 = n1
        var norm2 = n2
        genericWords.forEach { 
            norm1 = norm1.replace(it, "").trim()
            norm2 = norm2.replace(it, "").trim()
        }
        if (norm1.isNotBlank() && norm2.isNotBlank()) {
            if (norm1 == norm2) return true
            if (norm1.contains(norm2) || norm2.contains(norm1)) return true
            // Levenshtein
            val distance = levenshteinScore(norm1, norm2)
            val maxLength = maxOf(norm1.length, norm2.length)
            // Nếu khoảng cách <= 2 (hoặc 30% độ dài) thì coi như giống
            if (distance <= 2 && maxLength > 4) return true
        }
        return false
    }

    // =====================================================
    // RESIZE IMAGE
    // =====================================================
    fun resizeBitmapForAI(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxDim = 1024 
            val scale = minOf(
                maxDim.toFloat() / originalBitmap.width,
                maxDim.toFloat() / originalBitmap.height,
                1.0f  
            )
            return if (scale < 1.0f) {
                val newW = (originalBitmap.width * scale).toInt()
                val newH = (originalBitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
                originalBitmap.recycle()
                scaled
            } else originalBitmap
        } catch (e: Exception) {
            android.util.Log.e("IngredientScan", "Resize lỗi: ${e.message}")
            null
        }
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    }

    // =====================================================
    // SCAN INGREDIENTS — Core function
    // =====================================================
    fun scanIngredientImage(uri: Uri, existingIngredients: List<Ingredient>, context: Context) {
        viewModelScope.launch {
            _scanState.value = ScanState.Loading("Đang chuẩn bị ảnh... 🖼️")
            var loadingJob: kotlinx.coroutines.Job? = null
            try {
                // B1: Resize ảnh
                val bitmap = resizeBitmapForAI(uri, context)
                    ?: throw Exception("Không thể đọc ảnh. Vui lòng thử lại.")
                
                // Dynamic feedback
                loadingJob = launch {
                    val messages = listOf(
                        "Đang gửi ảnh lên mây... ☁️",
                        "AI đang đọc hóa đơn... 🧠",
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

                // B2: Gọi Gemini Vision
                val model = Firebase.vertexAI.generativeModel(
                    modelName = "gemini-2.5-flash",
                    generationConfig = generationConfig {
                        temperature = 0.1f 
                    }
                )

                val prompt = """
Bạn là AI phân tích hóa đơn mua hàng / danh sách nguyên liệu nhà hàng.
Phân tích ảnh này và trả về CHỈ một mảng JSON các nguyên liệu, không giải thích, không markdown.
Bắt đầu bằng ký tự [ và kết thúc bằng ]

Format bắt buộc:
[
  {
    "name": "Tên nguyên liệu",
    "unit": "kg",
    "stock": 10.5
  }
]

Quy tắc:
- name: tên nguyên liệu.
- unit: đơn vị tính (kg, gram, lit, chai, hộp, quả, ...). Nếu không rõ, hãy để trống "".
- stock: số lượng (số thực). Nếu không đọc được số lượng, gán thành 0.0.
                """.trimIndent()

                val response = withTimeout(120000) {
                    model.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                }

                val raw = response.text ?: "[]"
                android.util.Log.d("IngredientScan", "Raw Gemini response: $raw")

                // B3: Parse + validate
                val parsed = parseAndValidate(raw)

                // B4: Dedup trong kết quả scan
                val deduplicated = parsed.distinctBy { it.name.lowercase().trim() }

                // B5: Đánh dấu trùng với nguyên liệu đã có trong DB
                val withDupFlag = deduplicated.map { item ->
                    val isDup = existingIngredients.any { isSimilar(it.name, item.name) }
                    item.copy(isPossibleDuplicate = isDup)
                }

                if (withDupFlag.isEmpty()) {
                    _scanState.value = ScanState.Error("Không tìm thấy nguyên liệu nào trong ảnh. Thử ảnh rõ hơn.")
                } else {
                    _scanState.value = ScanState.Success(withDupFlag)
                }

            } catch (e: Exception) {
                android.util.Log.e("IngredientScan", "Lỗi scan: ${e.message}")
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
