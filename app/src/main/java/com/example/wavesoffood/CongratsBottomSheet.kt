package com.example.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.wavesoffood.databinding.FragmentCongratsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CongratsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentCongratsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCongratsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.gohomebutton.setOnClickListener {
            val title = "New Order Placed !"
            val message = "Your order has been placed successfully. Thanks for ordering !"
            val timestamp = System.currentTimeMillis().toString()

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val notifyMap = mapOf(
                    "title" to title,
                    "message" to message,
                    "timestamp" to timestamp
                )
                FirebaseDatabase.getInstance().reference
                    .child("Users").child(uid).child("notifications").child(timestamp)
                    .setValue(notifyMap)
            }

            dismiss()
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
