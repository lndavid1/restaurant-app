package com.example.restaurant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.restaurant.R
import com.example.restaurant.ui.theme.WarmBrown
import kotlin.math.roundToInt

/**
 * FAB kéo-thả tự do trên màn hình.
 * - Không bị che bởi system navbar (dùng navigationBarsPadding + padding bottom).
 * - Cho phép người dùng kéo thả đến bất kỳ vị trí nào.
 * - Mặc định xuất hiện ở góc dưới-phải, cách bottom 24dp, cách right 16dp.
 *
 * Cách dùng: Đặt DraggableChatbotFab() trực tiếp trong Box cha bao ngoài Scaffold,
 * với alignment = Alignment.BottomEnd, KHÔNG dùng floatingActionButton slot của Scaffold.
 */
@Composable
fun DraggableChatbotFab(onClick: () -> Unit) {
    val density = LocalDensity.current

    // Offset kéo thả tích lũy (tính bằng px)
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Kích thước container để giới hạn kéo không ra ngoài màn hình
    var containerWidthPx by remember { mutableStateOf(0) }
    var containerHeightPx by remember { mutableStateOf(0) }

    val fabSizePx = with(density) { 64.dp.toPx() }
    val defaultPaddingPx = with(density) { 16.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 90.dp) // tránh bottom nav pill
            .onGloballyPositioned { coords ->
                containerWidthPx = coords.size.width
                containerHeightPx = coords.size.height
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = dragOffsetX.roundToInt(),
                        y = dragOffsetY.roundToInt()
                    )
                }
                .size(64.dp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val newX = dragOffsetX + dragAmount.x
                        val newY = dragOffsetY + dragAmount.y

                        // Giới hạn ngang: không ra ngoài trái/phải
                        val minX = -(containerWidthPx - fabSizePx - defaultPaddingPx)
                        val maxX = defaultPaddingPx

                        // Giới hạn dọc: không ra ngoài trên/dưới
                        val minY = -(containerHeightPx - fabSizePx - defaultPaddingPx)
                        val maxY = defaultPaddingPx

                        dragOffsetX = newX.coerceIn(minX, maxX)
                        dragOffsetY = newY.coerceIn(minY, maxY)
                    }
                }
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Trợ lý AI",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
