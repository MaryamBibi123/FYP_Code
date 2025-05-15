package com.example.signuplogina.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.signuplogina.fragments.GeneralPollFragment
import com.example.signuplogina.fragments.TradeBattleFragment



class PollPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GeneralPollFragment()
            1 -> TradeBattleFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }

    }
}
