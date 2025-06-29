package com.example.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.DetailsActivity
import com.example.wavesoffood.databinding.PopularItemBinding
import com.example.wavesoffood.model.MenuItem

class PopularAdapter(
    private val items: List<MenuItem>,
    private val context: Context
) : RecyclerView.Adapter<PopularAdapter.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder {
        val binding = PopularItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        val item = items[position]
        Log.d("PopularAdapter", "Binding item at position $position: $item")
        holder.bind(item)

        holder.itemView.setOnClickListener {
            Log.d("PopularAdapter", "Clicked item → $item")
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", item.foodName)
                putExtra("MenuItemPrice", item.foodPrice)
                putExtra("MenuItemImage", item.foodImage)
                putExtra("MenuItemDescription", item.foodDescription)
                putExtra("MenuItemIngredients", item.foodIngredients)
                putExtra("HotelUserId", item.hotelUserId)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class PopularViewHolder(private val binding: PopularItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            binding.popularfoodname.text = item.foodName ?: "N/A"
            binding.popularfoodprice.text = item.foodPrice ?: "₹0"
            Glide.with(binding.root.context)
                .load(item.foodImage)
                .into(binding.popularfoodimage)
        }
    }
}
