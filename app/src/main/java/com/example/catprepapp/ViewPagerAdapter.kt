package com.example.catprepapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ScheduleFragment()
            1 -> LogFragment()
            2 -> DashboardFragment()
            3 -> CatBotFragment()
            else -> ScheduleFragment()
        }
    }
}
