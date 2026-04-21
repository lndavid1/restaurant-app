package com.example.restaurant.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.Product
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _addToCartEvents = MutableSharedFlow<Int>()
    val addToCartEvents: SharedFlow<Int> = _addToCartEvents.asSharedFlow()

    private val _placeOrderEvents = MutableSharedFlow<Unit>()
    val placeOrderEvents: SharedFlow<Unit> = _placeOrderEvents.asSharedFlow()

    private var generativeModel: GenerativeModel? = null
    private var isInitialized = false

    private val sharedPreferences = application.getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var currentUserToken: String = ""

    fun initializeUser(token: String?) {
        val newToken = token ?: "guest"
        if (currentUserToken != newToken) {
            currentUserToken = newToken
            isInitialized = false
            loadMessages()
        }
    }

    private fun saveMessages(messagesToSave: List<ChatMessage>) {
        if (currentUserToken.isEmpty() || currentUserToken == "guest") return
        val key = "chat_history_$currentUserToken"
        val json = gson.toJson(messagesToSave)
        sharedPreferences.edit().putString(key, json).apply()
    }

    private fun loadMessages() {
        if (currentUserToken.isEmpty() || currentUserToken == "guest") {
            _messages.value = emptyList()
            return
        }
        val key = "chat_history_$currentUserToken"
        val json = sharedPreferences.getString(key, null)
        if (json != null) {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            val loadedMessages: List<ChatMessage> = gson.fromJson(json, type)
            _messages.value = loadedMessages
        } else {
            _messages.value = emptyList()
        }
    }

    fun startChat(products: List<Product>) {
        if (currentUserToken.isEmpty() || isInitialized) return
        isInitialized = true

        val menuString = products.joinToString(separator = "\n") { 
             "- ${it.name} (ID: ${it.id}): ${it.price} VND (${it.description ?: "Không có mô tả"}) - Tình trạng: ${if(it.is_available == 1) "Còn hàng" else "Hết hàng"}"
        }

        val systemContext = """
            Bạn là một trợ lý ảo ẩm thực vô cùng thông minh, năng động và mang đậm phong cách Gen Z.
            Nhiệm vụ của bạn:
            1. Trả lời đầy đủ, chi tiết nhưng sử dụng ngôn ngữ linh hoạt, trendy, vui vẻ (dùng slang Gen Z một tự nhiên như: keo lỳ, slay, xịn xò, chê, 10 điểm không có nhưng...).
            2. Tư vấn món ăn thật khéo léo dựa trên sở thích và trạng thái của khách hàng. Tuyệt đối KHÔNG gợi ý các món đã hết hàng.
            3. QUAN TRỌNG: Nếu bạn gợi ý một món ăn, hãy tự nhiên hỏi khách xem họ có muốn thêm món đó vào giỏ hàng luôn không.
            4. TỰ ĐỘNG THÊM GIỎ HÀNG: Khi khách hàng đồng ý thêm món ăn (ví dụ nói "ok", "thêm đi", "chốt", hoặc chủ động gọi món), bạn BẮT BUỘC chèn một đánh dấu ẩn vào cuối câu trả lời với cú pháp: [ADD_TO_CART: ID_món_ăn] (ví dụ: [ADD_TO_CART: 5]). Nếu nhiều món thì chèn nhiều tag.
            5. GỬI ĐƠN CHO BẾP: Khi khách hàng đã gọi xong và yêu cầu gửi/chốt đơn xuống bếp (ví dụ: "gửi đơn đi", "mang lên nhé", "lên món"), bạn HÃY chèn đánh dấu ẩn với cú pháp: [PLACE_ORDER]. Nhớ nhắc khách thư giãn chờ món trong giây lát!
        """.trimIndent()

        generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
            modelName = "gemini-2.5-flash",
            systemInstruction = content { 
                text("$systemContext\nDưới đây là danh sách thực đơn:\n$menuString") 
            },
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
        
        if (_messages.value.isEmpty()) {
            val initialMessage = listOf(ChatMessage("Helu bạn! Mình là trợ lý ăn uống hệ Gen Z siêu slay đây. Hôm nay thèm gì cứ tâm sự để mình gợi ý nha, chốt đơn nhanh gọn lẹ lun nè!", isUser = false))
            _messages.value = initialMessage
            saveMessages(initialMessage)
        }
    }

    fun sendMessage(userText: String, hasActiveTable: Boolean = true) {
        if (userText.isBlank()) return

        val newMessages = _messages.value.toMutableList()
        newMessages.add(ChatMessage(userText, isUser = true))
        newMessages.add(ChatMessage("", isUser = false, isLoading = true))
        _messages.value = newMessages
        saveMessages(newMessages)

        // Trích xuất 4 tin nhắn gần nhất (không tính tin đang loading) để tiết kiệm ngữ cảnh
        val validHistory = _messages.value.filter { !it.isLoading && !it.isError }.dropLast(1)
        val slidingWindow = validHistory.takeLast(4)
        val historyContent = slidingWindow.map { msg ->
            content(role = if (msg.isUser) "user" else "model") { text(msg.text) }
        }

        viewModelScope.launch {
            try {
                // Khởi tạo một phiên nháp (chat session rỗng hoặc ngắn) chỉ bao gồm sliding window
                val currentChat = generativeModel?.startChat(historyContent)
                
                val augmentedUserText = if (hasActiveTable) {
                    "[Hệ thống: Khách hàng ĐÃ có bàn. Hãy tiếp tục tư vấn, chốt đơn tự động (dùng [ADD_TO_CART: id]) và gửi bếp (dùng [PLACE_ORDER]) nếu khách yêu cầu.]\nNgười dùng nói: $userText"
                } else {
                    "[Hệ thống: Khách hàng CHƯA có bàn. CẤM dùng tag [ADD_TO_CART] và [PLACE_ORDER]. Nhắc khách hàng vui lòng thoát ra chọn bàn trước rồi mới được gọi món!]\nNgười dùng nói: $userText"
                }
                
                val response = currentChat?.sendMessage(augmentedUserText)
                
                val rawResponse = response?.text ?: "Xin lỗi, mình không thể trả lời."
                
                // Parse ADD_TO_CART tags
                val cartTagRegex = Regex("\\[ADD_TO_CART:\\s*(\\d+)\\]")
                val matches = cartTagRegex.findAll(rawResponse)
                
                for (match in matches) {
                    match.groupValues[1].toIntOrNull()?.let { productId ->
                        _addToCartEvents.emit(productId)
                    }
                }

                // Parse PLACE_ORDER tag
                val placeOrderRegex = Regex("\\[PLACE_ORDER\\]")
                if (placeOrderRegex.containsMatchIn(rawResponse)) {
                    _placeOrderEvents.emit(Unit)
                }
                
                val finalDisplayText = rawResponse.replace(cartTagRegex, "").replace(placeOrderRegex, "").trim()
                
                val updatedMessages = _messages.value.toMutableList()
                val loadingIndex = updatedMessages.indexOfLast { it.isLoading }
                if (loadingIndex != -1) {
                    updatedMessages[loadingIndex] = ChatMessage(finalDisplayText, isUser = false)
                }
                _messages.value = updatedMessages
                saveMessages(updatedMessages)
            } catch (e: Exception) {
                e.printStackTrace()
                val updatedMessages = _messages.value.toMutableList()
                val loadingIndex = updatedMessages.indexOfLast { it.isLoading }
                if (loadingIndex != -1) {
                    updatedMessages[loadingIndex] = ChatMessage("Mạng có vấn đề hoặc cấu hình Vertex AI lỗi: ${e.message}", isUser = false, isError = true)
                }
                _messages.value = updatedMessages
                saveMessages(updatedMessages)
            }
        }
    }
}
