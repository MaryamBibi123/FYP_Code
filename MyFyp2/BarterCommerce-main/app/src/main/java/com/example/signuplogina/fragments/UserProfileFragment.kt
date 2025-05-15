package com.example.signuplogina.fragments

// UserProfileFragment.kt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.signuplogina.databinding.FragmentUserProfileBinding
import com.example.signuplogina.modal.Feedback
import com.example.signuplogina.modal.FeedbackTags
import com.example.signuplogina.User
import com.example.signuplogina.com.example.signuplogina.BidsPagerAdapter
import com.example.signuplogina.modal.UserRatingStats
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.*

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var bidsPagerAdapter: BidsPagerAdapter


    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = arguments?.getParcelable<User>("user")
        user?.let { loadUserProfile(it) }
        bidsPagerAdapter = BidsPagerAdapter(this,user?.userid)
        binding.viewPager.adapter = bidsPagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "WishList"
                1 -> "Listings"
                else -> ""
            }
        }.attach()
    }

    private fun loadUserProfile(user: User) {
        binding.userName.text = user.fullName
        Glide.with(this)
            .load(user.imageUrl)
            .placeholder(com.example.signuplogina.R.drawable.ic_profile)
            .into(binding.userImage)

        val stats = user.ratings
        if (stats != null) {
            binding.ratingBar.rating = stats.averageRating
            binding.ratingValue.text = "${stats.averageRating} / 5"
            binding.reviewCountText.text = "Reviews: ${stats.feedbackList.size}"
            binding.exchangeText.text = "Exchanges: ${stats.totalExchanges}"
            binding.successRateText.text = "Success Rate: ${stats.successRate.toInt()}%"

//            // Show top tag if available
//            val topTag = stats.feedbackList.values.firstOrNull()?.tags?.firstOrNull()
//            binding.tagTextView.text = topTag?.label ?: "No Tag"

            val allTags = stats.feedbackList.values.flatMap { it.tags }
            val mostFrequentTag = allTags
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            binding.tagTextView.text = mostFrequentTag?.label ?: "No Tag"

        } else {
            Toast.makeText(requireContext(), "No ratings found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
