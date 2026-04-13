package com.example.restaurant

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp

class MainApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Sử dụng tối đa 25% RAM trống cho hình ảnh
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB cho ổ đĩa
                    .build()
            }
            .respectCacheHeaders(false) // Buộc lưu cache ngay cả khi Firebase Storage không gửi header cache phù hợp
            .crossfade(true) // Hiệu ứng mờ dần giúp UI nhìn mượt và tối ưu hơn
            .build()
    }
}
