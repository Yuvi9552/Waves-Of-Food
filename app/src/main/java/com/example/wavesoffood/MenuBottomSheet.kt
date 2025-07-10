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

    // will hold the user’s position
    private var userLat = 0.0
    private var userLng = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        requestUserLocation { lat, lng ->
            userLat = lat
            userLng = lng
            retrieveNearbyMenuItems()
        }

        return binding.root
    }

    /** Ask permission and fetch last known location */
    private fun requestUserLocation(onGot: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 3001)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) onGot(loc.latitude, loc.longitude)
            else Toast
                .makeText(context, "Could not determine your location", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 3001 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation { lat, lng ->
                userLat = lat; userLng = lng
                retrieveNearbyMenuItems()
            }
        }
    }

    /** Read all hotels, filter by ≤ 5 km, collect their menu items */
    private fun retrieveNearbyMenuItems() {
        database = FirebaseDatabase.getInstance()
        val hotelUsersRef = database.reference.child("Hotel Users")

        hotelUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()
                for (hotelSnap in snapshot.children) {
                    val addr = hotelSnap.child("address")
                    val lat = addr.child("latitude").getValue(Double::class.java)
                    val lng = addr.child("longitude").getValue(Double::class.java)
                    val hotelName = hotelSnap.child("nameOfResturant")
                        .getValue(String::class.java) ?: "Unknown Hotel"

                    if (lat != null && lng != null &&
                        isWithinRadius(userLat, userLng, lat, lng, 5.0)
                    ) {
                        // add all menu items from this hotel
                        val hotelUserId = hotelSnap.key.orEmpty()
                        for (itemSnap in hotelSnap.child("menu").children) {
                            itemSnap.getValue(MenuItem::class.java)?.let { item ->
                                menuItems.add(
                                    item.copy(
                                        hotelUserId   = hotelUserId,
                                        hotelName     = hotelName,
                                        hotelLatitude = lat,
                                        hotelLongitude= lng
                                    )
                                )
                            }
                        }
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load menu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Haversine formula */
    private fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusKm: Double
    ): Boolean {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon/2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c <= radiusKm
    }

    /** Hook up adapter */
    private fun setAdapter() {
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
