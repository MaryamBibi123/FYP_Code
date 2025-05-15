package com.example.signuplogina

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.signuplogina.com.example.signuplogina.BidsPagerAdapter
import com.example.signuplogina.databinding.FragmentBidsBinding
import com.google.android.material.tabs.TabLayoutMediator

class BidsFragment : Fragment() {

    private lateinit var binding: FragmentBidsBinding
    private lateinit var bidsPagerAdapter: BidsPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentBidsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up ViewPager2 with the adapter
        bidsPagerAdapter = BidsPagerAdapter(this)
        binding.viewPager.adapter = bidsPagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Placed Bids"
                1 -> "My Listings"
                else -> ""
            }
        }.attach()
    }
}
