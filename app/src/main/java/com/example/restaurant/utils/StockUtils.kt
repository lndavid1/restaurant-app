package com.example.restaurant.utils

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
