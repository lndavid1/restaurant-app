package com.example.restaurant.data.repository

import android.util.Log
import com.example.restaurant.data.model.Voucher
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VoucherRepository {
    private val db = FirebaseFirestore.getInstance()
    private val vouchersCollection = db.collection("vouchers")

    fun observeAllVouchers(): Flow<List<Voucher>> = callbackFlow {
        val listener = vouchersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VoucherRepo", "Listen failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val vouchers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Voucher::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.valid_until }
                    trySend(vouchers)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun createVoucher(voucher: Voucher): Boolean {
        return try {
            // Check if code exists
            val existing = vouchersCollection.whereEqualTo("code", voucher.code).get().await()
            if (!existing.isEmpty) {
                return false // Code exists
            }
            vouchersCollection.add(voucher).await()
            true
        } catch (e: Exception) {
            Log.e("VoucherRepo", "Error creating voucher", e)
            false
        }
    }

    suspend fun updateVoucher(voucher: Voucher): Boolean {
        return try {
            vouchersCollection.document(voucher.id).set(voucher).await()
            true
        } catch (e: Exception) {
            Log.e("VoucherRepo", "Error updating voucher", e)
            false
        }
    }

    suspend fun incrementVoucherUsage(id: String): Boolean {
        return try {
            vouchersCollection.document(id).update("times_used", com.google.firebase.firestore.FieldValue.increment(1)).await()
            true
        } catch (e: Exception) {
            Log.e("VoucherRepo", "Error incrementing voucher usage", e)
            false
        }
    }

    suspend fun deleteVoucher(id: String): Boolean {
        return try {
            vouchersCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            Log.e("VoucherRepo", "Error deleting voucher", e)
            false
        }
    }
}
