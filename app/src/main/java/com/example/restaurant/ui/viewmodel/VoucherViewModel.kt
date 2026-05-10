package com.example.restaurant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.model.Voucher
import com.example.restaurant.data.repository.VoucherRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoucherViewModel : ViewModel() {
    private val repository = VoucherRepository()

    private val _vouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        fetchVouchers()
    }

    private fun fetchVouchers() {
        viewModelScope.launch {
            repository.observeAllVouchers().collect {
                _vouchers.value = it
            }
        }
    }

    fun createVoucher(voucher: Voucher) {
        viewModelScope.launch {
            val success = repository.createVoucher(voucher)
            if (success) {
                _toastMessage.emit("Tạo voucher thành công!")
            } else {
                _toastMessage.emit("Lỗi: Mã voucher đã tồn tại hoặc có lỗi mạng.")
            }
        }
    }

    fun updateVoucher(voucher: Voucher) {
        viewModelScope.launch {
            val success = repository.updateVoucher(voucher)
            if (success) {
                _toastMessage.emit("Cập nhật voucher thành công!")
            } else {
                _toastMessage.emit("Lỗi khi cập nhật voucher.")
            }
        }
    }

    fun incrementVoucherUsage(id: String) {
        viewModelScope.launch {
            repository.incrementVoucherUsage(id)
        }
    }

    fun deleteVoucher(id: String) {
        viewModelScope.launch {
            val success = repository.deleteVoucher(id)
            if (success) {
                _toastMessage.emit("Đã xóa voucher.")
            } else {
                _toastMessage.emit("Lỗi khi xóa voucher.")
            }
        }
    }
}
