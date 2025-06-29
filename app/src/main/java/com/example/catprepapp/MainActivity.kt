package com.example.catprepapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeToTopic()

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavView)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.isUserInputEnabled = false 

        // Listener to select the ViewPager page when a tab is clicked
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> viewPager.setCurrentItem(0, false)
                R.id.navigation_inbox -> viewPager.setCurrentItem(1, false)
                R.id.navigation_schedule -> viewPager.setCurrentItem(2, false)
                R.id.navigation_log -> viewPager.setCurrentItem(3, false)
                R.id.navigation_catbot -> viewPager.setCurrentItem(4, false)
            }
            true
        }

        // Listener to highlight the correct tab when swiping (though swiping is disabled)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavView.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("daily_summary")
    }
}
