package com.example.wavesoffood.model

import java.io.Serializable

data class CartItems(
    var foodNames: String? = null,
    var foodPrice: String? = null,
    var foodImage: String? = null,
    var foodQuantity: Int? = null,
    var foodDescriptions: String? = null,
    var foodIngredients: String? = null,
    var hotelName: String? = null,
    var hotelLatitude: Double? = null,   // ✅ NEW
    var hotelLongitude: Double? = null   // ✅ NEW
) : Serializable

