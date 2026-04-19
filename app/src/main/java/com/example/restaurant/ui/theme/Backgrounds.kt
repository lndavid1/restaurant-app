package com.example.restaurant.ui.theme

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import java.util.Random

// Global cached noise bitmap for performance
private var cachedNoiseBitmap: ImageBitmap? = null

private fun getNoiseBitmap(): ImageBitmap {
    if (cachedNoiseBitmap != null) return cachedNoiseBitmap!!

    val size = 256
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val random = Random()
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        val v = random.nextInt(256)
        // Very low alpha (approx 4-5%) for subtle noise to avoid overpowering the gradient
        val a = 12
        pixels[i] = AndroidColor.argb(a, v, v, v)
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    cachedNoiseBitmap = bitmap.asImageBitmap()
    return cachedNoiseBitmap!!
}

/**
 * Premium background modifier with a soft gradient and a subtle noise overlay.
 * Inspired by iOS design vibes to prevent color banding and give a frosted, premium feel.
 */
fun Modifier.premiumBackground(): Modifier = composed {
    val noiseBrush = remember {
        ShaderBrush(ImageShader(getNoiseBitmap(), TileMode.Repeated, TileMode.Repeated))
    }

    this.drawBehind {
        // 1. Soft, warm premium gradient
        val gradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFFF5D6B6), // Soft peach/orange
                Color(0xFFF4EDE5), // Creamy center
                Color(0xFFDDE3E1)  // Cool light grayish blue
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        drawRect(brush = gradient)

        // 2. Noise texture overlay
        drawRect(brush = noiseBrush)
    }
}
