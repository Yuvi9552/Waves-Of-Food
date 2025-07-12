package com.example.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.CartAdapter
import com.example.wavesoffood.databinding.ActivityCartBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter

    private val foodNames = mutableListOf<String>()
    private val foodPrices = mutableListOf<String>()
    private val foodDescriptions = mutableListOf<String>()
    private val foodImages = mutableListOf<String>()
    private val foodQuantity = mutableListOf<Int>()
    private val foodIngredients = mutableListOf<String>()
    private val hotelNames = mutableListOf<String>() // ✅ Added hotel name list
    private val itemKeys = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        retrieveCartItems()

        binding.proceedbuttoncart.setOnClickListener {
            getOrderItemsDetails()
        }
    }

    private fun retrieveCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.reference.child("user").child(userId).child("CartItems")

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodNames.clear()
                foodPrices.clear()
                foodDescriptions.clear()
                foodImages.clear()
                foodQuantity.clear()
                foodIngredients.clear()
                hotelNames.clear() // ✅ Clear hotel names
                itemKeys.clear()

                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItems::class.java)
                    item?.let {
                        foodNames.add(it.foodNames ?: "")
                        foodPrices.add(it.foodPrice ?: "")
                        foodDescriptions.add(it.foodDescriptions ?: "")
                        foodImages.add(it.foodImage ?: "")
                        foodQuantity.add(it.foodQuantity ?: 1)
                        foodIngredients.add(it.foodIngredients ?: "")
                        hotelNames.add(it.hotelName ?: "N/A") // ✅ Fetch hotel name
                        itemKeys.add(itemSnapshot.key ?: "")
                    }
                }

                if (foodNames.isNotEmpty()) {
                    cartAdapter = CartAdapter(
                        this@CartActivity,
                        foodNames,
                        foodPrices,
                        foodDescriptions,
                        foodImages,
                        foodQuantity,
                        foodIngredients,
                        hotelNames, // ✅ Pass to adapter
                        itemKeys
                    )
                    binding.cartrecyclerview.layoutManager = LinearLayoutManager(this@CartActivity)
                    binding.cartrecyclerview.adapter = cartAdapter
                    binding.cartrecyclerview.visibility = View.VISIBLE
                    binding.emptyCartText.visibility = View.GONE
                } else {
                    binding.cartrecyclerview.visibility = View.GONE
                    binding.emptyCartText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartActivity, "Failed to load cart items", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderItemsDetails() {
        val userId = auth.currentUser?.uid ?: return
        val orderRef = database.reference.child("user").child(userId).child("CartItems")

        val foodName = mutableListOf<String>()
        val foodPrice = mutableListOf<String>()
        val foodDescriptions = mutableListOf<String>()
        val foodImage = mutableListOf<String>()
        val foodIngredients = mutableListOf<String>()
        val foodQuantities = cartAdapter.getUpdatedItemQuantities()

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (foodSnapshot in snapshot.children) {
                    val item = foodSnapshot.getValue(CartItems::class.java)
                    item?.let {
                        foodName.add(it.foodNames ?: "")
                        foodPrice.add(it.foodPrice ?: "")
                        foodDescriptions.add(it.foodDescriptions ?: "")
                        foodImage.add(it.foodImage ?: "")
                        foodIngredients.add(it.foodIngredients ?: "")
                    }
                }

                val intent = Intent(this@CartActivity, PayQuickActivity::class.java).apply {
                    putStringArrayListExtra("FoodItemName", ArrayList(foodName))
                    putStringArrayListExtra("FoodItemPrice", ArrayList(foodPrice))
                    putStringArrayListExtra("FoodItemImage", ArrayList(foodImage))
                    putStringArrayListExtra("FoodItemDescriptions", ArrayList(foodDescriptions))
                    putStringArrayListExtra("FoodItemIngredients", ArrayList(foodIngredients))
                    putIntegerArrayListExtra("FoodItemQuantity", ArrayList(foodQuantities))
                    putExtra("fromCart", true)
                }
                startActivity(intent)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartActivity, "Error retrieving order", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
