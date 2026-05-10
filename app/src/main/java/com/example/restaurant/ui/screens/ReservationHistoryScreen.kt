package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusRed
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.ReservationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationHistoryScreen(
    token: String,
    viewModel: ReservationViewModel,
    onNavigateBack: () -> Unit
) {
    val userReservations by viewModel.userReservations.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchUserReservations(token)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đặt bàn", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBG,
                    titleContentColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = CreamBG
    ) { padding ->
        if (userReservations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("Bạn chưa có lịch sử đặt bàn nào.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(userReservations) { reservation ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Khách: ${reservation.user_name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                
                                val statusColor = when (reservation.status) {
                                    "pending" -> StatusYellow
                                    "confirmed" -> StatusGreen
                                    "completed" -> Color.Gray
                                    "cancelled" -> StatusRed
                                    else -> Color.Gray
                                }
                                val statusText = when (reservation.status) {
                                    "pending" -> "Chờ duyệt"
                                    "confirmed" -> "Đã nhận"
                                    "completed" -> "Hoàn thành"
                                    "cancelled" -> "Đã hủy"
                                    else -> reservation.status
                                }

                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(reservation.date, fontSize = 14.sp, color = Color.DarkGray)
                                
                                Spacer(Modifier.width(16.dp))
                                
                                Icon(Icons.Default.Schedule, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(reservation.time, fontSize = 14.sp, color = Color.DarkGray)
                                
                                Spacer(Modifier.width(16.dp))
                                
                                Icon(Icons.Default.People, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${reservation.guest_count} người", fontSize = 14.sp, color = Color.DarkGray)
                            }

                            if (reservation.note.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Ghi chú: ${reservation.note}", fontSize = 13.sp, color = Color.Gray)
                            }
                            
                            if (reservation.status == "pending") {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { viewModel.updateStatus(reservation.id, "cancelled") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                                ) {
                                    Text("Hủy đặt bàn")
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
