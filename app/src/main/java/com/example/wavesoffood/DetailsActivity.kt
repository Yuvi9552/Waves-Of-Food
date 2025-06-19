package com.example.wavesoffood

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.ActivityDetailsBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var auth: FirebaseAuth

    private var foodName: String? = null
    private var foodImage: String? = null
    private var foodDescription: String? = null
    private var foodIngredients: String? = null
    private var foodPrice: String? = null

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

        // Bind data to views
        binding.detailefoodname.text = foodName
        binding.descriptiontext.text = foodDescription
        binding.ingridientstext.text = foodIngredients

        Glide.with(this)
            .load(Uri.parse(foodImage))
            .into(binding.descriptionimage)

        binding.descriptionimage.setOnClickListener { finish() }

        binding.addtocartbutton.setOnClickListener { addItemToCart() }
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
