package com.example.wavesoffood.model

data class MenuItem(
    val foodName: String? = null,
    val foodPrice: String? = null,
    val foodDescription: String? = null,
    val foodImage: String? = null,
    val foodIngredients: String? = null,
    var hotelName: String? = null,
    val hotelUserId: String? = null // ✅ NEW FIELD
)
