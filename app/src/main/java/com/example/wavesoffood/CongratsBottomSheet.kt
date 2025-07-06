package com.example.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.wavesoffood.databinding.FragmentCongratsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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
            sendInstantNotification()

            dismiss()

            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun sendInstantNotification() {
        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("title", "New Order Placed!")
            putExtra("message", "Your order has been placed successfully. Thanks for ordering!")
        }

        requireContext().sendBroadcast(intent) // ✅ Correct usage
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
