package com.example.restaurant.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// ─── Loại thông báo ─────────────────────────────────────────────────────────
enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

// ─── Model dữ liệu thông báo ─────────────────────────────────────────────────
data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 2800
)

// ─── Singleton controller — gọi từ bất cứ đâu ───────────────────────────────
object AppToast {
    private val _events = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun show(message: String, type: ToastType = ToastType.INFO, durationMs: Long = 2800) {
        _events.tryEmit(ToastMessage(message, type, durationMs))
    }

    fun success(message: String) = show(message, ToastType.SUCCESS)
    fun error(message: String) = show(message, ToastType.ERROR)
    fun warning(message: String) = show(message, ToastType.WARNING)
    fun info(message: String) = show(message, ToastType.INFO)
}

// ─── Host — đặt ở root composable (MainActivity) ─────────────────────────────
@Composable
fun AppToastHost() {
    var current by remember { mutableStateOf<ToastMessage?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppToast.events.collect { msg ->
            // Nếu đang hiển thị, ẩn trước rồi show cái mới
            if (visible) {
                visible = false
                delay(200)
            }
            current = msg
            visible = true
            delay(msg.durationMs)
            visible = false
            delay(350)
            current = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            current?.let { toast ->
                ToastCard(toast)
            }
        }
    }
}

// ─── Card UI ─────────────────────────────────────────────────────────────────
@Composable
private fun ToastCard(toast: ToastMessage) {
    val config = when (toast.type) {
        ToastType.SUCCESS -> ToastConfig(
            icon = Icons.Default.CheckCircle,
            iconBg = Color(0xFF2E7D32),
            cardBg = Color(0xFF1B5E20).copy(alpha = 0.97f),
            textColor = Color.White,
            progressColor = Color(0xFF66BB6A)
        )
        ToastType.ERROR -> ToastConfig(
            icon = Icons.Default.Cancel,
            iconBg = Color(0xFFC62828),
            cardBg = Color(0xFF7F0000).copy(alpha = 0.97f),
            textColor = Color.White,
            progressColor = Color(0xFFEF5350)
        )
        ToastType.WARNING -> ToastConfig(
            icon = Icons.Default.Warning,
            iconBg = Color(0xFFE65100),
            cardBg = Color(0xFF4E342E).copy(alpha = 0.97f),
            textColor = Color.White,
            progressColor = Color(0xFFFF7043)
        )
        ToastType.INFO -> ToastConfig(
            icon = Icons.Default.Info,
            iconBg = Color(0xFF1565C0),
            cardBg = Color(0xFF0D3875).copy(alpha = 0.97f),
            textColor = Color.White,
            progressColor = Color(0xFF42A5F5)
        )
    }

    // Progress bar animation
    val progress = remember { Animatable(1f) }
    LaunchedEffect(toast) {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = toast.durationMs.toInt(), easing = LinearEasing)
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(config.cardBg, RoundedCornerShape(20.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Icon circle
                    Surface(
                        shape = CircleShape,
                        color = config.iconBg,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Message text
                    Text(
                        text = toast.message,
                        color = config.textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Progress bar countdown
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value)
                            .fillMaxHeight()
                            .background(config.progressColor, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    )
                }
            }
        }
    }
}

private data class ToastConfig(
    val icon: ImageVector,
    val iconBg: Color,
    val cardBg: Color,
    val textColor: Color,
    val progressColor: Color
)
