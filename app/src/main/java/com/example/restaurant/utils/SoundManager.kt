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

    // 2. Chống Spam Logic (Debounce)
    private var lastPlayedTime = 0L
    private const val DEBOUNCE_DELAY_MS = 2500L // 2.5 giây

    // 3. Quản lý Memory - Cache MediaPlayer instance
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Hàm phát âm thanh cốt lõi. Có đầy đủ xử lý: 
     * - Check isSoundEnabled 
     * - Check Debounce
     * - Cache MediaPlayer tái sử dụng
     */
    @Synchronized
    private fun play(context: Context) {
        if (!_isSoundEnabled.value) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPlayedTime < DEBOUNCE_DELAY_MS) {
            return // Đang spam (nhiều event kích cùng lúc), bỏ qua
        }
        lastPlayedTime = currentTime

        try {
            if (mediaPlayer == null) {
                // Sử dụng âm báo mặc định của Android Notification
                // Về sau bạn có thể đổi thành R.raw.xyz nếu có file mp3
                val defaultUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(context.applicationContext, defaultUri)
                
                mediaPlayer?.setOnCompletionListener {
                    // Cố tình KHÔNG release để cache lại dùng lần sau (Tránh khởi tạo GC churn)
                    // Thay vào đó chỉ tua về đầu
                    it.seekTo(0)
                }
            } else {
                mediaPlayer?.seekTo(0)
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup nếu xảy ra sự cố phần cứng
            release()
        }
    }

    // Các hàm Helper đóng vai trò Event-Driven 
    // Tách biệt theo Context nghiệp vụ để sau này gán file MP3 cự thể cho từng cái
    fun playNewOrderSound(context: Context) = play(context)
    fun playCallStaffSound(context: Context) = play(context)
    fun playPaymentRequestSound(context: Context) = play(context)
    fun playSuccessSound(context: Context) = play(context)
    fun playOrderCompletedSound(context: Context) = play(context)
    
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
