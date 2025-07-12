package com.example.wavesoffood.Fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.PayQuickActivity
import com.example.wavesoffood.adapter.CartAdapter
import com.example.wavesoffood.databinding.FragmentCartBinding
import com.example.wavesoffood.model.CartItems
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.*

class CartFragment : Fragment() {

    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var userLat = 0.0
    private var userLng = 0.0

    private val foodNames = mutableListOf<String>()
    private val foodPrices = mutableListOf<String>()
    private val foodDescriptions = mutableListOf<String>()
    private val foodImages = mutableListOf<String>()
    private val foodQuantity = mutableListOf<Int>()
    private val foodIngredients = mutableListOf<String>()
    private val hotelNames = mutableListOf<String>()
    private val distances = mutableListOf<String>()
    private val times = mutableListOf<String>()
    private val itemKeys = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        requestUserLocation()

        binding.proceedbuttoncart.setOnClickListener {
            getOrderItemsDetails()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.beginTransaction()
                        .replace(com.example.wavesoffood.R.id.fragment_container, HomeFragment())
                        .commit()
                }
            })

        return binding.root
    }

    private fun requestUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            loadSavedLocation()
            retrieveCartItems()
            return
        }

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
    }

    private fun loadSavedLocation() {
        val prefs = requireContext().getSharedPreferences("UserLocation", Context.MODE_PRIVATE)
        userLat = prefs.getFloat("lat", 0f).toDouble()
        userLng = prefs.getFloat("lng", 0f).toDouble()
    }

    private fun retrieveCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.reference.child("user").child(userId).child("CartItems")

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || context == null) return

                foodNames.clear()
                foodPrices.clear()
                foodDescriptions.clear()
                foodImages.clear()
                foodQuantity.clear()
                foodIngredients.clear()
                hotelNames.clear()
                distances.clear()
                times.clear()
                itemKeys.clear()

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
                        val hotel = it.hotelName ?: "Unknown Hotel"
                        hotelNames.add(hotel)
                        itemKeys.add(itemSnapshot.key ?: "")
                        hotelSet.add(hotel)
                    }
                }

                fetchHotelCoordinatesAndContinue(hotelSet.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
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
                        distances.add("%.1f km".format(distance))
                        times.add("$estimatedTime min")
                    } else {
                        distances.add("N/A")
                        times.add("N/A")
                    }
                }

                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
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
        return R * c * 1.3 // buffer 30% more
    }

    private fun estimateDeliveryTime(distanceKm: Double): Int {
        val speed = 30.0
        return ceil((distanceKm / speed) * 60).toInt()
    }

    private fun setAdapter() {
        if (!isAdded || context == null) return

        if (foodNames.isNotEmpty()) {
            cartAdapter = CartAdapter(
                requireContext(),
                foodNames,
                foodPrices,
                foodDescriptions,
                foodImages,
                foodQuantity,
                foodIngredients,
                hotelNames,
                distances,
                times,
                itemKeys
            )
            binding.cartrecyclerview.layoutManager = LinearLayoutManager(requireContext())
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

                orderNow(
                    foodName,
                    foodPrice,
                    foodImage,
                    foodDescriptions,
                    foodIngredients,
                    foodQuantities
                )
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
