package com.example.restaurant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Reservation
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.AuthViewModel
import com.example.restaurant.ui.viewmodel.ReservationViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(
    token: String,
    authViewModel: AuthViewModel,
    reservationViewModel: ReservationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by authViewModel.userProfile.collectAsState()

    var userName by remember { mutableStateOf(userProfile?.get("fullName") as? String ?: "") }
    var userPhone by remember { mutableStateOf(userProfile?.get("phone") as? String ?: "") }
    var guestCount by remember { mutableIntStateOf(2) }
    var note by remember { mutableStateOf("") }
    
    // Khởi tạo ngày giờ mặc định (ngày mai, 18:00)
    val defaultCalendar = Calendar.getInstance().apply { 
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 18)
        set(Calendar.MINUTE, 0)
    }
    var selectedDateMillis by remember { mutableStateOf<Long?>(defaultCalendar.timeInMillis) }
    var selectedHour by remember { mutableIntStateOf(18) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)

    val userReservations by reservationViewModel.userReservations.collectAsState()

    LaunchedEffect(Unit) {
        if (userProfile == null) {
            authViewModel.loadUserProfile(token)
        }
        reservationViewModel.fetchUserReservations(token)
    }

    val activeReservation = remember(userReservations) {
        userReservations.firstOrNull { it.status == "pending" || it.status == "confirmed" }
    }

    LaunchedEffect(Unit) {
        reservationViewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt bàn trước", fontWeight = FontWeight.ExtraBold) },
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
        containerColor = CreamBG,
        bottomBar = {
            if (activeReservation == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                        Button(
                            onClick = {
                                if (userName.isBlank() || userPhone.isBlank() || selectedDateMillis == null) {
                                    Toast.makeText(context, "Vui lòng nhập đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val dateStr = sdfDate.format(Date(selectedDateMillis!!))
                                val timeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
                                
                                val reservation = Reservation(
                                    user_id = token,
                                    user_name = userName,
                                    user_phone = userPhone,
                                    date = dateStr,
                                    time = timeStr,
                                    guest_count = guestCount,
                                    note = note
                                )
                                reservationViewModel.createReservation(reservation) {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                        ) {
                            Text("Xác nhận đặt bàn", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (activeReservation != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventSeat, contentDescription = null, modifier = Modifier.size(80.dp), tint = WarmBrown)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Chức năng bị khóa",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bạn đang có một lịch đặt bàn chưa hoàn thành (đang chờ duyệt hoặc đã nhận). Vui lòng sử dụng xong hoặc hủy để có thể đặt lịch mới.",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Đã hiểu")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- THÔNG TIN LIÊN HỆ ---
            item {
                Text("Thông tin liên hệ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Tên người đặt *") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = WarmBrown) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown)
                        )
                        OutlinedTextField(
                            value = userPhone,
                            onValueChange = { userPhone = it },
                            label = { Text("Số điện thoại *") },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = WarmBrown) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown)
                        )
                    }
                }
            }

            // --- THỜI GIAN & SỐ NGƯỜI ---
            item {
                Text("Chi tiết đặt bàn", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // Chọn ngày
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = WarmBrown.copy(alpha=0.1f), modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = WarmBrown, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Ngày đến", color = Color.Gray, fontSize = 13.sp)
                                    val dateStr = selectedDateMillis?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "Chọn ngày"
                                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))

                        // Chọn giờ
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = WarmBrown.copy(alpha=0.1f), modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Schedule, null, tint = WarmBrown, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Giờ đến", color = Color.Gray, fontSize = 13.sp)
                                    Text(String.format("%02d:%02d", selectedHour, selectedMinute), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))

                        // Chọn số người
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = WarmBrown.copy(alpha=0.1f), modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.People, null, tint = WarmBrown, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Số người", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (guestCount > 1) guestCount-- },
                                    modifier = Modifier.size(36.dp).background(Color(0xFFF5F5F5), CircleShape)
                                ) { Icon(Icons.Default.Remove, "Giảm") }
                                
                                Text("$guestCount", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                
                                IconButton(
                                    onClick = { if (guestCount < 20) guestCount++ },
                                    modifier = Modifier.size(36.dp).background(WarmBrown.copy(alpha=0.1f), CircleShape)
                                ) { Icon(Icons.Default.Add, "Tăng", tint = WarmBrown) }
                            }
                        }
                    }
                }
            }

            // --- GHI CHÚ ---
            item {
                Text("Ghi chú đặc biệt", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Dị ứng, đặt tiệc sinh nhật, yêu cầu ghế trẻ em...") },
                    leadingIcon = { Icon(Icons.Default.Notes, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WarmBrown
                    )
                )
                Spacer(Modifier.height(40.dp))
            }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Chọn", color = WarmBrown) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Chọn giờ đến") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("Chọn", color = WarmBrown) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy", color = Color.Gray) }
            }
        )
    }
}
