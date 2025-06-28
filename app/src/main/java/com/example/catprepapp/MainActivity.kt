package com.example.catprepapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavView)

        viewPager.adapter = ViewPagerAdapter(this)

        // Disable swiping for now, navigation will be through tabs only
        viewPager.isUserInputEnabled = false

        // Link BottomNavigationView with ViewPager
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_schedule -> viewPager.setCurrentItem(0, false)
                R.id.navigation_log -> viewPager.setCurrentItem(1, false)
                R.id.navigation_dashboard -> viewPager.setCurrentItem(2, false)
                R.id.navigation_catbot -> viewPager.setCurrentItem(3, false)
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
