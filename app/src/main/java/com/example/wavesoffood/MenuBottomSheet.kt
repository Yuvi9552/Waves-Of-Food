package com.example.wavesoffood

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentMenuBottomSheetBinding
import com.example.wavesoffood.model.MenuItem
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.*
import kotlin.math.*

class MenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentMenuBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private val menuItems = mutableListOf<MenuItem>()

    private var userLat = 0.0
    private var userLng = 0.0
    private var isLocationAvailable = false
    private var isUsingSavedLocation = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestUserLocation()
        } else {
            loadSavedLocationOrFallback()
            retrieveMenuItems()
        }

        return binding.root
    }

    private fun requestUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                if (loc != null) {
                    userLat = loc.latitude
                    userLng = loc.longitude
                    isLocationAvailable = true
                    isUsingSavedLocation = false
                    saveLocationToPrefs(userLat, userLng)
                } else {
                    loadSavedLocationOrFallback()
                }
                retrieveMenuItems()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            loadSavedLocationOrFallback()
            retrieveMenuItems()
        }
    }

    private fun saveLocationToPrefs(lat: Double, lng: Double) {
        val prefs = requireContext().getSharedPreferences("UserLocation", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("lat", lat.toFloat())
            putFloat("lng", lng.toFloat())
            apply()
        }
    }

    private fun loadSavedLocationOrFallback() {
        val prefs = requireContext().getSharedPreferences("UserLocation", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 0f).toDouble()
        val lng = prefs.getFloat("lng", 0f).toDouble()

        if (lat != 0.0 && lng != 0.0) {
            userLat = lat
            userLng = lng
            isLocationAvailable = true
            isUsingSavedLocation = true
        } else {
            isLocationAvailable = false
            isUsingSavedLocation = false
            Toast.makeText(requireContext(), "Showing all hotels (No location available)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 3001 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation()
        } else {
            loadSavedLocationOrFallback()
            retrieveMenuItems()
        }
    }

    private fun retrieveMenuItems() {
        if (!isAdded || _binding == null) return

        database = FirebaseDatabase.getInstance()
        val hotelUsersRef = database.reference.child("Hotel Users")

        hotelUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                menuItems.clear()

                for (hotelSnap in snapshot.children) {
                    val addr = hotelSnap.child("address")
                    val lat = addr.child("latitude").getValue(Double::class.java)
                    val lng = addr.child("longitude").getValue(Double::class.java)
                    val hotelName = hotelSnap.child("nameOfResturant").getValue(String::class.java) ?: "Unknown Hotel"

                    val includeHotel = if (lat != null && lng != null) {
                        if (!isUsingSavedLocation && isLocationAvailable) {
                            isWithinRadius(userLat, userLng, lat, lng, 5.0)
                        } else true
                    } else false

                    if (includeHotel) {
                        val hotelUserId = hotelSnap.key.orEmpty()
                        for (itemSnap in hotelSnap.child("menu").children) {
                            itemSnap.getValue(MenuItem::class.java)?.let { item ->
                                val distance = if (lat != null && lng != null && isLocationAvailable) {
                                    calculateDistance(userLat, userLng, lat, lng) * 1.3 // Apply buffer
                                } else null

                                val estimatedTime = distance?.let { d -> estimateDeliveryTime(d) }

                                menuItems.add(
                                    item.copy(
                                        hotelUserId = hotelUserId,
                                        hotelName = hotelName,
                                        hotelLatitude = lat,
                                        hotelLongitude = lng,
                                        distanceInKm = distance,
                                        estimatedTimeMin = estimatedTime
                                    )
                                )
                            }
                        }
                    }
                }

                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                Toast.makeText(context, "Failed to load menu", Toast.LENGTH_SHORT).show()
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
        return R * c
    }

    private fun estimateDeliveryTime(distanceKm: Double): Int {
        val bikeSpeedKmPerHour = 30.0
        val minutesPerKm = 60.0 / bikeSpeedKmPerHour
        return ceil(distanceKm * minutesPerKm).toInt()
    }




    private fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusKm: Double
    ): Boolean {
        return calculateDistance(lat1, lon1, lat2, lon2) <= radiusKm
    }

    private fun setAdapter() {
        if (!isAdded || _binding == null) return
        binding.menurecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MenuAdapter(menuItems, requireContext(), userLat, userLng)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
