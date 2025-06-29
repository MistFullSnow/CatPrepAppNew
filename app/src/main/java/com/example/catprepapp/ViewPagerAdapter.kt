package com.example.catprepapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment() // Was ScheduleFragment
            1 -> InboxFragment()
            2 -> ScheduleFragment() // Was LogFragment
            3 -> LogFragment()      // Was DashboardFragment
            4 -> CatBotFragment()   // Stays the same
            else -> DashboardFragment()
        }
    }
}
