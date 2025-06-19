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

    constructor(
        userId: String,
        name: String,
        foodItemName: ArrayList<String>,
        foodItemPrice: ArrayList<String>,
        foodItemImage: ArrayList<String>,
        foodItemQuantities: ArrayList<Int>,
        address: String,
        totalAmount: String,
        phone: String,
        time: Long,
        itemPushKey: String?,
        orderAccepted: Boolean,
        paymentReceived: Boolean
    ) : this() {
        this.userId = userId
        this.userNames = name
        this.foodNames = foodItemName
        this.foodPrices = foodItemPrice
        this.foodImages = foodItemImage
        this.foodQuantities = foodItemQuantities
        this.address = address
        this.totalPrices = totalAmount
        this.phoneNumber = phone
        this.currentTime = time
        this.itemPushkey = itemPushKey
        this.orderAccepted = orderAccepted
        this.paymentReceived = paymentReceived
    }
}
