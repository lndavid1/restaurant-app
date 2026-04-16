package com.example.restaurant.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.DailyRevenue
import com.example.restaurant.data.model.Order
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.restaurant.utils.toVndFormat

data class AIInsightState(
    val isLoading: Boolean = false,
    val content: String = "",
    val error: String? = null
)

class AdminAnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val _insightState = MutableStateFlow(AIInsightState())
    val insightState: StateFlow<AIInsightState> = _insightState.asStateFlow()

    private val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
        modelName = "gemini-2.5-flash",
        systemInstruction = content {
            text("""
                Bạn là Giám đốc Vận hành (COO) chuyên nghiệp của hệ thống nhà hàng. 
                BẮT BUỘC TRẢ VỀ DUY NHẤT MỘT PHẢN HỒI JSON HỢP LỆ VỚI CẤU TRÚC SAU (không dùng markdown code blocks như ```json):
                {
                  "summary": ["Doanh thu bình quân...", "Ngày cao nhất..."],
                  "patterns": ["Thứ 7 khách tăng vọt", "Ngày rớt khách..."],
                  "actions": ["Tăng tồn kho món A...", "Chạy Flash Sale món B..."]
                }
                Quy tắc viêt Insight:
                - Khẩu quyết: "Nói nhanh, nói gắt, nói trúng". 
                - 1 insight = 1 ý ngắn gọn, súc tích. Không dùng văn xuôi dài dòng.
                - Báo cáo số tiền VÀO TRỰC TIẾP insight. Dữ liệu tôi gửi đã được format chuẩn VND (VD: 5,000,000 VND), BẠN KHÔNG ĐƯỢC TỰ Ý ĐỔI TỶ GIÁ THÀNH VÀI TỶ HOẶC VÀI TRĂM TRIỆU.
            """.trimIndent())
        },
        generationConfig = generationConfig {
            temperature = 0.2f // Giảm gắt nhiệt độ để tránh AI ngẫu hứng hỏng form JSON
            responseMimeType = "application/json" // Ép cứng JSON format
        }
    )

    fun generateInsights(orders: List<Order>, revenues: List<DailyRevenue>) {
        if (_insightState.value.isLoading) return
        _insightState.value = AIInsightState(isLoading = true)

        viewModelScope.launch {
            try {
                // Tiền xử lý dữ liệu giảm token payload
                val revenueMap = revenues.sortedByDescending { it.date }.take(7).associate { it.date to it.revenue }
                var totalRevenue = 0.0
                revenueMap.values.forEach { totalRevenue += it }
                
                // Format tiền chuẩn VND ngay từ đầu
                val formattedTotal = totalRevenue.toVndFormat() + " VND"
                val formattedRevenueMap = revenueMap.mapValues { it.value.toVndFormat() + " VND" }

                // Tính toán tần suất sản phẩm để ra Best seller/Worst seller
                val productCountMap = mutableMapOf<String, Int>()
                orders.forEach { order ->
                    order.items_detail?.forEach { item ->
                        productCountMap[item.name] = (productCountMap[item.name] ?: 0) + item.quantity
                    }
                }

                val sortedProducts = productCountMap.entries.sortedByDescending { it.value }
                val topProducts = sortedProducts.take(3).map { "${it.key}: ${it.value} lượt" }
                val slowProducts = sortedProducts.takeLast(3).map { "${it.key}: ${it.value} lượt" }

                val dataContext = """
                    [DỮ LIỆU ĐẦU VÀO TỒN ĐỌNG 7 NGÀY QUA]
                    - Tổng doanh thu 7 ngày: $formattedTotal
                    - Doanh thu theo ngày: $formattedRevenueMap
                    - 3 Món bán lướt sóng (Top): $topProducts
                    - 3 Món gặm nhấm kho (Bottom): $slowProducts
                    
                    YÊU CẦU: Nhìn vào bảng dữ liệu này, hãy khai hỏa khối óc COO và xuất JSON ngay!
                """.trimIndent()

                val response = generativeModel.generateContent(dataContext)
                val responseText = response.text ?: "Lỗi ngầm: Không nhận được chữ nào từ AI."

                _insightState.value = AIInsightState(isLoading = false, content = responseText)
            } catch (e: Exception) {
                e.printStackTrace()
                _insightState.value = AIInsightState(isLoading = false, error = e.localizedMessage ?: "Kết nối AI quá hạn")
            }
        }
    }
    
    fun dismissError() {
        _insightState.value = _insightState.value.copy(error = null)
    }
}
