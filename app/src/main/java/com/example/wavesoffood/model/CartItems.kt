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
    var hotelUserId: String? = null,         // ✅ Added to support full details in Cart & DetailsActivity
    var hotelLatitude: Double? = null,
    var hotelLongitude: Double? = null
) : Serializable
