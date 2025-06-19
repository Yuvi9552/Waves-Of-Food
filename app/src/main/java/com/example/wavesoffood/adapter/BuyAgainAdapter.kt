package com.example.wavesoffood.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.BuyagainitemBinding

class BuyAgainAdapter(
    private val foodNames: MutableList<String>,
    private val foodPrices: MutableList<String>,
    private val foodImages: MutableList<String>,
    private val context: Context
) : RecyclerView.Adapter<BuyAgainAdapter.BuyAgainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyAgainViewHolder {
        val binding = BuyagainitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuyAgainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuyAgainViewHolder, position: Int) {
        holder.bind(
            foodNames[position],
            foodPrices[position],
            foodImages[position]
        )
    }

    override fun getItemCount(): Int = foodNames.size

    inner class BuyAgainViewHolder(private val binding: BuyagainitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, price: String, imageUrl: String) {
            binding.buyagainfoodname.text = name
            binding.buyagainfoodprice.text = price
            Glide.with(context)
                .load(Uri.parse(imageUrl))
                .into(binding.buyagaianfoodimage)
        }
    }
}
