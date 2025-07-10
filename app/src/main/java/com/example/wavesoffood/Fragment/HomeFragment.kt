package com.example.wavesoffood.Fragment

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.example.wavesoffood.MenuBottomSheet
import com.example.wavesoffood.R
import com.example.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentHomeBinding
import com.example.wavesoffood.model.MenuItem
import com.google.android.gms.location.*
import com.google.firebase.database.*
import kotlin.math.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private val menuItems = mutableListOf<MenuItem>()

    private var userLat = 0.0
    private var userLng = 0.0
    private var isLocationAvailable = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.viewmenubutton.setOnClickListener {
            MenuBottomSheet().show(parentFragmentManager, null)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImageSlider()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestUserLocation { lat, lng ->
                if (!isAdded || _binding == null) return@requestUserLocation
                userLat = lat
                userLng = lng
                isLocationAvailable = true
                retrieveAndDisplayMenuItems()
            }
        } else {
            isLocationAvailable = false
            retrieveAndDisplayMenuItems()
        }
    }

    private fun setupImageSlider() {
        val imageList = listOf(
            SlideModel(R.drawable.banner1, ScaleTypes.FIT),
            SlideModel(R.drawable.banner2, ScaleTypes.FIT),
            SlideModel(R.drawable.banner3, ScaleTypes.FIT)
        )
        binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)
        binding.imageSlider.setItemClickListener(object : ItemClickListener {
            override fun onItemSelected(position: Int) {
                if (!isAdded || _binding == null) return
                Toast.makeText(requireContext(), "Selected Image $position", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun requestUserLocation(onGot: (Double, Double) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                if (loc != null) {
                    onGot(loc.latitude, loc.longitude)
                } else {
                    isLocationAvailable = false
                    retrieveAndDisplayMenuItems()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            isLocationAvailable = false
            Toast.makeText(context, "Location permission error", Toast.LENGTH_SHORT).show()
            retrieveAndDisplayMenuItems()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation { lat, lng ->
                if (!isAdded || _binding == null) return@requestUserLocation
                userLat = lat
                userLng = lng
                isLocationAvailable = true
                retrieveAndDisplayMenuItems()
            }
        } else {
            isLocationAvailable = false
            retrieveAndDisplayMenuItems()
        }
    }

    private fun retrieveAndDisplayMenuItems() {
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

                    val withinRadius = if (isLocationAvailable && lat != null && lng != null) {
                        isWithinRadius(userLat, userLng, lat, lng, 5.0)
                    } else true

                    if (withinRadius) {
                        val menuSnap = hotelSnap.child("menu")
                        for (itemSnap in menuSnap.children) {
                            itemSnap.getValue(MenuItem::class.java)?.let { item ->
                                menuItems.add(
                                    item.copy(
                                        hotelUserId = hotelSnap.key ?: "",
                                        hotelName = hotelName,
                                        hotelLatitude = lat,
                                        hotelLongitude = lng
                                    )
                                )
                            }
                        }
                    }
                }

                if (!isAdded || _binding == null) return

                binding.popularrecyclerview.layoutManager = LinearLayoutManager(requireContext())
                binding.popularrecyclerview.adapter = MenuAdapter(menuItems, requireContext())

                if (!isLocationAvailable) {
                    Toast.makeText(requireContext(), "Showing all Hotels", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                Toast.makeText(context, "Failed to load menu items.", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
