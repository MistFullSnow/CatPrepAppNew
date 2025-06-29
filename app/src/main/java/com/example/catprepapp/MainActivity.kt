package com.example.catprepapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeToTopic()

        viewPager = findViewById(R.id.viewPager)
        bottomNavView = findViewById(R.id.bottomNavView)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        // --- SIMPLIFIED LOTTIE SETUP ---
        bottomNavView.setOnItemSelectedListener { item ->
            handleNavigation(item)
            true
        }
        
        // Set the initial selected item and animation
        bottomNavView.selectedItemId = R.id.navigation_dashboard
    }

    private fun handleNavigation(selectedItem: MenuItem) {
        val navItems = mapOf(
            R.id.navigation_dashboard to 0,
            R.id.navigation_inbox to 1,
            R.id.navigation_schedule to 2,
            R.id.navigation_log to 3,
            R.id.navigation_catbot to 4
        )
        
        // Set the correct page in the ViewPager
        val position = navItems[selectedItem.itemId] ?: 0
        viewPager.setCurrentItem(position, false)

        // --- SIMPLIFIED ANIMATION HANDLING ---
        // We animate the icon of the selected item and reset the others
        for (i in 0 until bottomNavView.menu.size()) {
            val menuItem = bottomNavView.menu.getItem(i)
            val lottieView = menuItem.actionView as? LottieAnimationView
            
            if (menuItem.itemId == selectedItem.itemId) {
                lottieView?.playAnimation()
            } else {
                lottieView?.cancelAnimation()
                lottieView?.progress = 0f
            }
        }
    }
    
    // This function is for subscribing to notifications
    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("daily_summary")
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) "Subscription successful" else "Subscription failed"
                // You can remove this toast in the final app
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }
}
