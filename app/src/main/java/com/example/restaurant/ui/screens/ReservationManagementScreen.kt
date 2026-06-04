package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Reservation
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusRed
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.ReservationViewModel
import com.example.restaurant.ui.viewmodel.RestaurantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationManagementScreen(
    reservationViewModel: ReservationViewModel,
    restaurantViewModel: RestaurantViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chờ duyệt", "Đã nhận", "Lịch sử")
    var searchQuery by remember { mutableStateOf("") }
    
    val allReservations by reservationViewModel.allReservations.collectAsState()
    val tables by restaurantViewModel.tables.collectAsState()
    val notificationViewModel: com.example.restaurant.ui.viewmodel.NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    var showTableDialog by remember { mutableStateOf(false) }
    var selectedReservationForTable by remember { mutableStateOf<Reservation?>(null) }

    LaunchedEffect(Unit) {
        reservationViewModel.fetchAllReservations()
        // Dùng token giả hoặc không cần nếu repository observeTables đã lưu cache, nhưng nên fetch để lấy ds bàn mới nhất
        // Trong trường hợp Admin, có thể gọi một hàm không cần token nếu backend đã tối ưu
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Đặt bàn", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = CreamBG
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Custom Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) WarmBrown else Color(0xFFF5F5F5),
                        onClick = { selectedTab = index }
                    ) {
                        Text(
                            title,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm theo tên, SĐT...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = WarmBrown
                ),
                singleLine = true
            )

            val baseList = when (selectedTab) {
                0 -> allReservations.filter { it.status == "pending" }
                1 -> allReservations.filter { it.status == "confirmed" }
                else -> allReservations.filter { it.status == "completed" || it.status == "cancelled" }
            }
            
            val filteredList = baseList.filter {
                it.user_name.contains(searchQuery, ignoreCase = true) ||
                it.user_phone.contains(searchQuery, ignoreCase = true)
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventSeat, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("Không có dữ liệu", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList, key = { it.id }) { reservation ->
                        AdminReservationCard(
                            reservation = reservation, 
                            viewModel = reservationViewModel,
                            onCustomerArrived = {
                                if (reservation.table_id != null) {
                                    restaurantViewModel.assignTableToCustomer(
                                        customerId = reservation.user_id,
                                        tableId = reservation.table_id,
                                        onComplete = {
                                            reservationViewModel.updateStatus(reservation.id, "completed")
                                        }
                                    )
                                } else {
                                    selectedReservationForTable = reservation
                                    showTableDialog = true
                                }
                            },
                            onStatusChange = { newStatus ->
                                reservationViewModel.updateStatus(reservation.id, newStatus)
                                if (reservation.table_id != null) {
                                    val table = tables.find { it.id == reservation.table_id }
                                    if (table != null) {
                                        if (newStatus == "confirmed") {
                                            restaurantViewModel.updateTableAdmin(table.copy(status = "reserved", reserved_time = "${reservation.time} - ${reservation.date}"))
                                        } else if (newStatus == "cancelled") {
                                            restaurantViewModel.updateTableAdmin(table.copy(status = "available", reserved_time = null))
                                        }
                                    }
                                }
                                val msg = if (newStatus == "confirmed") "Đơn đặt bàn của bạn đã được xác nhận!" else "Đơn đặt bàn của bạn đã bị từ chối."
                                val type = if (newStatus == "confirmed") "success" else "warning"
                                notificationViewModel.sendNotification(reservation.user_id, "Cập nhật đặt bàn", msg, type)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTableDialog && selectedReservationForTable != null) {
        val reservationToAssign = selectedReservationForTable!!
        val availableTables = tables.filter { it.status == "available" }
            .sortedWith(compareBy({ it.table_number.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }, { it.table_number }))
            
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text("Gán bàn cho khách", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Khách: ${reservationToAssign.user_name}")
                    Spacer(Modifier.height(16.dp))
                    if (availableTables.isEmpty()) {
                        Text("Hiện không có bàn trống!", color = Color.Red)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(availableTables) { table ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF5F5F5),
                                    onClick = {
                                        restaurantViewModel.assignTableToCustomer(
                                            customerId = reservationToAssign.user_id,
                                            tableId = table.id,
                                            onComplete = {
                                                reservationViewModel.updateStatus(reservationToAssign.id, "completed")
                                            }
                                        )
                                        showTableDialog = false
                                        selectedReservationForTable = null
                                    }
                                ) {
                                    Text(table.table_number, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTableDialog = false }) { Text("Hủy", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun AdminReservationCard(
    reservation: Reservation, 
    viewModel: ReservationViewModel,
    onCustomerArrived: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(reservation.user_name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(reservation.user_phone, fontSize = 14.sp, color = Color.Gray)
                    }
                }
                
                // Status Badge
                val (bgColor, textColor, label) = when (reservation.status) {
                    "pending" -> Triple(StatusYellow.copy(alpha = 0.15f), StatusYellow, "Chờ duyệt")
                    "confirmed" -> Triple(StatusGreen.copy(alpha = 0.15f), StatusGreen, "Đã nhận")
                    "completed" -> Triple(Color(0xFF2196F3).copy(alpha = 0.15f), Color(0xFF2196F3), "Đã đến")
                    else -> Triple(StatusRed.copy(alpha = 0.15f), StatusRed, "Đã hủy")
                }
                Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
                    Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
            Spacer(Modifier.height(12.dp))

            // Body
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Thời gian", fontSize = 12.sp, color = Color.Gray)
                    Text("${reservation.time} - ${reservation.date}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Số lượng", fontSize = 12.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                        Spacer(Modifier.width(4.dp))
                        Text("${reservation.guest_count} khách", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = WarmBrown)
                    }
                }
            }
            if (reservation.table_number != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventSeat, null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                    Spacer(Modifier.width(4.dp))
                    Text("Bàn đã chọn: ${reservation.table_number}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                }
            }

            if (reservation.note.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0xFFFFF9C4).copy(alpha=0.3f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.AutoMirrored.Filled.Notes, null, modifier = Modifier.size(16.dp), tint = Color(0xFFF57F17))
                        Spacer(Modifier.width(8.dp))
                        Text(reservation.note, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }

            // Actions
            if (reservation.status == "pending") {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onStatusChange("cancelled") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Từ chối")
                    }
                    Button(
                        onClick = { onStatusChange("confirmed") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nhận bàn")
                    }
                }
            } else if (reservation.status == "confirmed") {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCustomerArrived,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Khách đã đến", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
