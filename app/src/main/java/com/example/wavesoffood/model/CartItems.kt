package com.example.wavesoffood.model

data class CartItems(
    var foodNames: String? = null,
    var foodPrice: String? = null,
    var foodDescriptions: String? = null,
    var foodImage: String? = null,
    var foodQuantity: Int? = null,
    var foodIngredients: String? = null
)
