    package com.example.wavesoffood.adapter

    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.view.LayoutInflater
    import android.view.ViewGroup
    import android.widget.Toast
    import androidx.recyclerview.widget.RecyclerView
    import com.bumptech.glide.Glide
    import com.example.wavesoffood.DetailsActivity
    import com.example.wavesoffood.databinding.CartItemBinding
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.DatabaseReference
    import com.google.firebase.database.FirebaseDatabase

    class CartAdapter(
        private val context: Context,
        private val foodNames: MutableList<String>,
        private val foodPrices: MutableList<String>,
        private val foodDescriptions: MutableList<String>,
        private val foodImages: MutableList<String>,
        private val foodQuantities: MutableList<Int>,
        private val foodIngredients: MutableList<String>,
        private val hotelNames: MutableList<String>,
        private val hotelUserIds: MutableList<String>,         // ✅ New: hotel user IDs
        private val distances: MutableList<String>,
        private val times: MutableList<String>,
        private val itemKeys: MutableList<String>
    ) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

        private val auth = FirebaseAuth.getInstance()
        private val userId = auth.currentUser?.uid ?: ""
        private val cartRef: DatabaseReference = FirebaseDatabase.getInstance()
            .reference.child("user").child(userId).child("CartItems")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CartViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int = foodNames.size

        fun getUpdatedItemQuantities(): MutableList<Int> {
            return foodQuantities.toMutableList()
        }

        inner class CartViewHolder(private val binding: CartItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(position: Int) {
                binding.cartfoodname.text = foodNames[position]
                binding.cartitemprice.text = foodPrices[position]
                binding.cartitemquantity.text = foodQuantities[position].toString()
                binding.cartHotelName.text = hotelNames[position]
                binding.cartDistanceTime.text = "${distances[position]} • ${times[position]}"

                Glide.with(context)
                    .load(Uri.parse(foodImages[position]))
                    .placeholder(android.R.color.darker_gray)
                    .into(binding.cartimage)

                // ✅ Launch DetailsActivity with full extras
                binding.root.setOnClickListener {
                    val intent = Intent(context, DetailsActivity::class.java).apply {
                        putExtra("MenuItemName", foodNames[position])
                        putExtra("MenuItemPrice", foodPrices[position])
                        putExtra("MenuItemImage", foodImages[position])
                        putExtra("MenuItemDescription", foodDescriptions[position])
                        putExtra("MenuItemIngredients", foodIngredients[position])
                        putExtra("HotelUserId", hotelUserIds[position])  // ✅ Include hotel ID
                        putExtra("HotelName", hotelNames[position])      // ✅ Include hotel name
                    }
                    context.startActivity(intent)
                }

                binding.plusbutton.setOnClickListener {
                    if (foodQuantities[position] < 10) {
                        foodQuantities[position]++
                        updateQuantityInFirebase(position)
                    }
                }

                binding.minusbutton.setOnClickListener {
                    if (foodQuantities[position] > 1) {
                        foodQuantities[position]--
                        updateQuantityInFirebase(position)
                    }
                }

                binding.deletebutton.setOnClickListener {
                    deleteItem(position)
                }
            }

            private fun updateQuantityInFirebase(position: Int) {
                val key = itemKeys[position]
                val quantity = foodQuantities[position]

                cartRef.child(key).child("foodQuantity").setValue(quantity)
                    .addOnSuccessListener {
                        binding.cartitemquantity.text = quantity.toString()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show()
                    }
            }

            private fun deleteItem(position: Int) {
                val key = itemKeys[position]

                cartRef.child(key).removeValue()
                    .addOnSuccessListener {
                        foodNames.removeAt(position)
                        foodPrices.removeAt(position)
                        foodDescriptions.removeAt(position)
                        foodImages.removeAt(position)
                        foodQuantities.removeAt(position)
                        foodIngredients.removeAt(position)
                        hotelNames.removeAt(position)
                        hotelUserIds.removeAt(position)  // ✅ Remove hotel ID too
                        distances.removeAt(position)
                        times.removeAt(position)
                        itemKeys.removeAt(position)

                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, itemCount)

                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
