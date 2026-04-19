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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.example.restaurant.data.model.Product
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.utils.toVndFormat
import com.example.restaurant.ui.viewmodel.StockStatus
import androidx.compose.material.icons.filled.Block
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person

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
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = WarmBrown
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.restaurant.R.drawable.app_logo),
                    contentDescription = "Trợ lý món ăn",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(WarmBrown, WarmBrown.copy(alpha = 0.85f))
                        ))
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(bottom = 8.dp) // extra padding
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isCustomer) {
                                IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(alpha=0.2f), CircleShape)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                            Column {
                                Text(tableName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                                Text("Khám phá thực đơn hấp dẫn", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCustomer) {
                                IconButton(onClick = onLogout, modifier = Modifier.background(Color.White.copy(alpha=0.2f), CircleShape)) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
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
                                IconButton(onClick = onNavigateToCart, modifier = Modifier.background(Color.White.copy(alpha=0.2f), CircleShape)) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
                                }
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
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Món ngon hôm nay bạn muốn ăn gì?", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = WarmBrown) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        Surface(
                            onClick = { viewModel.fetchProducts(category.id) },
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.dp, WarmBrown.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmBrown
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

    val config = LocalConfiguration.current
    val imgSize = (config.screenWidthDp * 0.22f).dp.coerceIn(72.dp, 100.dp)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showDetail = true },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Image with Hot badge overlay
            Box {
                if (!product.image_url.isNullOrEmpty()) {
                    AsyncImage(
                        model = product.image_url,
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(imgSize)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(imgSize).background(Color(0xFFF0EDE8), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Fastfood, null, tint = WarmBrown.copy(alpha=0.5f), modifier = Modifier.size(imgSize * 0.4f))
                    }
                }
                if (isHot) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF4757),
                        shadowElevation = 2.dp
                    ) {
                        Text("🔥", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF1A1A2E), maxLines = 2)
                Spacer(modifier = Modifier.height(3.dp))
                Text("${product.price.toVndFormat()} ₫", color = WarmBrown, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                
                if (!product.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(product.description ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                // Badge Tình trạng kho
                if (stockStatus == StockStatus.OUT_OF_STOCK) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color.Red.copy(alpha=0.1f)) {
                        Text("Hết hàng", fontSize=11.sp, color=Color.Red, fontWeight=FontWeight.Bold, modifier=Modifier.padding(horizontal=6.dp, vertical=3.dp))
                    }
                } else if (stockStatus == StockStatus.LOW_STOCK) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE5A65A).copy(alpha=0.1f)) {
                        Text("Sắp hết", fontSize=11.sp, color=Color(0xFFE5A65A), fontWeight=FontWeight.Bold, modifier=Modifier.padding(horizontal=6.dp, vertical=3.dp))
                    }
                }
            }
            // +/- pill controls
            if (stockStatus != StockStatus.OUT_OF_STOCK) {
                if (quantity > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CreamBG,
                        border = BorderStroke(1.dp, WarmBrown.copy(alpha=0.2f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
                            }
                            Text(quantity.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 3.dp))
                            IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = WarmBrown)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = WarmBrown,
                        modifier = Modifier.size(36.dp).clickable { onAdd() },
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
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
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
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
