package com.example.restaurant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.AuthState
import com.example.restaurant.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun ChangePasswordDialog(
    uid: String,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Info, 2: OTP & New Pass
    var countdown by remember { mutableStateOf(0) }
    var inputOtp by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var lockErrorMsg by remember { mutableStateOf<String?>(null) } 

    val otpState by authViewModel.otpState.collectAsState()
    val changePasswordState by authViewModel.changePasswordState.collectAsState()

    LaunchedEffect(otpState) {
        when (otpState) {
            is AuthState.Success -> {
                val otpResp = otpState as AuthState.Success
                if (otpResp.role == "otp_sent") {
                    step = 2
                    countdown = 60
                    Toast.makeText(context, "Mã OTP đã được gửi tới Email của bạn!", Toast.LENGTH_SHORT).show()
                }
            }
            is AuthState.Error -> {
                val err = (otpState as AuthState.Error).message
                if (err.contains("sai quá 5 lần") || err.contains("khóa tạm thời")) {
                    lockErrorMsg = "Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 5 phút."
                } else {
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(changePasswordState) {
        when (changePasswordState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                authViewModel.resetOtpStates()
                onDismiss()
            }
            is AuthState.Error -> {
                val err = (changePasswordState as AuthState.Error).message
                if (err.contains("sai quá 5 lần")) {
                    lockErrorMsg = "Bạn đã nhập sai quá 5 lần. Vui lòng thử lại sau 5 phút."
                } else {
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(
        onDismissRequest = {
            authViewModel.resetOtpStates()
            onDismiss()
        },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Đổi Mật Khẩu", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
                Spacer(modifier = Modifier.height(16.dp))

                if (lockErrorMsg != null) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(lockErrorMsg!!, color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            authViewModel.resetOtpStates()
                            onDismiss() 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                    ) {
                        Text("Đóng")
                    }
                } else if (step == 1) {
                    Text(
                        "Hệ thống sẽ gửi một mã OTP gồm 6 số vào địa chỉ Email liên kết của bạn để tiến hành đổi mật khẩu.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = { 
                                authViewModel.resetOtpStates()
                                onDismiss() 
                            }
                        ) { Text("Hủy", color = Color.Gray) }
                        Button(
                            onClick = { authViewModel.requestPasswordChangeOTP(uid) },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                            enabled = otpState !is AuthState.Loading
                        ) {
                            if (otpState is AuthState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Gửi OTP")
                            }
                        }
                    }
                } else {
                    Text("Nhập mã OTP", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OtpInputField(otpText = inputOtp, onOtpChange = { if (it.length <= 6) inputOtp = it })
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Nhập lại mật khẩu mới") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (countdown > 0) {
                        Text("Gửi lại sau ${countdown}s", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Text(
                            "Gửi lại mã OTP", 
                            color = WarmBrown, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                authViewModel.requestPasswordChangeOTP(uid)
                            }.padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = { 
                                authViewModel.resetOtpStates()
                                onDismiss() 
                            }
                        ) { Text("Hủy", color = Color.Gray) }
                        Button(
                            onClick = {
                                if (inputOtp.length != 6) {
                                    Toast.makeText(context, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (newPass.length < 6) {
                                    Toast.makeText(context, "Mật khẩu phải từ 6 ký tự", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (newPass != confirmPass) {
                                    Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                authViewModel.verifyOTPAndChangePassword(uid, inputOtp, newPass)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                            enabled = changePasswordState !is AuthState.Loading
                        ) {
                            if (changePasswordState is AuthState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Xác nhận")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtpInputField(otpText: String, onOtpChange: (String) -> Unit) {
    BasicTextField(
        value = otpText,
        onValueChange = onOtpChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                repeat(6) { index ->
                    val char = when {
                        index >= otpText.length -> ""
                        else -> otpText[index].toString()
                    }
                    val isFocused = otpText.length == index || (otpText.length == 6 && index == 5)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) WarmBrown else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    )
}
