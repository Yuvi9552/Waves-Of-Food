package com.example.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.CartActivity
import com.example.wavesoffood.RecentBuyItems
import com.example.wavesoffood.databinding.BuyagainitemBinding
import com.example.wavesoffood.model.CartItems
import com.example.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BuyAgainAdapter(
    private val previousOrders: List<OrderDetails>,
    private val context: Context
) : RecyclerView.Adapter<BuyAgainAdapter.BuyAgainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyAgainViewHolder {
        val binding = BuyagainitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuyAgainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuyAgainViewHolder, position: Int) {
        holder.bind(previousOrders[position])
    }

    override fun getItemCount(): Int = previousOrders.size

    inner class BuyAgainViewHolder(private val binding: BuyagainitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderDetails) {
            val name = order.foodNames?.firstOrNull() ?: "Food"
            val price = order.foodPrices?.firstOrNull() ?: "₹0"
            val image = order.foodImages?.firstOrNull()
            val quantity = order.foodQuantities?.firstOrNull() ?: 1
            val description = order.foodDescriptions?.firstOrNull() ?: "Reordered item"
            val ingredients = order.foodIngredients?.firstOrNull() ?: "Same as previous"
            val hotelUserId = order.hotelUserId
            val fallbackHotelName = order.hotelName ?: "N/A"

            binding.buyagainfoodname.text = name
            binding.buyagainfoodprice.text = price
            binding.foodquantityview2.text = quantity.toString()
            binding.buyagainhotelname.text = fallbackHotelName

            if (!image.isNullOrEmpty()) {
                Glide.with(context).load(Uri.parse(image)).into(binding.buyagaianfoodimage)
            }

            // 🔁 Handle "Buy Again"
            binding.buyagainfoodbutton.setOnClickListener {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val cartRef = FirebaseDatabase.getInstance().reference
                    .child("user").child(userId).child("CartItems").push()

                fun addToCart(
                    hotelName: String,
                    latitude: Double? = null,
                    longitude: Double? = null
                ) {
                    val cartItem = CartItems(
                        foodNames = name,
                        foodPrice = price,
                        foodImage = image,
                        foodQuantity = quantity,
                        foodDescriptions = description,
                        foodIngredients = ingredients,
                        hotelName = hotelName,
                        hotelUserId = hotelUserId,
                        hotelLatitude = latitude,
                        hotelLongitude = longitude
                    )

                    cartRef.setValue(cartItem).addOnSuccessListener {
                        Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show()

                        Handler(Looper.getMainLooper()).postDelayed({
                            context.startActivity(Intent(context, CartActivity::class.java))
                        }, 300)
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to add to cart", Toast.LENGTH_SHORT).show()
                    }
                }

                if (!hotelUserId.isNullOrEmpty()) {
                    val hotelRef = FirebaseDatabase.getInstance().reference
                        .child("Hotel Users").child(hotelUserId)

                    hotelRef.get().addOnSuccessListener { snapshot ->
                        val hotelName = snapshot.child("nameOfResturant").getValue(String::class.java) ?: fallbackHotelName
                        val lat = snapshot.child("latitude").getValue(Double::class.java)
                        val lon = snapshot.child("longitude").getValue(Double::class.java)

                        binding.buyagainhotelname.text = hotelName
                        addToCart(hotelName, lat, lon)

                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to fetch hotel info", Toast.LENGTH_SHORT).show()
                        addToCart(fallbackHotelName)
                    }
                } else {
                    addToCart(fallbackHotelName)
                }
            }

            // 👀 Navigate to full recent order
            binding.root.setOnClickListener {
                val intent = Intent(context, RecentBuyItems::class.java)
                intent.putExtra("RecentBuyOrderItem", arrayListOf(order))
                context.startActivity(intent)
            }
        }
    }
}
