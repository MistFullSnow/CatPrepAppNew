package com.example.catprepapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This links to the activity_main.xml file we updated
        setContentView(R.layout.activity_main)

        // Find the views from our XML layout
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        // Create and set the adapter
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Define the titles for our tabs
        val tabTitles = arrayOf("Schedule", "Log", "Dashboard", "CatBot")

        // Connect the TabLayout with the ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}
