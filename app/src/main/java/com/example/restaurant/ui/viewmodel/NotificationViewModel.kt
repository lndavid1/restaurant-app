package com.example.restaurant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.NotificationMessage
import com.example.restaurant.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<NotificationMessage>>(emptyList())
    val notifications: StateFlow<List<NotificationMessage>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun fetchUserNotifications(userId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            repository.observeUserNotifications(userId).collect { list ->
                _notifications.value = list
                _unreadCount.value = list.count { !it.is_read }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun sendNotification(userId: String, title: String, body: String, type: String = "info") {
        viewModelScope.launch {
            val notif = NotificationMessage(
                user_id = userId,
                title = title,
                body = body,
                type = type
            )
            repository.createNotification(notif)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    fun deleteAllNotifications(userId: String) {
        viewModelScope.launch {
            repository.deleteAllUserNotifications(userId)
        }
    }
}
