package com.example.restaurant.data.model

data class Ingredient(
    val id: Int = 0,
    val name: String = "",
    val unit: String = "",
    val stock: Double = 0.0,
    val updated_at: String? = null
)
