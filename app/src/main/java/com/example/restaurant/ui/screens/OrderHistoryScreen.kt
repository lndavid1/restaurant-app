package com.example.restaurant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Order
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.utils.toVndFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    token: String,
    viewModel: RestaurantViewModel,
    onNavigateBack: () -> Unit
) {
    val orderHistory by viewModel.orderHistory.collectAsState()
    var orderToReview by remember { mutableStateOf<Order?>(null) }
    var orderToDetail by remember { mutableStateOf<Order?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") } // all | active | done
    val isRefreshing = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.fetchOrderHistory(token)
    }

    // Filtered list
    val filteredHistory = remember(orderHistory, searchQuery, statusFilter) {
        orderHistory
            .filter { order ->
                when (statusFilter) {
                    "active" -> order.order_status in listOf("pending", "processing")
                    "done" -> order.order_status == "completed"
                    else -> true
                }
            }
            .filter { order ->
                if (searchQuery.isBlank()) true
                else order.id.toString().contains(searchQuery) ||
                     (order.table_number?.contains(searchQuery, ignoreCase = true) == true) ||
                     (order.items_detail?.any { it.name.contains(searchQuery, ignoreCase = true) } == true)
            }
            .sortedByDescending { it.created_at }
    }

    if (orderToReview != null) {
        ReviewDialog(
            order = orderToReview!!,
            onDismiss = { orderToReview = null },
            onSubmitReview = { productId, rating, comment ->
                viewModel.submitReview(token, productId, rating, comment) {
                    orderToReview = null
                }
            }
        )
    }

    if (orderToDetail != null) {
        OrderDetailBottomSheet(
            order = orderToDetail!!,
            onDismiss = { orderToDetail = null },
            onReview = { orderToReview = it; orderToDetail = null }
        )
    }

    Scaffold(
        containerColor = CreamBG,
        topBar = {
            TopAppBar(
                title = { Text("L\u1ecbch s\u1eed \u0111\u01a1n h\u00e0ng", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isRefreshing.value = true
                            viewModel.fetchOrderHistory(token)
                            kotlinx.coroutines.delay(600)
                            isRefreshing.value = false
                        }
                    }) {
                        Icon(
                            if (isRefreshing.value) Icons.Default.HourglassEmpty else Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = WarmBrown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBG,
                    titleContentColor = Color(0xFF1A1A2E),
                    navigationIconContentColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

            // Search bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 3.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tìm theo #đơn, tên bàn, tên món...", color = Color.LightGray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = WarmBrown, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // Status filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("all", "Tất cả", orderHistory.size),
                    Triple("active", "Đang xử lý", orderHistory.count { it.order_status in listOf("pending", "processing") }),
                    Triple("done", "Hoàn tất", orderHistory.count { it.order_status == "completed" })
                ).forEach { (key, label, count) ->
                    val isActive = statusFilter == key
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isActive) WarmBrown else Color.White,
                        border = BorderStroke(1.dp, if (isActive) WarmBrown else Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { statusFilter = key }
                    ) {
                        Text(
                            "$label ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(80.dp), shadowElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank() || statusFilter != "all") "Không tìm thấy đơn hàng phù hợp"
                            else "Chưa có đơn hàng nào!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A2E)
                        )
                        Text("Các đơn hàng sẽ hiển thị tại đây.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { order ->
                        OrderHistoryCard(
                            order = order,
                            onReviewClick = { orderToReview = it },
                            onDetailClick = { orderToDetail = it }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailBottomSheet(
    order: Order,
    onDismiss: () -> Unit,
    onReview: (Order) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Chi tiết đơn #${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1A1A2E))
                    val statusLabel = when (order.order_status) {
                        "pending" -> "Chờ xử lý"
                        "processing" -> "Đang nấu"
                        "completed" -> "Hoàn tất"
                        "cancelled" -> "Đã hủy"
                        else -> order.order_status
                    }
                    val statusColor = when (order.order_status) {
                        "pending" -> StatusYellow
                        "processing" -> Color(0xFF2196F3)
                        "completed" -> StatusGreen
                        else -> Color.Gray
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.1f)) {
                        Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(8.dp))
            // Meta info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(order.created_at.take(16).replace("T", " "), fontSize = 13.sp, color = Color.Gray)
                if (!order.table_number.isNullOrBlank()) {
                    Text("  •  ${order.table_number}", fontSize = 13.sp, color = Color.Gray)
                }
                if (order.order_type == "takeaway") {
                    Text("  •  Mang về", fontSize = 13.sp, color = WarmBrown, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Text("Danh sách món", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))

            order.items_detail?.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = WarmBrown.copy(alpha = 0.1f), modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${item.quantity}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = WarmBrown)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(item.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    }
                    Text("${(item.price * item.quantity).toLong().toVndFormat()} đ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            // Totals
            if (order.discount_amount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Giảm giá", fontSize = 13.sp, color = Color.Gray)
                    Text("-${order.discount_amount.toLong().toVndFormat()} đ", fontSize = 13.sp, color = StatusGreen, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tổng thanh toán", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${order.total_amount.toLong().toVndFormat()} đ", fontSize = 20.sp, fontWeight = FontWeight.Black, color = WarmBrown)
            }
            // Payment method
            val payLabel = when {
                !order.vnpay_qr_url.isNullOrBlank() -> "VNPay"
                order.payos_order_code != null -> "PayOS"
                else -> "Tiền mặt"
            }
            Text("Thanh toán qua: $payLabel", fontSize = 12.sp, color = Color.Gray)

            Spacer(Modifier.height(20.dp))

            if (order.order_status == "completed") {
                Button(
                    onClick = { onReview(order) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đánh giá món ăn", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onReviewClick: (Order) -> Unit,
    onDetailClick: (Order) -> Unit = {}
) {
    val statusLabel = when (order.order_status) {
        "pending" -> "Chờ xử lý"
        "processing" -> "Đang nấu"
        "completed" -> "Hoàn tất"
        "cancelled" -> "Đã hủy"
        else -> "Không xác định"
    }
    val statusColor = when (order.order_status) {
        "pending" -> StatusYellow
        "processing" -> Color(0xFF2196F3)
        "completed" -> StatusGreen
        else -> Color.Gray
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onDetailClick(order) },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ReceiptLong, null, tint = statusColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Đơn hàng #${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(order.created_at.take(16).replace("T", " "), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = statusColor.copy(alpha = 0.1f)) {
                    Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))

            order.items_detail?.take(3)?.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity}x ${item.name}", fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    Text("${(item.price * item.quantity).toLong().toVndFormat()} đ", fontSize = 14.sp, color = Color(0xFF1A1A2E))
                }
            }
            if ((order.items_detail?.size ?: 0) > 3) {
                Text("... và ${(order.items_detail?.size ?: 0) - 3} món khác", fontSize = 13.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tổng thanh toán", fontSize = 14.sp, color = Color.Gray)
                Text("${order.total_amount.toLong().toVndFormat()} đ", fontSize = 18.sp, fontWeight = FontWeight.Black, color = WarmBrown)
            }

            if (order.order_status == "completed") {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onReviewClick(order) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Đánh giá món ăn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSubmitReview: (Int, Int, String) -> Unit
) {
    var selectedProductId by remember { mutableStateOf<Int?>(order.items_detail?.firstOrNull()?.product_id) }
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đánh giá đơn hàng #${order.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                Text("Chọn món ăn:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    items(order.items_detail ?: emptyList()) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedProductId = item.product_id }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedProductId == item.product_id,
                                onClick = { selectedProductId = item.product_id },
                                colors = RadioButtonDefaults.colors(selectedColor = WarmBrown)
                            )
                            Text(item.name, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đánh giá (1-5 sao):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i <= rating) Color(0xFFFFB300) else Color.LightGray,
                            modifier = Modifier.size(32.dp).clickable { rating = i }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Nhận xét của bạn") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProductId != null) {
                        onSubmitReview(selectedProductId!!, rating, comment)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                enabled = selectedProductId != null
            ) {
                Text("Gửi đánh giá", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
