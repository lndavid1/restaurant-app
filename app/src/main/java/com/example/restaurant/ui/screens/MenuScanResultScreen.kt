package com.example.restaurant.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Product
import com.example.restaurant.data.model.ScannedMenuItem
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.MenuScanViewModel
import com.example.restaurant.ui.viewmodel.RestaurantViewModel


private val StatusOrange = Color(0xFFF57C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScanResultScreen(
    scanViewModel: MenuScanViewModel,
    restaurantViewModel: RestaurantViewModel,
    adminToken: String,
    onBack: () -> Unit,
    onScanAgain: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scanState by scanViewModel.scanState.collectAsState()
    val products by restaurantViewModel.products.collectAsState()
    val categories by restaurantViewModel.categories.collectAsState()
    val allIngredients by restaurantViewModel.ingredients.collectAsState()

    // Picker ảnh
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onScanAgain(uri)
    }

    Scaffold(
        containerColor = Color(0xFFF7F3EE),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(WarmBrown, WarmBrown.copy(alpha = 0.75f))
                        )
                    )
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scanViewModel.resetState(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("QUÉT MENU AI", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 2.sp)
                        Text("Kết quả phân tích", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.clickable { imagePicker.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quét lại", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = scanState) {
            is MenuScanViewModel.ScanState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = WarmBrown, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(state.message, color = WarmBrown, fontWeight = FontWeight.Medium)
                        Text("Vui lòng chờ trong giây lát", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            is MenuScanViewModel.ScanState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Không quét được", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Thử ảnh khác")
                        }
                    }
                }
            }

            is MenuScanViewModel.ScanState.Success -> {
                // Dùng SnapshotStateList để chỉ recompose đúng item thay đổi
                val editableItems: SnapshotStateList<com.example.restaurant.data.model.ScannedMenuItem> =
                    remember(state.items) { state.items.toMutableStateList() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                ) {
                    // Header tổng kết
                    Surface(color = Color.White, shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "🤖 AI phát hiện ${editableItems.size} món",
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                                    )
                                    val selectedCount = editableItems.count { it.isSelected }
                                    Text(
                                        "$selectedCount món được chọn để lưu",
                                        fontSize = 12.sp, color = WarmBrown
                                    )
                                }
                                TextButton(onClick = { imagePicker.launch("image/*") }) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Quét lại", color = WarmBrown, fontSize = 12.sp)
                                }
                            }

                                // Legend
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    LegendChip(color = StatusOrange, label = "Giá=0 / SL nghi vấn lớn")
                                    LegendChip(color = StatusYellow, label = "Có thể trùng")
                                    LegendChip(color = Color(0xFF1565C0), label = "AI Sug. Recipe")
                                }
                        }
                    }

                    // Danh sách món
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        // Dùng name làm stable key → LazyColumn tái dùng được đúng item
                        itemsIndexed(editableItems, key = { _, item -> item.name }) { index, item ->
                            ScannedItemCard(
                                item = item,
                                categories = categories,
                                ingredientsList = allIngredients,
                                // SnapshotStateList: chỉ update đúng index, không copy toàn bộ list
                                onToggle = {
                                    editableItems[index] = editableItems[index].copy(isSelected = !editableItems[index].isSelected)
                                },
                                onNameChange = { newName ->
                                    editableItems[index] = editableItems[index].copy(name = newName)
                                },
                                onPriceChange = { newPrice ->
                                    editableItems[index] = editableItems[index].copy(price = newPrice)
                                },
                                onRecipeChange = { newRecipe ->
                                    editableItems[index] = editableItems[index].copy(recipe = newRecipe)
                                }
                            )
                        }
                    }

                    // Nút lưu
                    val selectedItems = editableItems.filter { it.isSelected && it.name.isNotBlank() }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Button(
                            onClick = {
                                val groupedItems = selectedItems.groupBy { it.category.trim().ifBlank { "Khác" } }
                                var savedCount = 0

                                groupedItems.forEach { (catName, itemsInCategory) ->
                                    val existingCat = categories.find { it.name.trim().equals(catName, ignoreCase = true) }
                                    if (existingCat != null) {
                                        itemsInCategory.forEach { scanned ->
                                            val product = Product(
                                                id = 0,
                                                category_id = existingCat.id,
                                                name = scanned.name,
                                                description = scanned.description.ifBlank { null },
                                                price = scanned.price.toDouble(),
                                                is_available = 1,
                                                image_url = scanned.image_url,
                                                recipe = scanned.recipe
                                            )
                                            restaurantViewModel.addProduct(adminToken, product)
                                            savedCount++
                                        }
                                    } else {
                                        restaurantViewModel.addCategory(catName) { newCat ->
                                            itemsInCategory.forEach { scanned ->
                                                val product = Product(
                                                    id = 0,
                                                    category_id = newCat.id,
                                                    name = scanned.name,
                                                    description = scanned.description.ifBlank { null },
                                                    price = scanned.price.toDouble(),
                                                    is_available = 1,
                                                    image_url = scanned.image_url,
                                                    recipe = scanned.recipe
                                                )
                                                restaurantViewModel.addProduct(adminToken, product)
                                            }
                                        }
                                        savedCount += itemsInCategory.size
                                    }
                                }
                                Toast.makeText(context, "✅ Đã thêm $savedCount món vào thực đơn!", Toast.LENGTH_SHORT).show()
                                scanViewModel.resetState()
                                onBack()
                            },
                            enabled = selectedItems.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmBrown,
                                disabledContainerColor = Color.LightGray
                            )
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Lưu ${selectedItems.size} món đã chọn",
                                fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

// =====================================================
// CARD TỪng món quét được
// =====================================================
@Composable
fun ScannedItemCard(
    item: ScannedMenuItem,
    categories: List<com.example.restaurant.data.model.Category>,
    ingredientsList: List<com.example.restaurant.data.model.Ingredient>,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (Long) -> Unit,
    onRecipeChange: (List<com.example.restaurant.data.model.RecipeItem>) -> Unit
) {
    val borderColor = when {
        item.isPossibleDuplicate && item.isSelected -> StatusYellow
        item.price == 0L && item.isSelected -> StatusOrange
        item.isSelected -> WarmBrown.copy(alpha = 0.4f)
        else -> Color.LightGray.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (item.isSelected) Color.White else Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row: checkbox + badges
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = WarmBrown)
                )
                Spacer(Modifier.width(4.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = WarmBrown.copy(alpha = 0.1f)
                ) {
                    val badgeText = item.category.ifBlank { "Khác" }
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp, color = WarmBrown, fontWeight = FontWeight.Medium
                    )
                }

                if (item.isPossibleDuplicate) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusYellow.copy(alpha = 0.15f)) {
                        Text("⚠ Có thể trùng", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 10.sp, color = StatusYellow)
                    }
                }
                if (item.price == 0L) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusOrange.copy(alpha = 0.12f)) {
                        Text("💰 Cần điền giá", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 10.sp, color = StatusOrange)
                    }
                }
            }

            // --- HÌNH ẢNH MÓN AI FETCH HOẶC FALLBACK ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.image_url.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = item.image_url,
                        contentDescription = "Hình món ăn",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                    
                    // Badge Feedback AI
                    Surface(
                        shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                        color = Color(0xFF1565C0).copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            "✨ AI Phân Tích", 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 10.sp, 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Nút đổi ảnh
                    IconButton(
                        onClick = { /* Tương lai: Chọn ảnh mới */ },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha=0.6f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, "Đổi ảnh", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                } else {
                    val fallbackEmoji = when {
                        item.category.contains("nước", true) || item.category.contains("sinh tố", true) || item.category.contains("đồ uống", true) -> "🥤"
                        item.category.contains("cơm", true) -> "🍛"
                        item.category.contains("bún", true) || item.category.contains("phở", true) || item.category.contains("mì", true) -> "🍜"
                        item.category.contains("bánh", true) || item.category.contains("tráng miệng", true) -> "🍰"
                        else -> "🍲"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fallbackEmoji, fontSize = 48.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Không tìm thấy ảnh chuẩn",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                        color = Color.Gray.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            "⚠ Low Confidence", 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 10.sp, 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Tên món (có thể sửa)
            var editingName by remember(item.name) { mutableStateOf(false) }
            if (editingName) {
                var tempName by remember { mutableStateOf(item.name) }
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Tên món", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onNameChange(tempName); editingName = false }) {
                            Icon(Icons.Default.Check, null, tint = StatusGreen)
                        }
                    }
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { editingName = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Giá tiền (có thể sửa, highlight cam nếu = 0)
            var editingPrice by remember(item.price) { mutableStateOf(false) }
            val priceColor = if (item.price == 0L) StatusOrange else Color(0xFF333333)
            if (editingPrice) {
                var tempPrice by remember { mutableStateOf(if (item.price > 0) item.price.toString() else "") }
                OutlinedTextField(
                    value = tempPrice,
                    onValueChange = { tempPrice = it },
                    label = { Text("Giá (VND)", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            onPriceChange(tempPrice.toLongOrNull() ?: 0L)
                            editingPrice = false
                        }) {
                            Icon(Icons.Default.Check, null, tint = StatusGreen)
                        }
                    }
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (item.price > 0) "${item.price.toVndFormatLong()} VNĐ" else "⚡ Chưa có giá — nhấn ✏ để điền",
                        color = priceColor, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { editingPrice = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (item.description.isNotBlank()) {
                Text(item.description, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
            }

            // --- AI SUGGESTED RECIPE UI ---
            val recipe = item.recipe
            if (!recipe.isNullOrEmpty()) {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖 AI Suggested Recipe", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    Spacer(Modifier.width(8.dp))
                    Text("(Kiểm tra số lượng trước khi lưu)", fontSize = 10.sp, color = Color.Gray)
                }
                
                // Cache lookup map tránh O(n*m) trong loop
                val ingredientMap = remember(ingredientsList) {
                    ingredientsList.associateBy { it.id.toString() }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recipe.forEachIndexed { rIndex, rItem ->
                        val ingName = ingredientMap[rItem.ingredient_id]?.name ?: "ID ${rItem.ingredient_id}"

                        var editingQuantity by remember { mutableStateOf(false) }
                        var tempQuantity by remember { mutableStateOf(rItem.quantity.toString()) }
                        
                        val isSuspicious = rItem.quantity >= 1000.0 // Highlight nếu vượt 1000 đơn vị (vd 1000kg)

                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F4F8), RoundedCornerShape(8.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• $ingName", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                            
                            if (editingQuantity) {
                                OutlinedTextField(
                                    value = tempQuantity,
                                    onValueChange = { tempQuantity = it },
                                    modifier = Modifier.width(100.dp).height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val newQ = tempQuantity.toDoubleOrNull() ?: rItem.quantity
                                            val newRecipeList = recipe.toMutableList().also {
                                                it[rIndex] = it[rIndex].copy(quantity = newQ)
                                            }
                                            onRecipeChange(newRecipeList)
                                            editingQuantity = false
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Check, null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val qColor = if (isSuspicious) StatusOrange else Color.Black
                                    if (isSuspicious) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text("${rItem.quantity} ${rItem.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = qColor)
                                    IconButton(
                                        onClick = { editingQuantity = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
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

// =====================================================
// Legend chip
// =====================================================
@Composable
fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = androidx.compose.foundation.shape.CircleShape, color = color) {}
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

// Extension cho Long
private fun Long.toVndFormatLong(): String {
    return "%,d".format(this).replace(",", ".")
}
