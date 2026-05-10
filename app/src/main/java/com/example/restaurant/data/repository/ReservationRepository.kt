package com.example.restaurant.data.repository

import android.util.Log
import com.example.restaurant.data.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReservationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reservationsCollection = db.collection("reservations")

    fun observeUserReservations(userId: String): Flow<List<Reservation>> = callbackFlow {
        val listener = reservationsCollection
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReservationRepo", "Listen failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val reservations = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.created_at }
                    trySend(reservations)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeAllReservations(): Flow<List<Reservation>> = callbackFlow {
        val listener = reservationsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReservationRepo", "Listen failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val reservations = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.created_at }
                    trySend(reservations)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun createReservation(reservation: Reservation): Boolean {
        return try {
            reservationsCollection.add(reservation).await()
            true
        } catch (e: Exception) {
            Log.e("ReservationRepo", "Error creating reservation", e)
            false
        }
    }

    suspend fun updateReservationStatus(id: String, status: String): Boolean {
        return try {
            reservationsCollection.document(id).update("status", status).await()
            true
        } catch (e: Exception) {
            Log.e("ReservationRepo", "Error updating reservation", e)
            false
        }
    }
}
