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
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.example.restaurant.ui.theme.StatusRed
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
import com.example.restaurant.ui.theme.premiumBackground
import com.example.restaurant.ui.viewmodel.NotificationViewModel
import com.example.restaurant.ui.viewmodel.VoucherViewModel
import com.example.restaurant.ui.viewmodel.ReservationViewModel
import com.example.restaurant.data.model.Voucher

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
    onRequestPayment: (Int, Int, Double) -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    onNavigateToReservation: () -> Unit,
    onNavigateToReservationHistory: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var homeClickCount by remember { mutableIntStateOf(0) }
    val orders by restaurantViewModel.orders.collectAsState()
    val products by restaurantViewModel.products.collectAsState()
    val cartItems by restaurantViewModel.cartItems.collectAsState()
    val notificationViewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val voucherViewModel: VoucherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val reservationViewModel: ReservationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userReservations by reservationViewModel.userReservations.collectAsState()

    // Badges
    val activeOrderCount = orders.count { it.order_status == "pending" || it.order_status == "processing" }
    val pendingReservationCount = userReservations.count { it.status == "pending" }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load data
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile(token)
        restaurantViewModel.fetchProducts()
        restaurantViewModel.fetchOrders(token)
        restaurantViewModel.fetchInventory()
        notificationViewModel.fetchUserNotifications(token)
        val userId = authViewModel.userProfile.value?.get("id") as? String ?: ""
        if (userId.isNotBlank()) reservationViewModel.fetchUserReservations(userId)
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

    DisposableEffect(Unit) {
        onDispose {
            restaurantViewModel.stopPayOSPolling()
        }
    }

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


    Box(modifier = Modifier.fillMaxSize().premiumBackground()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { _ ->
            Column(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeTab(
                        token = token,
                        homeClickCount = homeClickCount,
                        restaurantViewModel = restaurantViewModel,
                        authViewModel = authViewModel,
                        notificationViewModel = notificationViewModel,
                        voucherViewModel = voucherViewModel,
                        onNavigateToTable = onNavigateToTable,
                        onNavigateToTakeaway = onNavigateToTakeaway,
                        onNavigateToMyTable = { selectedTab = 1 },
                        onNavigateToReservation = onNavigateToReservation,
                        pendingReservationCount = pendingReservationCount,
                        onNavigateToChatbot = onNavigateToChatbot,
                        onLogout = onLogout
                    )
                    1 -> NotificationsTab(
                        token = token,
                        restaurantViewModel = restaurantViewModel,
                        authViewModel = authViewModel,
                        voucherViewModel = voucherViewModel,
                        onOrderMore = onOrderMore,
                        onRequestPayment = onRequestPayment,
                        snackbarHostState = snackbarHostState
                    )
                    2 -> AboutTab(mapWebView = mapWebView)
                    3 -> SettingsTab(
                        token = token,
                        authViewModel = authViewModel,
                        onNavigateToOrderHistory = onNavigateToOrderHistory,
                        onNavigateToReservationHistory = onNavigateToReservationHistory,
                        onLogout = onLogout
                    )
                }
            }
        }

        // Nav pill nổi trực tiếp trên background — không qua Scaffold bottomBar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navTabDefs = listOf(
                        Triple("Home", Icons.Default.Home, 0),
                        Triple("Thông báo", Icons.Default.Notifications, 1),
                        Triple("Giới thiệu", Icons.Default.Info, 2),
                        Triple("Cài đặt", Icons.Default.Settings, 3)
                    )
                    navTabDefs.forEach { (label, icon, index) ->
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
                                BadgedBox(
                                    badge = {
                                        when (index) {
                                            1 -> if (activeOrderCount > 0) Badge(containerColor = Color(0xFFE53935), contentColor = Color.White) { Text(activeOrderCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                                            else -> {}
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

        // FAB kéo thả — đặt ngoài Scaffold để không bị system navbar che
        DraggableChatbotFab(onClick = onNavigateToChatbot)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    token: String,
    homeClickCount: Int,
    restaurantViewModel: RestaurantViewModel,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    voucherViewModel: VoucherViewModel,
    onNavigateToTable: () -> Unit,
    onNavigateToTakeaway: () -> Unit,
    onNavigateToMyTable: () -> Unit,
    onNavigateToReservation: () -> Unit,
    pendingReservationCount: Int,
    onNavigateToChatbot: () -> Unit,
    onLogout: () -> Unit
) {
    val screenConfig = LocalConfiguration.current
    val isCompact = screenConfig.screenWidthDp <= 360

    val userProfile by authViewModel.userProfile.collectAsState()
    val likedProducts = (userProfile?.get("liked_products") as? List<Number>)?.map { it.toInt() } ?: emptyList()
    val products by restaurantViewModel.products.collectAsState()
    val orders by restaurantViewModel.orders.collectAsState()
    val vouchers by voucherViewModel.vouchers.collectAsState()
    val loyaltyPoints = (userProfile?.get("loyaltyPoints") as? Number)?.toInt() ?: 0
    val rankPoints = (userProfile?.get("totalLoyaltyPoints") as? Number)?.toInt() ?: loyaltyPoints
    
    val images = products.mapNotNull { it.image_url?.takeIf { url -> url.isNotEmpty() } }.take(5)
    
    // Kiểm tra xem khách có đang có bàn chưa thanh toán không
    val hasActiveTable = orders.any { 
        it.user_id == token && 
        it.table_id != null && 
        it.table_id != 0 && 
        (it.payment_status == "unpaid" || it.payment_status == "requested")
    }
    
    var showTableBlockedDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    
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
    // "none" | "featured" | "bestseller"
    var viewAllMode by remember { mutableStateOf("none") }

    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) { products.filter { it.name.contains(searchQuery, ignoreCase = true) } }

    // Tính lượt bán trong tháng hiện tại
    val currentYearMonth = remember {
        val cal = java.util.Calendar.getInstance()
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        "%04d-%02d".format(y, m)
    }
    val monthlySalesCount = remember(orders, currentYearMonth) {
        val cnt = mutableMapOf<Int, Int>()
        orders.forEach { order ->
            if (order.created_at.startsWith(currentYearMonth) && order.payment_status == "paid") {
                order.items_detail?.forEach { item ->
                    cnt[item.product_id] = (cnt[item.product_id] ?: 0) + item.quantity
                }
            }
        }
        cnt
    }
    // Ngưỡng bán chạy: >= 15 lượt trong tháng
    val BESTSELLER_THRESHOLD = 15
    val bestSellerProducts = remember(monthlySalesCount, products) {
        products.filter { (monthlySalesCount[it.id] ?: 0) >= BESTSELLER_THRESHOLD }
            .sortedByDescending { monthlySalesCount[it.id] ?: 0 }
    }
    // Preview (horizontal scroll): top 10 bán chạy nhất (kể cả dưới ngưỡng nếu đã có lượt bán)
    val bestSellerPreview = remember(monthlySalesCount, products) {
        products.filter { (monthlySalesCount[it.id] ?: 0) > 0 }
            .sortedByDescending { monthlySalesCount[it.id] ?: 0 }
            .take(10)
    }

    LaunchedEffect(homeClickCount) {
        viewAllMode = "none"
        searchQuery = ""
    }

    if (showProductDialog != null) {
        ProductDetailDialog(product = showProductDialog!!) {
            showProductDialog = null
        }
    }
    
    val activeVouchers = remember(vouchers, rankPoints) {
        val currentTier = when {
            rankPoints >= 5000 -> "diamond"
            rankPoints >= 1000 -> "gold"
            else -> "all"
        }
        val now = System.currentTimeMillis()
        vouchers.filter { v ->
            val meetsTier = when (v.required_tier) {
                "diamond" -> currentTier == "diamond"
                "gold" -> currentTier == "diamond" || currentTier == "gold"
                else -> true
            }
            val isExpired = v.valid_until in 1..now
            val isUsedUp = v.usage_limit > 0 && v.times_used >= v.usage_limit
            meetsTier && !isExpired && !isUsedUp
        }.take(3) // chỉ lấy max 3 banner
    }
    val combinedCount = activeVouchers.size + images.size
    val pagerState = rememberPagerState(pageCount = { if(combinedCount == 0) 1 else combinedCount })

    // Auto-scroll logic
    LaunchedEffect(pagerState) {
        if (combinedCount > 1) {
            while (true) {
                delay(3500)
                val nextPage = (pagerState.currentPage + 1) % combinedCount
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
                    
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Notification Bell
                        val unreadCount by notificationViewModel.unreadCount.collectAsState()
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            IconButton(onClick = { showNotificationSheet = true }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Thông báo", tint = WarmBrown, modifier = Modifier.size(28.dp))
                            }
                            if (unreadCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Red,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(if (unreadCount > 9) "9+" else unreadCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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

        if (searchQuery.isBlank() && viewAllMode == "none") {
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
                    if (combinedCount > 0) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            if (page < activeVouchers.size) {
                                val v = activeVouchers[page]
                                // Dynamic background color based on tier
                                val bgColor = when(v.required_tier) {
                                    "diamond" -> Color(0xFF283593)
                                    "gold" -> Color(0xFFF57F17)
                                    else -> Color(0xFFD32F2F)
                                }
                                Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
                                    // Add some nice decorative circles
                                    Box(modifier = Modifier.offset(x = (-30).dp, y = (-50).dp).size(150.dp).background(Color.White.copy(alpha=0.1f), CircleShape))
                                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 30.dp).size(120.dp).background(Color.White.copy(alpha=0.1f), CircleShape))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White.copy(alpha = 0.2f)
                                            ) {
                                                val tierBadge = when(v.required_tier) {
                                                    "diamond" -> "👑 KIM CƯƠNG"
                                                    "gold" -> "⭐ HẠNG VÀNG"
                                                    else -> "🎁 HOT VOUCHER"
                                                }
                                                Text(
                                                    tierBadge,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            val discText = if (v.is_percent) "Giảm ${v.discount_amount.toInt()}%" else "Giảm ${v.discount_amount.toLong().toVndFormat()}đ"
                                            Text(
                                                discText,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 22.sp
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Mã: ${v.code}",
                                                color = Color.White.copy(alpha=0.9f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(60.dp), tint = Color.White.copy(alpha=0.8f))
                                    }
                                }
                            } else {
                                val imgIdx = page - activeVouchers.size
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = images[imgIdx],
                                        contentDescription = "Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
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
                                                "Khám Phá Menu Mới",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Món ăn hấp dẫn đang chờ bạn!",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE0E0E0)))
                    }

                    // Dot indicators
                    if (combinedCount > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(combinedCount) { i ->
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
                        title = "Tại bàn", 
                        icon = Icons.Default.TableRestaurant, 
                        gradientColors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)),
                        iconTint = Color(0xFFE65100),
                        onClick = {
                            if (hasActiveTable) showTableBlockedDialog = true
                            else onNavigateToTable()
                        }
                    )
                    ActionCircleBtn(
                        title = "Đặt chỗ", 
                        icon = Icons.Default.EventSeat, 
                        gradientColors = listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7)),
                        iconTint = Color(0xFF6A1B9A),
                        badgeCount = pendingReservationCount,
                        onClick = onNavigateToReservation
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

        if (searchQuery.isBlank() && viewAllMode == "none") {
            // Featured Items Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Món Nổi Bật ✨", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                    TextButton(
                        onClick = { viewAllMode = "featured" },
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
                                        if (product.review_count > 0) {
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
                                                    Text(String.format("%.1f", product.average_rating), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                        
                                        val isLiked = likedProducts.contains(product.id)
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-8).dp, y = 8.dp)
                                                .size(30.dp),
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha=0.9f),
                                            shadowElevation = 2.dp
                                        ) {
                                            IconButton(onClick = { authViewModel.toggleFavoriteProduct(token, product.id) }) {
                                                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.LightGray, modifier = Modifier.size(16.dp))
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

            // Top Món Bán Chạy 🔥
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top Món Bán Chạy 🔥", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
                    TextButton(
                        onClick = { viewAllMode = "bestseller" },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Xem thêm", fontSize = 14.sp, color = WarmBrown, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(12.dp), tint = WarmBrown)
                    }
                }
            }

            item {
                if (bestSellerPreview.isNotEmpty()) {
                    val cfg3 = LocalConfiguration.current
                    val cardWidth = (cfg3.screenWidthDp * 0.42f).dp.coerceIn(140.dp, 200.dp)
                    val imgHeight = (cardWidth.value * 0.72f).dp.coerceIn(100.dp, 150.dp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(bestSellerPreview, key = { it.id }) { product ->
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
                                                Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF5722), modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Hot", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFF5722))
                                            }
                                        }
                                        val isLiked = likedProducts.contains(product.id)
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-8).dp, y = 8.dp)
                                                .size(30.dp),
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha=0.9f),
                                            shadowElevation = 2.dp
                                        ) {
                                            IconButton(onClick = { authViewModel.toggleFavoriteProduct(token, product.id) }) {
                                                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                product.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                color = Color(0xFF1A1A2E),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (product.review_count > 0) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                                    Icon(androidx.compose.material.icons.Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(2.dp))
                                                    Text(String.format("%.1f", product.average_rating), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))

                                        Text(
                                            "${product.price.toLong()} ₫",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WarmBrown
                                        )
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
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có dữ liệu bán hàng", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Grid: featured / bestseller / search results
            val gridProducts = when {
                viewAllMode == "featured" -> products.filter { it.is_featured }
                viewAllMode == "bestseller" -> bestSellerProducts
                else -> filteredProducts
            }
            val gridTitle = when {
                viewAllMode == "featured" -> "✨ Món Nổi Bật"
                viewAllMode == "bestseller" -> "🔥 Bán Chạy Tháng Này (≥$BESTSELLER_THRESHOLD lượt)"
                else -> "Kết quả tìm kiếm"
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewAllMode != "none" && searchQuery.isBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(36.dp).clickable { viewAllMode = "none" }.padding(end = 12.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmBrown, modifier = Modifier.padding(6.dp))
                            }
                        }
                        Text(
                            gridTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A2E)
                        )
                    }
                }
            }
            
            if (gridProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (viewAllMode == "bestseller") Icons.Default.LocalFireDepartment else Icons.Default.SearchOff,
                                null, tint = Color.LightGray, modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (viewAllMode == "bestseller") "Chưa có món nào đạt $BESTSELLER_THRESHOLD lượt bán trong tháng này."
                                else if (viewAllMode == "featured") "Chưa có món nào được đánh dấu nổi bật."
                                else "Không tìm thấy món ăn nào.",
                                color = Color.Gray, fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(gridProducts.chunked(2)) { rowProducts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowProducts.forEach { product ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showProductDialog = product },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 6.dp
                            ) {
                                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.2f)
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
                                                Icon(Icons.Default.Fastfood, null, tint = WarmBrown.copy(alpha=0.5f), modifier = Modifier.size(32.dp))
                                            }
                                        }
                                        val isLikedG = likedProducts.contains(product.id)
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-8).dp, y = 8.dp)
                                                .size(30.dp),
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha=0.9f),
                                            shadowElevation = 2.dp
                                        ) {
                                            IconButton(onClick = { authViewModel.toggleFavoriteProduct(token, product.id) }) {
                                                Icon(if (isLikedG) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLikedG) Color.Red else Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, color = Color(0xFF1A1A2E))
                                        Spacer(Modifier.height(4.dp))
                                        Text("${product.price.toLong()} ₫", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = WarmBrown)
                                        // Hiện số lượt bán trong tháng nếu đang ở bestseller mode
                                        if (viewAllMode == "bestseller") {
                                            val sold = monthlySalesCount[product.id] ?: 0
                                            Spacer(Modifier.height(4.dp))
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFF5722).copy(alpha = 0.1f)) {
                                                Text(
                                                    "🔥 $sold lượt/tháng",
                                                    fontSize = 10.sp, color = Color(0xFFFF5722),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        val stockStatusMap by restaurantViewModel.productStockStatusMap.collectAsState()
                                        if (stockStatusMap[product.id] == StockStatus.OUT_OF_STOCK) {
                                            Spacer(Modifier.height(4.dp))
                                            Text("Hết hàng", fontSize=10.sp, color=Color.Red, fontWeight=FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        if (rowProducts.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

    }
    
    if (showNotificationSheet) {
        val notifications by notificationViewModel.notifications.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 500.dp)) {
                Text("Thông báo của bạn", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có thông báo nào.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(notifications, key = { it.id }) { notif ->
                            val bgColor = if (notif.is_read) Color(0xFFF5F5F5) else Color(0xFFFFF3E0)
                            val iconColor = when(notif.type) {
                                "success" -> StatusGreen
                                "warning" -> StatusRed
                                else -> Color(0xFF2196F3)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    if (!notif.is_read) notificationViewModel.markAsRead(notif.id) 
                                }
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Notifications, null, tint = iconColor, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(notif.title, fontWeight = if (notif.is_read) FontWeight.Normal else FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(notif.body, color = Color.DarkGray, fontSize = 13.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCircleBtn(title: String, icon: ImageVector, onClick: () -> Unit, gradientColors: List<Color>, iconTint: Color, badgeCount: Int = 0) {
    val cfg = LocalConfiguration.current
    val btnSize = (cfg.screenWidthDp * 0.17f).dp.coerceIn(54.dp, 76.dp)
    val iconSize = (btnSize.value * 0.44f).dp
    val labelFontSize = if (cfg.screenWidthDp < 380) 11.sp else 12.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(btnSize + 14.dp)) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(containerColor = Color(0xFFE53935), contentColor = Color.White) {
                        Text(badgeCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) {
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
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = labelFontSize, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun NotificationsTab(
    token: String,
    restaurantViewModel: RestaurantViewModel,
    authViewModel: AuthViewModel,
    voucherViewModel: VoucherViewModel,
    onOrderMore: (Int, String) -> Unit,
    onRequestPayment: (Int, Int, Double) -> Unit,
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
        restaurantViewModel.markCompletedIdsAsSeen(newlyCompleted.map { it.id }.toSet())
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
            authViewModel = authViewModel,
            voucherViewModel = voucherViewModel,
            onDismiss = { invoiceOrder = null },
            onConfirmPayment = { orderId, pointsUsed, discountAmount ->
                invoiceOrder = null
                onRequestPayment(orderId, pointsUsed, discountAmount)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBottomSheet(
    order: com.example.restaurant.data.model.Order,
    products: List<com.example.restaurant.data.model.Product>,
    authViewModel: AuthViewModel,
    voucherViewModel: VoucherViewModel,
    onDismiss: () -> Unit,
    onConfirmPayment: (Int, Int, Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val userProfile by authViewModel.userProfile.collectAsState()
    val loyaltyPoints = (userProfile?.get("loyaltyPoints") as? Number)?.toInt() ?: 0
    val rankPoints = (userProfile?.get("totalLoyaltyPoints") as? Number)?.toInt() ?: loyaltyPoints
    var usePoints by remember { mutableStateOf(false) }
    
    var promoCodeInput by remember { mutableStateOf("") }
    var appliedVoucher by remember { mutableStateOf<Voucher?>(null) }
    var promoMessage by remember { mutableStateOf("") }
    var promoIsError by remember { mutableStateOf(false) }
    var showVoucherWallet by remember { mutableStateOf(false) }
    
    val vouchers by voucherViewModel.vouchers.collectAsState()

    val pointsToUse = if (usePoints) minOf(loyaltyPoints, order.total_amount.toInt()) else 0
    val pointsDiscountAmount = pointsToUse.toDouble()
    
    val remainingAfterPoints = maxOf(0.0, order.total_amount - pointsDiscountAmount)
    
    val voucherDiscountAmount = appliedVoucher?.let { v ->
        var disc = if (v.is_percent) remainingAfterPoints * (v.discount_amount / 100.0) else v.discount_amount
        if (v.max_discount != null && v.max_discount > 0.0) {
            disc = minOf(disc, v.max_discount)
        }
        disc
    } ?: 0.0

    val totalDiscount = pointsDiscountAmount + voucherDiscountAmount
    val finalTotal = maxOf(0.0, order.total_amount - totalDiscount)

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
                    
                    if (loyaltyPoints > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmBrown.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dùng điểm tích luỹ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Bạn có ${loyaltyPoints.toLong().toVndFormat()} điểm", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = usePoints,
                                onCheckedChange = { usePoints = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = WarmBrown, checkedTrackColor = WarmBrown.copy(alpha=0.3f))
                            )
                        }
                        
                        if (usePoints) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Giảm giá (Điểm)", fontSize = 14.sp, color = Color(0xFFE65100))
                                Text("-${pointsDiscountAmount.toLong().toVndFormat()} ₫", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                        }
                    }

                    // --- Promo Code Input Section ---
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmBrown.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promoCodeInput,
                            onValueChange = { promoCodeInput = it.uppercase() },
                            placeholder = { Text("Nhập mã khuyến mãi", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarmBrown,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        
                        // Icon Button để mở Kho Voucher
                        IconButton(
                            onClick = { showVoucherWallet = true },
                            modifier = Modifier.size(50.dp).background(WarmBrown.copy(alpha=0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "Kho Voucher", tint = WarmBrown)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (promoCodeInput.isBlank()) {
                                    promoMessage = "Vui lòng nhập mã."
                                    promoIsError = true
                                    appliedVoucher = null
                                    return@Button
                                }
                                val v = vouchers.find { it.code.equals(promoCodeInput, ignoreCase = true) }
                                if (v == null) {
                                    promoMessage = "Mã không hợp lệ."
                                    promoIsError = true
                                    appliedVoucher = null
                                    return@Button
                                }
                                val now = System.currentTimeMillis()
                                if (v.valid_until > 0 && v.valid_until < now) {
                                    promoMessage = "Mã đã hết hạn."
                                    promoIsError = true
                                    appliedVoucher = null
                                    return@Button
                                }
                                if (v.usage_limit > 0 && v.times_used >= v.usage_limit) {
                                    promoMessage = "Mã đã hết lượt sử dụng."
                                    promoIsError = true
                                    appliedVoucher = null
                                    return@Button
                                }
                                if (order.total_amount < v.min_order_value) {
                                    promoMessage = "Đơn chưa đạt tối thiểu ${v.min_order_value.toLong().toVndFormat()}đ."
                                    promoIsError = true
                                    appliedVoucher = null
                                    return@Button
                                }
                                
                                val currentTier = when {
                                    rankPoints >= 5000 -> "diamond"
                                    rankPoints >= 1000 -> "gold"
                                    else -> "all"
                                }
                                val meetsTier = when (v.required_tier) {
                                    "diamond" -> currentTier == "diamond"
                                    "gold" -> currentTier == "diamond" || currentTier == "gold"
                                    else -> true
                                }
                                if (!meetsTier) {
                                    promoMessage = "Mã không dành cho hạng thành viên của bạn."
                                    promoIsError = true
                                    appliedVoucher = null
                                    return@Button
                                }
                                
                                appliedVoucher = v
                                promoCodeInput = v.code
                                promoMessage = "Đã áp dụng mã thành công!"
                                promoIsError = false
                            },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                        ) {
                            Text("Áp dụng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (promoMessage.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = promoMessage,
                            color = if (promoIsError) StatusRed else StatusGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (appliedVoucher != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Giảm giá (Voucher)", fontSize = 14.sp, color = StatusGreen)
                            Text("-${voucherDiscountAmount.toLong().toVndFormat()} ₫", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmBrown.copy(alpha = 0.2f), thickness = 1.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TỔNG THANH TOÁN", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A2E))
                        Text(
                            "${finalTotal.toLong().toVndFormat()} ₫",
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
                    onClick = { 
                        appliedVoucher?.let {
                            voucherViewModel.incrementVoucherUsage(it.id)
                        }
                        onConfirmPayment(order.id, pointsToUse, totalDiscount) 
                    },
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
    
    if (showVoucherWallet) {
        AlertDialog(
            onDismissRequest = { showVoucherWallet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Kho Voucher của bạn", fontWeight = FontWeight.ExtraBold) },
            text = {
                val currentTier = when {
                    rankPoints >= 5000 -> "diamond"
                    rankPoints >= 1000 -> "gold"
                    else -> "all"
                }
                val now = System.currentTimeMillis()
                val eligibleVouchers = vouchers.filter { v ->
                    val meetsTier = when (v.required_tier) {
                        "diamond" -> currentTier == "diamond"
                        "gold" -> currentTier == "diamond" || currentTier == "gold"
                        else -> true
                    }
                    val isExpired = v.valid_until in 1..now
                    val isUsedUp = v.usage_limit > 0 && v.times_used >= v.usage_limit
                    meetsTier && !isExpired && !isUsedUp
                }

                if (eligibleVouchers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Không có mã khuyến mãi khả dụng", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(eligibleVouchers, key = { it.id }) { v ->
                            val meetsMinOrder = order.total_amount >= v.min_order_value
                            val alpha = if (meetsMinOrder) 1f else 0.5f
                            
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarmBrown.copy(alpha=0.1f * alpha),
                                modifier = Modifier.fillMaxWidth().clickable(enabled = meetsMinOrder) {
                                    promoCodeInput = v.code
                                    appliedVoucher = v
                                    promoMessage = "Đã áp dụng mã thành công!"
                                    promoIsError = false
                                    showVoucherWallet = false
                                }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(v.code, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = WarmBrown.copy(alpha=alpha))
                                    Spacer(Modifier.height(4.dp))
                                    val discText = if (v.is_percent) "Giảm ${v.discount_amount.toInt()}%" else "Giảm ${v.discount_amount.toLong().toVndFormat()}đ"
                                    val maxText = if (v.is_percent && v.max_discount != null && v.max_discount > 0.0) " (Tối đa ${v.max_discount.toLong().toVndFormat()}đ)" else ""
                                    Text(discText + maxText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A1A2E).copy(alpha=alpha))
                                    Text("Đơn tối thiểu: ${v.min_order_value.toLong().toVndFormat()}đ", fontSize = 12.sp, color = Color.Gray.copy(alpha=alpha))
                                    if (!meetsMinOrder) {
                                        Spacer(Modifier.height(4.dp))
                                        val diff = v.min_order_value - order.total_amount
                                        Text("Cần mua thêm ${diff.toLong().toVndFormat()}đ", fontSize = 12.sp, color = StatusRed)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoucherWallet = false }) {
                    Text("Đóng", color = Color.Gray)
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
fun SettingsTab(token: String, authViewModel: AuthViewModel, onNavigateToOrderHistory: () -> Unit, onNavigateToReservationHistory: () -> Unit, onLogout: () -> Unit) {
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile(token)
    }
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

        // Loyalty Card & Rank
        val loyaltyPoints = (userProfile?.get("loyaltyPoints") as? Number)?.toInt() ?: 0
        val rankPoints = (userProfile?.get("totalLoyaltyPoints") as? Number)?.toInt() ?: loyaltyPoints
        
        val rankName = when {
            rankPoints >= 5000 -> "Thành Viên Kim Cương"
            rankPoints >= 1000 -> "Thành Viên Vàng"
            else -> "Thành Viên Khởi Đầu"
        }
        val rankColor = when {
            rankPoints >= 5000 -> Color(0xFF3F51B5)
            rankPoints >= 1000 -> Color(0xFFFF8F00)
            else -> Color(0xFF8D6E63)
        }
        val rankBgColor = when {
            rankPoints >= 5000 -> Color(0xFFE8EAF6)
            rankPoints >= 1000 -> Color(0xFFFFF8E1)
            else -> Color(0xFFEFEBE9)
        }
        val rankIcon = when {
            rankPoints >= 5000 -> Icons.Default.Star
            rankPoints >= 1000 -> Icons.Default.Stars
            else -> Icons.Default.Face
        }
        
        val nextRankPoints = when {
            rankPoints >= 5000 -> 0
            rankPoints >= 1000 -> 5000
            else -> 1000
        }
        val progress = if (nextRankPoints > 0) (rankPoints.toFloat() / nextRankPoints).coerceIn(0f, 1f) else 1f

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = rankBgColor,
            border = BorderStroke(1.dp, rankColor.copy(alpha = 0.5f)),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(rankIcon, contentDescription = "Rank", tint = rankColor, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Điểm tích luỹ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = rankColor)
                            Text(rankName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = rankColor)
                        }
                    }
                    Text("$loyaltyPoints", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = rankColor)
                }
                
                if (nextRankPoints > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tiến trình thăng hạng", fontSize = 12.sp, color = rankColor.copy(alpha = 0.8f))
                        Text("${loyaltyPoints}/${nextRankPoints}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = rankColor,
                        trackColor = rankColor.copy(alpha = 0.2f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

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
            onClick = onNavigateToOrderHistory,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown.copy(alpha=0.1f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(Icons.Default.ReceiptLong, null, tint = WarmBrown, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Lịch sử đơn hàng", color = WarmBrown, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToReservationHistory,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown.copy(alpha=0.1f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(Icons.Default.EventSeat, null, tint = WarmBrown, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Lịch sử đặt bàn", color = WarmBrown, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

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
        Text("About Me", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A2E))
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


