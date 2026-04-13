package com.example.restaurant.utils

import java.text.DecimalFormat

fun Number.toVndFormat(): String {
    val formatter = DecimalFormat("#,###")
    return formatter.format(this)
}
