package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.restaurant.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.AuthState
import com.example.restaurant.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (String, String) -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(WarmBrown, WarmBrown.copy(alpha = 0.80f), CreamBG), startY = 0f, endY = 800f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            // Brand header
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(72.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Tạo tài khoản", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color.White)
            Text("Điền thông tin để đăng ký", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(28.dp))

            // Form Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Thông tin cá nhân", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = fullName, onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Họ và tên") },
                        placeholder = { Text("Nguyễn Văn A", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray, unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f)
                        )
                    )
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        placeholder = { Text("name@email.com", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray, unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f)
                        )
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mật khẩu") },
                        placeholder = { Text("ít nhất 6 ký tự", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray, unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f)
                        )
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Số điện thoại") },
                        placeholder = { Text("0xx xxx xxxx", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray, unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f)
                        )
                    )
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Địa chỉ") },
                        placeholder = { Text("Số nhà, đường, phường/xã...", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Home, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray, unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f)
                        )
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.register(email, password, fullName, phone, address) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("TẠO TÀI KHOẢN", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 2.sp)
                        }
                    }

                    if (authState is AuthState.Error) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFEBEE)) {
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Login link
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.18f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Đã có tài khoản?", color = Color(0xFF3E1E00), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onNavigateToLogin,
                        shape = RoundedCornerShape(10.dp),
                        color = WarmBrown,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            "Đăng nhập",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val successState = authState as AuthState.Success
            onRegisterSuccess(successState.token, successState.role)
        }
    }
}
