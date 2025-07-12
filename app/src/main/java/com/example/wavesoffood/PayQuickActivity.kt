package com.example.wavesoffood

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.databinding.ActivityPayQuickBinding
import com.example.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PayQuickActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPayQuickBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference

    private lateinit var personName: String
    private lateinit var personAddress: String
    private lateinit var personPhone: String
    private lateinit var totalAmountCalculate: String

    private lateinit var foodItemName: ArrayList<String>
    private lateinit var foodItemPrice: ArrayList<String>
    private lateinit var foodItemImage: ArrayList<String>
    private lateinit var foodItemDescriptions: ArrayList<String>
    private lateinit var foodItemIngredients: ArrayList<String>
    private lateinit var foodItemQuantity: ArrayList<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPayQuickBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference

        binding.personAddress.isEnabled = false

        foodItemName = intent.getStringArrayListExtra("FoodItemName") ?: arrayListOf()
        foodItemPrice = intent.getStringArrayListExtra("FoodItemPrice") ?: arrayListOf()
        foodItemImage = intent.getStringArrayListExtra("FoodItemImage") ?: arrayListOf()
        foodItemDescriptions = intent.getStringArrayListExtra("FoodItemDescriptions") ?: arrayListOf()
        foodItemIngredients = intent.getStringArrayListExtra("FoodItemIngredients") ?: arrayListOf()
        foodItemQuantity = intent.getIntegerArrayListExtra("FoodItemQuantity") ?: arrayListOf()

        setUserData()

        totalAmountCalculate = "${calculateTotalAmount()} ₹"
        binding.totalcalculateamount.setText(totalAmountCalculate)
        binding.totalcalculateamount.isEnabled = false

        binding.placemyorderbutton.setOnClickListener {
            personName = binding.personName.text.toString().trim()
            personAddress = binding.personAddress.text.toString().trim()
            personPhone = binding.personPhone.text.toString().trim()

            if (personName.isBlank() || personAddress.isBlank() || personPhone.isBlank()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            } else {
                placeOrder()
            }
        }
    }

    private fun setUserData() {
        val user = auth.currentUser ?: return
        databaseRef.child("user").child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.personName.setText(
                        snapshot.child("name").getValue(String::class.java) ?: ""
                    )
                    binding.personPhone.setText(
                        snapshot.child("phone").getValue(String::class.java) ?: ""
                    )

                    val addressFromLastLocation = snapshot
                        .child("lastLocation")
                        .child("location")
                        .getValue(String::class.java)
                    binding.personAddress.setText(addressFromLastLocation ?: "No address available")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PayQuickActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun placeOrder() {
        val uid = auth.currentUser?.uid ?: return
        val time = System.currentTimeMillis()

        val foodMapByHotel = mutableMapOf<String, MutableList<Int>>()
        val menuRef = databaseRef.child("Hotel Users")

        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (i in foodItemName.indices) {
                    val image = foodItemImage[i]
                    for (hotelSnapshot in snapshot.children) {
                        val hotelId = hotelSnapshot.key ?: continue
                        val menuSnapshot = hotelSnapshot.child("menu")
                        for (menuItem in menuSnapshot.children) {
                            val dbImage = menuItem.child("foodImage").getValue(String::class.java)
                            if (dbImage == image) {
                                foodMapByHotel.getOrPut(hotelId) { mutableListOf() }.add(i)
                                break
                            }
                        }
                    }
                }

                if (foodMapByHotel.isEmpty()) {
                    Toast.makeText(this@PayQuickActivity, "No hotel info found for items", Toast.LENGTH_SHORT).show()
                    return
                }

                for ((hotelId, itemIndices) in foodMapByHotel) {
                    val hotelSnapshot = snapshot.child(hotelId)
                    val hotelName = hotelSnapshot.child("nameOfResturant").getValue(String::class.java) ?: "Unknown Hotel"

                    val itemNames = ArrayList<String>()
                    val itemPrices = ArrayList<String>()
                    val itemImages = ArrayList<String>()
                    val itemQuantities = ArrayList<Int>()
                    var totalForHotel = 0

                    for (i in itemIndices) {
                        itemNames.add(foodItemName[i])
                        itemPrices.add(foodItemPrice[i])
                        itemImages.add(foodItemImage[i])
                        itemQuantities.add(foodItemQuantity[i])

                        val price = foodItemPrice[i]
                            .replace("₹", "")
                            .replace("[^0-9]".toRegex(), "")
                            .toIntOrNull() ?: 0
                        totalForHotel += price * foodItemQuantity[i]
                    }

                    val orderKey = databaseRef.push().key ?: continue
                    val order = OrderDetails(
                        userId = uid,
                        userNames = personName,
                        foodNames = itemNames,
                        foodPrices = itemPrices,
                        foodImages = itemImages,
                        foodQuantities = itemQuantities,
                        address = personAddress,
                        totalPrices = "${totalForHotel} ₹",
                        phoneNumber = personPhone,
                        currentTime = time,
                        itemPushkey = orderKey,
                        orderAccepted = false,
                        paymentReceived = false,
                        hotelUserId = hotelId,
                        hotelName = hotelName // ✅ ADDED
                    )

                    databaseRef.child("Hotel Users").child(hotelId).child("OrderDetails").child(orderKey).setValue(order)
                    databaseRef.child("user").child(uid).child("BuyHistory").child(orderKey).setValue(order)
                }

                val fullOrderKey = databaseRef.push().key ?: return
                val fullOrder = OrderDetails(
                    userId = uid,
                    userNames = personName,
                    foodNames = foodItemName,
                    foodPrices = foodItemPrice,
                    foodImages = foodItemImage,
                    foodQuantities = foodItemQuantity,
                    address = personAddress,
                    totalPrices = totalAmountCalculate,
                    phoneNumber = personPhone,
                    currentTime = time,
                    itemPushkey = fullOrderKey,
                    orderAccepted = false,
                    paymentReceived = false,
                    hotelUserId = null,
                    hotelName = "Multiple Hotels" // ✅ for full summary
                )
                databaseRef.child("orderDetails").child(fullOrderKey).setValue(fullOrder)

                databaseRef.child("user").child(uid).child("CartItems").removeValue()

                CongratsBottomSheet().show(supportFragmentManager, "Congrats")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PayQuickActivity, "Error fetching hotel menus", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotalAmount(): Int {
        var total = 0
        for (i in foodItemPrice.indices) {
            val price = foodItemPrice[i]
                .replace("₹", "")
                .replace("[^0-9]".toRegex(), "")
                .toIntOrNull() ?: 0
            total += price * (foodItemQuantity.getOrNull(i) ?: 1)
        }
        return total
    }
}
