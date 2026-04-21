package com.example.restaurant.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SoundManager {
    // 1. Quản lý trạng thái Bật/Tắt âm thanh toàn cục (Toggle)
    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    fun toggleSound() {
        _isSoundEnabled.value = !_isSoundEnabled.value
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
    }

    // 2. Chống Spam Logic (Debounce theo từng loại tác vụ)
    // Mỗi loại âm thanh có bộ đếm thời gian riêng — tránh âm này chặn âm kia
    private val lastPlayedTimeMap = mutableMapOf<String, Long>()
    private const val DEBOUNCE_DELAY_MS = 2500L // 2.5 giây

    // 3. Quản lý Memory - Cache MediaPlayer instance
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Hàm phát âm thanh cốt lõi. Có đầy đủ xử lý:
     * - Check isSoundEnabled
     * - Check Debounce độc lập theo [soundKey] (không bị chặn giữa các tác vụ khác nhau)
     * - Cache MediaPlayer tái sử dụng
     */
    @Synchronized
    private fun play(context: Context, soundKey: String) {
        if (!_isSoundEnabled.value) return

        val currentTime = System.currentTimeMillis()
        val lastPlayed = lastPlayedTimeMap[soundKey] ?: 0L
        if (currentTime - lastPlayed < DEBOUNCE_DELAY_MS) {
            return // Cùng loại sự kiện spam liên tiếp → bỏ qua
        }
        lastPlayedTimeMap[soundKey] = currentTime

        try {
            // Luôn release và tạo mới để tránh IllegalStateException khi player ở trạng thái lỗi
            release()
            val defaultUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context.applicationContext, defaultUri)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            release()
        }
    }

    // Các hàm Helper đóng vai trò Event-Driven
    // Mỗi hàm truyền soundKey riêng → debounce độc lập, không chặn lẫn nhau
    fun playNewOrderSound(context: Context) = play(context, "new_order")
    fun playCallStaffSound(context: Context) = play(context, "call_staff")
    fun playPaymentRequestSound(context: Context) = play(context, "payment_request")
    fun playSuccessSound(context: Context) = play(context, "success")
    fun playOrderCompletedSound(context: Context) = play(context, "order_completed")
    
    // Giải phóng triệt để khi Destroy MainActivity
    @Synchronized
    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { e.printStackTrace() }
        mediaPlayer = null
    }
}
