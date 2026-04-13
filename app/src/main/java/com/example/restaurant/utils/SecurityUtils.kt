package com.example.restaurant.utils

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Mã hóa chuỗi đầu vào theo thuật toán SHA-256 dạng Hex.
     */
    fun hashSHA256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Sinh tự động 6 chữ số ngẫu nhiên.
     */
    fun generateOTP(): String {
        val otp = (100000..999999).random()
        return otp.toString()
    }
}
