package com.example.restaurant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.Reservation
import com.example.restaurant.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReservationViewModel : ViewModel() {
    private val repository = ReservationRepository()

    private val _userReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val userReservations: StateFlow<List<Reservation>> = _userReservations.asStateFlow()

    private val _allReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val allReservations: StateFlow<List<Reservation>> = _allReservations.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun fetchUserReservations(userId: String) {
        viewModelScope.launch {
            repository.observeUserReservations(userId).collect {
                _userReservations.value = it
            }
        }
    }

    fun fetchAllReservations() {
        viewModelScope.launch {
            repository.observeAllReservations().collect {
                _allReservations.value = it
            }
        }
    }

    fun createReservation(reservation: Reservation, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.createReservation(reservation)
            if (success) {
                _toastMessage.emit("Đặt bàn thành công! Vui lòng chờ xác nhận.")
                onSuccess()
            } else {
                _toastMessage.emit("Có lỗi xảy ra khi đặt bàn. Vui lòng thử lại.")
            }
        }
    }

    fun updateStatus(id: String, newStatus: String) {
        viewModelScope.launch {
            val success = repository.updateReservationStatus(id, newStatus)
            if (success) {
                val statusText = when(newStatus) {
                    "confirmed" -> "Đã nhận bàn"
                    "cancelled" -> "Đã từ chối"
                    "completed" -> "Đã hoàn thành"
                    else -> "Đã cập nhật"
                }
                _toastMessage.emit("Trạng thái: $statusText")
            } else {
                _toastMessage.emit("Lỗi khi cập nhật trạng thái.")
            }
        }
    }
}
