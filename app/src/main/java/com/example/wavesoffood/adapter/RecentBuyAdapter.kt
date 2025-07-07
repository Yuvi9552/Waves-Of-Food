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
import com.example.wavesoffood.databinding.RecentbuitemBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RecentBuyAdapter(
    private val context: Context,
    private val foodNames: ArrayList<String>,
    private val foodImages: ArrayList<String>,
    private val foodPrices: ArrayList<String>,
    private val foodQuantities: ArrayList<Int>
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

            Glide.with(context)
                .load(Uri.parse(foodImages[position]))
                .into(binding.recentbuyitemfoodimage)

            binding.buyagainfoodbutton2.setOnClickListener {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val cartRef = FirebaseDatabase.getInstance().reference
                        .child("user").child(userId).child("CartItems")
                        .push()

                    val cartItem = CartItems(
                        foodNames = foodNames[position],
                        foodPrice = foodPrices[position],
                        foodImage = foodImages[position],
                        foodQuantity = foodQuantities[position],
                        foodDescriptions = "Reordered item",
                        foodIngredients = "Same as previous"
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
