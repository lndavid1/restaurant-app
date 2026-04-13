package com.example.restaurant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.restaurant.R
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Order
import com.example.restaurant.data.model.RestaurantTable
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusRed
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.utils.toVndFormat
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableMapScreen(
    token: String,
    viewModel: RestaurantViewModel,
    isCustomer: Boolean = false,
    onTableSelected: (Int, String) -> Unit,
    onNavigateToChatbot: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val tables by viewModel.tables.collectAsState()
    val orders by viewModel.orders.collectAsState()

    var showActionDialog by remember { mutableStateOf(false) }
    var selectedTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showServiceDialog by remember { mutableStateOf(false) }
    
    // Khai báo state cho VNPay
    // State theo dõi order đang chờ VNPAY (để lắng nghe khi được thanh toán)
    var vnpayOrder by remember { mutableStateOf<Order?>(null) }
    var showVnpaySentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchTables(token)
        viewModel.fetchOrders(token)
    }

    // Lắng nghe sự kiện thanh toán thành công TỰ ĐỘNG cho MỌI ĐƠN HÀNG (realtime từ Firestore)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(orders) {
        val currentPaidIds = orders.filter { it.payment_status == "paid" }.map { it.id }.toSet()
        val newlyPaid = currentPaidIds - viewModel.knownPaidIds

        newlyPaid.forEach { paidId ->
            com.example.restaurant.utils.SoundManager.playSuccessSound(ctx)
            android.widget.Toast.makeText(ctx, "✅ Đơn #$paidId đã được thanh toán qua Cloud thành công!", android.widget.Toast.LENGTH_LONG).show()

            // Xoá URL thanh toán khỏi order và tự động giải phóng bàn
            viewModel.clearVnpayUrl(paidId)
            val paidOrder = orders.find { it.id == paidId }
            if (paidOrder != null) {
                if (paidOrder.table_id != null && paidOrder.table_id != 0) {
                    viewModel.checkoutTable(token, paidOrder.table_id)
                } else {
                    viewModel.checkoutOrder(token, paidId)
                }
            }
            
            // Đóng dialog chờ quét QR phía app Nhân viên nếu đang mở cho đơn này
            if (vnpayOrder?.id == paidId) {
                vnpayOrder = null
                showVnpaySentDialog = false
            }
        }
        
        viewModel.knownPaidIds.addAll(newlyPaid)
    }

    // Dialog xác nhận phía nhân viên: "QR đã hiển thị trên màn hình khách"
    if (showVnpaySentDialog && vnpayOrder != null) {
        AlertDialog(
            onDismissRequest = { showVnpaySentDialog = false },
            containerColor = Color.White,
            title = { Text("✅ Đã gửi QR cho khách", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Đơn hàng: #${vnpayOrder!!.id}", fontSize = 14.sp)
                    Text("Tổng: ${vnpayOrder!!.total_amount.toVndFormat()} VND", color = WarmBrown, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(48.dp), tint = Color(0xFF005BAA))
                    Spacer(Modifier.height(8.dp))
                    Text("Mã QR đã hiển thị trên điện thoại của khách.", textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray)
                    Text("Hệ thống sẽ tự cập nhật khi khách thanh toán xong.", textAlign = TextAlign.Center, fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVnpaySentDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                ) { Text("Đóng") }
            }
        )
    }

    if (showActionDialog && selectedTable != null) {
        val currentOrder = orders.find { it.table_id == selectedTable!!.id && (it.payment_status == "unpaid" || it.payment_status == "requested") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showActionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // TITLE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thao tác: ${selectedTable!!.table_number}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (selectedTable!!.needs_service) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Color.Red.copy(alpha=0.1f)) {
                                Text(
                                    "🔔 GỌI P.V",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // INFO
                    if (currentOrder != null) {
                        Text("Mã Hóa đơn: #${currentOrder.id}", fontSize = 13.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tổng tiền: ${currentOrder.total_amount.toVndFormat()} VNĐ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WarmBrown
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    Text("Vui lòng chọn thao tác bên dưới:", fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))

                    // ACTION BUTTONS
                    if (currentOrder?.payment_status == "requested") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                "⚠️ Khách đang yêu cầu thanh toán!",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (currentOrder?.payment_status != "requested") {
                        Button(
                            onClick = {
                                onTableSelected(selectedTable!!.id, selectedTable!!.table_number)
                                showActionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Gọi thêm món", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (currentOrder != null) {
                        Button(
                            onClick = {
                                val url = com.example.restaurant.utils.VNPayHelper.generatePaymentUrl(currentOrder.id.toString(), currentOrder.total_amount)
                                viewModel.saveVnpayUrlToOrder(currentOrder.id, url)
                                vnpayOrder = currentOrder
                                showVnpaySentDialog = true
                                showActionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BAA)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Thanh toán VNPAY", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (selectedTable!!.needs_service) {
                        Button(
                            onClick = {
                                viewModel.clearStaffCall(selectedTable!!.id)
                                showActionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("🔕 Tắt chuông báo phục vụ", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.checkoutTable(token, selectedTable!!.id)
                            showActionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9B76)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Tiền mặt (Xong)", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    // Dialog đặc biệt khi bàn đang gọi phục vụ
    if (showServiceDialog && selectedTable != null) {
        AlertDialog(
            onDismissRequest = { showServiceDialog = false },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Khách gọi phục vụ!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Red
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Red.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                selectedTable!!.table_number,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Red
                            )
                            Text(
                                "Đang cần được phục vụ",
                                fontSize = 13.sp,
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Vui lòng ra phục vụ khách và xác nhận đã hoàn tất!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearStaffCall(selectedTable!!.id)
                        showServiceDialog = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9B76))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đã phục vụ xong!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showServiceDialog = false
                        showActionDialog = true
                    }
                ) { Text("Xem chi tiết bàn", color = WarmBrown) }
            }
        )
    }

    // Không hiện tab bar cho khách hàng hoặc khi đang xem customer
    if (isCustomer) {
        // Layout đơn giản cho khách hàng
        CustomerTableMapLayout(
            token = token,
            viewModel = viewModel,
            tables = tables,
            orders = orders,
            onTableSelected = onTableSelected,
            onNavigateToChatbot = onNavigateToChatbot,
            onLogout = onLogout,
            onBack = onBack
        )
        return
    }

    // Giao diện nhân viên: 2 tab - Sơ đồ bàn + Thanh toán
    val paymentRequests = orders.filter { it.payment_status in listOf("requested", "unpaid", "payment_approved", "cash_requested", "online_requested") }
    val requestedCount = orders.count { it.payment_status == "requested" || it.payment_status == "cash_requested" || it.payment_status == "online_requested" }
    val callingCount = tables.count { it.needs_service }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(orders) {
        val currentReqIds = orders.filter { it.payment_status == "requested" || it.payment_status == "cash_requested" || it.payment_status == "online_requested" }.map { it.id }.toSet()
        val newlyReq = currentReqIds - viewModel.knownReqIds
        
        if (newlyReq.isNotEmpty()) {
            android.widget.Toast.makeText(context, "🔔 Nhận được yêu cầu thanh toán mới!", android.widget.Toast.LENGTH_LONG).show()
            com.example.restaurant.utils.SoundManager.playPaymentRequestSound(context)
        }
        viewModel.knownReqIds.addAll(newlyReq)
    }
    
    LaunchedEffect(tables) {
        val currentCalling = tables.filter { it.needs_service }.map { it.id }.toSet()
        val newlyCalling = currentCalling - viewModel.knownCallingIds
        
        if (newlyCalling.isNotEmpty()) {
            android.widget.Toast.makeText(context, "🔔 Khách đang gọi phục vụ!", android.widget.Toast.LENGTH_LONG).show()
            com.example.restaurant.utils.SoundManager.playCallStaffSound(context)
        }
        viewModel.knownCallingIds.addAll(newlyCalling)
    }
    
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }


    Scaffold(
        containerColor = CreamBG,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChatbot,
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = WarmBrown
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Trợ lý món ăn",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab Sơ đồ bàn
                        val navItems = listOf(
                            Triple("Sơ đồ bàn", Icons.Default.TableRestaurant, 0),
                            Triple("Thanh toán", Icons.Default.AttachMoney, 1)
                        )
                        navItems.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.85f,
                                animationSpec = spring(dampingRatio = 0.5f),
                                label = "navScale"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .background(
                                            if (isSelected) WarmBrown.copy(alpha = 0.12f) else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 18.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BadgedBox(
                                        badge = {
                                            val count = if (index == 0) callingCount else if (index == 1) requestedCount else 0
                                            if (count > 0) {
                                                Badge(containerColor = Color.Red) {
                                                    Text(count.toString(), fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) WarmBrown else Color(0xFFADB5BD),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) WarmBrown else Color(0xFFADB5BD)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CreamBG)
        ) {
            when (selectedTab) {
                0 -> EmployeeTableMapTab(
                    token = token,
                    viewModel = viewModel,
                    tables = tables,
                    orders = orders,
                    onTableSelected = onTableSelected,
                    onLogout = onLogout,
                    showActionDialog = showActionDialog,
                    onShowDialog = { table ->
                        selectedTable = table
                        if (table.needs_service) {
                            showServiceDialog = true // ưu tiên hiện dialog phục vụ trước
                        } else {
                            showActionDialog = true
                        }
                    }
                )
                1 -> EmployeePaymentTab(
                    token = token,
                    viewModel = viewModel,
                    orders = paymentRequests,
                    onVnPayClick = { order ->
                        val url = com.example.restaurant.utils.VNPayHelper.generatePaymentUrl(order.id.toString(), order.total_amount)
                        viewModel.saveVnpayUrlToOrder(order.id, url)
                        vnpayOrder = order
                        showVnpaySentDialog = true
                    }
                )
            }
        }
    }
}

// =====================================================
// TAB 1: SƠ ĐỒ BÀN (dành cho nhân viên) - Dashboard Design
// =====================================================
@Composable
fun EmployeeTableMapTab(
    token: String,
    viewModel: RestaurantViewModel,
    tables: List<RestaurantTable>,
    orders: List<Order>,
    onTableSelected: (Int, String) -> Unit,
    onLogout: () -> Unit,
    showActionDialog: Boolean,
    onShowDialog: (RestaurantTable) -> Unit
) {
    val totalTables = tables.size
    val occupiedTables = tables.count { it.status == "occupied" }
    val availableTables = tables.count { it.status == "available" }
    val callingService = tables.count { it.needs_service }
    val requestedPayment = orders.count { it.payment_status == "requested" }

    Column(modifier = Modifier.fillMaxSize()) {
        // ─── DASHBOARD HEADER ───
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(WarmBrown, WarmBrown.copy(alpha = 0.80f))
                    )
                )
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("WELCOME BACK", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                        Text("Quản lý nhà hàng", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Row {
                        val isSoundEnabled by com.example.restaurant.utils.SoundManager.isSoundEnabled.collectAsState()
                        IconButton(onClick = { com.example.restaurant.utils.SoundManager.toggleSound() }) {
                            Icon(
                                if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, 
                                contentDescription = "Bật/tắt âm thanh", 
                                tint = Color.White, 
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = {
                            viewModel.fetchTables(token)
                            viewModel.fetchOrders(token)
                        }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat card helper - dùng defaultMinSize thay vì height cố định
                    @Composable fun StatCard(label: String, value: String, bg: Color) {
                        Surface(
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 76.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = bg
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium, lineHeight = 13.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                    StatCard("Trống", "$availableTables", Color(0xFF6B9B76).copy(alpha = 0.9f))
                    StatCard("Đang dùng", "$occupiedTables", Color(0xFFD9534F).copy(alpha = 0.9f))
                    StatCard("Gọi PV", "$callingService", Color(0xFFFF9800).copy(alpha = 0.95f))
                    StatCard("Yêu cầu TT", "$requestedPayment", Color(0xFF005BAA).copy(alpha = 0.9f))
                }
            }
        }

        // ─── CARD GRID ───
        if (tables.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarmBrown)
            }
        } else {
            val sortedTables = remember(tables) { tables.sortedWith(compareBy({ it.table_number.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }, { it.table_number })) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sortedTables, key = { it.id }) { table ->
                    TableCardView(
                        table = table,
                        orders = orders,
                        onClick = {
                            if (table.status == "occupied" || table.needs_service) {
                                onShowDialog(table)
                            } else {
                                onTableSelected(table.id, table.table_number)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TableCardView(table: RestaurantTable, orders: List<Order> = emptyList(), onClick: () -> Unit) {
    val isPaymentRequested = remember(orders, table.id) {
        orders.any { it.table_id == table.id && it.payment_status == "requested" }
    }
    val isCalling = table.needs_service
    val currentOrder = remember(orders, table.id) {
        orders.find { it.table_id == table.id && (it.payment_status == "unpaid" || it.payment_status == "requested" || it.payment_status == "cash_requested" || it.payment_status == "online_requested" || it.payment_status == "payment_approved") }
    }

    val (statusColor, statusLabel) = remember(isCalling, isPaymentRequested, table.status) {
        when {
            isCalling -> Pair(Color(0xFFFF9800), "🔔 Gọi phục vụ")
            isPaymentRequested -> Pair(Color(0xFF005BAA), "💳 Yêu cầu TT")
            table.status == "available" -> Pair(com.example.restaurant.ui.theme.StatusGreen, "🟢 Trống")
            table.status == "reserved" -> Pair(com.example.restaurant.ui.theme.StatusYellow, "🟡 Đặt trước")
            else -> Pair(com.example.restaurant.ui.theme.StatusRed, "🔴 Đang dùng")
        }
    }

    val backgroundBrush = remember(isCalling, isPaymentRequested, table.status) {
        when {
            isCalling -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFF8E1)))
            isPaymentRequested -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFF3E5F5)))
            table.status == "available" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9)))
            table.status == "reserved" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFF3E0)))
            else -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFCE4EC)))
        }
    }

    // Chỉ khởi tạo animation khi thực sự cần (bàn đang gọi phục vụ)
    val blinkAlpha = if (isCalling) {
        val infiniteTransition = rememberInfiniteTransition(label = "callBlink_${table.id}")
        infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Reverse),
            label = "callBlinkAlpha"
        ).value
    } else 1f

    val borderColor = when {
        isCalling -> Color.Red.copy(alpha = blinkAlpha)
        isPaymentRequested -> Color(0xFF005BAA)
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(125.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (isCalling || isPaymentRequested) 8.dp else 2.dp,
        border = if (isCalling || isPaymentRequested) BorderStroke(1.5.dp, borderColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Table number + status dot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        table.table_number,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1A2E)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Box(modifier = Modifier.padding(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(statusColor, CircleShape)
                            )
                        }
                    }
                }
                
                // Capacity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(6.dp))
                    Text("${table.capacity} khách", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                
                // Status label + optional amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    ) {
                        Text(
                            statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (currentOrder != null) {
                        Text(
                            "${currentOrder.total_amount.toLong().toVndFormat()} đ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A2E)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================
// TAB 2: THANH TOÁN (nhân viên xử lý)
// =====================================================
@Composable
fun EmployeePaymentTab(
    token: String,
    viewModel: RestaurantViewModel,
    orders: List<Order>,
    onVnPayClick: (Order) -> Unit
) {
    var confirmingOrder by remember { mutableStateOf<Order?>(null) }

    if (confirmingOrder != null) {
        AlertDialog(
            onDismissRequest = { confirmingOrder = null },
            title = { Text("Xác nhận thanh toán") },
            text = {
                Column {
                    Text(
                        "Hóa đơn: #${confirmingOrder!!.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Bàn: ${confirmingOrder!!.table_number ?: "Mang về"}", color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tổng tiền: ${confirmingOrder!!.total_amount.toVndFormat()} VND",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = WarmBrown
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val order = confirmingOrder!!
                        if (order.table_id != null && order.table_id != 0) {
                            viewModel.checkoutTable(token, order.table_id)
                        } else {
                            viewModel.checkoutOrder(token, order.id)
                        }
                        confirmingOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9B76))
                ) { Text("Xác nhận TT") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingOrder = null }) { Text("Hủy") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Premium Header for Payment
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(WarmBrown, WarmBrown.copy(alpha = 0.80f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("QUẢN LÝ TÀI CHÍNH", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                    Text("Yêu cầu thanh toán", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                val requestedCount = orders.count { it.payment_status == "requested" || it.payment_status == "cash_requested" || it.payment_status == "online_requested" }
                if (requestedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFD9534F), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$requestedCount Đơn chờ",
                                color = Color(0xFFD9534F),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(shape = CircleShape, color = Color(0xFF6B9B76).copy(alpha=0.15f)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF6B9B76), modifier = Modifier.padding(16.dp).size(56.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tuyệt vời!",
                        color = Color(0xFF1A1A2E),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        "Không có yêu cầu thanh toán nào đang chờ",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Sắp xếp: cash_requested/online_requested trước, requested sau, rồi đến unpaid/payment_approved
            val sortedOrders = remember(orders) {
                orders.sortedWith(compareBy {
                    when (it.payment_status) {
                        "cash_requested" -> 0
                        "online_requested" -> 1
                        "requested" -> 2
                        "payment_approved" -> 3
                        else -> 4
                    }
                })
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(sortedOrders, key = { it.id }) { order ->
                    val isRequested = order.payment_status == "requested"
                    val borderColor = if (isRequested) Color(0xFFD9534F) else Color.LightGray.copy(alpha = 0.5f)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(if (isRequested) 1.5.dp else 1.dp, if (isRequested) Color(0xFFD9534F).copy(alpha=0.5f) else Color.LightGray.copy(alpha = 0.2f)),
                        shadowElevation = if (isRequested) 8.dp else 2.dp
                    ) {
                        Column {
                            // Header Banner Giống Customer role
                            val statusColor = when {
                                order.payment_status == "cash_requested" -> Color(0xFF6B9B76)
                                order.payment_status == "online_requested" -> Color(0xFF005BAA)
                                isRequested -> Color(0xFFD9534F)
                                order.payment_status == "payment_approved" -> Color.Gray
                                else -> Color(0xFFE5A65A)
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = statusColor.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ReceiptLong, null, tint = statusColor, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Mã đơn: #${order.id}", 
                                            fontWeight = FontWeight.ExtraBold, 
                                            fontSize = 16.sp, 
                                            color = statusColor
                                        )
                                    }
                                    Text(
                                        "${order.table_number ?: "Mang về"}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1A1A2E),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)) {
                                // ID Hành khách và Badge Trạng thái hạ xuống hàng dưới!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Khách hàng: ${order.user_id.take(8).uppercase()}",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                    
                                    // Badge trạng thái thanh toán
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = statusColor.copy(alpha = 0.15f)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                            val iconState = when {
                                                order.payment_status == "cash_requested" -> Icons.Default.Money
                                                order.payment_status == "online_requested" -> Icons.Default.Receipt
                                                isRequested -> Icons.Default.NotificationsActive
                                                order.payment_status == "payment_approved" -> Icons.Default.HourglassTop
                                                else -> Icons.Default.Help
                                            }
                                            Icon(
                                                iconState,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = statusColor
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                when {
                                                    order.payment_status == "cash_requested" -> "Tiền Mặt"
                                                    order.payment_status == "online_requested" -> "Ting Ting"
                                                    isRequested -> "Gọi TT"
                                                    order.payment_status == "payment_approved" -> "Đang đợi..."
                                                    else -> "Chưa rõ"
                                                },
                                                color = statusColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                            Spacer(Modifier.height(12.dp))

                            // Danh sách món
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF9F9F9)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    val items = order.items_detail ?: emptyList()
                                    items.take(3).forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• ${item.name}", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.weight(1f), maxLines = 1)
                                            Text("x${item.quantity}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (items.size > 3) {
                                        Text("... và ${items.size - 3} món khác", fontSize = 11.sp, color = WarmBrown, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top=4.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Tổng tiền + nút
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TỔNG TIỀN", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${order.total_amount.toVndFormat()} ₫",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = WarmBrown
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (order.payment_status == "requested" || order.payment_status == "unpaid") {
                                        Button(
                                            onClick = { viewModel.approvePaymentRequest(order.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BAA)),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Text("Chấp thuận", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (order.payment_status == "cash_requested" || order.payment_status == "payment_approved") {
                                        Button(
                                            onClick = { confirmingOrder = order },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (order.payment_status == "cash_requested") Color(0xFF6B9B76) else Color.Gray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Đã thu đủ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (order.payment_status == "online_requested") {
                                        Button(
                                            onClick = { confirmingOrder = order },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BAA)),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Đã nhận tiền", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// =====================================================
// LAYOUT ĐƠN GIẢN CHO KHÁCH HÀNG (không có tab)
// =====================================================
@Composable
fun CustomerTableMapLayout(
    token: String,
    viewModel: RestaurantViewModel,
    tables: List<RestaurantTable>,
    orders: List<Order>,
    onTableSelected: (Int, String) -> Unit,
    onNavigateToChatbot: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)?
) {
    Scaffold(
        containerColor = CreamBG,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChatbot,
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = WarmBrown
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Trợ lý món ăn",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CreamBG),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(WarmBrown, WarmBrown.copy(alpha = 0.8f))
                    ))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (onBack != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp).clickable { onBack() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Dùng bữa tại nhà hàng", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Chọn bàn để bắt đầu gọi món", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
            }

            // Body
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sơ đồ bàn", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(StatusGreen, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trống", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(10.dp).background(StatusRed, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Có khách", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (tables.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WarmBrown)
                    }
                } else {
                    val sortedTables = remember(tables) { tables.sortedWith(compareBy({ it.table_number.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }, { it.table_number })) }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(sortedTables, key = { it.id }) { table ->
                            CustomerTableCardView(
                                table = table,
                                orders = orders,
                                onClick = {
                                    if (table.status != "occupied") {
                                        onTableSelected(table.id, table.table_number)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerTableCardView(table: RestaurantTable, orders: List<Order>, onClick: () -> Unit) {
    val isAvailable = table.status == "available"
    val statusColor = if (isAvailable) StatusGreen else StatusRed
    val bgColor = if (isAvailable) Color.White else Color(0xFFF9F9F9)
    val textColor = if (isAvailable) Color(0xFF1A1A2E) else Color.Gray
    
    val icon = when {
        table.capacity <= 2 -> Icons.Default.Person
        table.capacity <= 4 -> Icons.Default.Home
        else -> Icons.Default.AccountBox
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        shadowElevation = if (isAvailable) 8.dp else 2.dp,
        border = BorderStroke(1.dp, if (isAvailable) StatusGreen.copy(alpha=0.3f) else Color.LightGray.copy(alpha=0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    table.table_number,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Icon(icon, null, tint = statusColor, modifier = Modifier.padding(6.dp).size(18.dp))
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${table.capacity} Khách", fontSize = 13.sp, color = Color.Gray)
                Text(if (isAvailable) "Thêm" else "Đang dùng", fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCircleView(table: RestaurantTable, orders: List<Order> = emptyList(), onClick: () -> Unit) {
    val statusColor = when {
        table.status == "available" -> StatusGreen
        table.status == "reserved" -> StatusYellow
        orders.any { it.table_id == table.id && it.payment_status == "requested" } -> Color(0xFFE5A65A)
        else -> StatusRed // occupied
    }

    val isPaymentRequested = orders.any { it.table_id == table.id && it.payment_status == "requested" }
    val isCalling = table.needs_service

    val icon = when {
        table.capacity <= 2 -> Icons.Default.Person
        table.capacity <= 4 -> Icons.Default.Home
        else -> Icons.Default.AccountBox
    }

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = if (isCalling) Color.Red.copy(alpha = blinkAlpha * 0.2f) else Color.Transparent,
            border = BorderStroke(
                width = if (isCalling) 4.dp else 2.dp, 
                color = when {
                    isCalling -> Color.Red.copy(alpha = blinkAlpha)
                    isPaymentRequested -> Color(0xFFE5A65A)
                    else -> WarmBrown
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        table.table_number,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        if (isPaymentRequested) {
                            Icon(Icons.Default.AttachMoney, null, modifier = Modifier.size(20.dp), tint = Color(0xFFE5A65A))
                        } else {
                            Icon(icon, null, modifier = Modifier.size(22.dp), tint = Color.Black)
                        }
                    }
                    if (isPaymentRequested) {
                        Text("đ. thanh toán", fontSize = 9.sp, color = Color(0xFFE5A65A))
                    } else if (isCalling) {
                        Text("🔔 GỌI PHỤC VỤ", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
