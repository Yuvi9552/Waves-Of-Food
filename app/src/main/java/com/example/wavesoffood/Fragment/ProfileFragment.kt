package com.example.wavesoffood.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.wavesoffood.LoginActivity
import com.example.wavesoffood.databinding.FragmentProfileBinding
import com.example.wavesoffood.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        disableEditing()
        setUserData()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.beginTransaction()
                        .replace(
                            com.example.wavesoffood.R.id.fragment_container,
                            HomeFragment()
                        )
                        .commit()
                }
            }
        )

        // Save button
        binding.profilesavebuttonid.setOnClickListener {
            val name = binding.profilenameid.text.toString()
            val email = binding.profileemailid.text.toString()
            val address = binding.profileaddressid.text.toString()
            val phone = binding.profilephoneid.text.toString()
            updateUserData(name, email, address, phone)
        }

        // Logout button
        binding.logoutbutton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Click to Edit button
        binding.clickToEdit.setOnClickListener {
            toggleEditing()
        }

        return binding.root
    }

    private fun disableEditing() {
        binding.profilenameid.isEnabled = false
        binding.profileemailid.isEnabled = false
        binding.profilephoneid.isEnabled = false
        binding.profileaddressid.isEnabled = false
        binding.profilesavebuttonid.isEnabled = false
    }

    private fun toggleEditing() {
        val isEditable = !binding.profilenameid.isEnabled
        binding.profilenameid.isEnabled = isEditable
        binding.profileemailid.isEnabled = isEditable
        binding.profilephoneid.isEnabled = isEditable
        binding.profileaddressid.isEnabled = isEditable
        binding.profilesavebuttonid.isEnabled = isEditable

        if (isEditable) {
            Toast.makeText(requireContext(), "Edit mode enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("user").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(UserModel::class.java)?.let {
                    binding.profilenameid.setText(it.name)
                    binding.profileemailid.setText(it.email)
                    binding.profilephoneid.setText(it.phone)
                }
                val loc = snapshot.child("location").getValue(String::class.java)
                binding.profileaddressid.setText(loc ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserData(name: String, email: String, address: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "location" to address
        )

        database.getReference("user").child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                disableEditing() // 🔒 Disable fields after saving
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Profile Update Failed", Toast.LENGTH_SHORT).show()
            }
    }
}
