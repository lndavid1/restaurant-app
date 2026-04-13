package com.example.restaurant.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PayOSWorkerClient {
    /**
     * *** THAY URL NÀY sau khi deploy Worker ***
     * Sau khi chạy: wrangler deploy
     * URL sẽ có dạng: https://payos-payment.<ten-tai-khoan>.workers.dev/
     */
    private const val BASE_URL = "https://payos-payment.vmc0886165119.workers.dev/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val instance: PayOSWorkerService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(PayOSWorkerService::class.java)
    }
}

