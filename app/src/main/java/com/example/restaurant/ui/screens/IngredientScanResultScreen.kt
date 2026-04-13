package com.example.restaurant.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurant.data.model.Ingredient
import com.example.restaurant.data.model.ScannedIngredientItem
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.StatusGreen
import com.example.restaurant.ui.theme.StatusYellow
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.IngredientScanViewModel
import com.example.restaurant.ui.viewmodel.RestaurantViewModel

private val StatusOrange = Color(0xFFF57C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScanResultScreen(
    scanViewModel: IngredientScanViewModel,
    restaurantViewModel: RestaurantViewModel,
    onBack: () -> Unit,
    onScanAgain: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scanState by scanViewModel.scanState.collectAsState()
    val allIngredients by restaurantViewModel.ingredients.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onScanAgain(uri)
    }

    Scaffold(
        containerColor = CreamBG,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kết quả Quét nguyên liệu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Kiểm tra và lưu vào kho", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scanViewModel.resetState(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        when (val state = scanState) {
            is IngredientScanViewModel.ScanState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = WarmBrown, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(state.message, color = WarmBrown, fontWeight = FontWeight.Medium)
                        Text("Vui lòng chờ trong giây lát", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            is IngredientScanViewModel.ScanState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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

            is IngredientScanViewModel.ScanState.Success -> {
                var editableItems by remember(state.items) {
                    mutableStateOf(state.items.toMutableList())
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Surface(color = Color.White, shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "🤖 AI phát hiện ${editableItems.size} nguyên liệu",
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                                    )
                                    val selectedCount = editableItems.count { it.isSelected }
                                    Text(
                                        "$selectedCount mục được chọn để lưu",
                                        fontSize = 12.sp, color = WarmBrown
                                    )
                                }
                                TextButton(onClick = { imagePicker.launch("image/*") }) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = WarmBrown)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Quét lại", color = WarmBrown, fontSize = 12.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                                LegendChip(color = StatusOrange, label = "SL = 0 (cần điền)")
                                LegendChip(color = StatusYellow, label = "Cảnh báo trùng tên")
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        itemsIndexed(editableItems, key = { i, _ -> i }) { index, item ->
                            ScannedIngredientCard(
                                item = item,
                                allIngredients = allIngredients,
                                onToggle = {
                                    editableItems = editableItems.toMutableList().also {
                                        it[index] = it[index].copy(isSelected = !it[index].isSelected)
                                    }
                                },
                                onNameChange = { newName ->
                                    editableItems = editableItems.toMutableList().also {
                                        it[index] = it[index].copy(name = newName)
                                    }
                                },
                                onStockChange = { newStock ->
                                    editableItems = editableItems.toMutableList().also {
                                        it[index] = it[index].copy(stock = newStock)
                                    }
                                },
                                onUnitChange = { newUnit ->
                                    editableItems = editableItems.toMutableList().also {
                                        it[index] = it[index].copy(unit = newUnit)
                                    }
                                }
                            )
                        }
                    }

                    val selectedItems = editableItems.filter { it.isSelected && it.name.isNotBlank() }
                    Surface(color = Color.White, shadowElevation = 8.dp) {
                        Button(
                            onClick = {
                                var savedCount = 0
                                selectedItems.forEach { scanned ->
                                    // Tạo ingredient mới
                                    val ingredient = Ingredient(
                                        id = 0,
                                        name = scanned.name,
                                        unit = scanned.unit,
                                        stock = scanned.stock,
                                        updated_at = null
                                    )
                                    restaurantViewModel.addIngredient(ingredient)
                                    savedCount++
                                }
                                Toast.makeText(context, "✅ Đã lưu $savedCount nguyên liệu vào kho!", Toast.LENGTH_SHORT).show()
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
                                "Lưu ${selectedItems.size} mục vào kho",
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

@Composable
fun ScannedIngredientCard(
    item: ScannedIngredientItem,
    allIngredients: List<Ingredient>,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onStockChange: (Double) -> Unit,
    onUnitChange: (String) -> Unit
) {
    val borderColor = when {
        item.isPossibleDuplicate && item.isSelected -> StatusYellow
        item.stock <= 0.0 && item.isSelected -> StatusOrange
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
                    Text(
                        item.unit,
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
                if (item.stock <= 0.0) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusOrange.copy(alpha = 0.12f)) {
                        Text("⚠ Yêu cầu SL", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 10.sp, color = StatusOrange)
                    }
                }
            }

            var editingName by remember(item.name) { mutableStateOf(false) }
            if (editingName) {
                var tempName by remember { mutableStateOf(item.name) }
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Tên NL", fontSize = 12.sp) },
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                var editingStock by remember(item.stock) { mutableStateOf(false) }
                val stockColor = if (item.stock <= 0.0) StatusOrange else Color(0xFF333333)
                if (editingStock) {
                    var tempStock by remember { mutableStateOf(if (item.stock > 0) item.stock.toString() else "") }
                    OutlinedTextField(
                        value = tempStock,
                        onValueChange = { tempStock = it },
                        label = { Text("Số lượng", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                onStockChange(tempStock.toDoubleOrNull() ?: 0.0)
                                editingStock = false
                            }) {
                                Icon(Icons.Default.Check, null, tint = StatusGreen)
                            }
                        }
                    )
                } else {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (item.stock > 0) "SL: ${item.stock}" else "⚡ Cần điền SL",
                            color = stockColor, fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { editingStock = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                var editingUnit by remember(item.unit) { mutableStateOf(false) }
                if (editingUnit) {
                    var tempUnit by remember { mutableStateOf(item.unit) }
                    OutlinedTextField(
                        value = tempUnit,
                        onValueChange = { tempUnit = it },
                        label = { Text("Đơn vị", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarmBrown, focusedLabelColor = WarmBrown
                        ),
                        trailingIcon = {
                            IconButton(onClick = { onUnitChange(tempUnit); editingUnit = false }) {
                                Icon(Icons.Default.Check, null, tint = StatusGreen)
                            }
                        }
                    )
                } else {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ĐV: ${item.unit}",
                            color = Color(0xFF333333), fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { editingUnit = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
        }
    }
}
