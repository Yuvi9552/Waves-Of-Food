package com.example.wavesoffood.model

/**
 * Represents a food item in a hotel's menu, with associated hotel info and coordinates.
 */
data class MenuItem(
    val foodName: String?          = null,
    val foodPrice: String?         = null,
    val foodDescription: String?   = null,
    val foodImage: String?         = null,
    val foodIngredients: String?   = null,
    var hotelName: String?         = null,
    val hotelUserId: String?       = null,
    val hotelLatitude: Double?     = null,  // added geo-coordinate
    val hotelLongitude: Double?    = null   // added geo-coordinate
)
