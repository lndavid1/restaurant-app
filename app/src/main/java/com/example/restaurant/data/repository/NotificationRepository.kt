package com.example.restaurant.data.repository

import android.util.Log
import com.example.restaurant.data.model.NotificationMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    fun observeUserNotifications(userId: String): Flow<List<NotificationMessage>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Listen failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NotificationMessage::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.created_at }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun createNotification(notification: NotificationMessage): Boolean {
        return try {
            val docRef = if (notification.id.isEmpty()) notificationsCollection.document() else notificationsCollection.document(notification.id)
            val newNotification = notification.copy(id = docRef.id)
            docRef.set(newNotification).await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error creating notification", e)
            false
        }
    }

    suspend fun markAsRead(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId).update("is_read", true).await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error updating notification", e)
            false
        }
    }

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error deleting notification", e)
            false
        }
    }

    suspend fun deleteAllUserNotifications(userId: String): Boolean {
        return try {
            val snapshot = notificationsCollection.whereEqualTo("user_id", userId).get().await()
            db.runBatch { batch ->
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error deleting all notifications", e)
            false
        }
    }
}
