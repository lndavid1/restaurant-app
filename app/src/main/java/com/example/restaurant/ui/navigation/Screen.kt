package com.example.restaurant.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object TableMap : Screen("table_map") // Giao diện chính của nhân viên
    object Order : Screen("order/{tableId}/{tableName}") { // Giao diện chọn món theo bàn
        fun createRoute(tableId: Int, tableName: String) = "order/$tableId/$tableName"
    }
    object Cart : Screen("cart")
    object Chatbot : Screen("chatbot")
    object AdminDashboard : Screen("admin_dashboard")
    object KitchenDashboard : Screen("kitchen_dashboard")
    object CustomerDashboard : Screen("customer_dashboard")
}
