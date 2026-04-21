package com.example.restaurant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.restaurant.ui.navigation.Screen
import com.example.restaurant.ui.screens.*
import com.example.restaurant.ui.theme.RestaurantTheme
import com.example.restaurant.ui.viewmodel.AuthViewModel
import com.example.restaurant.ui.viewmodel.RestaurantViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RestaurantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RestaurantApp()
                }
            }
        }
    }

    /**
     * Được gọi khi app đang chạy (singleTask) và nhận deep link từ browser
     * (ví dụ: restaurantapp://payment?status=PAID sau khi PayOS redirect).
     * Không cần xử lý gì đặc biệt — polling đã chạy ngầm qua startPayOSPolling.
     * Override này chỉ cần tồn tại để Android không tạo Activity mới.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun RestaurantApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Lưu session vào SharedPreferences để survive background process kill
    // (xảy ra khi user rời app sang browser thanh toán PayOS/VNPAY)
    val prefs = remember { context.getSharedPreferences("restaurant_session", Context.MODE_PRIVATE) }

    var userToken by remember { mutableStateOf<String?>(prefs.getString("user_token", null)) }
    var currentUserRole by remember { mutableStateOf<String?>(prefs.getString("user_role", null)) }
    var currentTableId by remember { mutableIntStateOf(0) }

    val restaurantViewModel: RestaurantViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    // Helper lưu / xóa session
    val saveSession: (String, String) -> Unit = { token, role ->
        prefs.edit().putString("user_token", token).putString("user_role", role).apply()
    }
    val clearSession: () -> Unit = {
        prefs.edit().remove("user_token").remove("user_role").apply()
    }

    // Nếu đã có session (quay lại sau khi thanh toán), bỏ qua Splash → thẳng dashboard
    // remember: startDestination của NavHost chỉ đọc 1 lần — không cần tính lại mỗi recompose
    val startDest = remember(userToken, currentUserRole) {
        if (!userToken.isNullOrBlank()) {
            when (currentUserRole) {
                "admin" -> Screen.AdminDashboard.route
                "employee" -> Screen.TableMap.route
                "kitchen" -> Screen.KitchenDashboard.route
                else -> Screen.CustomerDashboard.route
            }
        } else {
            Screen.Splash.route
        }
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Splash.route) {
            SplashScreen(onNext = {
                navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { token, role ->
                    userToken = token
                    currentUserRole = role
                    saveSession(token, role)
                    when (role) {
                        "admin" -> navController.navigate(Screen.AdminDashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                        "employee" -> navController.navigate(Screen.TableMap.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                        "kitchen" -> navController.navigate(Screen.KitchenDashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                        else -> navController.navigate(Screen.CustomerDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { token, role ->
                    userToken = token
                    currentUserRole = role
                    saveSession(token, role)
                    when (role) {
                        "admin" -> navController.navigate(Screen.AdminDashboard.route) { popUpTo(Screen.Register.route) { inclusive = true } }
                        "employee" -> navController.navigate(Screen.TableMap.route) { popUpTo(Screen.Register.route) { inclusive = true } }
                        "kitchen" -> navController.navigate(Screen.KitchenDashboard.route) { popUpTo(Screen.Register.route) { inclusive = true } }
                        else -> navController.navigate(Screen.CustomerDashboard.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.TableMap.route) {
            userToken?.let { token ->
                TableMapScreen(
                    token = token,
                    viewModel = restaurantViewModel,
                    isCustomer = (currentUserRole == "customer" || currentUserRole == null),
                    onTableSelected = { id, name ->
                        currentTableId = id
                        if (id != 0) {
                            restaurantViewModel.claimTable(token, id)
                        }
                        navController.navigate(Screen.Order.createRoute(id, name))
                    },
                    onNavigateToChatbot = {
                        navController.navigate(Screen.Chatbot.route)
                    },
                    onLogout = {
                        restaurantViewModel.clearAllData()
                        clearSession()
                        userToken = null
                        currentUserRole = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Screen.Order.route,
            arguments = listOf(navArgument("tableId") { type = NavType.IntType }, navArgument("tableName") { type = NavType.StringType })
        ) { backStackEntry ->
            val tableId = backStackEntry.arguments?.getInt("tableId") ?: 0
            val tableName = backStackEntry.arguments?.getString("tableName") ?: "Bàn"
            
            val onNavigateBack = {
                // Giải phóng bàn nếu chưa có món nào - áp dụng cho cả khách và nhân viên
                if (tableId != 0) {
                    userToken?.let { restaurantViewModel.releaseTableIfEmpty(it, tableId) }
                }
                currentTableId = 0
                navController.popBackStack()
            }
            
            androidx.activity.compose.BackHandler {
                onNavigateBack()
            }
            
            userToken?.let { token ->
                HomeScreen(
                    token = token,
                    viewModel = restaurantViewModel,
                    tableName = tableName,
                    isCustomer = (currentUserRole == "customer" || currentUserRole == null),
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                    onNavigateToChatbot = { navController.navigate(Screen.Chatbot.route) },
                    onBack = { onNavigateBack() },
                    onLogout = {
                        // Giải phóng bàn khi logout khỏi trang gọi món (nếu chưa có món)
                        if (tableId != 0) {
                            restaurantViewModel.releaseTableIfEmpty(token, tableId)
                        }
                        currentTableId = 0
                        restaurantViewModel.clearAllData()
                        clearSession()
                        userToken = null
                        currentUserRole = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        }
        composable(Screen.Cart.route) {
            userToken?.let { token ->
                CartScreen(
                    token = token,
                    viewModel = restaurantViewModel,
                    tableId = currentTableId,
                    onNavigateBack = { navController.popBackStack() },
                    onOrderSuccess = {
                        currentTableId = 0
                        if (currentUserRole == "employee") {
                            navController.navigate(Screen.TableMap.route) { popUpTo(Screen.TableMap.route) { inclusive = true } }
                        } else {
                            navController.navigate(Screen.CustomerDashboard.route) { 
                                popUpTo(Screen.Cart.route) { inclusive = true } 
                            }
                        }
                    }
                )
            }
        }
        composable(Screen.CustomerDashboard.route) {
            userToken?.let { token ->
                CustomerDashboardScreen(
                    token = token,
                    restaurantViewModel = restaurantViewModel,
                    authViewModel = authViewModel,
                    onNavigateToTable = {
                        navController.navigate(Screen.TableMap.route)
                    },
                    onNavigateToTakeaway = {
                        currentTableId = 0
                        navController.navigate(Screen.Order.createRoute(0, "Mang về"))
                    },
                    onOrderMore = { tableId, tableName ->
                        currentTableId = tableId
                        navController.navigate(Screen.Order.createRoute(tableId, tableName))
                    },
                    onRequestPayment = { orderId ->
                        restaurantViewModel.requestPayment(orderId)
                    },
                    onNavigateToChatbot = {
                        navController.navigate(Screen.Chatbot.route)
                    },
                    onLogout = {
                        restaurantViewModel.clearAllData()
                        clearSession()
                        userToken = null
                        currentUserRole = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        }
        
        composable(Screen.AdminDashboard.route) {
            userToken?.let { token ->
                AdminDashboardScreen(
                    token = token,
                    viewModel = restaurantViewModel,
                    onLogout = {
                        restaurantViewModel.clearAllData()
                        clearSession()
                        userToken = null
                        currentUserRole = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        }
        composable(Screen.KitchenDashboard.route) {
            userToken?.let { token ->
                KitchenDashboardScreen(
                    token = token,
                    viewModel = restaurantViewModel,
                    onLogout = {
                        restaurantViewModel.clearAllData()
                        clearSession()
                        userToken = null
                        currentUserRole = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        }
        composable(Screen.Chatbot.route) {
            ChatbotScreen(
                onNavigateBack = { navController.popBackStack() },
                restaurantViewModel = restaurantViewModel,
                authViewModel = authViewModel,
                token = userToken,
                currentTableId = currentTableId
            )
        }
    }
}
