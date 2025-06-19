package com.example.wavesoffood.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.wavesoffood.databinding.FragmentProfileBinding
import com.example.wavesoffood.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setUserData()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction()
                    .replace(com.example.wavesoffood.R.id.fragment_container, HomeFragment())
                    .commit()
            }
        })

        binding.profilesavebuttonid.setOnClickListener {
            val name = binding.profilenameid.text.toString()
            val email = binding.profileemailid.text.toString()
            val address = binding.profileaddressid.text.toString()
            val phone = binding.profilephoneid.text.toString()
            updateUserData(name, email, address, phone)
        }

        return binding.root
    }

    private fun setUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("user").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(UserModel::class.java)?.let {
                    binding.profilenameid.setText(it.name)
                    binding.profileaddressid.setText(it.address)
                    binding.profileemailid.setText(it.email)
                    binding.profilephoneid.setText(it.phone)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserData(name: String, email: String, address: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return
        val userData = mapOf(
            "name" to name,
            "address" to address,
            "email" to email,
            "phone" to phone
        )

        database.getReference("user").child(userId).setValue(userData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Profile Update Failed", Toast.LENGTH_SHORT).show()
            }
    }
}
