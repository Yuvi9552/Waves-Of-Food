package com.example.wavesoffood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentMenuBottomSheetBinding
import com.example.wavesoffood.model.MenuItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.*

class MenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentMenuBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private val menuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)
        retrieveAllMenuItemsFromAllHotelUsers()
        return binding.root
    }

    private fun retrieveAllMenuItemsFromAllHotelUsers() {
        database = FirebaseDatabase.getInstance()
        val hotelUsersRef = database.reference.child("Hotel Users")

        hotelUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()

                for (userSnapshot in snapshot.children) {
                    val hotelUserId = userSnapshot.key ?: continue
                    val hotelName = userSnapshot.child("nameOfResturant").getValue(String::class.java) ?: "Unknown Hotel"

                    val menuRef = userSnapshot.child("menu")
                    for (menuItemSnapshot in menuRef.children) {
                        val menuItem = menuItemSnapshot.getValue(MenuItem::class.java)
                        menuItem?.let {
                            val updatedItem = it.copy(
                                hotelUserId = hotelUserId,
                                hotelName = hotelName // ✅ Add hotel name
                            )
                            menuItems.add(updatedItem)
                        }
                    }
                }

                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

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
