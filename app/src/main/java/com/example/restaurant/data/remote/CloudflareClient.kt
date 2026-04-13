package com.example.restaurant.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudflareClient {
    // CHÚ Ý: Cần thay the URL này bằng đường dẫn Cloudflare Worker thực tế của bạn
    private const val BASE_URL = "https://send-otp-email.vmc0886165119.workers.dev/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val instance: CloudflareApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(CloudflareApiService::class.java)
    }
}
