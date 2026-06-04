package com.example.restaurant.data.model

import com.google.firebase.firestore.PropertyName

data class Reservation(
    val id: String = "",
    val user_id: String = "",
    val user_name: String = "",
    val user_phone: String = "",
    val date: String = "",          // YYYY-MM-DD
    val time: String = "",          // HH:mm
    val guest_count: Int = 2,
    val note: String = "",
    val status: String = "pending", // pending | confirmed | cancelled | completed
    val created_at: Long = System.currentTimeMillis(),
    val table_id: Int? = null,
    val table_number: String? = null
)

data class NotificationMessage(
    val id: String = "",
    val user_id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "info", // info | success | warning
    val is_read: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)
