package com.example.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.CartActivity
import com.example.wavesoffood.DetailsActivity
import com.example.wavesoffood.databinding.RecentbuitemBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RecentBuyAdapter(
    private val context: Context,
    private val foodNames: ArrayList<String>,
    private val foodImages: ArrayList<String>,
    private val foodPrices: ArrayList<String>,
    private val foodQuantities: ArrayList<Int>,
    private val foodDescriptions: ArrayList<String>,       // ✅ Add description
    private val foodIngredients: ArrayList<String>,        // ✅ Add ingredients
    private val hotelNames: ArrayList<String>,
    private val hotelUserId: String?
) : RecyclerView.Adapter<RecentBuyAdapter.RecentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding = RecentbuitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = foodNames.size

    inner class RecentViewHolder(private val binding: RecentbuitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.recentbuyitemfoodname.text = foodNames[position]
            binding.recentbuyitemfoodprice.text = foodPrices[position]
            binding.foodquantityview.text = foodQuantities[position].toString()
            binding.recentbuyitemhotelname.text = hotelNames[position]

            Glide.with(context)
                .load(Uri.parse(foodImages[position]))
                .into(binding.recentbuyitemfoodimage)

            // ✅ Click on item opens DetailsActivity with proper extras
            binding.root.setOnClickListener {
                val intent = Intent(context, DetailsActivity::class.java).apply {
                    putExtra("MenuItemName", foodNames[position])
                    putExtra("MenuItemPrice", foodPrices[position])
                    putExtra("MenuItemImage", foodImages[position])
                    putExtra("MenuItemDescription", foodDescriptions[position])
                    putExtra("MenuItemIngredients", foodIngredients[position])
                    putExtra("HotelUserId", hotelUserId)
                    putExtra("HotelName", hotelNames[position])
                }
                context.startActivity(intent)
            }

            // ✅ Buy again
            binding.buyagainfoodbutton2.setOnClickListener {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val cartRef = FirebaseDatabase.getInstance().reference
                    .child("user").child(userId).child("CartItems").push()

                if (!hotelUserId.isNullOrEmpty()) {
                    val hotelRef = FirebaseDatabase.getInstance().reference
                        .child("Hotel Users").child(hotelUserId).child("nameOfResturant")

                    hotelRef.get().addOnSuccessListener { snapshot ->
                        val hotelName = snapshot.getValue(String::class.java) ?: hotelNames[position]

                        val cartItem = CartItems(
                            foodNames = foodNames[position],
                            foodPrice = foodPrices[position],
                            foodImage = foodImages[position],
                            foodQuantity = foodQuantities[position],
                            foodDescriptions = foodDescriptions[position],       // ✅ Proper value
                            foodIngredients = foodIngredients[position],         // ✅ Proper value
                            hotelName = hotelName,
                            hotelUserId = hotelUserId
                        )

                        cartRef.setValue(cartItem).addOnSuccessListener {
                            Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show()
                            context.startActivity(Intent(context, CartActivity::class.java))
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to add to cart", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to get hotel info", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    val cartItem = CartItems(
                        foodNames = foodNames[position],
                        foodPrice = foodPrices[position],
                        foodImage = foodImages[position],
                        foodQuantity = foodQuantities[position],
                        foodDescriptions = foodDescriptions[position],
                        foodIngredients = foodIngredients[position],
                        hotelName = hotelNames[position]
                    )

                    cartRef.setValue(cartItem).addOnSuccessListener {
                        Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, CartActivity::class.java))
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to add to cart", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
