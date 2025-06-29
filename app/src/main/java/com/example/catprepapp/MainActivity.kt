package com.example.catprepapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        FirebaseMessaging.getInstance().subscribeToTopic("daily_summary")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to daily_summary topic"
                if (!task.isSuccessful) {
                    msg = "Subscription to daily_summary failed"
                }
                // This Toast is just for debugging, you can remove it later
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavView)

        viewPager.adapter = ViewPagerAdapter(this)

        // Disable swiping for now, navigation will be through tabs only
        viewPager.isUserInputEnabled = false

        // Link BottomNavigationView with ViewPager
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> viewPager.setCurrentItem(0, false) // New
                R.id.navigation_inbox -> viewPager.setCurrentItem(1, false) // NEW
                R.id.navigation_schedule -> viewPager.setCurrentItem(2, false) // New
                R.id.navigation_log -> viewPager.setCurrentItem(3, false)      // New
                R.id.navigation_catbot -> viewPager.setCurrentItem(4, false)   // Stays the same
            }
            true
        }

        // Link ViewPager with BottomNavigationView for highlighting the correct tab
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavView.menu.getItem(position).isChecked = true
            }
        })
    }
}
