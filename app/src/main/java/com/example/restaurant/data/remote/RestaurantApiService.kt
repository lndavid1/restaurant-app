package com.example.restaurant.data.remote

import com.example.restaurant.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface RestaurantApiService {
    @POST("api/auth.php?action=register")
    suspend fun register(@Body request: RegisterRequest): Response<Map<String, String>>

    @POST("api/auth.php?action=login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/categories.php")
    suspend fun getCategories(): List<Category>

    @GET("api/products.php")
    suspend fun getProducts(@Query("category_id") categoryId: Int? = null): List<Product>

    @GET("api/admin_tables.php")
    suspend fun getTables(): List<RestaurantTable>

    @POST("api/orders.php")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body request: OrderRequest
    ): Response<Map<String, Any>>

    @GET("api/orders.php")
    suspend fun getOrders(@Header("Authorization") token: String): List<Order>

    @PUT("api/orders.php")
    suspend fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Body request: OrderStatusUpdateRequest
    ): Response<Map<String, String>>

    @POST("api/admin_tables.php?action=checkout")
    suspend fun checkoutTable(
        @Header("Authorization") token: String,
        @Body request: CheckoutRequest
    ): Response<Map<String, String>>

    @GET("api/ai_recommendation.php")
    suspend fun getAIRecommendations(@Header("Authorization") token: String): List<Recommendation>

    // ADMIN PRODUCTS (Auth bypassed via Admin-Role header)
    @POST("api/admin_products.php")
    suspend fun addProduct(@Header("Admin-Role") isAdmin: String = "true", @Body product: Product): Response<Map<String, String>>

    @PUT("api/admin_products.php")
    suspend fun updateProduct(@Header("Admin-Role") isAdmin: String = "true", @Body product: Product): Response<Map<String, String>>

    @DELETE("api/admin_products.php")
    suspend fun deleteProduct(@Header("Admin-Role") isAdmin: String = "true", @Query("id") id: Int): Response<Map<String, String>>
    
    // ADMIN TABLES
    @POST("api/admin_tables.php")
    suspend fun addTable(@Header("Admin-Role") isAdmin: String = "true", @Body table: RestaurantTable): Response<Map<String, String>>

    @PUT("api/admin_tables.php")
    suspend fun updateTable(@Header("Admin-Role") isAdmin: String = "true", @Body table: RestaurantTable): Response<Map<String, String>>

    @DELETE("api/admin_tables.php")
    suspend fun deleteTable(@Header("Admin-Role") isAdmin: String = "true", @Query("id") id: Int): Response<Map<String, String>>
    
    // ADMIN INVENTORY
    @GET("api/admin_inventory.php")
    suspend fun getInventory(@Header("Admin-Role") isAdmin: String = "true"): List<Ingredient>

    @POST("api/admin_inventory.php")
    suspend fun addIngredient(@Header("Admin-Role") isAdmin: String = "true", @Body item: Ingredient): Response<Map<String, String>>

    @PUT("api/admin_inventory.php")
    suspend fun updateIngredient(@Header("Admin-Role") isAdmin: String = "true", @Body item: Ingredient): Response<Map<String, String>>

    @DELETE("api/admin_inventory.php")
    suspend fun deleteIngredient(@Header("Admin-Role") isAdmin: String = "true", @Query("id") id: Int): Response<Map<String, String>>
}
