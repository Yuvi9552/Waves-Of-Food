package com.example.wavesoffood.Fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.wavesoffood.RecentBuyItems
import com.example.wavesoffood.adapter.BuyAgainAdapter
import com.example.wavesoffood.databinding.FragmentHistoryBinding
import com.example.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var listOfOrderItem: MutableList<OrderDetails> = mutableListOf()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.recentbuyitemsdisplay.visibility = View.INVISIBLE

        fetchBuyHistory()

        binding.recentbuyitemsdisplay.setOnClickListener { seeItemsRecentBuy() }
        binding.receivedbutton.setOnClickListener { updateOrderStatus() }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction()
                    .replace(com.example.wavesoffood.R.id.fragment_container, HomeFragment())
                    .commit()
            }
        })

        return binding.root
    }

    private fun fetchBuyHistory() {
        userId = auth.currentUser?.uid ?: return
        val historyRef = database.reference.child("user").child(userId).child("BuyHistory")
        val sortedQuery = historyRef.orderByChild("currentTime")

        sortedQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                listOfOrderItem.clear()

                for (buySnapshot in snapshot.children) {
                    val item = buySnapshot.getValue(OrderDetails::class.java)
                    item?.let { listOfOrderItem.add(it) }
                }

                listOfOrderItem.reverse()

                if (listOfOrderItem.isNotEmpty()) {
                    setDataInRecentBuyItem()
                    setPreviousBuyItemsRecyclerView()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateOrderStatus() {
        val itemPushKey = listOfOrderItem.firstOrNull()?.itemPushkey ?: return
        database.reference.child("CompletedOrder").child(itemPushKey)
            .child("paymentReceived").setValue(true)
    }

    private fun seeItemsRecentBuy() {
        if (!isAdded || listOfOrderItem.isEmpty()) return
        val intent = Intent(requireContext(), RecentBuyItems::class.java)
        intent.putExtra("RecentBuyOrderItem", ArrayList(listOfOrderItem))
        startActivity(intent)
    }

    private fun setDataInRecentBuyItem() {
        if (!isAdded) return

        binding.recentbuyitemsdisplay.visibility = View.VISIBLE
        val item = listOfOrderItem.firstOrNull() ?: return

        binding.buyagainfoodnamehistory.text = item.foodNames?.firstOrNull() ?: ""
        binding.buyagainfoodpricehistory.text = item.foodPrices?.firstOrNull() ?: ""

        val imageUrl = item.foodImages?.firstOrNull()
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(Uri.parse(imageUrl)).into(binding.foodimages)
        }

        if (item.orderAccepted) {
            binding.orderstatus.background.setTint(Color.GREEN)
            binding.receivedbutton.visibility = View.VISIBLE
        } else {
            binding.receivedbutton.visibility = View.GONE
        }
    }

    private fun setPreviousBuyItemsRecyclerView() {
        if (!isAdded || context == null) return

        val names = mutableListOf<String>()
        val prices = mutableListOf<String>()
        val images = mutableListOf<String>()

        for (i in 1 until listOfOrderItem.size) {
            listOfOrderItem[i].foodNames?.firstOrNull()?.let { names.add(it) }
            listOfOrderItem[i].foodPrices?.firstOrNull()?.let { prices.add(it) }
            listOfOrderItem[i].foodImages?.firstOrNull()?.let { images.add(it) }
        }

        binding.buyagainrecyler.layoutManager = LinearLayoutManager(requireContext())
        buyAgainAdapter = BuyAgainAdapter(names, prices, images, requireContext())
        binding.buyagainrecyler.adapter = buyAgainAdapter
    }
}
