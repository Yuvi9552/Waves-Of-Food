package com.example.wavesoffood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.Fragment.HomeFragment
import com.example.wavesoffood.adapter.NotificationAdapter
import com.example.wavesoffood.databinding.FragmentNotificationBottomBinding
import com.example.wavesoffood.model.AppNotification
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Notification_Bottom_Fragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNotificationBottomBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<AppNotification>()
    private val keyMap = mutableMapOf<String, String>() // timestamp -> FirebaseKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismiss()
                    loadHomeFragment()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBottomBinding.inflate(inflater, container, false)
        fetchNotifications()
        return binding.root
    }

    private fun fetchNotifications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("notifications")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                keyMap.clear()

                for (snap in snapshot.children) {
                    val item = snap.getValue(AppNotification::class.java)
                    val key = snap.key ?: continue
                    val timestamp = item?.timestamp
                    if (!timestamp.isNullOrEmpty()) {
                        notificationList.add(item)
                        keyMap[timestamp] = key
                    }
                }

                notificationList.sortByDescending { it.timestamp?.toLongOrNull() ?: 0L }

                if (!::notificationAdapter.isInitialized) {
                    notificationAdapter = NotificationAdapter(notificationList)
                    binding.notificationrecylerview.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = notificationAdapter
                        setupSwipeToDelete(this)
                    }
                } else {
                    notificationAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removedItem = notificationAdapter.deleteItem(position)
                val timestamp = removedItem?.timestamp ?: return
                val firebaseKey = keyMap[timestamp] ?: return

                FirebaseDatabase.getInstance().getReference("Users")
                    .child(uid).child("notifications").child(firebaseKey)
                    .removeValue()
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete notification", Toast.LENGTH_SHORT).show()
                    }
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadHomeFragment() {
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
