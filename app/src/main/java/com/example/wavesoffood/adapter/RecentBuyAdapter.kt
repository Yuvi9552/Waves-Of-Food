package com.example.wavesoffood.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.RecentbuitemBinding

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

            val imageUri = Uri.parse(foodImages[position])
            Glide.with(context).load(imageUri).into(binding.recentbuyitemfoodimage)
        }
    }
}
