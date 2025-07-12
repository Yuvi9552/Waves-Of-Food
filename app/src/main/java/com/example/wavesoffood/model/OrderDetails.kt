package com.example.wavesoffood.model

import java.io.Serializable

class OrderDetails() : Serializable {
    var userId: String? = null
    var userNames: String? = null
    var foodNames: MutableList<String>? = null
    var foodImages: MutableList<String>? = null
    var foodPrices: MutableList<String>? = null
    var foodQuantities: MutableList<Int>? = null
    var address: String? = null
    var totalPrices: String? = null
    var phoneNumber: String? = null
    var orderAccepted: Boolean = false
    var paymentReceived: Boolean = false
    var itemPushkey: String? = null
    var currentTime: Long = 0
    var hotelUserId: String? = null
    var hotelName: String? = null // ✅ Added field

    constructor(
        userId: String,
        userNames: String,
        foodNames: MutableList<String>,
        foodPrices: MutableList<String>,
        foodImages: MutableList<String>,
        foodQuantities: MutableList<Int>,
        address: String,
        totalPrices: String,
        phoneNumber: String,
        currentTime: Long,
        itemPushkey: String?,
        orderAccepted: Boolean,
        paymentReceived: Boolean,
        hotelUserId: String? = null,
        hotelName: String? = null // ✅ Include in constructor
    ) : this() {
        this.userId = userId
        this.userNames = userNames
        this.foodNames = foodNames
        this.foodPrices = foodPrices
        this.foodImages = foodImages
        this.foodQuantities = foodQuantities
        this.address = address
        this.totalPrices = totalPrices
        this.phoneNumber = phoneNumber
        this.currentTime = currentTime
        this.itemPushkey = itemPushkey
        this.orderAccepted = orderAccepted
        this.paymentReceived = paymentReceived
        this.hotelUserId = hotelUserId
        this.hotelName = hotelName // ✅ Assign value
    }
}
