package com.example.restaurant.data.remote

import com.example.restaurant.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    /**
     * DEBUG  → 10.0.2.2 là IP localhost dành riêng cho Android Emulator
     * RELEASE → Thay bằng URL server thật của bạn (VD: https://api.yourrestaurant.com/backend/)
     *
     * ⚠️ QUAN TRỌNG: Trước khi build release, hãy cập nhật RELEASE_URL bên dưới!
     */
    private const val DEBUG_URL   = "http://10.0.2.2/backend/"
    private const val RELEASE_URL = "http://10.0.2.2/backend/" // TODO: đổi thành URL server thật

    private val BASE_URL get() = if (BuildConfig.DEBUG) DEBUG_URL else RELEASE_URL

    private val logging = HttpLoggingInterceptor().apply {
        // Chỉ log request/response trong debug — tắt trên release để bảo mật và tăng tốc
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)   // Tránh treo app khi server chậm
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: RestaurantApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(RestaurantApiService::class.java)
    }
}
