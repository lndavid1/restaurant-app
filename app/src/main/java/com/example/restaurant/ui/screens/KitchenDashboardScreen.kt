package com.example.restaurant.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Ingredient
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusRed
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.theme.premiumBackground
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.utils.toVndFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenDashboardScreen(
    token: String,
    viewModel: RestaurantViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Đơn hàng", "Nguyên liệu")
    val context = LocalContext.current

    // collectLatest trong LaunchedEffect đã lifecycle-safe (tied to Composition)
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().premiumBackground(),
        containerColor = Color.Transparent,
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navItems = listOf(
                            Triple("Đơn hàng", Icons.AutoMirrored.Filled.List, 0),
                            Triple("Nguyên liệu", Icons.Default.Build, 1)
                        )
                        navItems.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.85f,
                                animationSpec = spring(dampingRatio = 0.5f), label = "scale"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).clickable { selectedTab = index }.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .background(if (isSelected) WarmBrown.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 22.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = if (isSelected) WarmBrown else Color(0xFFADB5BD), modifier = Modifier.size(22.dp))
                                }
                                Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) WarmBrown else Color(0xFFADB5BD))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            val orders by viewModel.orders.collectAsState()

            when (selectedTab) {
                0 -> {
                    // Dashboard header chỉ hiện ở tab Đơn hàng
                    val pendingCount = orders.count { it.order_status == "pending" }
                    val processingCount = orders.count { it.order_status == "processing" }
                    val doneCount = orders.count { it.order_status == "completed" }

                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(orders) {
                        val currentPendingIds = orders.filter { it.order_status == "pending" }.map { it.id }.toSet()
                        val newlyPending = currentPendingIds - viewModel.knownPendingIds
                        if (newlyPending.isNotEmpty()) {
                            com.example.restaurant.utils.SoundManager.playNewOrderSound(ctx)
                        }
                        viewModel.markPendingIdsAsSeen(newlyPending)
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color(0xFF2D1B00), WarmBrown)
                                )
                            )
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("KITCHEN", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), letterSpacing = 3.sp)
                                    Text("👨‍🍳 Bếp Trung Tâm", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                Row {
                                    val isSoundEnabled by com.example.restaurant.utils.SoundManager.isSoundEnabled.collectAsState()
                                    IconButton(onClick = { com.example.restaurant.utils.SoundManager.toggleSound() }) {
                                        Icon(
                                            if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, 
                                            contentDescription = "Bật/tắt âm thanh", 
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    IconButton(onClick = onLogout) {
                                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = StatusYellow.copy(alpha = 0.9f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Chờ", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("$pendingCount", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF2196F3).copy(alpha = 0.9f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Đang nấu", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("$processingCount", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = StatusGreen.copy(alpha = 0.9f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Xong", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("$doneCount", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    KitchenOrderList(token, viewModel)
                }
                1 -> KitchenIngredientInventory(viewModel)
            }
        }
    }
}

// =====================================================
// DANH SÁCH ĐƠN HÀNG
// =====================================================
@Composable
fun KitchenOrderList(token: String, viewModel: RestaurantViewModel) {
    val orders by viewModel.orders.collectAsState()
    var showCompleted by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchOrders(token)
    }

    val filteredOrders = if (showCompleted) {
        orders.filter { it.order_status == "completed" }
    } else {
        orders.filter { it.order_status != "completed" && it.order_status != "cancelled" }
    }

    // Dialog xác nhận xóa toàn bộ đơn đã phục vụ
    if (showClearConfirmDialog) {
        val completedCount = orders.count { it.order_status == "completed" }
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = androidx.compose.ui.graphics.Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(StatusRed.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, tint = StatusRed, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Xóa đơn đã phục vụ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.5f))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Thao tác này sẽ xóa vĩnh viễn $completedCount đơn hàng có trạng thái \"Đã xong\" khỏi hệ thống.",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Surface(
                        color = StatusRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = StatusRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hành động này không thể hoàn tác!", color = StatusRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedOrders(token)
                        showClearConfirmDialog = false
                        showCompleted = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Xóa tất cả ($completedCount đơn)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hủy bỏ", color = androidx.compose.ui.graphics.Color.Gray)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Toggle Phục vụ / Lịch sử
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Đơn hàng" to false, "Lịch sử" to true).forEach { (label, value) ->
                val isActive = showCompleted == value
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) WarmBrown else androidx.compose.ui.graphics.Color.Transparent,
                    onClick = { showCompleted = value }
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Nút xóa toàn bộ chỉ hiện khi ở tab Lịch sử và có đơn đã xong
        if (showCompleted && filteredOrders.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xóa ${filteredOrders.size} đơn đã phục vụ", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (showCompleted) "Chưa có đơn hoàn thành" else "Không có đơn nào chờ xử lý",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        border = BorderStroke(
                            1.dp,
                            when (order.order_status) {
                                "pending" -> StatusYellow.copy(alpha = 0.5f)
                                "processing" -> androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = 0.4f)
                                else -> androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Tiêu đề đơn hàng
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Đơn #${order.id}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        order.table_number ?: "Mang về",
                                        color = androidx.compose.ui.graphics.Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                KitchenStatusBadge(order.order_status)
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))

                            // Danh sách món
                            order.items_detail?.forEach { item ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, fontSize = 14.sp)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CreamBG
                                    ) {
                                        Text(
                                            "x${item.quantity}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = WarmBrown
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.4f))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${order.total_amount.toVndFormat()} VNĐ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = WarmBrown
                                )

                                when (order.order_status) {
                                    "pending" -> Button(
                                        onClick = {
                                            viewModel.updateOrderStatus(token, order.id, "processing")
                                            // Không cần gọi fetchTables() — Firestore observer tự cập nhật
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow)
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Duyệt đơn", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                    }
                                    "processing" -> Button(
                                        onClick = {
                                            viewModel.updateOrderStatus(token, order.id, "completed")
                                            // Không cần gọi fetchTables() — Firestore observer tự cập nhật
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                                    ) {
                                        Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Hoàn thành", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// =====================================================
// BADGE TRẠNG THÁI ĐƠN
// =====================================================
@Composable
fun KitchenStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "pending" -> Triple(StatusYellow.copy(alpha = 0.15f), StatusYellow, "CHỜ DUYỆT")
        "processing" -> Triple(Color(0xFF2196F3).copy(alpha = 0.12f), Color(0xFF2196F3), "ĐANG NẤU")
        "completed" -> Triple(StatusGreen.copy(alpha = 0.15f), StatusGreen, "ĐÃ XONG")
        else -> Triple(StatusRed.copy(alpha = 0.15f), StatusRed, "ĐÃ HỦY")
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

// =====================================================
// QUẢN LÝ NGUYÊN LIỆU
// =====================================================
@Composable
fun KitchenIngredientInventory(viewModel: RestaurantViewModel) {
    val ingredients by viewModel.ingredients.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var ingredientToDelete by remember { mutableStateOf<Ingredient?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchInventory()
    }

    if (showDialog) {
        KitchenIngredientEditDialog(
            ingredient = selectedIngredient,
            onDismiss = { showDialog = false },
            onConfirm = { data ->
                if (selectedIngredient == null) viewModel.addIngredient(data)
                else viewModel.updateIngredient(data.copy(id = selectedIngredient!!.id))
                showDialog = false
            }
        )
    }

    if (ingredientToDelete != null) {
        KitchenDeleteConfirmDialog(
            name = ingredientToDelete!!.name,
            onDismiss = { ingredientToDelete = null },
            onConfirm = {
                viewModel.deleteIngredient(ingredientToDelete!!.id)
                ingredientToDelete = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Header với số lượng + nút Nhập kho
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nguyên liệu:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("${ingredients.size} loại", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WarmBrown)
                }
            }
            Button(
                onClick = { selectedIngredient = null; showDialog = true },
                modifier = Modifier.weight(1f).height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null)
                    Text("Nhập kho", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Thanh tìm kiếm
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm nguyên liệu...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        val filteredIngredients = if (searchQuery.isBlank()) ingredients
            else ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }

        // Danh sách nguyên liệu
        if (filteredIngredients.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isBlank()) "Kho bếp trống" else "Không tìm thấy \"$searchQuery\"",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredIngredients, key = { it.id }) { item ->
                val isLow = item.stock < 5
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isLow) StatusRed.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon trạng thái tồn kho
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isLow) StatusRed.copy(alpha = 0.1f) else StatusGreen.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isLow) Icons.Default.Warning else Icons.Default.Check,
                                null,
                                tint = if (isLow) StatusRed else StatusGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Tồn: ${item.stock} ${item.unit}",
                                color = if (isLow) StatusRed else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = if (isLow) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        TextButton(
                            onClick = { selectedIngredient = item; showDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Sửa", color = WarmBrown, fontSize = 13.sp)
                        }
                        Text(" - ", color = Color.Gray, fontSize = 13.sp)
                        TextButton(
                            onClick = { ingredientToDelete = item },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Xóa", color = StatusRed, fontSize = 13.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        } // end else
    }
}

// =====================================================
// DIALOG THÊM / SỬA NGUYÊN LIỆU
// =====================================================
@Composable
fun KitchenIngredientEditDialog(
    ingredient: Ingredient?,
    onDismiss: () -> Unit,
    onConfirm: (Ingredient) -> Unit
) {
    var name by remember { mutableStateOf(ingredient?.name ?: "") }
    var unit by remember { mutableStateOf(ingredient?.unit ?: "") }
    var stock by remember { mutableStateOf(ingredient?.stock?.toString() ?: "0") }
    val isEdit = ingredient != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(WarmBrown.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isEdit) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = WarmBrown,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isEdit) "Cập nhật nguyên liệu" else "Nhập kho mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên nguyên liệu") },
                    placeholder = { Text("VD: Thịt bò", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Star, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    readOnly = isEdit,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        focusedLabelColor = WarmBrown
                    )
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Đơn vị tính") },
                    placeholder = { Text("VD: kg, lít, cái", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Info, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        focusedLabelColor = WarmBrown
                    )
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Số lượng tồn kho") },
                    placeholder = { Text("VD: 10", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        focusedLabelColor = WarmBrown
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(Ingredient(0, name, unit, stock.toDoubleOrNull() ?: 0.0)) },
                enabled = name.isNotBlank() && unit.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "Cập nhật" else "Nhập kho", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}

// =====================================================
// DIALOG XÁC NHẬN XÓA NGUYÊN LIỆU
// =====================================================
@Composable
fun KitchenDeleteConfirmDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(StatusRed.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Xóa nguyên liệu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        },
        text = {
            Text(
                "Bạn có chắc muốn xóa \"$name\" khỏi kho? Thao tác này không thể hoàn tác.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Xác nhận xóa", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hủy bỏ", color = Color.Gray)
            }
        }
    )
}
