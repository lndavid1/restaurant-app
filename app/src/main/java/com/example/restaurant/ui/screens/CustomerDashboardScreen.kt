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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.restaurant.utils.toVndFormat

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
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
    val products by restaurantViewModel.products.collectAsState()

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
    
    // WebView được khởi tạo lazy — chỉ tạo khi tab "Giới thiệu" (index 2) được mở
    // DisposableEffect sẽ destroy đúng cách khi tab thay đổi — tránh memory leak
    var mapWebView by remember { mutableStateOf<WebView?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(selectedTab) {
        if (selectedTab == 2) {
            val wv = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
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
            mapWebView = wv
        }
        onDispose {
            // Full combo destroy — tránh RAM leak âm thầm
            mapWebView?.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
            mapWebView = null
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
    val screenConfig = LocalConfiguration.current
    val isCompact = screenConfig.screenWidthDp <= 360

    val userProfile by authViewModel.userProfile.collectAsState()
    val products by restaurantViewModel.products.collectAsState()
    val orders by restaurantViewModel.orders.collectAsState()
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
            title = { Text("⚠️ Đang dùng bàn", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn đang có hóa đơn chưa thanh toán. Vui lòng gọi thanh toán và đợi xác nhận trước khi đặt bàn mới.") },
            confirmButton = {
                TextButton(onClick = { showTableBlockedDialog = false }) { Text("Đã hiểu", color = WarmBrown, fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
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
                delay(3500)
                val nextPage = (pagerState.currentPage + 1) % images.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp) // padding dư cho bottom bar
    ) {
        // Cụm Header hiện đại (Welcome dọc & Avatar tròn mượt)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.White, CreamBG)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = if (isCompact) 16.dp else 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val name = userProfile?.get("fullName") as? String ?: "Khách hàng"
                        val firstName = name.trim().split(" ").lastOrNull() ?: name
                        val cfg = LocalConfiguration.current
                        Text(
                            "Xin chào, $firstName 👋",
                            fontSize = if (cfg.screenWidthDp < 380) 20.sp else 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A2E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Hôm nay bạn muốn dùng gì?",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    val avatarUrl = userProfile?.get("avatarUrl") as? String
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(2.dp, WarmBrown.copy(alpha=0.3f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, null, tint = Color.LightGray, modifier = Modifier.fillMaxSize().padding(4.dp))
                        }
                    }
                }
            }
        }

        // Search Bar (Đưa lên trên chuẩn Insight App)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("Tìm món ăn, đồ uống...", color = Color.Gray, fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = WarmBrown) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = WarmBrown,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    Surface(
                        shape = CircleShape,
                        color = WarmBrown,
                        modifier = Modifier.padding(end = 6.dp).size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (searchQuery.isBlank() && !viewAllProducts) {
            // Promo Banner Carousel (Thiết kế tràn viền hiện đại)
            item {
                val bannerRatio = if (isCompact) 2.8f else 2.2f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bannerRatio)
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
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
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE0E0E0)))
                    }
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                    // Promo text
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFF5252).copy(alpha = 0.9f)
                        ) {
                            Text(
                                "Mã Ưu Đãi: BIGSALE🔥",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Giảm 20% cho đơn hàng đầu tiên!",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    // Dot indicators
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(images.size) { i ->
                                val isSelected = pagerState.currentPage == i
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 16.dp else 5.dp, 5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.White.copy(alpha=0.4f))
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Action Buttons Row (Dạng thiếp đổ bóng sang trọng)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionCircleBtn(
                        title = "Đặt bàn", 
                        icon = Icons.Default.TableRestaurant, 
                        gradientColors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)),
                        iconTint = Color(0xFFE65100),
                        onClick = {
                            if (hasActiveTable) showTableBlockedDialog = true
                            else onNavigateToTable()
                        }
                    )
                    ActionCircleBtn(
                        title = "Bàn của tôi", 
                        icon = Icons.Default.Restaurant, 
                        gradientColors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
                        iconTint = Color(0xFF2E7D32),
                        onClick = onNavigateToMyTable
                    )
                    ActionCircleBtn(
                        title = "Mang về", 
                        icon = Icons.Default.ShoppingBag, 
                        gradientColors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)),
                        iconTint = Color(0xFF1565C0),
                        onClick = onNavigateToTakeaway
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (searchQuery.isBlank()) {
            // Featured Items Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Món Nổi Bật ✨", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                    TextButton(
                        onClick = { viewAllProducts = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Xem thêm", 
                            fontSize = 14.sp, 
                            color = WarmBrown, 
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(12.dp), tint = WarmBrown)
                    }
                }
            }

            // Premium Featured Cards (Trượt ngang)
            item {
                val hotProducts = remember(products) { products.filter { it.is_featured } }
                if (hotProducts.isNotEmpty()) {
                    val cfg3 = LocalConfiguration.current
                    val cardWidth = (cfg3.screenWidthDp * 0.42f).dp.coerceIn(140.dp, 200.dp)
                    val imgHeight = (cardWidth.value * 0.72f).dp.coerceIn(100.dp, 150.dp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(hotProducts, key = { it.id }) { product ->
                            Surface(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .clickable { showProductDialog = product },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 10.dp
                            ) {
                                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(imgHeight)
                                    ) {
                                        if (!product.image_url.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = product.image_url,
                                                contentDescription = product.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0EDE8)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Fastfood, null, tint = WarmBrown.copy(alpha=0.5f), modifier = Modifier.size(40.dp))
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.9f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("4.8", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(
                                            product.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            color = Color(0xFF1A1A2E)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${product.price.toLong()} ₫",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WarmBrown
                                        )
                                        // Dùng SSOT cached map — O(1), không recompute trong render
                                        val stockStatus by restaurantViewModel.productStockStatusMap.collectAsState()
                                        if (stockStatus[product.id] == StockStatus.OUT_OF_STOCK) {
                                            Spacer(Modifier.height(4.dp))
                                            Text("Hết hàng", fontSize=10.sp, color=Color.Red, fontWeight=FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Kết quả tìm kiếm / Tất cả món ăn dạng List cao cấp
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewAllProducts && searchQuery.isBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(36.dp).clickable { viewAllProducts = false }.padding(end = 12.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmBrown, modifier = Modifier.padding(6.dp))
                            }
                        }
                        Text(
                            if (searchQuery.isBlank()) "Tất cả Món ngon" else "Kết quả tìm kiếm",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A2E)
                        )
                    }
                }
            }
            
            if (filteredProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Không tìm thấy món ăn nào.", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable { showProductDialog = product },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!product.image_url.isNullOrEmpty()) {
                                AsyncImage(
                                    model = product.image_url,
                                    contentDescription = product.name,
                                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.size(70.dp).background(Color(0xFFF0EDE8), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Fastfood, null, tint = WarmBrown.copy(alpha=0.5f), modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF1A1A2E))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("⭐ 4.8  |  🔥 Best Seller", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${product.price.toLong()} ₫", color = WarmBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                                // Dùng SSOT cached map — O(1), không recompute trong render
                                val stockStatusMap by restaurantViewModel.productStockStatusMap.collectAsState()
                                if (stockStatusMap[product.id] == StockStatus.OUT_OF_STOCK) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color.Red.copy(alpha=0.1f)) {
                                        Text("Hết hàng", fontSize=11.sp, color=Color.Red, fontWeight=FontWeight.Bold, modifier=Modifier.padding(horizontal=8.dp, vertical=4.dp))
                                    }
                                }
                            }
                            Icon(Icons.Default.AddCircle, null, tint = WarmBrown, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCircleBtn(title: String, icon: ImageVector, onClick: () -> Unit, gradientColors: List<Color>, iconTint: Color) {
    val cfg = LocalConfiguration.current
    val btnSize = (cfg.screenWidthDp * 0.17f).dp.coerceIn(54.dp, 76.dp)
    val iconSize = (btnSize.value * 0.44f).dp
    val labelFontSize = if (cfg.screenWidthDp < 380) 11.sp else 12.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(btnSize + 14.dp)) {
        Surface(
            modifier = Modifier.size(btnSize).clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(colors = gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(iconSize))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = labelFontSize, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
    val products by restaurantViewModel.products.collectAsState()
    // Chỉ hiển thị các đơn chưa thanh toán của user này
    val myOrders = orders.filter { it.user_id == token && it.payment_status != "paid" }

    var editingOrder by remember { mutableStateOf<com.example.restaurant.data.model.Order?>(null) }

    // Hóa đơn xem trước thanh toán
    var invoiceOrder by remember { mutableStateOf<com.example.restaurant.data.model.Order?>(null) }

    // ID-set — chỉ phát sound đúng 1 lần / order, không spam
    val notifiedCompletedIds = remember { mutableSetOf<Int>() }

    LaunchedEffect(myOrders) {
        val newlyCompleted = myOrders.filter {
            it.order_status == "completed" && it.id !in notifiedCompletedIds
        }
        if (newlyCompleted.isNotEmpty()) {
            com.example.restaurant.utils.SoundManager.playOrderCompletedSound(context)
            newlyCompleted.forEach { notifiedCompletedIds.add(it.id) }
        }
        restaurantViewModel.knownCompletedIds.addAll(newlyCompleted.map { it.id })
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                                            .padding(top = 20.dp, bottom = 100.dp)) {
        Text("Thông tin Đơn hàng", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
        Spacer(modifier = Modifier.height(16.dp))

        if (myOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Bạn chưa có đơn hàng nào đang xử lý", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            val sortedOrders = remember(myOrders) { myOrders.sortedByDescending { it.created_at } }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(sortedOrders, key = { it.id }) { order ->
                    val statusColor = when (order.order_status) {
                        "pending" -> Color(0xFFE5A65A) // Vàng
                        "processing" -> Color(0xFFE5805A) // Cam
                        "completed" -> Color(0xFF4CAF50) // Xanh lục neon
                        else -> Color.Gray
                    }
                    val statusBgColor = statusColor.copy(alpha = 0.08f)
                    val statusText = when (order.order_status) {
                        "pending" -> "Đang đợi Bếp xác nhận"
                        "processing" -> "Bếp đang chế biến món ăn!"
                        "completed" -> "Món ăn đã sẵn sàng. Xin mời dùng!"
                        else -> order.order_status
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                        shadowElevation = 8.dp
                    ) {
                        Column {
                            // Header Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = statusBgColor
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Receipt, null, tint = statusColor, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Mã đơn: #${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = statusColor)
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor) {
                                        Text(
                                            text = if(order.order_status == "completed") "Hoàn thành" else "Đang xử lý", 
                                            color = Color.White, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 11.sp, 
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Body Info
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = statusColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(statusText, fontSize = 15.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                val itemsDesc = order.items_detail?.joinToString { "${it.name} x${it.quantity}" } ?: ""
                                Text(itemsDesc, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha=0.3f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Actions Row
                                if (order.order_status == "pending") {
                                    Button(
                                        onClick = { editingOrder = order },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        border = BorderStroke(2.dp, WarmBrown)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = WarmBrown)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Chỉnh sửa yêu cầu", color = WarmBrown, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    }
                                }
                                
                                if (order.payment_status == "unpaid") {
                                    if (order.order_status == "pending") Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (order.table_id != null && order.table_id != 0) {
                                            Button(
                                                onClick = { restaurantViewModel.callStaff(order.table_id) },
                                                modifier = Modifier.weight(1f).height(48.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2).copy(alpha=0.1f)),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4A90E2))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Phục vụ", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4A90E2))
                                            }
                                        }
                                        Button(
                                            onClick = { onOrderMore(order.table_id ?: 0, order.table_number ?: "Mang Về") },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown.copy(alpha=0.1f)),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Gọi thêm", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = WarmBrown)
                                        }
                                        Button(
                                            onClick = { invoiceOrder = order },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9534F)),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Hóa đơn", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        }
                                    }

                                } else if (order.payment_status == "requested") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFFFF3E0)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.HourglassTop, null, modifier = Modifier.size(20.dp), tint = Color(0xFFE65100))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Đang xử lý yêu cầu thanh toán...", fontSize = 14.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (order.payment_status == "payment_approved") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Vui lòng chọn hình thức:", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
                                        
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
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAE2070))
                                        ) {
                                            Text("Thanh toán PayOS (VietQR)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
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
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BAA))
                                        ) {
                                            Text("Thanh toán VNPAY", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        }

                                        // 3. Tiền mặt
                                        Button(
                                            onClick = { restaurantViewModel.requestCashPayment(order.id) },
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            border = BorderStroke(2.dp, Color(0xFF388E3C))
                                        ) {
                                            Text("Tiền mặt", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
                                        }
                                    }
                                } else if (order.payment_status == "cash_requested") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PointOfSale, null, modifier = Modifier.size(20.dp), tint = Color(0xFF2E7D32))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("NV đang tới thu tiền mặt, vui lòng đợi!", fontSize = 14.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (order.payment_status == "online_requested") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFE3F2FD)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(20.dp), tint = Color(0xFF1565C0))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Đang đồng bộ giao dịch ngân hàng...", fontSize = 14.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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

    // Hóa đơn xem trước — hiện trước khi gọi thanh toán
    if (invoiceOrder != null) {
        InvoiceBottomSheet(
            order = invoiceOrder!!,
            products = products,
            onDismiss = { invoiceOrder = null },
            onConfirmPayment = { orderId ->
                invoiceOrder = null
                onRequestPayment(orderId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBottomSheet(
    order: com.example.restaurant.data.model.Order,
    products: List<com.example.restaurant.data.model.Product>,
    onDismiss: () -> Unit,
    onConfirmPayment: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 4.dp)
        ) {
            // ── Header hóa đơn ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFFD9534F), Color(0xFFB71C1C))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ReceiptLong, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("HÓA ĐƠN THANH TOÁN", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Mã đơn: #${order.id}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    order.table_number?.let {
                        Text("Bàn: $it", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Danh sách món ──
            Text("CHI TIẾT MÓN ĂN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(10.dp))

            val items = order.items_detail ?: emptyList()
            if (items.isEmpty()) {
                Text("Không có thông tin món ăn", color = Color.Gray, fontSize = 13.sp)
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFAF8F5),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        // Header row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Món", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("SL", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            Text("Thành tiền", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.4f))

                        items.forEachIndexed { idx, item ->
                            val actualPrice = if (item.price > 0) item.price else products.find { it.id == item.product_id }?.price ?: 0.0
                            val lineTotal = (actualPrice * item.quantity).toLong()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                                    if (actualPrice > 0) {
                                        Text(
                                            "${actualPrice.toLong().toVndFormat()} đ/món",
                                            fontSize = 11.sp, color = Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    "x${item.quantity}",
                                    fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp).padding(top = 2.dp)
                                )
                                Text(
                                    if (lineTotal > 0) "${lineTotal.toVndFormat()} đ" else "—",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lineTotal > 0) Color(0xFF1A1A2E) else Color.LightGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (idx < items.size - 1) {
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Tổng cộng ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.5.dp, WarmBrown.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tạm tính", fontSize = 14.sp, color = Color.Gray)
                        Text("${order.total_amount.toLong().toVndFormat()} ₫", fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phí dịch vụ", fontSize = 14.sp, color = Color.Gray)
                        Text("Miễn phí", fontSize = 14.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmBrown.copy(alpha = 0.2f), thickness = 1.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TỔNG THANH TOÁN", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A2E))
                        Text(
                            "${order.total_amount.toLong().toVndFormat()} ₫",
                            fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFD9534F)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Ghi chú thời gian
            if (order.created_at.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, null, tint = Color.LightGray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tạo lúc: ${order.created_at.take(16).replace("T", " ")}", fontSize = 11.sp, color = Color.LightGray)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Nút hành động ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Nút đóng
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.LightGray)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đóng", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                // Nút xác nhận thanh toán
                Button(
                    onClick = { onConfirmPayment(order.id) },
                    modifier = Modifier.weight(2f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD9534F)
                    )
                ) {
                    Icon(Icons.Default.AttachMoney, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Xác nhận Thanh toán", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
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
    
    val draftItems = remember { mutableStateListOf(*(order.items_detail?.toTypedArray() ?: emptyArray())) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp, top = 8.dp)) {
            Text("Chỉnh sửa yêu cầu #${order.id}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Có thể thay đổi trước khi bếp xác nhận", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(24.dp))

            if (draftItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Đã xóa tất cả món", color = Color.Red.copy(alpha=0.7f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    itemsIndexed(draftItems.toList(), key = { _, item -> item.name }) { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF9F9F9)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color=Color(0xFF1A1A2E))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp, border = BorderStroke(1.dp, Color.LightGray)) {
                                        IconButton(onClick = {
                                            val currentIdx = draftItems.indexOf(item)
                                            if (currentIdx != -1) {
                                                if (item.quantity > 1) {
                                                    draftItems[currentIdx] = item.copy(quantity = item.quantity - 1)
                                                } else {
                                                    val removed = draftItems.removeAt(currentIdx)
                                                    scope.launch {
                                                        val snackbarResult = snackbarHostState.showSnackbar(
                                                            message = "Đã bỏ ${removed.name}",
                                                            actionLabel = "KHÔI PHỤC",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                            draftItems.add(removed)
                                                        }
                                                    }
                                                }
                                            }
                                        }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Remove, null, tint = WarmBrown) }
                                    }
                                    
                                    Text("${item.quantity}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
                                    
                                    Surface(shape = CircleShape, color = WarmBrown, shadowElevation = 2.dp) {
                                        IconButton(onClick = {
                                            val currentIdx = draftItems.indexOf(item)
                                            if (currentIdx != -1) {
                                                draftItems[currentIdx] = item.copy(quantity = item.quantity + 1)
                                            }
                                        }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, null, tint = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onConfirm(draftItems.toList()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
            ) {
                Text("Lưu & Cập Nhật", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
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
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 100.dp)
    ) {
        Text("Hồ sơ & Tuỳ chọn", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
        Spacer(modifier = Modifier.height(30.dp))

        // Avatar Section
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, WarmBrown, CircleShape)
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
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                }
            }
            // Edit icon overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(32.dp),
                shape = CircleShape,
                color = WarmBrown,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Input Fields (Settings Card)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Thông tin cá nhân", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { authViewModel.updateUserProfile(token, fullName, phone, address, imageUri) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                ) {
                    if (updateState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Cập nhật hồ sơ", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                
                if (updateState is AuthState.Success) {
                    Text("Cập nhật thành công!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // System Settings Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Cài đặt hệ thống", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { com.example.restaurant.utils.SoundManager.toggleSound() }
                        .padding(vertical = 8.dp)
                ) {
                    val isSoundEnabled by com.example.restaurant.utils.SoundManager.isSoundEnabled.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if(isSoundEnabled) WarmBrown.copy(alpha=0.1f) else Color.LightGray.copy(alpha=0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, null, tint = if(isSoundEnabled) WarmBrown else Color.Gray, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Âm thanh thông báo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                            Text(if (isSoundEnabled) "Chuông đang bật" else "Chuông đã tắt", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = { com.example.restaurant.utils.SoundManager.toggleSound() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = WarmBrown, uncheckedTrackColor = Color.LightGray)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E).copy(alpha=0.05f)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Security, null, tint = Color(0xFF1A1A2E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đổi mật khẩu bảo mật", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF0F0))
        ) {
            Icon(Icons.Default.Logout, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Đăng xuất khỏi hệ thống", color = Color(0xFFD32F2F), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AboutTab(mapWebView: WebView?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Thêm vuốt dọc cho màn hình nhỏ
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 100.dp)
    ) {
        Text("Về chúng tôi", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color.White, Color(0xFFFFF3E0))
                    ))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("DEV: Vũ Minh Chuyên", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = WarmBrown)
                    Text("Class: CNTTK21D", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(shape = CircleShape, color = WarmBrown.copy(alpha=0.1f), modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = WarmBrown, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Địa chỉ", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Đường Z115, X. Quyết Thắng\nTP. Thái Nguyên", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFF1976D2).copy(alpha=0.1f), modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Language, null, tint = Color(0xFF1976D2), modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Website", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("tuyensinh.ictu.edu.vn", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFF4267B2).copy(alpha=0.1f), modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Facebook, null, tint = Color(0xFF4267B2), modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Fanpage", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Đại học CNTT & TT Thái Nguyên", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bản đồ vị trí", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Map, null, tint = WarmBrown)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (mapWebView != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(2.dp, Color.White)
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
        } else {
            // Placeholder khi WebView chưa load
            Surface(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF0EDE8)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, null, tint = WarmBrown.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Đang tải bản đồ...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}


