package com.example.restaurant.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.data.model.OrderItemDetail
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.restaurant.data.model.*
import com.example.restaurant.ui.theme.*
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.ui.viewmodel.StockStatus
import com.example.restaurant.ui.viewmodel.MenuScanViewModel
import com.example.restaurant.ui.viewmodel.IngredientScanViewModel
import com.example.restaurant.ui.viewmodel.AdminAnalyticsViewModel
import com.example.restaurant.ui.viewmodel.AIInsightState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restaurant.utils.toVndFormat
import com.example.restaurant.ui.theme.premiumBackground
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    token: String,
    viewModel: RestaurantViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Quản lý bàn", "Thực đơn", "Thống kê")
    val context = LocalContext.current

    // Sub-screen state for invoices
    var showInvoiceList by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    // collectLatest trong LaunchedEffect đã lifecycle-safe (tied to Composition)
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Navigate to invoice detail screen
    if (selectedOrder != null) {
        AdminInvoiceDetailScreen(
            order = selectedOrder!!,
            token = token,
            viewModel = viewModel,
            onBack = { selectedOrder = null }
        )
        return
    }

    // Navigate to invoice list screen
    if (showInvoiceList) {
        AdminInvoiceTodayScreen(
            token = token,
            viewModel = viewModel,
            onBack = { showInvoiceList = false },
            onOrderClick = { order -> selectedOrder = order }
        )
        return
    }

    // IngredientScan state
    val ingredientScanViewModel: IngredientScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val ingredientScanState by ingredientScanViewModel.scanState.collectAsState()
    var showIngredientScanResult by remember { mutableStateOf(false) }

    if (showIngredientScanResult) {
        IngredientScanResultScreen(
            scanViewModel = ingredientScanViewModel,
            restaurantViewModel = viewModel,
            onBack = { showIngredientScanResult = false; viewModel.fetchInventory() },
            onScanAgain = { uri ->
                ingredientScanViewModel.scanIngredientImage(uri, viewModel.ingredients.value, context)
            }
        )
        return
    }

    Scaffold(
        containerColor = CreamBG,
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
                            Triple("Tổng quan", Icons.Default.Home, 0),
                            Triple("Quản lý bàn", Icons.Default.TableRestaurant, 1),
                            Triple("Thực đơn", Icons.Default.RestaurantMenu, 2),
                            Triple("Thống kê", Icons.Default.BarChart, 3),
                            Triple("Kho NL", Icons.Default.ShoppingCart, 4),
                        )
                        navItems.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.85f,
                                animationSpec = spring(dampingRatio = 0.5f), label = "navScale"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).clickable { selectedTab = index }.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .background(if (isSelected) WarmBrown.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = if (isSelected) WarmBrown else Color(0xFFADB5BD), modifier = Modifier.size(22.dp))
                                }
                                Text(label, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) WarmBrown else Color(0xFFADB5BD))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().premiumBackground()
        ) {
            val allTables by viewModel.tables.collectAsState()
            val allOrders by viewModel.orders.collectAsState()

            when (selectedTab) {
                0 -> {
                    // Dashboard header chỉ hiện ở Tổng quan
                    val occupiedCount = allTables.count { it.status == "occupied" }
                    val availableCount = allTables.count { it.status == "available" }

                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(WarmBrown, WarmBrown.copy(alpha = 0.80f))
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
                                    Text("WELCOME BACK", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), letterSpacing = 2.sp)
                                    Text("Admin Dashboard", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                TextButton(onClick = onLogout) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Stat cards với wrapContentHeight để không bị cắt
                                Surface(
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Tổng bàn", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("${allTables.size}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF6B9B76).copy(alpha = 0.9f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Trống", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("$availableCount", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFD9534F).copy(alpha = 0.9f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Đang dùng", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("$occupiedCount", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    AdminMainHome(token, viewModel, modifier = Modifier.weight(1f))
                }
                1 -> AdminTableManager(token, viewModel)
                2 -> AdminProductInventory(token, viewModel)
                3 -> AdminStatsView(token, viewModel, onInvoiceListClick = { showInvoiceList = true })
                4 -> AdminIngredientInventory(
                         token = token,
                         viewModel = viewModel,
                         onScanClick = { uri ->
                             ingredientScanViewModel.scanIngredientImage(uri, viewModel.ingredients.value, context)
                             showIngredientScanResult = true
                         }
                     )
            }
        }
    }
}

// =====================================================
// TAB HOME: Dashboard B?n (Card Grid)
// =====================================================
@Composable
fun AdminMainHome(token: String, viewModel: RestaurantViewModel, modifier: Modifier = Modifier) {
    val tables by viewModel.tables.collectAsState()
    val orders by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchTables(token) }

    val sortedTables = remember(tables) { tables.sortedWith(compareBy({ it.table_number.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }, { it.table_number })) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sortedTables, key = { it.id }) { table ->
            TableCardView(table = table, orders = orders, onClick = {})
        }
    }
}

// =====================================================
// TAB 1: Quản lý bàn - theo m?u sketch 1
// =====================================================
@Composable
fun AdminTableManager(token: String, viewModel: RestaurantViewModel) {
    val tables by viewModel.tables.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var tableToDelete by remember { mutableStateOf<RestaurantTable?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchTables(token)
    }

    if (showDialog) {
        TableEditDialog(
            table = selectedTable,
            onDismiss = { showDialog = false },
            onConfirm = { tableData ->
                if (selectedTable == null) viewModel.addTable(tableData)
                else viewModel.updateTableAdmin(tableData.copy(id = selectedTable!!.id))
                showDialog = false
            }
        )
    }

    if (tableToDelete != null) {
        DeleteConfirmDialog(
            title = "Xóa bàn",
            message = "Bạn có chắc muốn xóa \"${tableToDelete!!.table_number}\"? Thao tác này không thể hoàn tác.",
            onDismiss = { tableToDelete = null },
            onConfirm = {
                viewModel.deleteTableAdmin(tableToDelete!!.id)
                tableToDelete = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // --- Thanh "Tổng số bàn" + "Thêm bàn" ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f).height(96.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableRestaurant, null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tổng số bàn", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${tables.size}", fontWeight = FontWeight.Black, fontSize = 28.sp, color = StatusGreen)
                    }
                }
            }
            Surface(
                modifier = Modifier.height(96.dp).clickable { selectedTable = null; showDialog = true },
                shape = RoundedCornerShape(20.dp),
                color = WarmBrown,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm bàn", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- Grid 2 cột danh sách bàn ---
        val sortedTables2 = remember(tables) { tables.sortedWith(compareBy({ it.table_number.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }, { it.table_number })) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(sortedTables2, key = { it.id }) { table ->
                val statusColor = when (table.status) {
                    "available" -> StatusGreen
                    "occupied" -> StatusRed
                    else -> StatusYellow
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Thể hiện trạng thái bàn bằng một dải hẹp bên trái
                        Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(statusColor))
                        
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(table.table_number, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1A1A2E))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        when (table.status) { "available" -> "Trống"; "occupied" -> "Đang dùng"; else -> "Khác" },
                                        color = statusColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(Modifier.width(4.dp))
                                Text("${table.capacity} khách", color = Color.Gray, fontSize = 11.sp)
                            }
                            
                            // Nút hành động sửa/xóa dùng icon thay cho text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = WarmBrown.copy(alpha = 0.1f),
                                    modifier = Modifier.size(32.dp).clickable { selectedTable = table; showDialog = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, null, tint = WarmBrown, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = StatusRed.copy(alpha = 0.1f),
                                    modifier = Modifier.size(32.dp).clickable { tableToDelete = table }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Delete, null, tint = StatusRed, modifier = Modifier.size(14.dp))
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

@Composable
fun TableEditDialog(table: RestaurantTable?, onDismiss: () -> Unit, onConfirm: (RestaurantTable) -> Unit) {
    var name by remember { mutableStateOf(table?.table_number ?: "") }
    var capacity by remember { mutableStateOf(table?.capacity?.toString() ?: "4") }
    val isEdit = table != null

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
                        if (isEdit) "Sửa thông tin bàn" else "Thêm bàn mới",
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
                    label = { Text("Tên bàn") },
                    placeholder = { Text("VD: Bàn 5", color = Color.LightGray) },
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
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = { Text("Sức chứa (người)") },
                    placeholder = { Text("VD: 4", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = WarmBrown) },
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
                onClick = { onConfirm(RestaurantTable(0, name, capacity.toIntOrNull() ?: 4, table?.status ?: "available")) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "Cập nhật" else "Thêm bàn", fontWeight = FontWeight.Bold)
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
// TAB 2: Thức ăn - theo menu sketch 2
// =====================================================
@Composable
fun AdminProductInventory(token: String, viewModel: RestaurantViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    // MenuScan state
    val scanViewModel: MenuScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val scanState by scanViewModel.scanState.collectAsState()
    var showScanResult by remember { mutableStateOf(false) }

    // Nếu đang hiển thị kết quả quét
    if (showScanResult) {
        MenuScanResultScreen(
            scanViewModel = scanViewModel,
            restaurantViewModel = viewModel,
            adminToken = token,
            onBack = { showScanResult = false; viewModel.fetchProducts() },
            onScanAgain = { uri ->
                scanViewModel.scanMenuImage(uri, products, categories, ingredients, context)
            }
        )
        return
    }

    // Launcher chọn ảnh menu để quét
    val menuImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showScanResult = true
            scanViewModel.scanMenuImage(uri, products, categories, ingredients, context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
        viewModel.fetchCategories()
        viewModel.fetchInventory()
    }

    // remember wrap — tránh recompute mỗi recompose
    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter {
            (selectedCategoryId == null || it.category_id == selectedCategoryId) &&
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    if (showDialog) {
        ProductEditDialog(
            product = selectedProduct,
            categories = categories,
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            onConfirm = { productData ->
                if (selectedProduct == null) viewModel.addProduct(token, productData)
                else viewModel.updateProduct(token, productData.copy(id = selectedProduct!!.id))
                showDialog = false
            }
        )
    }

    if (productToDelete != null) {
        DeleteConfirmDialog(
            title = "Xóa món ăn",
            message = "Bạn có chắc muốn xóa \"${productToDelete!!.name}\"? Thao tác này không thể hoàn tác.",
            onDismiss = { productToDelete = null },
            onConfirm = {
                viewModel.deleteProduct(token, productToDelete!!.id)
                productToDelete = null
            }
        )
    }

    if (showClearAllConfirm) {
        DeleteConfirmDialog(
            title = "Xóa toàn bộ thực đơn",
            message = "CẢNH BÁO: BẠN CÓ CHẮC MUỐN XÓA TẤT CẢ MÓN ĂN HIỆN CÓ? Thao tác này không thể hoàn tác.",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                viewModel.clearAllProducts(token)
                showClearAllConfirm = false
            }
        )
    }

    val featuredCount = products.count { it.is_featured }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F3EE)),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {

        // Header gradient
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(WarmBrown, WarmBrown.copy(alpha = 0.75f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Quan ly thuc don", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${products.size} mon",
                            color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${categories.size} danh muc  -  $featuredCount noi bat",
                            color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(20.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center) { Text("\uD83C\uDF5C", fontSize = 11.sp) }
                            Box(Modifier.size(20.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center) { Text("\uD83C\uDF57", fontSize = 11.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(20.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center) { Text("\uD83E\uDD57", fontSize = 11.sp) }
                            Box(Modifier.size(20.dp).background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center) { Text("\uD83C\uDF79", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }

        // Stat cards 1x2
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFFB300).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("\u2B50", fontSize = 16.sp) }
                            Spacer(Modifier.width(8.dp))
                            Text("Noi bat", fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "$featuredCount mon",
                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Hien thi uu tien",
                            fontSize = 11.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Medium
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(WarmBrown.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("\uD83D\uDDC2\uFE0F", fontSize = 16.sp) }
                            Spacer(Modifier.width(8.dp))
                            Text("Danh muc", fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${categories.size}",
                            fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${filteredProducts.size} mon hien thi",
                            fontSize = 11.sp, color = WarmBrown, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Action bar
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tim kiem mon an...", color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = WarmBrown, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f).clickable { menuImagePicker.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1565C0),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("AI Quet", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f).clickable { selectedProduct = null; showDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = WarmBrown,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Them mon", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            modifier = Modifier.clickable { showClearAllConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD9534F),
                            shadowElevation = 1.dp
                        ) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DeleteForever, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Category chips
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text("Danh muc", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            val isSelected = selectedCategoryId == null
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) WarmBrown else Color(0xFFF7F3EE),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.clickable { selectedCategoryId = null }
                            ) {
                                Text(
                                    text = "Tat ca (${products.size})",
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        items(categories) { category ->
                            val isSelected = selectedCategoryId == category.id
                            val count = products.count { it.category_id == category.id }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) WarmBrown else Color(0xFFF7F3EE),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.clickable { selectedCategoryId = category.id }
                            ) {
                                Text(
                                    text = "${category.name} ($count)",
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Product list
        if (filteredProducts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("\uD83C\uDF7D\uFE0F", fontSize = 36.sp)
                            Text("Chua co mon nao", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (product.is_featured) Color(0xFFFFFBF0) else Color.White,
                    shadowElevation = if (product.is_featured) 3.dp else 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!product.image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = product.image_url,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFF7F3EE), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RestaurantMenu, null, tint = WarmBrown.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (product.is_featured) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${product.price.toVndFormat()}d", color = WarmBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                val stockStatus = viewModel.getProductStockStatus(product)
                                val (statusText, statusColor) = when (stockStatus) {
                                    StockStatus.OK -> "Con hang" to StatusGreen
                                    StockStatus.LOW_STOCK -> "Sap het" to StatusYellow
                                    StockStatus.OUT_OF_STOCK -> "Het hang" to StatusRed
                                    StockStatus.NO_RECIPE -> "Chua co CT" to Color.Gray
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.13f)) {
                                    Text(
                                        statusText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.toggleProductFeatured(product.id, !product.is_featured) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (product.is_featured) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Noi bat",
                                    tint = if (product.is_featured) Color(0xFFFFB300) else Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Row {
                                TextButton(
                                    onClick = { selectedProduct = product; showDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text("Sua", color = WarmBrown, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                Text("|", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                                TextButton(
                                    onClick = { productToDelete = product },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text("Xoa", color = StatusRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
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
fun ProductEditDialog(
    product: Product?,
    categories: List<com.example.restaurant.data.model.Category>,
    viewModel: RestaurantViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toLong()?.toString() ?: "") }
    var selectedCatId by remember { mutableIntStateOf(product?.category_id ?: 1) }
    var ingredients by remember { mutableStateOf(product?.ingredients ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf(product?.image_url ?: "") }
    var isUploading by remember { mutableStateOf(false) }
    val isEdit = product != null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Recipe state
    val allIngredients by viewModel.ingredients.collectAsState()
    var recipeItems = remember { mutableStateListOf<RecipeItem>().also { it.addAll(product?.recipe ?: emptyList()) } }
    var showAddRecipe by remember { mutableStateOf(false) }
    var recipeIngId by remember { mutableStateOf("") }
    var recipeQty by remember { mutableStateOf("") }
    var recipeUnit by remember { mutableStateOf("gram") }
    var recipeWaste by remember { mutableStateOf("0") }
    var recipeIngExpanded by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            imageUrl = ""
        }
    }

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
                        if (isEdit) "Sửa thông tin món" else "Thêm món mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                // ---- Vùng chọn ảnh ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CreamBG)
                        .border(1.5.dp, WarmBrown.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val previewModel: Any? = imageUri ?: imageUrl.takeIf { it.isNotBlank() }
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = "ảnh món",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(WarmBrown, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Đổi ảnh", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = WarmBrown,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Nhấn để chọn ảnh", color = WarmBrown, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("JPG, PNG tối đa 5MB", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                // ---- Tên món ----
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên món") },
                    placeholder = { Text("VD: Phở bò", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Star, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        focusedLabelColor = WarmBrown
                    )
                )

                // ---- Giá tiền ----
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Giá tiền (VND)") },
                    placeholder = { Text("VD: 65000", color = Color.LightGray) },
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

                // ---- Thành phần món ăn ----
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Thành phần món ăn") },
                    placeholder = { Text("VD: Thịt bò, hành tây, gia vị", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.RestaurantMenu, null, tint = WarmBrown) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown,
                        focusedLabelColor = WarmBrown
                    )
                )

                // ---- Danh mục ----
                var showAddCategory by remember { mutableStateOf(false) }
                var newCategoryName by remember { mutableStateOf("") }

                if (categories.isNotEmpty() || showAddCategory) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Danh mục:", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                        TextButton(
                            onClick = { showAddCategory = !showAddCategory },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(if (showAddCategory) "Hủy thêm" else "+ Thêm danh mục", fontSize = 12.sp, color = WarmBrown)
                        }
                    }

                    if (showAddCategory) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                placeholder = { Text("Tên danh mục mới", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmBrown,
                                    focusedLabelColor = WarmBrown
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        viewModel.addCategory(newCategoryName) { newCat ->
                                            selectedCatId = newCat.id
                                            showAddCategory = false
                                            newCategoryName = ""
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("Lưu")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (cat in categories) {
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarmBrown.copy(alpha = 0.15f),
                                    selectedLabelColor = WarmBrown
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedCatId == cat.id,
                                    selectedBorderColor = WarmBrown
                                )
                            )
                        }
                    }

                    // ---- PHẦN ĐỊNH LƯỢNG NGUYÊN LIỆU (RECIPE) ----
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8F0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBrown.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Định lượng nguyên liệu", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = WarmBrown)
                                TextButton(
                                    onClick = { showAddRecipe = !showAddRecipe; recipeIngId = ""; recipeQty = ""; recipeWaste = "0" },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(if (showAddRecipe) Icons.Default.Close else Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (showAddRecipe) "Hủy" else "Thêm", fontSize = 12.sp, color = WarmBrown)
                                }
                            }

                            recipeItems.forEachIndexed { index, ri ->
                                val ingName = allIngredients.find { it.id.toString() == ri.ingredient_id }?.name
                                    ?: "ID: ${ri.ingredient_id}"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = WarmBrown.copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(ingName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            val wasteText = if (ri.waste_percent > 0) " (hao ${(ri.waste_percent * 100).toInt()}%)" else ""
                                            Text("${ri.quantity} ${ri.unit}$wasteText", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        IconButton(
                                            onClick = { recipeItems.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            if (recipeItems.isEmpty()) {
                                Text("• Chưa có nguyên liệu định lượng. Kho sẽ không tự trừ cho món này.",
                                    fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
                            }

                            if (showAddRecipe) {
                                HorizontalDivider(color = WarmBrown.copy(alpha = 0.2f))

                                ExposedDropdownMenuBox(
                                    expanded = recipeIngExpanded,
                                    onExpandedChange = { recipeIngExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = allIngredients.find { it.id.toString() == recipeIngId }?.name ?: "Chọn nguyên liệu...",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Nguyên liệu", fontSize = 12.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipeIngExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    ExposedDropdownMenu(
                                        expanded = recipeIngExpanded,
                                        onDismissRequest = { recipeIngExpanded = false }
                                    ) {
                                        allIngredients.forEach { ing ->
                                            DropdownMenuItem(
                                                text = { Text("${ing.name} (${ing.unit})", fontSize = 13.sp) },
                                                onClick = {
                                                    recipeIngId = ing.id.toString()
                                                    recipeUnit = ing.unit
                                                    recipeIngExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = recipeQty,
                                        onValueChange = { recipeQty = it },
                                        label = { Text("Số lượng", fontSize = 11.sp) },
                                        suffix = { Text(recipeUnit, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                                        )
                                    )
                                    OutlinedTextField(
                                        value = recipeWaste,
                                        onValueChange = { recipeWaste = it },
                                        label = { Text("Hao hụt %", fontSize = 11.sp) },
                                        suffix = { Text("%", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        val qty = recipeQty.toDoubleOrNull() ?: 0.0
                                        val waste = (recipeWaste.toDoubleOrNull() ?: 0.0) / 100.0
                                        if (recipeIngId.isNotBlank() && qty > 0) {
                                            val newItem = RecipeItem(
                                                ingredient_id = recipeIngId,
                                                quantity = qty,
                                                unit = recipeUnit,
                                                waste_percent = waste
                                            )
                                            recipeItems.add(newItem)
                                            recipeIngId = ""; recipeQty = ""; recipeWaste = "0"
                                            showAddRecipe = false
                                        }
                                    },
                                    enabled = recipeIngId.isNotBlank() && recipeQty.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Thêm vào công thức", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isUploading = true
                        var finalImageUrl = imageUrl
                        if (imageUri != null) {
                            val uploadedUrl = viewModel.uploadProductImage(imageUri!!)
                            if (uploadedUrl != null) {
                                finalImageUrl = uploadedUrl
                            } else {
                                Toast.makeText(context, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
                                isUploading = false
                                return@launch
                            }
                        }
                        isUploading = false
                        onConfirm(
                            Product(
                                id = product?.id ?: 0,
                                category_id = selectedCatId,
                                name = name,
                                description = "",
                                price = price.toDoubleOrNull() ?: 0.0,
                                image_url = finalImageUrl,
                                is_available = 1,
                                category_name = null,
                                ingredients = ingredients,
                                recipe = if (recipeItems.isNotEmpty()) recipeItems.toList() else null
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && price.isNotBlank() && !isUploading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Đang upload...", fontWeight = FontWeight.Bold)
                } else {
                    Text(if (isEdit) "Cập nhật" else "Thêm món", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isUploading,
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
// DIALOG Xóa, xác nhận xóa - Dùng chung cho Bàn & món
// =====================================================
@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
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
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        },
        text = {
            Text(message, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
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

// =====================================================
// TAB 3: Thống kê (layout mới + AI Analytics)
// =====================================================
@Composable
fun AdminStatsView(token: String, viewModel: RestaurantViewModel, onInvoiceListClick: () -> Unit) {
    val orders by viewModel.orders.collectAsState()
    val history by viewModel.dailyRevenueHistory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchOrders(token)
        viewModel.fetchDailyRevenueHistory()
    }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val thisMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val todayRecord = history.find { it.date == todayStr }
    val todayRevenueFromOrders = orders
        .filter { it.created_at.startsWith(todayStr) && it.payment_status == "paid" }
        .sumOf { it.total_amount }
    val todayRevenue = todayRecord?.revenue ?: todayRevenueFromOrders
    val paidTodayOrders = orders.filter { it.created_at.startsWith(todayStr) && it.payment_status == "paid" }
    val allTodayOrders = orders.filter { it.created_at.startsWith(todayStr) }
    val monthRevenue = history.filter { it.date.startsWith(thisMonthStr) }.sumOf { it.revenue }

    val sortedHistory = remember(history) { history.sortedByDescending { it.date } }
    val recentHistory = history.sortedBy { it.date }.takeLast(7)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // --- KPI Cards ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = Color(0xFF5C3317)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(32.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Text("Hôm nay", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("${todayRevenue.toVndFormat()}đ", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Text("${paidTodayOrders.size} đơn TT • ${allTodayOrders.size} tổng", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = Color(0xFF1B5E20)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(32.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Text("Tháng ${thisMonthStr.takeLast(2)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("${monthRevenue.toVndFormat()}đ", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        val cnt = history.filter { it.date.startsWith(thisMonthStr) }.sumOf { it.order_count }
                        Text("$cnt đơn cả tháng", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // --- Hóa đơn hôm nay ---
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onInvoiceListClick() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, WarmBrown.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = WarmBrown, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Hóa đơn hôm nay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${allTodayOrders.size} hóa đơn", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = WarmBrown)
                }
            }
        }

        // --- Biểu đồ 7 ngày ---
        if (recentHistory.isNotEmpty()) {
            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Doanh thu 7 ngày gần nhất", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TextButton(onClick = { viewModel.syncDailyRevenue() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Filled.Refresh, null, tint = WarmBrown, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Đồng bộ", color = WarmBrown, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    RevenueHistoryChart(recentHistory)
                }
            }
        }

        // --- AI Analytics ---
        item {
            AIAnalyticsSection(orders = orders, revenues = history)
        }

        // --- Lịch sử ---
        item { Text("Lịch sử doanh thu", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

        if (sortedHistory.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chưa có lịch sử doanh thu", color = Color.Gray, fontSize = 14.sp)
                        Text("Dữ liệu sẽ xuất hiện sau khi đồng bộ hoặc có đơn thanh toán", color = Color.Gray.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { viewModel.syncDailyRevenue() }, border = BorderStroke(1.dp, WarmBrown)) {
                            Icon(Icons.Filled.Refresh, null, tint = WarmBrown, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Đồng bộ từ hóa đơn", color = WarmBrown, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(sortedHistory, key = { it.date }) { h ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(WarmBrown.copy(alpha = 0.6f), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(h.date, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${h.order_count} đơn hàng", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Text("${h.revenue.toVndFormat()}đ", fontWeight = FontWeight.ExtraBold, color = WarmBrown, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AIAnalyticsSection(orders: List<Order>, revenues: List<DailyRevenue>) {
    val analyticsVM: AdminAnalyticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val insightState by analyticsVM.insightState.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }

    // Parse JSON kết quả từ AI
    val parsedInsight = remember(insightState.content) {
        if (insightState.content.isBlank()) null
        else try {
            val obj = org.json.JSONObject(insightState.content)
            Triple(
                (0 until (obj.optJSONArray("summary")?.length() ?: 0)).map { obj.getJSONArray("summary").getString(it) },
                (0 until (obj.optJSONArray("patterns")?.length() ?: 0)).map { obj.getJSONArray("patterns").getString(it) },
                (0 until (obj.optJSONArray("actions")?.length() ?: 0)).map { obj.getJSONArray("actions").getString(it) }
            )
        } catch (e: Exception) { null }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header + Nút
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { if (parsedInsight != null) isExpanded = !isExpanded },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A237E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Phân tích AI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("COO Revenue Insights", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (parsedInsight != null) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 12.dp).size(24.dp)
                        )
                    }
                    Button(
                        onClick = { 
                            analyticsVM.generateInsights(orders, revenues) 
                            isExpanded = true
                        },
                        enabled = !insightState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color(0xFF1A237E),
                            disabledContainerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (insightState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF1A237E), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Phân tích", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Hiện lỗi
                if (insightState.error != null) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = StatusRed.copy(alpha = 0.1f)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = StatusRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(insightState.error!!, color = StatusRed, fontSize = 13.sp)
                        }
                    }
                }

                // Hiện kết quả AI dạng 3 cards
                if (parsedInsight != null) {
                    val (summaries, patterns, actions) = parsedInsight

                    // Card Tóm tắt
                    if (summaries.isNotEmpty()) {
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5E9)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Summarize, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("TÓM TẮT HIỆU SUẤT", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF1B5E20), letterSpacing = 0.5.sp)
                                }
                                summaries.forEach { s ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("▶", color = Color(0xFF388E3C), fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(s, fontSize = 13.sp, color = Color(0xFF1B5E20), lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Card Xu hướng
                    if (patterns.isNotEmpty()) {
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF3E0)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Insights, null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("PHÁT HIỆN XU HƯỚNG", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFBF360C), letterSpacing = 0.5.sp)
                                }
                                patterns.forEach { p ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("▶", color = Color(0xFFFF6D00), fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(p, fontSize = 13.sp, color = Color(0xFF4E342E), lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Card Hành động
                    if (actions.isNotEmpty()) {
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFE3F2FD)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("HÀNH ĐỘNG ĐỀ XUẤT", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0D47A1), letterSpacing = 0.5.sp)
                                }
                                actions.forEachIndexed { i, a ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Surface(shape = CircleShape, color = Color(0xFF1565C0)) {
                                            Text("${i+1}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(a, fontSize = 13.sp, color = Color(0xFF0D47A1), lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (!insightState.isLoading && insightState.content.isBlank() && insightState.error == null) {
                    // Trạng thái chưa phân tích
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFF1A237E).copy(alpha = 0.06f)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nhấn \"Phân tích\" để AI đọc số liệu 7 ngày", fontSize = 13.sp, color = Color(0xFF3949AB), textAlign = TextAlign.Center)
                            Text("và đưa ra đề xuất kinh doanh.", fontSize = 13.sp, color = Color(0xFF3949AB), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueHistoryChart(data: List<DailyRevenue>) {
    val maxRevenue = data.maxOfOrNull { it.revenue }?.takeIf { it > 0 } ?: 1.0

    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { day ->
                val barHeightValue = (day.revenue / maxRevenue).toFloat()

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val actualWeight = barHeightValue.coerceIn(0.05f, 1f)
                    val emptyWeight = 1f - actualWeight

                    if (emptyWeight > 0f) {
                        Spacer(modifier = Modifier.weight(emptyWeight))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(actualWeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(WarmBrown)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.date.takeLast(2),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// =====================================================
// Danh sách HÓA ĐƠN hôm nay
// =====================================================
@Composable
fun AdminInvoiceTodayScreen(
    token: String,
    viewModel: RestaurantViewModel,
    onBack: () -> Unit,
    onOrderClick: (Order) -> Unit
) {
    val orders by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchOrders(token)
    }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayOrders = remember(orders, today) { orders.filter { it.created_at.startsWith(today) } }
    val totalRevenue = remember(todayOrders) { todayOrders.filter { it.payment_status == "paid" }.sumOf { it.total_amount } }
    val paidCount = remember(todayOrders) { todayOrders.count { it.payment_status == "paid" } }
    val unpaidCount = remember(todayOrders) { todayOrders.count { it.payment_status != "paid" } }

    Scaffold(containerColor = Color(0xFFF7F3EE)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF7F3EE)),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ── Gradient Header ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(WarmBrown, WarmBrown.copy(alpha = 0.75f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp).clickable { onBack() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HOÁ ĐƠN", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 2.sp)
                            Text("Hôm nay · ${todayOrders.size} đơn", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Doanh thu: ${totalRevenue.toVndFormat()} VNĐ",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Stat Cards ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp)
                                        .background(StatusGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(18.dp)) }
                                Spacer(Modifier.width(8.dp))
                                Text("Đã thanh toán", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("$paidCount đơn", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1E293B))
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp)
                                        .background(StatusYellow.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.HourglassTop, null, tint = StatusYellow, modifier = Modifier.size(18.dp)) }
                                Spacer(Modifier.width(8.dp))
                                Text("Chưa thanh toán", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("$unpaidCount đơn", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1E293B))
                        }
                    }
                }
            }

            // ── Label ──
            item {
                Text(
                    "Danh sách hóa đơn",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E)
                )
            }

            // ── Invoice List ──
            if (todayOrders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Hôm nay chưa có hóa đơn nào", color = Color.Gray, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                items(todayOrders, key = { it.id }) { order ->
                    val isPaid = order.payment_status == "paid"
                    val statusColor = if (isPaid) StatusGreen else StatusYellow
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onOrderClick(order) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status dot
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPaid) Icons.Default.CheckCircle else Icons.Default.Receipt,
                                    null,
                                    tint = statusColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            if (isPaid) "Đã TT" else "Chưa TT",
                                            color = statusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${order.table_number ?: "Mang về"}  ·  ${order.items_detail?.sumOf { it.quantity } ?: 0} món",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${order.total_amount.toVndFormat()} đ",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = WarmBrown
                                )
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// =====================================================
// CHI TIẾT HÓA ĐƠN
// =====================================================
@Composable
fun AdminInvoiceDetailScreen(
    order: Order,
    token: String = "",
    viewModel: RestaurantViewModel? = null,
    onBack: () -> Unit
) {
    var isPaid by remember { mutableStateOf(order.payment_status == "paid") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val statusColor = if (isPaid) StatusGreen else StatusYellow
    val detailItems = order.items_detail ?: emptyList<OrderItemDetail>()
    val totalQty = detailItems.sumOf { it.quantity }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(StatusGreen.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hóa đơn: #${order.id}", fontWeight = FontWeight.Medium)
                    Text(
                        "Tổng tiền: ${order.total_amount.toVndFormat()} VNĐ",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = WarmBrown
                    )
                    Surface(color = StatusGreen.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Bàn sẽ được tự động giải phóng sau khi xác nhận.",
                            color = StatusGreen,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel?.checkoutOrder(token, order.id) { isPaid = true }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Hủy bỏ", color = Color.Gray) }
            }
        )
    }

    Scaffold(containerColor = Color(0xFFF7F3EE)) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF7F3EE))
        ) {
            // ── Gradient Header ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(WarmBrown, WarmBrown.copy(alpha = 0.75f))
                        )
                    )
                    .padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HOÁ ĐƠN", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 2.sp)
                        Text("#${order.id}  ·  ${order.table_number ?: "Mang về"}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    // Payment status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = if (isPaid) 0.25f else 0.15f)
                    ) {
                        Text(
                            if (isPaid) "✓ Đã TT" else "⏳ Chưa TT",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Stat Cards ──
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier.size(30.dp)
                                .background(WarmBrown.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("🍽️", fontSize = 15.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text("$totalQty món", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF1E293B))
                        Text("${detailItems.size} loại", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier.size(30.dp)
                                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(if (isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassTop, null, tint = statusColor, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${order.total_amount.toVndFormat()} đ",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = WarmBrown
                        )
                        Text(if (isPaid) "Đã thanh toán" else "Chưa thanh toán", fontSize = 11.sp, color = statusColor)
                    }
                }
            }

            // ── Item Table Card ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color(0xFFF7F3EE), Color.White)
                                ),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Món ăn", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(2f), fontSize = 13.sp, color = Color(0xFF1A1A2E))
                        Text("SL", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, fontSize = 13.sp, color = Color(0xFF1A1A2E))
                        Text("Đơn giá", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End, fontSize = 13.sp, color = Color(0xFF1A1A2E))
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(detailItems, key = { it.name }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, modifier = Modifier.weight(2f), fontSize = 14.sp, color = Color(0xFF1A1A2E))
                                Text(
                                    "×${item.quantity}",
                                    modifier = Modifier.weight(0.7f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmBrown
                                )
                                Text(
                                    "—",
                                    modifier = Modifier.weight(1.3f),
                                    textAlign = TextAlign.End,
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                    }

                    // Total footer
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color(0xFFF7F3EE), Color.White)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tổng cộng", fontSize = 13.sp, color = Color.Gray)
                            Text("$totalQty món · ${detailItems.size} loại", fontSize = 11.sp, color = Color.LightGray)
                        }
                        Text(
                            "${order.total_amount.toVndFormat()} VNĐ",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = WarmBrown
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action Button ──
            if (!isPaid && viewModel != null) {
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Xác nhận thanh toán", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(16.dp))
            } else if (isPaid) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = StatusGreen.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Đã thanh toán thành công!", color = StatusGreen, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// =====================================================
// TAB 4: QUẢN LÝ KHO NGUYÊN LIỆU (INGREDIENT INVENTORY)
// =====================================================
fun getLowStockThreshold(unit: String): Double {
    return when (unit.lowercase().trim()) {
        "kg", "kilogram" -> 4.0
        "lít", "lit", "l" -> 2.0
        "quả", "qua", "hộp", "hop" -> 20.0
        "chai" -> 2.0
        "g", "gram", "gam" -> 4000.0
        "ml" -> 2000.0
        else -> 5.0
    }
}

@Composable
fun AdminIngredientInventory(
    token: String,
    viewModel: RestaurantViewModel,
    onScanClick: (Uri) -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var ingredientToDelete by remember { mutableStateOf<Ingredient?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) onScanClick(uri) }

    LaunchedEffect(Unit) {
        viewModel.fetchInventory()
    }

    if (showDialog) {
        IngredientEditDialog(
            ingredient = selectedIngredient,
            onDismiss = { showDialog = false },
            onConfirm = { ingData ->
                if (selectedIngredient == null) viewModel.addIngredient(ingData)
                else viewModel.updateIngredient(ingData.copy(id = selectedIngredient!!.id))
                showDialog = false
            }
        )
    }

    if (ingredientToDelete != null) {
        DeleteConfirmDialog(
            title = "Xóa nguyên liệu",
            message = "Bạn có chắc muốn xóa \"${ingredientToDelete!!.name}\"? Thao tác này không thể hoàn tác.",
            onDismiss = { ingredientToDelete = null },
            onConfirm = {
                viewModel.deleteIngredient(ingredientToDelete!!.id)
                ingredientToDelete = null
            }
        )
    }

    if (showClearAllConfirm) {
        DeleteConfirmDialog(
            title = "Xóa toàn bộ kho",
            message = "Bạn có chắc muốn xóa TẤT CẢ ${ingredients.size} nguyên liệu? Thao tác này không thể hoàn tác!",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                viewModel.clearAllIngredients()
                showClearAllConfirm = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // --- Top Stats (Tương tự hình tham khảo) ---
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val totalLowStock = ingredients.count { it.stock < getLowStockThreshold(it.unit) }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Inventory2, null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("${ingredients.size}", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF1A1A2E))
                    Spacer(Modifier.height(4.dp))
                    Text("Tổng chủng loại", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Warning, null, tint = Color(0xFFD9534F), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("$totalLowStock", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFFD9534F))
                    Spacer(Modifier.height(4.dp))
                    Text("Sắp hết hàng", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // --- Tìm kiếm + Thêm NL ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tìm nguyên liệu", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmBrown,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF9F9F9)
                ),
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp), tint = Color.Gray) }
            )
            // Scanner AI
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF005BAA).copy(alpha = 0.1f),
                modifier = Modifier.size(54.dp).clickable { imagePicker.launch("image/*") }
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF005BAA)) }
            }
            // Add new
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = WarmBrown,
                modifier = Modifier.size(54.dp).clickable { selectedIngredient = null; showDialog = true }
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White) }
            }
            // Delete all
            if (ingredients.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFD9534F).copy(alpha = 0.1f),
                    modifier = Modifier.size(54.dp).clickable { showClearAllConfirm = true }
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, tint = Color(0xFFD9534F)) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        val filteredList = if (searchQuery.isBlank()) ingredients else {
            ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        if (filteredList.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isBlank()) "Kho bếp trống" else "Không tìm thấy nguyên liệu", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredList, key = { it.id }) { ing ->
                    IngredientAdminCard(
                        ingredient = ing,
                        onEdit = { selectedIngredient = ing; showDialog = true },
                        onDelete = { ingredientToDelete = ing }
                    )
                }
            }
        }
    }
}

@Composable
fun IngredientAdminCard(
    ingredient: Ingredient,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val threshold = getLowStockThreshold(ingredient.unit)
    val stockRatio = (ingredient.stock / (threshold * 5.0)).coerceIn(0.0, 1.0).toFloat() // Thanh tiến trình max = 5x threshold
    val colorStock = if (ingredient.stock < threshold) Color(0xFFD9534F) else if (ingredient.stock < threshold * 2.5) Color(0xFFFF9800) else StatusGreen

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ingredient.name, fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF1A1A2E))
                    Spacer(Modifier.height(4.dp))
                    Text("Đơn vị: ${ingredient.unit}", color = Color.Gray, fontSize = 12.sp)
                }
                // Nút hành động
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFFF5F5F5), modifier = Modifier.size(36.dp).clickable(onClick = onEdit)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                    }
                    Surface(shape = CircleShape, color = Color(0xFFFBE9E7), modifier = Modifier.size(36.dp).clickable(onClick = onDelete)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, tint = Color(0xFFD9534F), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thanh tiến trình cảnh báo Min Stock (như ảnh demo)
                Box(modifier = Modifier.weight(1f).height(6.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(3.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(if(ingredient.stock <= 0) 0.05f else stockRatio).height(6.dp).background(colorStock, RoundedCornerShape(3.dp)))
                }
                Spacer(Modifier.width(20.dp))
                // Số lượng kho
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${if (ingredient.stock % 1.0 == 0.0) ingredient.stock.toInt() else ingredient.stock}",
                        fontWeight = FontWeight.Black, fontSize = 22.sp, color = colorStock
                    )
                }
            }
        }
    }
}

@Composable
fun IngredientEditDialog(
    ingredient: Ingredient?,
    onDismiss: () -> Unit,
    onConfirm: (Ingredient) -> Unit
) {
    var name by remember { mutableStateOf(ingredient?.name ?: "") }
    var stockStr by remember { mutableStateOf(ingredient?.stock?.toString() ?: "") }
    var unit by remember { mutableStateOf(ingredient?.unit ?: "gram") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (ingredient == null) "Thêm Nguyên Liệu" else "Sửa Nguyên Liệu", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên NL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Số lượng") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Đơn vị tính (kg, gram, quả,...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val stock = stockStr.toDoubleOrNull() ?: 0.0
                        onConfirm(Ingredient(
                            id = ingredient?.id ?: 0,
                            name = name.trim(),
                            stock = stock,
                            unit = unit.trim(),
                            updated_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        ))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
            ) {
                Text("Lưu", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}
