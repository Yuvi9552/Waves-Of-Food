package com.example.wavesoffood

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.wavesoffood.Fragment.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var notificationBell: ImageView
    private lateinit var locationIcon: ImageView // 👈 Added location icon reference
    private lateinit var notificationRef: DatabaseReference
    private lateinit var prefs: SharedPreferences

    private var isNotificationListenerRegistered = false
    private val PREFS_NAME = "notification_prefs"
    private val SHOWN_KEYS = "shown_keys"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        requestNotificationPermission()
        requestExactAlarmPermission()

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        notificationBell = findViewById(R.id.notificationbell)
        locationIcon = findViewById(R.id.locationicon) // 👈 Initialize location icon

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val fragment = when (menuItem.itemId) {
                R.id.homeFragment -> HomeFragment()
                R.id.searchFragment -> SearchFragment()
                R.id.cartFragment -> CartFragment()
                R.id.historyFragment -> HistoryFragment()
                R.id.profileFragment -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }

        val openCart = intent.getBooleanExtra("openCart", false)
        bottomNavigationView.selectedItemId =
            if (openCart) R.id.cartFragment else R.id.homeFragment

        notificationBell.setOnClickListener {
            loadFragment(Notification_Bottom_Fragment())
        }

        // 👇 Handle location icon click
        locationIcon.setOnClickListener {
            val intent = Intent(this@MainActivity, ChooseLocation::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)

        }

        if (!isNotificationListenerRegistered) {
            listenForOrderNotifications()
            isNotificationListenerRegistered = true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun listenForOrderNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        notificationRef = FirebaseDatabase.getInstance().reference
            .child("Users").child(userId).child("notifications")

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return

                val shownSet = prefs.getStringSet(SHOWN_KEYS, emptySet()) ?: emptySet()
                if (shownSet.contains(key)) return

                val title = snapshot.child("title").getValue(String::class.java) ?: "Order Update"
                val message = snapshot.child("message").getValue(String::class.java) ?: return

                val notifId = key.hashCode()
                showLocalNotification(title, message, notifId)

                val updatedSet = shownSet.toMutableSet()
                updatedSet.add(key)
                prefs.edit().putStringSet(SHOWN_KEYS, updatedSet).apply()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        notificationRef.addChildEventListener(childEventListener)
    }

    private fun showLocalNotification(title: String, message: String, notificationId: Int) {
        val channelId = "user_order_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about order status updates"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
