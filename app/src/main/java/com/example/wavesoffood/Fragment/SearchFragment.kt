package com.example.wavesoffood.Fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.R
import com.example.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentSearchBinding
import com.example.wavesoffood.model.MenuItem
import com.google.android.gms.location.*
import com.google.firebase.database.*
import kotlin.math.*

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat = 0.0
    private var userLng = 0.0
    private var isLocationAvailable = false
    private var isUsingSavedLocation = false

    private lateinit var database: FirebaseDatabase
    private var adapter: MenuAdapter? = null
    private val originalMenuItems = mutableListOf<MenuItem>()

    private var currentSearchQuery = ""
    private var selectedCategory = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                }
            }
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        setupSearchView()
        setupChipGroup()
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
            if (isAdded) {
                Toast.makeText(requireContext(), "Showing all Hotels (No location)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun retrieveMenuItems() {
        database = FirebaseDatabase.getInstance()
        val hotelUsersRef = database.reference.child("Hotel Users")

        hotelUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                originalMenuItems.clear()

                for (userSnapshot in snapshot.children) {
                    val addr = userSnapshot.child("address")
                    val lat = addr.child("latitude").getValue(Double::class.java)
                    val lng = addr.child("longitude").getValue(Double::class.java)
                    val hotelName = userSnapshot.child("nameOfResturant").getValue(String::class.java) ?: "Unknown Hotel"

                    val shouldInclude = if (lat != null && lng != null) {
                        if (isLocationAvailable && !isUsingSavedLocation) {
                            isWithinRadius(userLat, userLng, lat, lng, 5.0)
                        } else true
                    } else false

                    if (shouldInclude && lat != null && lng != null) {
                        val hotelUserId = userSnapshot.key ?: continue
                        for (itemSnapshot in userSnapshot.child("menu").children) {
                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                            menuItem?.let {
                                val distance = calculateDistance(userLat, userLng, lat, lng) * 1.3
                                val timeEst = estimateDeliveryTime(distance)

                                originalMenuItems.add(
                                    it.copy(
                                        hotelUserId = hotelUserId,
                                        hotelName = hotelName,
                                        hotelLatitude = lat,
                                        hotelLongitude = lng,
                                        distanceInKm = distance,
                                        estimatedTimeMin = timeEst
                                    )
                                )
                            }
                        }
                    }
                }

                if (!isAdded || _binding == null) return
                filterMenuItems()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
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

    private fun isWithinRadius(lat1: Double, lon1: Double, lat2: Double, lon2: Double, radiusKm: Double): Boolean {
        return calculateDistance(lat1, lon1, lat2, lon2) <= radiusKm
    }

    private fun setAdapter(menuList: List<MenuItem>) {
        if (!isAdded || context == null || _binding == null) return
        adapter = MenuAdapter(menuList, requireContext(), userLat, userLng)
        binding.menurecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.menurecycler.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty()
                filterMenuItems()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                filterMenuItems()
                return true
            }
        })
    }

    private fun setupChipGroup() {
        // "All" chip outside scroll
        binding.chipAll.setOnClickListener {
            binding.chipGroup.clearCheck()
            selectedCategory = "All"
            binding.chipAll.isChecked = true
            filterMenuItems()
        }

        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedId)
                selectedCategory = chip?.text?.toString() ?: "All"
                binding.chipAll.isChecked = false
            } else {
                selectedCategory = "All"
                binding.chipAll.isChecked = true
            }
            filterMenuItems()
        }
    }

    private fun filterMenuItems() {
        val filtered = originalMenuItems.filter {
            val matchQuery = it.foodName?.contains(currentSearchQuery, ignoreCase = true) == true ||
                    it.hotelName?.contains(currentSearchQuery, ignoreCase = true) == true
            val matchCategory = selectedCategory == "All" ||
                    it.category?.equals(selectedCategory, ignoreCase = true) == true
            matchQuery && matchCategory
        }
        setAdapter(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
