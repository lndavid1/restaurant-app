package com.example.restaurant.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ── Request/Response cho PayOS Worker ────────────────────────────────────────
data class PayOSCreateRequest(
    val order_id: Int,
    val amount: Long
)

data class PayOSCreateResponse(
    val status: String = "",
    val checkout_url: String? = null,
    val order_code: Long? = null,
    val error: String? = null
)

data class PayOSStatusResponse(
    val paid: Boolean = false,
    val order_code: String? = null
)

/**
 * Retrofit interface trỏ tới Cloudflare Worker payos-payment.
 * Base URL được cấu hình trong PayOSWorkerClient.
 */
interface PayOSWorkerService {

    /** Tạo link thanh toán PayOS */
    @POST("create")
    suspend fun createPayment(
        @Body request: PayOSCreateRequest
    ): Response<PayOSCreateResponse>

    /** Polling: kiểm tra xem orderCode đã thanh toán chưa (Worker đọc từ KV) */
    @GET("status")
    suspend fun checkStatus(
        @Query("order_code") orderCode: Long
    ): Response<PayOSStatusResponse>
}
