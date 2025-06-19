package com.example.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.DetailsActivity
import com.example.wavesoffood.databinding.PopularItemBinding

class PopularAdapter(
    private val items: List<String>,
    private val prices: List<String>,
    private val images: List<Int>,
    private val context: Context
) : RecyclerView.Adapter<PopularAdapter.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder {
        val binding = PopularItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        val itemName = items[position]
        val itemPrice = prices[position]
        val itemImage = images[position]

        holder.bind(itemName, itemPrice, itemImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", itemName)
                putExtra("MenuItemImage", itemImage)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class PopularViewHolder(private val binding: PopularItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, price: String, imageRes: Int) {
            binding.popularfoodname.text = name
            binding.popularfoodprice.text = price
            binding.popularfoodimage.setImageResource(imageRes)
        }
    }
}
