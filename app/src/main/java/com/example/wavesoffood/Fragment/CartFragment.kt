package com.example.wavesoffood.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.PayQuickActivity
import com.example.wavesoffood.adapter.CartAdapter
import com.example.wavesoffood.databinding.FragmentCartBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartFragment : Fragment() {

    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter

    private val foodNames = mutableListOf<String>()
    private val foodPrices = mutableListOf<String>()
    private val foodDescriptions = mutableListOf<String>()
    private val foodImages = mutableListOf<String>()
    private val foodQuantity = mutableListOf<Int>()
    private val foodIngredients = mutableListOf<String>()
    private val itemKeys = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // ✅ Load items from Firebase
        retrieveCartItems()

        // ✅ Proceed button to PayQuickActivity
        binding.proceedbuttoncart.setOnClickListener {
            getOrderItemsDetails()
        }

        // ✅ Back navigation to HomeFragment
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction()
                    .replace(com.example.wavesoffood.R.id.fragment_container, HomeFragment())
                    .commit()
            }
        })

        return binding.root
    }

    private fun retrieveCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.reference.child("user").child(userId).child("CartItems")

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || context == null) return

                // Clear previous lists
                foodNames.clear()
                foodPrices.clear()
                foodDescriptions.clear()
                foodImages.clear()
                foodQuantity.clear()
                foodIngredients.clear()
                itemKeys.clear()

                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItems::class.java)
                    item?.let {
                        foodNames.add(it.foodNames ?: "")
                        foodPrices.add(it.foodPrice ?: "")
                        foodDescriptions.add(it.foodDescriptions ?: "")
                        foodImages.add(it.foodImage ?: "")
                        foodQuantity.add(it.foodQuantity ?: 1)
                        foodIngredients.add(it.foodIngredients ?: "")
                        itemKeys.add(itemSnapshot.key ?: "")
                    }
                }

                // Show cart items
                if (foodNames.isNotEmpty()) {
                    cartAdapter = CartAdapter(requireContext(), foodNames, foodPrices, foodDescriptions, foodImages, foodQuantity, foodIngredients, itemKeys)
                    binding.cartrecyclerview.layoutManager = LinearLayoutManager(requireContext())
                    binding.cartrecyclerview.adapter = cartAdapter
                    binding.cartrecyclerview.visibility = View.VISIBLE
                    binding.emptyCartText.visibility = View.GONE
                } else {
                    binding.cartrecyclerview.visibility = View.GONE
                    binding.emptyCartText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    private fun getOrderItemsDetails() {
        val userId = auth.currentUser?.uid ?: return
        val orderRef = database.reference.child("user").child(userId).child("CartItems")

        val foodName = mutableListOf<String>()
        val foodPrice = mutableListOf<String>()
        val foodDescriptions = mutableListOf<String>()
        val foodImage = mutableListOf<String>()
        val foodIngredients = mutableListOf<String>()
        val foodQuantities = cartAdapter.getUpdatedItemQuantities()

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (foodSnapshot in snapshot.children) {
                    val item = foodSnapshot.getValue(CartItems::class.java)
                    item?.let {
                        foodName.add(it.foodNames ?: "")
                        foodPrice.add(it.foodPrice ?: "")
                        foodDescriptions.add(it.foodDescriptions ?: "")
                        foodImage.add(it.foodImage ?: "")
                        foodIngredients.add(it.foodIngredients ?: "")
                    }
                }

                orderNow(foodName, foodPrice, foodImage, foodDescriptions, foodIngredients, foodQuantities)
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    private fun orderNow(
        foodName: MutableList<String>,
        foodPrice: MutableList<String>,
        foodImage: MutableList<String>,
        foodDescriptions: MutableList<String>,
        foodIngredients: MutableList<String>,
        foodQuantities: MutableList<Int>
    ) {
        if (!isAdded || context == null) return

        val intent = Intent(requireContext(), PayQuickActivity::class.java).apply {
            putStringArrayListExtra("FoodItemName", ArrayList(foodName))
            putStringArrayListExtra("FoodItemPrice", ArrayList(foodPrice))
            putStringArrayListExtra("FoodItemImage", ArrayList(foodImage))
            putStringArrayListExtra("FoodItemDescriptions", ArrayList(foodDescriptions))
            putStringArrayListExtra("FoodItemIngredients", ArrayList(foodIngredients))
            putIntegerArrayListExtra("FoodItemQuantity", ArrayList(foodQuantities))
            putExtra("fromCart", true)
        }
        startActivity(intent)
    }
}
