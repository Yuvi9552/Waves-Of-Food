package com.example.wavesoffood

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestUserLocation { lat, lng ->
                if (!isAdded || _binding == null) return@requestUserLocation
                userLat = lat
                userLng = lng
                isLocationAvailable = true
                retrieveMenuItems()
            }
        } else {
            retrieveMenuItems() // fallback if no location access
        }

        return binding.root
    }

    private fun requestUserLocation(onGot: (Double, Double) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                loc?.let {
                    onGot(it.latitude, it.longitude)
                } ?: run {
                    retrieveMenuItems()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            retrieveMenuItems()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 3001 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation { lat, lng ->
                if (!isAdded || _binding == null) return@requestUserLocation
                userLat = lat
                userLng = lng
                isLocationAvailable = true
                retrieveMenuItems()
            }
        } else {
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
                    val hotelName = hotelSnap.child("nameOfResturant")
                        .getValue(String::class.java) ?: "Unknown Hotel"

                    val includeHotel = if (isLocationAvailable && lat != null && lng != null) {
                        isWithinRadius(userLat, userLng, lat, lng, 5.0)
                    } else true

                    if (includeHotel) {
                        val hotelUserId = hotelSnap.key.orEmpty()
                        for (itemSnap in hotelSnap.child("menu").children) {
                            itemSnap.getValue(MenuItem::class.java)?.let { item ->
                                menuItems.add(
                                    item.copy(
                                        hotelUserId = hotelUserId,
                                        hotelName = hotelName,
                                        hotelLatitude = lat,
                                        hotelLongitude = lng
                                    )
                                )
                            }
                        }
                    }
                }

                setAdapter()

                if (!isLocationAvailable) {
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                Toast.makeText(context, "Failed to load menu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusKm: Double
    ): Boolean {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c <= radiusKm
    }

    private fun setAdapter() {
        if (!isAdded || _binding == null) return
        binding.menurecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MenuAdapter(menuItems, requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
