package com.example.wavesoffood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.NotificationAdapter
import com.example.wavesoffood.databinding.FragmentNotificationBottomBinding
import com.example.wavesoffood.Fragment.HomeFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class Notification_Bottom_Fragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNotificationBottomBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle system back button press
        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismiss() // Close bottom sheet
                    loadHomeFragment() // Load HomeFragment manually
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBottomBinding.inflate(inflater, container, false)

        val notifications = listOf(
            "Your Order Has Been Canceled Successfully",
            "Order Has Been Taken By The Driver",
            "Congrats Your Order Placed"
        )

        val notificationImages = listOf(
            R.drawable.sademoji,
            R.drawable.car,
            R.drawable.group_805
        )

        binding.notificationrecylerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = NotificationAdapter(
                ArrayList(notifications),
                ArrayList(notificationImages)
            )
        }

        return binding.root
    }

    private fun loadHomeFragment() {
        // Replace fragment_container with your actual layout ID that holds fragments
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
