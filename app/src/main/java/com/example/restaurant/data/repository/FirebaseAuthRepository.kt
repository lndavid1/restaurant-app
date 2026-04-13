package com.example.restaurant.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import com.example.restaurant.utils.SecurityUtils
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<Pair<String, String>> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                val document = firestore.collection("users").document(user.uid).get().await()
                val role = document.getString("role") ?: "customer"
                Result.success(Pair(user.uid, role))
            } else {
                Result.failure(Exception("Đăng nhập thất bại: Không tìm thấy User"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi đăng nhập: ${e.localizedMessage}"))
        }
    }

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        address: String
    ): Result<Pair<String, String>> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                val userData = hashMapOf(
                    "email" to email,
                    "fullName" to fullName,
                    "phone" to phone,
                    "address" to address,
                    "role" to "customer",
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userData).await()
                Result.success(Pair(user.uid, "customer"))
            } else {
                Result.failure(Exception("Đăng ký thất bại: Không thể tạo User"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi đăng ký: ${e.localizedMessage}"))
        }
    }

    suspend fun getUserProfile(uid: String): Result<Map<String, Any>> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.failure(Exception("Không tìm thấy thông tin User"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi lấy thông tin: ${e.localizedMessage}"))
        }
    }

    suspend fun updateUserProfile(uid: String, fullName: String, phone: String, address: String, avatarUri: Uri? = null): Result<Boolean> {
        return try {
            var avatarUrl: String? = null
            if (avatarUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")
                storageRef.putFile(avatarUri).await()
                avatarUrl = storageRef.downloadUrl.await().toString()
            }
            
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName,
                "phone" to phone,
                "address" to address
            )
            if (avatarUrl != null) {
                updates["avatarUrl"] = avatarUrl
            }
            firestore.collection("users").document(uid).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi cập nhật: ${e.localizedMessage}"))
        }
    }

    suspend fun requestPasswordChangeOTP(uid: String): Result<String> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (!document.exists()) {
                return Result.failure(Exception("Không tìm thấy thông tin tài khoản"))
            }

            val email = document.getString("email") ?: ""
            if (email.isEmpty()) {
                return Result.failure(Exception("Tài khoản chưa cập nhật Email. Vui lòng cập nhật trước."))
            }

            val otpRef = firestore.collection("otp_recovery").document(email)
            val otpDoc = otpRef.get().await()
            val now = System.currentTimeMillis()

            if (otpDoc.exists()) {
                val lockUntil = otpDoc.getLong("lockUntil") ?: 0L
                if (now < lockUntil) {
                    return Result.failure(Exception("Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 5 phút"))
                }

                val lastSentAt = otpDoc.getLong("lastSentAt") ?: 0L
                if (now - lastSentAt < 60000) {
                    val retryAfter = 60 - (now - lastSentAt) / 1000
                    return Result.failure(Exception("Vui lòng đợi $retryAfter giây để gửi lại OTP."))
                }
            }

            val plainOtp = SecurityUtils.generateOTP()
            val hashedOtp = SecurityUtils.hashSHA256(plainOtp)

            val otpData = mapOf(
                "hashedOtp" to hashedOtp,
                "expiresAt" to now + 3 * 60 * 1000,
                "attempts" to 0,
                "lockUntil" to 0L,
                "lastSentAt" to now
            )
            otpRef.set(otpData).await()
            
            // Call Cloudflare Worker to send Email
            val mailResp = com.example.restaurant.data.remote.CloudflareClient.instance.sendEmailOTP(
                com.example.restaurant.data.remote.EmailOtpRequest(email, plainOtp)
            )
            
            if (mailResp.isSuccessful) {
                Result.success("Sent")
            } else {
                otpRef.delete().await() // Rollback if email failed to send
                val errorDetails = mailResp.errorBody()?.string() ?: ""
                Result.failure(Exception("Không thể gửi thư qua Cloudflare: ${mailResp.code()} - $errorDetails"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi gửi OTP: ${e.localizedMessage}"))
        }
    }

    suspend fun verifyOTPAndChangePassword(uid: String, inputOtp: String, newPass: String): Result<Boolean> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            val email = document.getString("email") ?: ""
            if (email.isEmpty()) return Result.failure(Exception("Không tìm thấy Email."))

            val otpRef = firestore.collection("otp_recovery").document(email)
            val otpDoc = otpRef.get().await()
            val now = System.currentTimeMillis()

            if (!otpDoc.exists()) {
                return Result.failure(Exception("Mã OTP không tồn tại."))
            }

            val lockUntil = otpDoc.getLong("lockUntil") ?: 0L
            if (now < lockUntil) {
                return Result.failure(Exception("Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 5 phút"))
            }

            val expiresAt = otpDoc.getLong("expiresAt") ?: 0L
            if (now > expiresAt) {
                return Result.failure(Exception("Mã OTP đã hết hạn."))
            }

            val hashedOtp = otpDoc.getString("hashedOtp") ?: ""
            val inputHash = SecurityUtils.hashSHA256(inputOtp)

            var attempts = otpDoc.getLong("attempts")?.toInt() ?: 0

            if (inputHash != hashedOtp) {
                attempts++
                if (attempts >= 5) {
                    otpRef.update(
                        mapOf(
                            "attempts" to attempts,
                            "lockUntil" to now + 5 * 60 * 1000
                        )
                    ).await()
                    return Result.failure(Exception("Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 5 phút"))
                } else {
                    otpRef.update("attempts", attempts).await()
                    return Result.failure(Exception("Mã OTP không đúng. Bạn còn ${5 - attempts} lần thử."))
                }
            }

            otpRef.delete().await()

            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.uid == uid) {
                currentUser.updatePassword(newPass).await()
            } else {
                return Result.failure(Exception("Chưa đăng nhập đúng tài khoản."))
            }

            Result.success(true)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("CREDENTIAL_TOO_OLD_LOGIN_AGAIN")) {
                Result.failure(Exception("Phiên đăng nhập đã quá cũ. Vui lòng đăng xuất và đăng nhập lại để đổi mật khẩu."))
            } else {
                Result.failure(Exception("Lỗi vô hiệu: $msg"))
            }
        }
    }
}
