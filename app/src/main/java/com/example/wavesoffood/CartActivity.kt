// CartActivity.kt
package com.example.wavesoffood

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.CartAdapter
import com.example.wavesoffood.databinding.ActivityCartBinding
import com.example.wavesoffood.model.CartItems
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.*

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val foodNames = mutableListOf<String>()
    private val foodPrices = mutableListOf<String>()
    private val foodDescriptions = mutableListOf<String>()
    private val foodImages = mutableListOf<String>()
    private val foodQuantity = mutableListOf<Int>()
    private val foodIngredients = mutableListOf<String>()
    private val hotelNames = mutableListOf<String>()
    private val hotelUserIds = mutableListOf<String>()  // ✅ Added
    private val itemKeys = mutableListOf<String>()
    private val distanceList = mutableListOf<String>()
    private val timeList = mutableListOf<String>()

    private var userLat = 0.0
    private var userLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocationThenLoadCart()

        binding.proceedbuttoncart.setOnClickListener {
            getOrderItemsDetails()
        }
    }

    private fun getCurrentLocationThenLoadCart() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLat = location.latitude
                    userLng = location.longitude
                } else {
                    loadSavedLocation()
                }
                retrieveCartItems()
            }.addOnFailureListener {
                loadSavedLocation()
                retrieveCartItems()
            }
        } else {
            loadSavedLocation()
            retrieveCartItems()
        }
    }

    private fun loadSavedLocation() {
        val prefs = getSharedPreferences("UserLocation", Context.MODE_PRIVATE)
        userLat = prefs.getFloat("lat", 0f).toDouble()
        userLng = prefs.getFloat("lng", 0f).toDouble()
    }

    private fun retrieveCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.reference.child("user").child(userId).child("CartItems")

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodNames.clear()
                foodPrices.clear()
                foodDescriptions.clear()
                foodImages.clear()
                foodQuantity.clear()
                foodIngredients.clear()
                hotelNames.clear()
                hotelUserIds.clear()  // ✅ Clear before reusing
                itemKeys.clear()
                distanceList.clear()
                timeList.clear()

                val hotelSet = mutableSetOf<String>()

                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItems::class.java)
                    item?.let {
                        foodNames.add(it.foodNames ?: "")
                        foodPrices.add(it.foodPrice ?: "")
                        foodDescriptions.add(it.foodDescriptions ?: "")
                        foodImages.add(it.foodImage ?: "")
                        foodQuantity.add(it.foodQuantity ?: 1)
                        foodIngredients.add(it.foodIngredients ?: "")
                        hotelNames.add(it.hotelName ?: "Unknown Hotel")
                        hotelUserIds.add(it.hotelUserId ?: "")  // ✅ Collect hotel user ID
                        itemKeys.add(itemSnapshot.key ?: "")
                        hotelSet.add(it.hotelName ?: "")
                    }
                }

                fetchHotelCoordinatesAndContinue(hotelSet.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartActivity, "Failed to load cart items", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchHotelCoordinatesAndContinue(hotelList: List<String>) {
        val hotelRef = database.reference.child("Hotel Users")

        hotelRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hotelCoordsMap = mutableMapOf<String, Pair<Double, Double>>()

                for (hotelSnap in snapshot.children) {
                    val hotelName = hotelSnap.child("nameOfResturant").getValue(String::class.java)
                    val lat = hotelSnap.child("address/latitude").getValue(Double::class.java)
                    val lng = hotelSnap.child("address/longitude").getValue(Double::class.java)

                    if (!hotelName.isNullOrBlank() && lat != null && lng != null) {
                        hotelCoordsMap[hotelName] = Pair(lat, lng)
                    }
                }

                for (hotelName in hotelNames) {
                    val coords = hotelCoordsMap[hotelName]
                    if (coords != null && userLat != 0.0 && userLng != 0.0) {
                        val distance = calculateDistance(userLat, userLng, coords.first, coords.second)
                        val estimatedTime = estimateDeliveryTime(distance)
                        distanceList.add("%.1f km".format(distance))
                        timeList.add("$estimatedTime min")
                    } else {
                        distanceList.add("N/A")
                        timeList.add("N/A")
                    }
                }

                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartActivity, "Failed to fetch hotel data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c * 1.3
    }

    private fun estimateDeliveryTime(distanceKm: Double): Int {
        val speed = 30.0
        return ceil((distanceKm / speed) * 60).toInt()
    }

    private fun setAdapter() {
        if (foodNames.isNotEmpty()) {
            cartAdapter = CartAdapter(
                context = this,
                foodNames = foodNames,
                foodPrices = foodPrices,
                foodDescriptions = foodDescriptions,
                foodImages = foodImages,
                foodQuantities = foodQuantity,
                foodIngredients = foodIngredients,
                hotelNames = hotelNames,
                hotelUserIds = hotelUserIds,  // ✅ Pass hotel user IDs
                distances = distanceList,
                times = timeList,
                itemKeys = itemKeys
            )
            binding.cartrecyclerview.layoutManager = LinearLayoutManager(this)
            binding.cartrecyclerview.adapter = cartAdapter
            binding.cartrecyclerview.visibility = View.VISIBLE
            binding.emptyCartText.visibility = View.GONE
        } else {
            binding.cartrecyclerview.visibility = View.GONE
            binding.emptyCartText.visibility = View.VISIBLE
        }
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

                val intent = Intent(this@CartActivity, PayQuickActivity::class.java).apply {
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

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartActivity, "Error retrieving order", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
