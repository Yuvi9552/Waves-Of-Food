package com.example.wavesoffood

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.databinding.ActivityPayQuickBinding
import com.example.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

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

        // Retrieve all data from Intent
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
            personAddress = binding.personAdress.text.toString().trim()
            personPhone = binding.personPhone.text.toString().trim()

            if (personName.isBlank() || personAddress.isBlank() || personPhone.isBlank()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            } else {
                placeOrder()
            }
        }
    }

    private fun placeOrder() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val time = System.currentTimeMillis()
        val pushKey = databaseRef.child("orderDetails").push().key ?: return

        val order = OrderDetails(
            uid,
            personName,
            foodItemName,
            foodItemPrice,
            foodItemImage,
            foodItemQuantity,
            personAddress,
            totalAmountCalculate,
            personPhone,
            time,
            pushKey,
            orderAccepted = false,
            paymentReceived = false
        )

        // Save order in general orderDetails node
        databaseRef.child("orderDetails").child(pushKey).setValue(order)
            .addOnSuccessListener {
                // Also save under user's BuyHistory
                databaseRef.child("user").child(uid).child("BuyHistory").child(pushKey).setValue(order)

                // Clear cart
                databaseRef.child("user").child(uid).child("CartItems").removeValue()

                // Show confirmation
                CongratsBottomSheet().show(supportFragmentManager, "Congrats")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateTotalAmount(): Int {
        var total = 0
        for (i in foodItemPrice.indices) {
            val price = foodItemPrice[i].replace("₹", "").replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
            val qty = foodItemQuantity.getOrNull(i) ?: 1
            total += price * qty
        }
        return total
    }

    private fun setUserData() {
        val user = auth.currentUser ?: return
        databaseRef.child("user").child(user.uid).get()
            .addOnSuccessListener { snapshot ->
                binding.apply {
                    personName.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                    personAdress.setText(snapshot.child("address").getValue(String::class.java) ?: "")
                    personPhone.setText(snapshot.child("phone").getValue(String::class.java) ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }
}
