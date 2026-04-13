package com.example.restaurant.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class EmailOtpRequest(
    val email: String,
    val otp: String
)

interface CloudflareApiService {
    @POST("/")
    suspend fun sendEmailOTP(@Body request: EmailOtpRequest): Response<Map<String, Any>>
}
