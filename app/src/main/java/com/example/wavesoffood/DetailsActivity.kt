package com.example.wavesoffood

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.ActivityDetailsBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var auth: FirebaseAuth

    private var foodName: String? = null
    private var foodImage: String? = null
    private var foodDescription: String? = null
    private var foodIngredients: String? = null
    private var foodPrice: String? = null
    private var hotelUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Get Intent Data
        foodName = intent.getStringExtra("MenuItemName")
        foodDescription = intent.getStringExtra("MenuItemDescription")
        foodIngredients = intent.getStringExtra("MenuItemIngredients")
        foodPrice = intent.getStringExtra("MenuItemPrice")
        foodImage = intent.getStringExtra("MenuItemImage")
        hotelUserId = intent.getStringExtra("HotelUserId")

        // Set Food Details
        binding.detailefoodname.text = foodName ?: "No Name"
        binding.descriptiontext.text = foodDescription ?: "No Description"
        binding.ingridientstext.text = foodIngredients ?: "No Ingredients"

        try {
            Glide.with(this)
                .load(Uri.parse(foodImage))
                .into(binding.descriptionimage)
        } catch (e: Exception) {
            Log.e("DetailsActivity", "Image load error: ${e.localizedMessage}")
        }

        binding.descriptionimage.setOnClickListener { finish() }

        binding.addtocartbutton.setOnClickListener { addItemToCart() }

        binding.gotocartbutton.setOnClickListener {
            // ✅ Directly open CartActivity, not MainActivity
            val intent = Intent(this@DetailsActivity, CartActivity::class.java)
            startActivity(intent)
            finish()
        }

        if (!hotelUserId.isNullOrEmpty()) {
            fetchHotelDetails(hotelUserId!!)
        } else {
            Log.e("DetailsActivity", "HotelUserId is NULL or EMPTY. Cannot fetch hotel data.")
        }
    }

    private fun fetchHotelDetails(hotelUserId: String) {
        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("Hotel Users")
            .child(hotelUserId)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hotelName = snapshot.child("nameOfResturant").getValue(String::class.java)
                val hotelAddress = snapshot.child("address").getValue(String::class.java)
                val hotelPhone = snapshot.child("phone").getValue(String::class.java)

                binding.restaurantNameText.text = hotelName ?: "N/A"
                binding.restaurantAddressText.text = hotelAddress ?: "N/A"
                binding.restaurantPhoneText.text = hotelPhone ?: "N/A"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HotelData", "DB Error: ${error.message}")
                Toast.makeText(this@DetailsActivity, "Failed to load hotel info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addItemToCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: return

        val cartItem = CartItems(
            foodNames = foodName,
            foodPrice = foodPrice,
            foodDescriptions = foodDescription,
            foodImage = foodImage,
            foodQuantity = 1,
            foodIngredients = foodIngredients
        )

        database.child("user").child(userId).child("CartItems").push()
            .setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Item Added to Cart", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Add Item", Toast.LENGTH_SHORT).show()
            }
    }
}
