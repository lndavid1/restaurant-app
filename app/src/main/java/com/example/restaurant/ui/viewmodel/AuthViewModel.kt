package com.example.restaurant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurant.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String, val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = FirebaseAuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            result.onSuccess { (uid, role) ->
                _authState.value = AuthState.Success(uid, role)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun register(email: String, password: String, fullName: String, phone: String, address: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(email, password, fullName, phone, address)
            result.onSuccess { (uid, role) ->
                _authState.value = AuthState.Success(uid, role)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }

    private val _userProfile = MutableStateFlow<Map<String, Any>?>(null)
    val userProfile: StateFlow<Map<String, Any>?> = _userProfile

    private val _updateProfileState = MutableStateFlow<AuthState>(AuthState.Idle)
    val updateProfileState: StateFlow<AuthState> = _updateProfileState

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            val result = repository.getUserProfile(uid)
            result.onSuccess { data ->
                _userProfile.value = data
            }
        }
    }

    fun updateUserProfile(uid: String, fullName: String, phone: String, address: String, avatarUri: android.net.Uri? = null) {
        viewModelScope.launch {
            _updateProfileState.value = AuthState.Loading
            val result = repository.updateUserProfile(uid, fullName, phone, address, avatarUri)
            result.onSuccess {
                _updateProfileState.value = AuthState.Success(uid, "updated")
                // Refetch to refresh UI
                loadUserProfile(uid)
            }.onFailure { e ->
                _updateProfileState.value = AuthState.Error(e.message ?: "Update failed")
            }
        }
    }

    private val _otpState = MutableStateFlow<AuthState>(AuthState.Idle)
    val otpState: StateFlow<AuthState> = _otpState
    
    private val _changePasswordState = MutableStateFlow<AuthState>(AuthState.Idle)
    val changePasswordState: StateFlow<AuthState> = _changePasswordState

    fun requestPasswordChangeOTP(uid: String) {
        viewModelScope.launch {
            _otpState.value = AuthState.Loading
            val result = repository.requestPasswordChangeOTP(uid)
            result.onSuccess { plainOtp ->
                _otpState.value = AuthState.Success(plainOtp, "otp_sent")
            }.onFailure { e ->
                _otpState.value = AuthState.Error(e.message ?: "Lỗi gửi OTP")
            }
        }
    }

    fun verifyOTPAndChangePassword(uid: String, inputOtp: String, newPass: String) {
        viewModelScope.launch {
            _changePasswordState.value = AuthState.Loading
            val result = repository.verifyOTPAndChangePassword(uid, inputOtp, newPass)
            result.onSuccess {
                _changePasswordState.value = AuthState.Success(uid, "password_changed")
            }.onFailure { e ->
                _changePasswordState.value = AuthState.Error(e.message ?: "Lỗi đổi mật khẩu")
            }
        }
    }

    fun resetOtpStates() {
        _otpState.value = AuthState.Idle
        _changePasswordState.value = AuthState.Idle
    }
}
