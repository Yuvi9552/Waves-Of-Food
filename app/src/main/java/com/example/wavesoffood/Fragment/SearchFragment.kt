package com.example.wavesoffood.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentSearchBinding
import com.example.wavesoffood.model.MenuItem
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var database: FirebaseDatabase
    private var adapter: MenuAdapter? = null
    private val originalMenuItems = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isAdded) return
                parentFragmentManager.beginTransaction()
                    .replace(com.example.wavesoffood.R.id.fragment_container, HomeFragment())
                    .addToBackStack(null)
                    .commit()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        retrieveMenuItems()
        setUpSearchView()
        return binding.root
    }

    private fun retrieveMenuItems() {
        database = FirebaseDatabase.getInstance()
        val menuRef = database.reference.child("menu")

        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                originalMenuItems.clear()
                for (itemSnapshot in snapshot.children) {
                    itemSnapshot.getValue(MenuItem::class.java)?.let {
                        originalMenuItems.add(it)
                    }
                }

                setAdapter(originalMenuItems)
            }

            override fun onCancelled(error: DatabaseError) {
                // Optional: log or handle error
            }
        })
    }

    private fun setAdapter(menuList: List<MenuItem>) {
        if (!isAdded || context == null) return
        adapter = MenuAdapter(menuList, requireContext())
        binding.menurecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.menurecycler.adapter = adapter
    }

    private fun setUpSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterMenuItems(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterMenuItems(it) }
                return true
            }
        })
    }

    private fun filterMenuItems(query: String) {
        if (!isAdded) return

        val filteredList = originalMenuItems.filter {
            it.foodName?.contains(query, ignoreCase = true) == true
        }

        setAdapter(filteredList)
    }
}
