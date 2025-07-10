package com.example.wavesoffood.Fragment

import android.Manifest
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
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.example.wavesoffood.MenuBottomSheet
import com.example.wavesoffood.R
import com.example.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentHomeBinding
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

    private lateinit var database: FirebaseDatabase
    private var adapter: MenuAdapter? = null
    private val originalMenuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        requestUserLocation { lat, lng ->
            userLat = lat
            userLng = lng
            retrieveMenuItems()
        }

        setupSearchView()
    }

    private fun requestUserLocation(onGot: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 2001)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                onGot(loc.latitude, loc.longitude)
            } else {
                Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation { lat, lng ->
                userLat = lat
                userLng = lng
                retrieveMenuItems()
            }
        }
    }

    private fun retrieveMenuItems() {
        database = FirebaseDatabase.getInstance()
        val hotelUsersRef = database.reference.child("Hotel Users")

        hotelUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                originalMenuItems.clear()

                for (userSnapshot in snapshot.children) {
                    val addr = userSnapshot.child("address")
                    val lat = addr.child("latitude").getValue(Double::class.java)
                    val lng = addr.child("longitude").getValue(Double::class.java)
                    val hotelName = userSnapshot.child("nameOfResturant")
                        .getValue(String::class.java) ?: "Unknown Hotel"

                    if (lat != null && lng != null && isWithinRadius(userLat, userLng, lat, lng, 5.0)) {
                        val hotelUserId = userSnapshot.key ?: continue
                        for (itemSnapshot in userSnapshot.child("menu").children) {
                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                            menuItem?.let {
                                originalMenuItems.add(
                                    it.copy(
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

                setAdapter(originalMenuItems)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setAdapter(menuList: List<MenuItem>) {
        if (!isAdded || context == null) return
        adapter = MenuAdapter(menuList, requireContext())
        binding.menurecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.menurecycler.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterMenuItems(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMenuItems(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterMenuItems(query: String) {
        val filteredList = originalMenuItems.filter {
            it.foodName?.contains(query, ignoreCase = true) == true ||
                    it.hotelName?.contains(query, ignoreCase = true) == true
        }
        setAdapter(filteredList)
    }

    /** Haversine formula check */
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
