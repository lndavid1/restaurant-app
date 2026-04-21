package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.utils.toVndFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    token: String,
    viewModel: RestaurantViewModel,
    tableId: Int,
    onNavigateBack: () -> Unit,
    onOrderSuccess: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val total = cartItems.sumOf { it.first.price * it.second }

    Scaffold(
        containerColor = CreamBG,
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(WarmBrown, WarmBrown.copy(alpha = 0.80f))))
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("Giỏ hàng của bạn", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        Text(
                            "${cartItems.size} mon - ${cartItems.sumOf { it.second }} sp",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            if (cartItems.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = CreamBG, modifier = Modifier.size(80.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingBag, null, tint = WarmBrown.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Giỏ hàng trống!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                        Text("Thêm món ăn để tiến hành đặt", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                        ) {
                            Text("Chọn món ăn", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Cart items list
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(cartItems, key = { it.first.id }) { (product, quantity) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Quantity badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WarmBrown.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "x$quantity",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = WarmBrown
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A1A2E))
                                    Text(
                                        "${product.price.toVndFormat()} VND / món",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    "${(product.price * quantity).toVndFormat()} d",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WarmBrown,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${cartItems.size} loại món", fontSize = 13.sp, color = Color.Gray)
                                Text(
                                    "Tổng: ${cartItems.sumOf { it.second }} sản phẩm",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            if (cartItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Checkout summary card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tổng cộng", fontSize = 13.sp, color = Color.Gray)
                                Text(
                                    "${total.toVndFormat()} VND",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WarmBrown
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "Đã tính thuế",
                                    color = StatusGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.placeOrder(token, tableId, onOrderSuccess) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                        ) {
                            Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("XÁC NHẬN - GỬI BẾP", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}
