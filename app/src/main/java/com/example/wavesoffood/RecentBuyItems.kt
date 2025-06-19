package com.example.wavesoffood

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.RecentBuyAdapter
import com.example.wavesoffood.databinding.ActivityRecentBuyItemsBinding
import com.example.wavesoffood.model.OrderDetails

class RecentBuyItems : AppCompatActivity() {

    private val binding by lazy {
        ActivityRecentBuyItemsBinding.inflate(layoutInflater)
    }

    private var allFoodNames = arrayListOf<String>()
    private var allFoodImages = arrayListOf<String>()
    private var allFoodPrices = arrayListOf<String>()
    private var allFoodQuantities = arrayListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val recentOrderItems = intent.getSerializableExtra("RecentBuyOrderItem") as? ArrayList<OrderDetails>
        recentOrderItems?.firstOrNull()?.let { order ->
            allFoodNames = ArrayList(order.foodNames ?: listOf())
            allFoodImages = ArrayList(order.foodImages ?: listOf())
            allFoodPrices = ArrayList(order.foodPrices ?: listOf())
            allFoodQuantities = ArrayList(order.foodQuantities ?: listOf())
        }

        setAdapter()
    }

    private fun setAdapter() {
        binding.recentViewrecycler.apply {
            layoutManager = LinearLayoutManager(this@RecentBuyItems)
            adapter = RecentBuyAdapter(
                this@RecentBuyItems,
                allFoodNames,
                allFoodImages,
                allFoodPrices,
                allFoodQuantities
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
