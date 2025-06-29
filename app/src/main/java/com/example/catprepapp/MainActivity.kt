package com.example.catprepapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavView: BottomNavigationView
    private val lottieAnimations = mutableMapOf<Int, LottieAnimationView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeToTopic()

        viewPager = findViewById(R.id.viewPager)
        bottomNavView = findViewById(R.id.bottomNavView)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Disable swiping

        // --- NEW LOTTIE SETUP LOGIC ---
        setupBottomNavWithLottie()
    }

    private fun setupBottomNavWithLottie() {
        // Define our tabs: Menu ID, Title, Lottie JSON filename
        val navItems = listOf(
            Triple(R.id.navigation_dashboard, "Dashboard", "dashboard.json"),
            Triple(R.id.navigation_inbox, "Inbox", "inbox.json"),
            Triple(R.id.navigation_schedule, "Schedule", "schedule.json"),
            Triple(R.id.navigation_log, "Log", "log.json"),
            Triple(R.id.navigation_catbot, "CatBot", "catbot.json")
        )

        // Manually add items to the BottomNavigationView
        navItems.forEachIndexed { index, item ->
            bottomNavView.menu.add(0, item.first, index, item.second)
            val lottie = LottieAnimationView(this).apply {
                setAnimation(item.third)
                repeatCount = LottieDrawable.INFINITE
            }
            lottieAnimations[item.first] = lottie
            bottomNavView.menu.getItem(index).icon = LottieDrawable().apply {
                addAnimatorUpdateListener {
                    lottie.progress = it.animatedValue as Float
                }
                lottie.playAnimation()
            }
        }

        // Handle tab selection
        // Replace the existing setOnItemSelectedListener block with this one
        bottomNavView.setOnItemSelectedListener { selectedItem ->
            // --- NEW: Correct way to find the index of the selected item ---
            var selectedIndex = 0
            for (i in 0 until bottomNavView.menu.size()) {
                if (bottomNavView.menu.getItem(i) == selectedItem) {
                    selectedIndex = i
                    break
                }
            }
            viewPager.setCurrentItem(selectedIndex, false)
            // --- End of correction ---
        
            // Play animation for selected tab, reset others
            lottieAnimations.forEach { (id, lottieView) ->
                if (id == selectedItem.itemId) {
                    lottieView.playAnimation()
                } else {
                    lottieView.cancelAnimation()
                    lottieView.progress = 0f
                }
            }
            true
        }

        // Select the first item by default
        bottomNavView.selectedItemId = navItems.first().first
    }

    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("daily_summary")
            .addOnCompleteListener { task ->
                var msg = "Subscription successful"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                // Optional: You can remove this Toast in the final version
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }
}
