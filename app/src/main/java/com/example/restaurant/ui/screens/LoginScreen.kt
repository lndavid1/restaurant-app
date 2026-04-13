package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.restaurant.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.AuthState
import com.example.restaurant.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String, String) -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE) }
    var rememberMe by remember { mutableStateOf(sharedPref.getBoolean("rememberMe", false)) }

    // Auto-fill if remembered
    LaunchedEffect(Unit) {
        if (rememberMe) {
            email = sharedPref.getString("email", "") ?: ""
            password = sharedPref.getString("password", "") ?: ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WarmBrown, WarmBrown.copy(alpha = 0.85f), CreamBG),
                    startY = 0f, endY = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Brand Logo
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(88.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Restaurant", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.White, letterSpacing = 1.sp)
            Text("ICTU", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(40.dp))

            // Form Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 20.dp,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("Đăng Nhập", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF1A1A2E))
                    Text("Nhập thông tin tài khoản và mật khẩu", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))

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
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mật Khẩu") },
                        placeholder = { Text("........", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Ẩn" else "Hiện", color = WarmBrown, fontSize = 12.sp)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = CreamBG.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    // Remember Me Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = WarmBrown)
                        )
                        Text("Ghi nhớ đăng nhập", fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            if (rememberMe) {
                                sharedPref.edit()
                                    .putBoolean("rememberMe", true)
                                    .putString("email", email)
                                    .putString("password", password)
                                    .apply()
                            } else {
                                sharedPref.edit().clear().apply()
                            }
                            viewModel.login(email, password) 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("ĐĂNG NHẬP", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 2.sp)
                        }
                    }

                    if (authState is AuthState.Error) {
                        Spacer(Modifier.height(12.dp))
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

            Spacer(Modifier.height(24.dp))

            // Register row - visible on CreamBG background at the bottom of gradient
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chưa có tài khoản?",
                        color = Color(0xFF3E1E00),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onNavigateToRegister,
                        shape = RoundedCornerShape(10.dp),
                        color = WarmBrown,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            "Đăng ký ngay",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val successState = authState as AuthState.Success
            onLoginSuccess(successState.token, successState.role)
        }
    }
}
