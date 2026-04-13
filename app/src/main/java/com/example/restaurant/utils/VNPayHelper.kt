package com.example.restaurant.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object VNPayHelper {
    const val vnp_TmnCode = "76ZQ6FFU" 
    const val vnp_HashSecret = "V6828R5P1ER686DVT7TGBG78KXAZV9W9" 
    const val vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
    const val vnp_ReturnUrl = "https://lndavid1.github.io/repay/vnpay_return.html" 

    fun generatePaymentUrl(orderId: String, amount: Double): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val vnp_CreateDate = sdf.format(Date())

        // Cấu hình tham số
        val vnp_Params = mutableMapOf<String, String>()
        vnp_Params["vnp_Version"] = "2.1.0"
        vnp_Params["vnp_Command"] = "pay"
        vnp_Params["vnp_TmnCode"] = vnp_TmnCode
        vnp_Params["vnp_Amount"] = (amount * 100).toLong().toString()
        vnp_Params["vnp_CurrCode"] = "VND"
        vnp_Params["vnp_TxnRef"] = orderId
        vnp_Params["vnp_OrderInfo"] = "Thanh toan don hang $orderId"
        vnp_Params["vnp_OrderType"] = "billpayment"
        vnp_Params["vnp_Locale"] = "vn"
        vnp_Params["vnp_ReturnUrl"] = vnp_ReturnUrl
        vnp_Params["vnp_IpAddr"] = "127.0.0.1" // IP ảo tĩnh cho app
        vnp_Params["vnp_CreateDate"] = vnp_CreateDate

        // Build query string và hash data
        val fieldNames = vnp_Params.keys.toList().sorted()
        val hashData = java.lang.StringBuilder()
        val query = java.lang.StringBuilder()

        for (fieldName in fieldNames) {
            val fieldValue = vnp_Params[fieldName]
            if (!fieldValue.isNullOrEmpty()) {
                // Build hash data
                hashData.append(fieldName)
                hashData.append('=')
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()))
                hashData.append('&')

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                query.append('=')
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()))
                query.append('&')
            }
        }
        
        // Xoá dấu '&' cuối cùng
        if (hashData.isNotEmpty()) hashData.setLength(hashData.length - 1)
        if (query.isNotEmpty()) query.setLength(query.length - 1)

        val vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString())
        query.append("&vnp_SecureHash=").append(vnp_SecureHash)

        return "$vnp_Url?${query.toString()}"
    }

    private fun hmacSHA512(key: String, data: String): String {
        return try {
            val hmac512 = Mac.getInstance("HmacSHA512")
            val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")
            hmac512.init(secretKey)
            val result = hmac512.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            val sb = java.lang.StringBuilder(2 * result.size)
            for (b in result) {
                sb.append(String.format("%02x", b.toInt() and 0xff))
            }
            sb.toString()
        } catch (ex: Exception) {
            ""
        }
    }

    fun generateQRCodeBitmap(content: String, size: Int = 512): android.graphics.Bitmap? {
        try {
            val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
            hints[com.google.zxing.EncodeHintType.MARGIN] = 1
            val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
