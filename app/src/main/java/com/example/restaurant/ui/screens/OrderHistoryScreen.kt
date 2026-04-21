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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
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
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import com.example.restaurant.utils.toVndFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    token: String,
    viewModel: RestaurantViewModel,
    onNavigateBack: () -> Unit
) {
    val orderHistory by viewModel.orderHistory.collectAsState()
    var orderToReview by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchOrderHistory(token)
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

    Scaffold(
        containerColor = CreamBG,
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
            if (orderHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(80.dp), shadowElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Chưa có đơn hàng nào!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                        Text("Các đơn hàng hoàn tất sẽ hiển thị tại đây.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(orderHistory, key = { it.id }) { order ->
                        OrderHistoryCard(order = order, onReviewClick = { orderToReview = it })
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(order: Order, onReviewClick: (Order) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = StatusGreen.copy(alpha=0.1f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ReceiptLong, null, tint = StatusGreen, modifier = Modifier.size(18.dp))
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
                Surface(shape = RoundedCornerShape(12.dp), color = StatusGreen.copy(alpha=0.1f)) {
                    Text("Hoàn tất", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
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
            HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tổng thanh toán", fontSize = 14.sp, color = Color.Gray)
                Text("${order.total_amount.toLong().toVndFormat()} đ", fontSize = 18.sp, fontWeight = FontWeight.Black, color = WarmBrown)
            }
            
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onReviewClick(order) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Đánh giá món ăn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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
