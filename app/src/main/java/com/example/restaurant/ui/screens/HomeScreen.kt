package com.example.restaurant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.restaurant.data.model.Product
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.utils.toVndFormat
import com.example.restaurant.ui.viewmodel.StockStatus
import androidx.compose.material.icons.filled.Block

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    token: String,
    viewModel: RestaurantViewModel,
    tableName: String,
    isCustomer: Boolean = false,
    onNavigateToCart: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // derivedStateOf: chỉ tính lại khi products hoặc searchQuery đổi
    val filteredProducts by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) products
            else products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    val ingredients by viewModel.ingredients.collectAsState() // Để viewmodel biết thay đổi kho

    LaunchedEffect(Unit) {
        viewModel.fetchCategories()
        viewModel.fetchProducts()
    }

    Scaffold(
        containerColor = CreamBG,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChatbot,
                containerColor = WarmBrown,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "Trợ lý món ăn")
            }
        },
        topBar = {
            val topBarBrush = remember { androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(WarmBrown, WarmBrown.copy(alpha = 0.80f))) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = topBarBrush)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isCustomer) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(tableName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                            Text("Thực đơn món ăn", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCustomer) {
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White)
                            }
                        }
                        BadgedBox(
                            badge = {
                                if (cartItems.isNotEmpty()) {
                                    Badge(containerColor = Color.White, contentColor = WarmBrown) {
                                        Text(cartItems.sumOf { it.second }.toString(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = onNavigateToCart) {
                                Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // --- Thanh tìm kiếm ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Bạn muốn tìm món gì?", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = WarmBrown.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        Surface(
                            onClick = { viewModel.fetchProducts(category.id) },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, WarmBrown.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy món ăn nào.", color = Color.Gray)
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val quantityInCart = remember(cartItems, product.id) {
                        cartItems.find { it.first.id == product.id }?.second ?: 0
                    }
                    val stockStatus = viewModel.getProductStockStatus(product)
                    ProductCustomerItem(
                        product = product,
                        quantity = quantityInCart,
                        stockStatus = stockStatus,
                        isHot = product.is_featured,
                        onAdd = { viewModel.addToCart(product) },
                        onRemove = { viewModel.removeFromCart(product) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ProductCustomerItem(
    product: Product,
    quantity: Int,
    stockStatus: StockStatus,
    isHot: Boolean = false,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    var showDetail by remember { mutableStateOf(false) }
    if (showDetail) { ProductDetailDialog(product = product, onDismiss = { showDetail = false }) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showDetail = true },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Image with Hot badge overlay
            Box {
                AsyncImage(
                    model = product.image_url,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFF0EDE8), RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isHot) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF4757)
                    ) {
                        Text("🔥", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
                Text("${product.price.toVndFormat()} VNĐ", color = WarmBrown, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                if (!product.description.isNullOrBlank()) {
                    Text(product.description ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Badge Tình trạng kho
                val (statusText, statusColor) = when (stockStatus) {
                    StockStatus.OK -> "Còn hàng" to com.example.restaurant.ui.theme.StatusGreen
                    StockStatus.LOW_STOCK -> "Sắp hết" to com.example.restaurant.ui.theme.StatusYellow
                    StockStatus.OUT_OF_STOCK -> "Hết hàng" to com.example.restaurant.ui.theme.StatusRed
                    StockStatus.NO_RECIPE -> "Không có CT" to Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // +/- pill controls
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (stockStatus == StockStatus.OUT_OF_STOCK) Color(0xFFF5F5F5) else CreamBG,
                shadowElevation = 1.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    if (stockStatus == StockStatus.OUT_OF_STOCK) {
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp), enabled = false) {
                            Icon(Icons.Default.Block, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        if (quantity > 0) {
                            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
                            }
                            Text(quantity.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WarmBrown)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(product: Product, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Chi tiết món ăn",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = WarmBrown
            )
        },
        text = {
            Column {
                if (!product.image_url.isNullOrEmpty()) {
                    AsyncImage(
                        model = product.image_url,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(text = product.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = "${product.price.toVndFormat()} VNĐ", color = WarmBrown, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                
                Text("📋 Thành phần món:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                if (!product.ingredients.isNullOrBlank()) {
                    Text(
                        text = product.ingredients ?: "",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                } else {
                    Text("Chưa cập nhật thành phần.", fontSize = 14.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                
                if (!product.description.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("ℹ️ Mô tả thêm:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(text = product.description ?: "", fontSize = 14.sp, color = Color.DarkGray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
            ) {
                Text("Đóng")
            }
        }
    )
}
