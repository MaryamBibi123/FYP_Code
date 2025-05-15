package com.example.signuplogina.com.example.signuplogina

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.signuplogina.MyListingFragment
import com.example.signuplogina.BidsPlacedFragment

class BidsPagerAdapter(fragment: Fragment, private val userId: String? = null) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            BidsPlacedFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
        } else {
            MyListingFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
        }
    }
}
