package com.example.wavesoffood.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private val menuItems = mutableListOf<MenuItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.viewmenubutton.setOnClickListener {
            MenuBottomSheet().show(parentFragmentManager, null)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageSlider()
        retrieveAndDisplayPopularItems()
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
                Toast.makeText(requireContext(), "Selected Image $position", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun retrieveAndDisplayPopularItems() {
        database = FirebaseDatabase.getInstance()
        val foodRef = database.reference.child("menu")

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()
                for (foodSnapshot in snapshot.children) {
                    val item = foodSnapshot.getValue(MenuItem::class.java)
                    item?.let { menuItems.add(it) }
                }
                displayRandomPopularItems()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load menu items.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayRandomPopularItems() {
        val randomItems = menuItems.shuffled().take(6)
        if (isAdded && context != null) {
            binding.popularrecyclerview.layoutManager = LinearLayoutManager(requireContext())
            binding.popularrecyclerview.adapter = MenuAdapter(randomItems, requireContext())
        }
    }
}
