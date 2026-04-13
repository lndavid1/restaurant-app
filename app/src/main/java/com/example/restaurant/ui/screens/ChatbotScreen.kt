package com.example.restaurant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.restaurant.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restaurant.ui.theme.CreamBG
import com.example.restaurant.ui.theme.WarmBrown
import com.example.restaurant.ui.viewmodel.ChatViewModel
import com.example.restaurant.ui.viewmodel.RestaurantViewModel
import com.example.restaurant.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    onNavigateBack: () -> Unit,
    restaurantViewModel: RestaurantViewModel,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel = viewModel(),
    token: String? = null
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val avatarUrl = userProfile?.get("avatarUrl") as? String
    val products by restaurantViewModel.products.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val orders by restaurantViewModel.orders.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val activeTableId = remember(orders, token) {
        if (token == null) 0
        else orders.find { 
            it.user_id == token && 
            it.table_id != null && it.table_id != 0 &&
            (it.payment_status == "unpaid" || it.payment_status == "requested")
        }?.table_id ?: 0
    }

    val hasActiveTable = activeTableId != 0

    LaunchedEffect(token) {
        chatViewModel.initializeUser(token)
    }

    LaunchedEffect(chatViewModel) {
        launch {
            chatViewModel.addToCartEvents.collect { productId ->
                val productToAdd = products.find { it.id == productId }
                if (productToAdd != null) {
                    restaurantViewModel.addToCart(productToAdd)
                    Toast.makeText(context, "Đã tự động thêm ${productToAdd.name} vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        launch {
            chatViewModel.placeOrderEvents.collect {
                if (token != null && restaurantViewModel.cartItems.value.isNotEmpty()) {
                    restaurantViewModel.placeOrder(token, activeTableId) {
                        Toast.makeText(context, "Đã gửi đơn báo bếp thành công!", Toast.LENGTH_LONG).show()
                    }
                } else if (restaurantViewModel.cartItems.value.isEmpty()) {
                    Toast.makeText(context, "Giỏ hàng hiện đang trống!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(products, token) {
        if (products.isNotEmpty()) {
            chatViewModel.startChat(products)
        } else {
            restaurantViewModel.fetchProducts()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(WarmBrown, WarmBrown.copy(alpha = 0.82f))))
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    // Bot avatar
                    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(38.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Bot Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Trợ lý ẩm thực", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                            Spacer(Modifier.width(4.dp))
                            Text("Trực tuyến", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CreamBG)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(messages) { message ->
                    ModernMessageItem(message = message, avatarUrl = avatarUrl)
                }
            }

            // Modern Input Row
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Bạn đang tìm kiếm món gì...", style = MaterialTheme.typography.bodyMedium, color = WarmBrown.copy(alpha = 0.5f)) },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CreamBG,
                            unfocusedContainerColor = CreamBG,
                            focusedBorderColor = WarmBrown.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    chatViewModel.sendMessage(inputText, hasActiveTable)
                                    inputText = ""
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(inputText, hasActiveTable)
                                inputText = ""
                            }
                        },
                        containerColor = WarmBrown,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, 
                            contentDescription = "Gửi",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernMessageItem(message: com.example.restaurant.ui.viewmodel.ChatMessage, avatarUrl: String? = null) {
    val isUser = message.isUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            AvatarIcon(isBot = true)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) WarmBrown else Color.White,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (message.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Bot đang gõ", style = MaterialTheme.typography.bodySmall.copy(color = WarmBrown.copy(alpha = 0.6f)))
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = WarmBrown)
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Color.White else Color.Black
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            AvatarIcon(isBot = false, userAvatarUrl = avatarUrl)
        }
    }
}

@Composable
fun AvatarIcon(isBot: Boolean, userAvatarUrl: String? = null) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = if (isBot) Color.White else WarmBrown.copy(alpha = 0.1f),
        contentColor = WarmBrown
    ) {
        if (isBot) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Bot Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            if (!userAvatarUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = userAvatarUrl,
                    contentDescription = "User Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
