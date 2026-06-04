package com.example.restaurant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Voucher
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusRed
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.VoucherViewModel
import com.example.restaurant.utils.toVndFormat
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherManagementScreen(
    viewModel: VoucherViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val vouchers by viewModel.vouchers.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedVoucher by remember { mutableStateOf<Voucher?>(null) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            AppToast.info(msg)
        }
    }

    if (showDialog) {
        VoucherEditDialog(
            voucher = selectedVoucher,
            onDismiss = { showDialog = false },
            onConfirm = { voucher ->
                if (selectedVoucher == null) viewModel.createVoucher(voucher)
                else viewModel.updateVoucher(voucher.copy(id = selectedVoucher!!.id))
                showDialog = false
            }
        )
    }

    if (voucherToDelete != null) {
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text("Xóa Voucher") },
            text = { Text("Bạn có chắc muốn xóa mã ${voucherToDelete?.code}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVoucher(voucherToDelete!!.id)
                    voucherToDelete = null
                }) { Text("Xóa", color = StatusRed) }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) { Text("Hủy", color = Color.Gray) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Khuyến mãi", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBG)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { selectedVoucher = null; showDialog = true },
                containerColor = WarmBrown,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        },
        containerColor = CreamBG
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            
            // Header stats
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tổng Voucher", color = Color.Gray, fontSize = 13.sp)
                        Text("${vouchers.size}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = WarmBrown)
                    }
                }
                val activeCount = vouchers.count { it.valid_until > System.currentTimeMillis() && it.times_used < it.usage_limit }
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Đang chạy", color = Color.Gray, fontSize = 13.sp)
                        Text("$activeCount", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = StatusGreen)
                    }
                }
            }

            if (vouchers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("Chưa có mã khuyến mãi nào", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vouchers, key = { it.id }) { voucher ->
                        val isExpired = voucher.valid_until < System.currentTimeMillis()
                        val isUsedUp = voucher.times_used >= voucher.usage_limit
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (isExpired || isUsedUp) Color.LightGray else WarmBrown.copy(alpha=0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = WarmBrown.copy(alpha=0.1f), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            voucher.code,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = WarmBrown,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    
                                    val tierText = when(voucher.required_tier) {
                                        "diamond" -> "Kim Cương"
                                        "gold" -> "Vàng"
                                        else -> "Tất cả"
                                    }
                                    val tierColor = when(voucher.required_tier) {
                                        "diamond" -> Color(0xFF3F51B5)
                                        "gold" -> Color(0xFFFF8F00)
                                        else -> Color.Gray
                                    }
                                    Surface(color = tierColor.copy(alpha=0.1f), shape = RoundedCornerShape(6.dp)) {
                                        Text(tierText, color = tierColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    val (badgeColor, badgeText) = when {
                                        isExpired -> Pair(Color.Gray, "Hết hạn")
                                        isUsedUp -> Pair(Color.Gray, "Hết lượt")
                                        else -> Pair(StatusGreen, "Đang chạy")
                                    }
                                    Surface(color = badgeColor.copy(alpha=0.15f), shape = RoundedCornerShape(6.dp)) {
                                        Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                
                                val discountText = if (voucher.is_percent) "Giảm ${voucher.discount_amount.toInt()}%" 
                                                   else "Giảm ${voucher.discount_amount.toLong().toVndFormat()}đ"
                                val maxText = if (voucher.is_percent && voucher.max_discount != null) " (Tối đa ${voucher.max_discount.toLong().toVndFormat()}đ)" else ""
                                Text("$discountText$maxText", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                
                                Text("Đơn tối thiểu: ${voucher.min_order_value.toLong().toVndFormat()}đ", fontSize = 13.sp, color = Color.Gray)
                                
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                                Spacer(Modifier.height(12.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("HSD: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(voucher.valid_until))}", fontSize = 12.sp, color = if(isExpired) StatusRed else Color.Gray)
                                        Text("Đã dùng: ${voucher.times_used} / ${voucher.usage_limit}", fontSize = 12.sp, color = if(isUsedUp) StatusRed else Color.Gray)
                                    }
                                    Row {
                                        IconButton(onClick = { selectedVoucher = voucher; showDialog = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, null, tint = WarmBrown, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { voucherToDelete = voucher }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = StatusRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) } // padding for FAB
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherEditDialog(
    voucher: Voucher?,
    onDismiss: () -> Unit,
    onConfirm: (Voucher) -> Unit
) {
    var code by remember { mutableStateOf(voucher?.code ?: "") }
    var isPercent by remember { mutableStateOf(voucher?.is_percent ?: false) }
    var discountAmount by remember { mutableStateOf(voucher?.discount_amount?.toString() ?: "") }
    var maxDiscount by remember { mutableStateOf(voucher?.max_discount?.toString() ?: "") }
    var minOrderValue by remember { mutableStateOf(voucher?.min_order_value?.toString() ?: "0") }
    var usageLimit by remember { mutableStateOf(voucher?.usage_limit?.toString() ?: "100") }
    var requiredTier by remember { mutableStateOf(voucher?.required_tier ?: "all") }
    
    val tiers = listOf("all" to "Tất cả thành viên", "gold" to "Hạng Vàng (>=1000đ)", "diamond" to "Hạng Kim Cương (>=5000đ)")
    var expandedTier by remember { mutableStateOf(false) }
    
    val defaultDate = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }.timeInMillis
    var validUntil by remember { mutableStateOf(voucher?.valid_until ?: defaultDate) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = validUntil)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(if (voucher == null) "Tạo Voucher mới" else "Sửa Voucher", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().replace(" ", "") },
                        label = { Text("Mã (VD: TET2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isPercent = false }) {
                            RadioButton(selected = !isPercent, onClick = { isPercent = false }, colors = RadioButtonDefaults.colors(selectedColor = WarmBrown))
                            Text("Giảm tiền")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isPercent = true }) {
                            RadioButton(selected = isPercent, onClick = { isPercent = true }, colors = RadioButtonDefaults.colors(selectedColor = WarmBrown))
                            Text("Giảm %")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = discountAmount,
                        onValueChange = { discountAmount = it },
                        label = { Text(if (isPercent) "Mức giảm (%)" else "Mức giảm (VNĐ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isPercent) {
                    item {
                        OutlinedTextField(
                            value = maxDiscount,
                            onValueChange = { maxDiscount = it },
                            label = { Text("Giảm tối đa (VNĐ) - Tùy chọn") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = minOrderValue,
                        onValueChange = { minOrderValue = it },
                        label = { Text("Đơn tối thiểu (VNĐ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = usageLimit,
                        onValueChange = { usageLimit = it },
                        label = { Text("Giới hạn số lượt dùng") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedTier,
                        onExpandedChange = { expandedTier = !expandedTier }
                    ) {
                        OutlinedTextField(
                            value = tiers.find { it.first == requiredTier }?.second ?: "Tất cả",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Áp dụng cho Hạng") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTier) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedBorderColor = WarmBrown
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTier,
                            onDismissRequest = { expandedTier = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            tiers.forEach { tier ->
                                DropdownMenuItem(
                                    text = { Text(tier.second) },
                                    onClick = {
                                        requiredTier = tier.first
                                        expandedTier = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(validUntil)),
                        onValueChange = {},
                        label = { Text("Ngày hết hạn") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, null, tint = WarmBrown)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalVoucher = Voucher(
                        id = voucher?.id ?: "", // backend auto gen if empty
                        code = code,
                        discount_amount = discountAmount.toDoubleOrNull() ?: 0.0,
                        is_percent = isPercent,
                        min_order_value = minOrderValue.toDoubleOrNull() ?: 0.0,
                        max_discount = maxDiscount.toDoubleOrNull().takeIf { isPercent && it != null && it > 0 },
                        usage_limit = usageLimit.toIntOrNull() ?: 100,
                        valid_until = validUntil,
                        times_used = voucher?.times_used ?: 0,
                        required_tier = requiredTier
                    )
                    onConfirm(finalVoucher)
                },
                enabled = code.isNotBlank() && discountAmount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { validUntil = it }
                    showDatePicker = false
                }) { Text("Chọn", color = WarmBrown) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
