package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import com.example.restaurant.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.view.ViewGroup
import coil.compose.AsyncImage
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.AuthState
import com.example.restaurant.ui.viewmodel.AuthViewModel
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.ui.viewmodel.StockStatus
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CustomerDashboardScreen(
    token: String,
    restaurantViewModel: RestaurantViewModel,
    authViewModel: AuthViewModel,
    onNavigateToTable: () -> Unit,
    onNavigateToTakeaway: () -> Unit,
    onOrderMore: (Int, String) -> Unit,
    onRequestPayment: (Int) -> Unit,
    onNavigateToChatbot: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var homeClickCount by remember { mutableIntStateOf(0) }
    val orders by restaurantViewModel.orders.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Load data
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile(token)
        restaurantViewModel.fetchProducts()
        restaurantViewModel.fetchOrders(token)
        restaurantViewModel.fetchInventory()
    }

    LaunchedEffect(restaurantViewModel) {
        restaurantViewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapWebView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true // Required for Google Maps
            settings.setSupportZoom(true)
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient() // Helps display correctly
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                    <style>
                        body, html { margin: 0; padding: 0; height: 100%; overflow: hidden; }
                        iframe { border: 0; width: 100%; height: 100%; }
                    </style>
                </head>
                <body>
                    <iframe 
                        src="https://maps.google.com/maps?q=Tr%C6%B0%E1%BB%9Dng+%C4%90%E1%BA%A1i+h%E1%BB%8Dc+C%C3%B4ng+ngh%E1%BB%87+th%C3%B4ng+tin+v%C3%A0+Truy%E1%BB%81n+th%C3%B4ng+(ICTU),+Th%C3%A1i+Nguy%C3%AAn&t=&z=16&ie=UTF8&iwloc=&output=embed"
                        allowfullscreen="" loading="lazy" referrerpolicy="no-referrer-when-downgrade">
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            loadDataWithBaseURL("https://www.google.com", htmlContent, "text/html", "UTF-8", null)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    shadowElevation = 16.dp,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple("Home", Icons.Default.Home, 0),
                            Triple("Thông báo", Icons.Default.Notifications, 1),
                            Triple("Giới thiệu", Icons.Default.Info, 2),
                            Triple("Cài đặt", Icons.Default.Settings, 3)
                        )
                        tabs.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            val scale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.85f,
                                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f),
                                label = "navScale"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { 
                                        if (index == 0) homeClickCount++
                                        selectedTab = index 
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .background(
                                            if (isSelected) WarmBrown.copy(alpha = 0.12f) else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) WarmBrown else Color(0xFFADB5BD),
                                        modifier = Modifier.size(22.dp)
                                    )
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
                .padding(padding)
                .fillMaxSize()
                .background(CreamBG)
        ) {
            when (selectedTab) {
                0 -> HomeTab(
                    token = token,
                    homeClickCount = homeClickCount,
                    restaurantViewModel = restaurantViewModel,
                    authViewModel = authViewModel,
                    onNavigateToTable = onNavigateToTable,
                    onNavigateToTakeaway = onNavigateToTakeaway,
                    onNavigateToMyTable = { selectedTab = 1 }, // Switch to notifications
                    onLogout = onLogout
                )
                1 -> NotificationsTab(
                    token = token,
                    restaurantViewModel = restaurantViewModel,
                    onOrderMore = onOrderMore,
                    onRequestPayment = onRequestPayment,
                    snackbarHostState = snackbarHostState
                )
                2 -> AboutTab(mapWebView = mapWebView)
                3 -> SettingsTab(token = token, authViewModel = authViewModel, onLogout = onLogout)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeTab(
    token: String,
    homeClickCount: Int,
    restaurantViewModel: RestaurantViewModel,
    authViewModel: AuthViewModel,
    onNavigateToTable: () -> Unit,
    onNavigateToTakeaway: () -> Unit,
    onNavigateToMyTable: () -> Unit,
    onLogout: () -> Unit
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val products by restaurantViewModel.products.collectAsState()
    val orders by restaurantViewModel.orders.collectAsState()
    val ingredients by restaurantViewModel.ingredients.collectAsState()
    val images = products.mapNotNull { it.image_url?.takeIf { url -> url.isNotEmpty() } }.take(5)
    
    // Kiểm tra xem khách có đang có bàn chưa thanh toán không
    val hasActiveTable = orders.any { 
        it.user_id == token && 
        it.table_id != null && 
        it.table_id != 0 && 
        (it.payment_status == "unpaid" || it.payment_status == "requested")
    }
    
    var showTableBlockedDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    if (showTableBlockedDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTableBlockedDialog = false },
            title = { Text("⚠️ Bạn đang có bàn") },
            text = { Text("Bạn đang có hóa đơn chưa thanh toán. Vui lòng gọi thanh toán và đợi nhân viên xác nhận trước khi đặt bàn mới.") },
            confirmButton = {
                TextButton(onClick = { showTableBlockedDialog = false }) { Text("Đã hiểu") }
            }
        )
    }
    
    var showProductDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.restaurant.data.model.Product?>(null) }
    var viewAllProducts by remember { mutableStateOf(false) }
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) { products.filter { it.name.contains(searchQuery, ignoreCase = true) } }

    LaunchedEffect(homeClickCount) {
        viewAllProducts = false
        searchQuery = ""
    }

    if (showProductDialog != null) {
        ProductDetailDialog(product = showProductDialog!!) {
            showProductDialog = null
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { if(images.isEmpty()) 1 else images.size })

    // Auto-scroll logic
    LaunchedEffect(pagerState) {
        if (images.size > 1) {
            while (true) {
                delay(3000)
                val nextPage = (pagerState.currentPage + 1) % images.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Greeting section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val name = userProfile?.get("fullName") as? String ?: "Khách hàng"
                    val firstName = name.trim().split(" ").lastOrNull() ?: name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Xin chào, $firstName 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A2E)
                        )
                        
                        val avatarUrl = userProfile?.get("avatarUrl") as? String
                        if (!avatarUrl.isNullOrBlank()) {
                            Spacer(Modifier.width(10.dp))
                            coil.compose.AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    Text(
                        "Bạn muốn ăn gì hôm nay?",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = WarmBrown.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp).clickable { onLogout() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = WarmBrown, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (searchQuery.isBlank() && !viewAllProducts) {
            // Promo Banner Carousel
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    if (images.isNotEmpty()) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            AsyncImage(
                                model = images[page],
                                contentDescription = "Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(WarmBrown.copy(alpha = 0.3f)))
                    }
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    // Promo text
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarmBrown
                        ) {
                            Text(
                                "🔥 Ưu đãi hôm nay",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Giảm 20% Bún Bò Huế — Chỉ hôm nay!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    // Dot indicators
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                        ) {
                            repeat(images.size) { i ->
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(if (pagerState.currentPage == i) 20.dp else 6.dp, 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (pagerState.currentPage == i) WarmBrown else Color.White.copy(alpha=0.5f))
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Action Buttons Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionCircleBtn(title = "Đặt bàn", icon = Icons.Default.TableRestaurant, onClick = {
                        if (hasActiveTable) showTableBlockedDialog = true
                        else onNavigateToTable()
                    })
                    ActionCircleBtn(title = "Bàn của tôi", icon = Icons.Default.Restaurant, onClick = onNavigateToMyTable)
                    ActionCircleBtn(title = "Mang về", icon = Icons.Default.ShoppingBag, onClick = onNavigateToTakeaway)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm kiếm món ăn...", color = Color.Gray) },
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (searchQuery.isBlank()) {
            // Featured Items Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐ Món nổi bật", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                    TextButton(
                        onClick = { viewAllProducts = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Xem tất cả ›", 
                            fontSize = 13.sp, 
                            color = WarmBrown, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Premium Featured Cards
            item {
                // Chi hien thi cac mon duoc admin danh dau sao
                val hotProducts = remember(products) { products.filter { it.is_featured } }
                if (hotProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Chua co mon noi bat nao.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(hotProducts, key = { it.id }) { product ->
                        Surface(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { showProductDialog = product },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    if (!product.image_url.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = product.image_url,
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0EDE8), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Fastfood, null, tint = WarmBrown, modifier = Modifier.size(40.dp))
                                        }
                                    }
                                    // Featured tag - hien thi tren tat ca vi da loc san
                                    Surface(
                                        modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF8C00)
                                    ) {
                                        Text(
                                            "⭐ Nổi bật",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        product.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = Color(0xFF1A1A2E)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    // Star rating
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(5) { star ->
                                            Icon(
                                                Icons.Default.Star,
                                                null,
                                                tint = if (star < 4) Color(0xFFFFC107) else Color(0xFFDDDDDD),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(" 4.0", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${product.price.toLong()} ₫",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WarmBrown
                                    )
                                    val stockStatus = restaurantViewModel.getProductStockStatus(product)
                                    if (stockStatus == StockStatus.OUT_OF_STOCK) {
                                        Spacer(Modifier.height(4.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color.Red.copy(alpha=0.15f)) {
                                            Text("Hết hàng", fontSize=9.sp, color=Color.Red, fontWeight=FontWeight.Bold, modifier=Modifier.padding(horizontal=4.dp, vertical=2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                } // end if hotProducts not empty
                Spacer(modifier = Modifier.height(80.dp))
            }
        } else {
            // Search Results or All Products Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewAllProducts && searchQuery.isBlank()) {
                        IconButton(onClick = { viewAllProducts = false }) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back", 
                                tint = WarmBrown,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        if (searchQuery.isBlank()) "Tất cả món ăn" else "Kết quả tìm kiếm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (filteredProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy món ăn nào.", color = Color.Gray)
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProductDialog = product },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!product.image_url.isNullOrEmpty()) {
                                AsyncImage(
                                    model = product.image_url,
                                    contentDescription = product.name,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.size(64.dp).background(Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Fastfood, null, tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${product.price.toLong()} VNĐ", color = WarmBrown, fontSize = 14.sp)
                                
                                val stockStatus = restaurantViewModel.getProductStockStatus(product)
                                if (stockStatus == StockStatus.OUT_OF_STOCK) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color.Red.copy(alpha=0.15f)) {
                                        Text("Hết hàng", fontSize=10.sp, color=Color.Red, fontWeight=FontWeight.Bold, modifier=Modifier.padding(horizontal=4.dp, vertical=2.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ActionCircleBtn(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(72.dp).clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(2.dp, WarmBrown.copy(alpha = 0.2f)),
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, tint = WarmBrown, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NotificationsTab(
    token: String,
    restaurantViewModel: RestaurantViewModel,
    onOrderMore: (Int, String) -> Unit,
    onRequestPayment: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val orders by restaurantViewModel.orders.collectAsState()
    // Chỉ hiển thị các đơn chưa thanh toán của user này
    val myOrders = orders.filter { it.user_id == token && it.payment_status != "paid" }
    
    var editingOrder by remember { mutableStateOf<com.example.restaurant.data.model.Order?>(null) }
    
    LaunchedEffect(myOrders) {
        val currentCompIds = myOrders.filter { it.order_status == "completed" }.map { it.id }.toSet()
        val newlyComp = currentCompIds - restaurantViewModel.knownCompletedIds
        if (newlyComp.isNotEmpty()) {
            com.example.restaurant.utils.SoundManager.playOrderCompletedSound(context)
        }
        restaurantViewModel.knownCompletedIds.addAll(newlyComp)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Bàn của tôi & Thông báo", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
        Spacer(modifier = Modifier.height(16.dp))

        if (myOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có đồ ăn nào đang được nấu", color = Color.Gray)
            }
        } else {
            val sortedOrders = remember(myOrders) { myOrders.sortedByDescending { it.created_at } }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(sortedOrders, key = { it.id }) { order ->
                    val statusColor = when (order.order_status) {
                        "pending" -> Color(0xFFE5A65A) // Vàng
                        "processing" -> Color(0xFFE5805A) // Cam
                        "completed" -> Color(0xFF6B9B76) // Xanh lục
                        else -> Color.Gray
                    }
                    val statusText = when (order.order_status) {
                        "pending" -> "Đang đợi Bếp xác nhận"
                        "processing" -> "Bếp đã nhận, đang chế biến món ăn!"
                        "completed" -> "Món ăn đã sẵn sàng. Xin mời dùng!"
                        else -> order.order_status
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Đơn hàng: #${order.id}", fontWeight = FontWeight.Bold)
                                Surface(shape = RoundedCornerShape(12.dp), color = statusColor.copy(alpha = 0.1f)) {
                                    Text(
                                        text = if(order.order_status == "completed") "Xong" else "Đang xử lý", 
                                        color = statusColor, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 12.sp, 
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(statusText, fontSize = 14.sp, color = statusColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            val itemsDesc = order.items_detail?.joinToString { "${it.name} x${it.quantity}" } ?: ""
                            Text(itemsDesc, fontSize = 12.sp, color = Color.Gray)
                            
                            if (order.order_status == "pending") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { editingOrder = order },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, WarmBrown)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Chỉnh sửa giỏ hàng", color = WarmBrown, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (order.payment_status == "unpaid") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (order.table_id != null && order.table_id != 0) {
                                        Button(
                                            onClick = { restaurantViewModel.callStaff(order.table_id) },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gọi p.vụ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Button(
                                        onClick = { onOrderMore(order.table_id ?: 0, order.table_number ?: "Mang Về") },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gọi thêm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onRequestPayment(order.id) },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9534F)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gọi t.toán", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                            } else if (order.payment_status == "requested") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE5A65A).copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp), tint = Color(0xFFE5A65A))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đang đợi nhân viên xác nhận thanh toán...", fontSize = 13.sp, color = Color(0xFFE5A65A), fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else if (order.payment_status == "payment_approved") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Chọn phương thức thanh toán:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                                    
                                    // 1. PayOS
                                    Button(
                                        onClick = {
                                            restaurantViewModel.createPayOSPayment(
                                                token   = token,
                                                orderId = order.id,
                                                onSuccess = { checkoutUrl, orderCode ->
                                                    restaurantViewModel.requestOnlinePayment(order.id)
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                    restaurantViewModel.startPayOSPolling(token, order.id, orderCode, order.table_id ?: 0, order.total_amount)
                                                },
                                                onError = { msg ->
                                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAE2070))
                                    ) {
                                        Text("💳 Thanh toán PayOS (VietQR)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    // 2. VNPAY
                                    Button(
                                        onClick = {
                                            restaurantViewModel.requestOnlinePayment(order.id)
                                            val url = com.example.restaurant.utils.VNPayHelper.generatePaymentUrl(order.id.toString(), order.total_amount)
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BAA))
                                    ) {
                                        Text("💳 Thanh toán VNPAY", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    // 3. Tiền mặt
                                    Button(
                                        onClick = { restaurantViewModel.requestCashPayment(order.id) },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9B76))
                                    ) {
                                        Text("💵 Thanh toán Tiền Mặt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            } else if (order.payment_status == "cash_requested") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF6B9B76).copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp), tint = Color(0xFF6B9B76))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đang đợi nhân viên đến bàn thu tiền mặt...", fontSize = 13.sp, color = Color(0xFF6B9B76), fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else if (order.payment_status == "online_requested") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF005BAA).copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp), tint = Color(0xFF005BAA))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đang đợi số dư báo về. Vui lòng đợi hoặc nhắc nhân viên xác nhận!", fontSize = 13.sp, color = Color(0xFF005BAA), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
    
    if (editingOrder != null) {
        EditOrderBottomSheet(
            order = editingOrder!!,
            onDismiss = { editingOrder = null },
            snackbarHostState = snackbarHostState,
            onConfirm = { updatedItems ->
                restaurantViewModel.updateOrderItems(editingOrder!!.id, updatedItems) {
                    editingOrder = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderBottomSheet(
    order: com.example.restaurant.data.model.Order,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onConfirm: (List<com.example.restaurant.data.model.OrderItemDetail>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Duplicate the list so we can draft changes safely inside bottom sheet
    val draftItems = remember { mutableStateListOf(*(order.items_detail?.toTypedArray() ?: emptyArray())) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Chỉnh sửa giỏ hàng #${order.id}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
            Text("Bếp chưa xác nhận nên bạn có thể điều chỉnh thoải mái!", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            if (draftItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Đã xóa tất cả món", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(draftItems.toList(), key = { _, item -> item.name }) { index, item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val currentIdx = draftItems.indexOf(item)
                                    if (currentIdx != -1) {
                                        if (item.quantity > 1) {
                                            draftItems[currentIdx] = item.copy(quantity = item.quantity - 1)
                                        } else {
                                            val removed = draftItems.removeAt(currentIdx)
                                            scope.launch {
                                                val snackbarResult = snackbarHostState.showSnackbar(
                                                    message = "Đã xóa ${removed.name}",
                                                    actionLabel = "HOÀN TÁC",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                    draftItems.add(removed) // Cứ nhét về cuối list cho an toàn tránh outofbounds
                                                }
                                            }
                                        }
                                    }
                                }) { Icon(Icons.Default.RemoveCircleOutline, null, tint = WarmBrown) }
                                
                                Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                
                                IconButton(onClick = {
                                    val currentIdx = draftItems.indexOf(item)
                                    if (currentIdx != -1) {
                                        draftItems[currentIdx] = item.copy(quantity = item.quantity + 1)
                                    }
                                }) { Icon(Icons.Default.AddCircleOutline, null, tint = WarmBrown) }
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(draftItems.toList()) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
            ) {
                Text("Xác nhận Cập nhật", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsTab(token: String, authViewModel: AuthViewModel, onLogout: () -> Unit) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val updateState by authViewModel.updateProfileState.collectAsState()
    
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var avatarUrl by remember { mutableStateOf("") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }

    // Khi có dữ liệu profile, điền vào form
    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it["fullName"] as? String ?: ""
            phone = it["phone"] as? String ?: ""
            address = it["address"] as? String ?: ""
            avatarUrl = it["avatarUrl"] as? String ?: ""
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            uid = token,
            authViewModel = authViewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Cài đặt tài khoản", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar Section
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
                .clickable { imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            val previewModel: Any? = imageUri ?: avatarUrl.takeIf { it.isNotBlank() }
            if (previewModel != null) {
                coil.compose.AsyncImage(
                    model = previewModel,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
            }
            
            // Edit icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(WarmBrown, CircleShape)
                    .padding(6.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Họ và tên") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Số điện thoại") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Địa chỉ") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.updateUserProfile(token, fullName, phone, address, imageUri) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
        ) {
            if (updateState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        if (updateState is AuthState.Success) {
            Text("Lưu thành công!", color = Color(0xFF6B9B76), modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().clickable { com.example.restaurant.utils.SoundManager.toggleSound() }.padding(vertical = 12.dp)
        ) {
            val isSoundEnabled by com.example.restaurant.utils.SoundManager.isSoundEnabled.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = WarmBrown.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, null, tint = WarmBrown, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Âm thanh thông báo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                    Text(if (isSoundEnabled) "Chuông đang Bật" else "Chuông đang Tắt", fontSize = 13.sp, color = Color.Gray)
                }
            }
            Switch(
                checked = isSoundEnabled,
                onCheckedChange = { com.example.restaurant.utils.SoundManager.toggleSound() },
                colors = SwitchDefaults.colors(checkedThumbColor = WarmBrown, checkedTrackColor = WarmBrown.copy(alpha = 0.5f))
            )
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { showChangePasswordDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmBrown)
        ) {
            Text("Đổi mật khẩu bảo mật (OTP)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Đăng xuất khỏi hệ thống", color = Color.Red)
        }
    }
}

@Composable
fun AboutTab(mapWebView: WebView) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Giới thiệu nhà hàng", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Vũ Minh Chuyên - CNTTK21D", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đường Z115, X. Quyết Thắng, TP. Thái Nguyên", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("tuyensinh.ictu.edu.vn", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Facebook, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đại học Công nghệ thông tin & TT", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Bản đồ vị trí", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarmBrown)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 80.dp), // chừa không gian cho bottom nav bar
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            AndroidView(
                factory = { 
                    mapWebView.apply {
                        (parent as? ViewGroup)?.removeView(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
