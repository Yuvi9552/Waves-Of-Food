package com.example.wavesoffood

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.wavesoffood.Fragment.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var notificationBell: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        notificationBell = findViewById(R.id.notificationbell)

        loadFragment(HomeFragment())

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

        notificationBell.setOnClickListener {
            loadFragment(Notification_Bottom_Fragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
