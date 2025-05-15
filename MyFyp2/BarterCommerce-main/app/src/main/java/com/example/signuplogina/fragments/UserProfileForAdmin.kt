package com.example.signuplogina.fragments // Or your admin fragments package

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels // If using fragment-ktx viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.User
import com.example.signuplogina.adapter.UserDetailsPagerAdapter
import com.example.signuplogina.databinding.FragmentUserProfileForAdminBinding
import com.example.signuplogina.mvvm.AdminViewModel // Assuming this ViewModel fetches user details
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.*

class UserProfileForAdmin : Fragment() {

    private var _binding: FragmentUserProfileForAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var userItemsPagerAdapter: UserDetailsPagerAdapter
    private val args: UserProfileForAdminArgs by navArgs() // If you use Safe Args
    private val viewModel: AdminViewModel by viewModels() // Use your AdminViewModel

    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileForAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user from arguments passed via Safe Args (or direct arguments bundle)
        val userFromArgs = args.user // Assuming 'user' is the arg name in your nav graph
        currentUserId = userFromArgs?.userid

        if (currentUserId.isNullOrBlank()) {
            Toast.makeText(context, "User ID not found.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        // Fetch the latest user details, including block status from UserRatingStats
        viewModel.fetchUserDetails(currentUserId!!)
        setupObservers() // Observe the fetched user details

        // Setup PagerAdapter - this can be done once userid is confirmed
        userItemsPagerAdapter = UserDetailsPagerAdapter(this, currentUserId!!)
        binding.viewPager.adapter = userItemsPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Pending"
                1 -> "Approved"
                2 -> "Rejected"
                else -> ""
            }
        }.attach()
    }

    private fun setupObservers() {
        viewModel.selectedUserDetails.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                loadUserProfileDisplay(user)
            } else {
                // Handle case where user details might fail to load after initial arg passing
                Toast.makeText(context, "Failed to load complete user details.", Toast.LENGTH_SHORT).show()
                // binding.userName.text = args.user.fullName ?: "User (Details Error)" // Display name from args as fallback
                // binding.tvUserBlockStatus.text = "Status: Error loading details"
            }
        }
        // No need to observe userActionOutcome if not performing actions here
    }

    private fun loadUserProfileDisplay(user: User) {
        binding.userName.text = user.fullName
        Glide.with(this)
            .load(user.imageUrl)
            .placeholder(R.drawable.ic_profile) // Make sure this drawable exists
            .error(R.drawable.default_profile_image) // And this one
            .into(binding.userImage)

        val stats = user.ratings
        if (stats != null) {
            binding.ratingBar.rating = stats.averageRating
            binding.ratingValue.text = String.format(Locale.US, "%.1f / 5", stats.averageRating)
            binding.reviewCountText.text = "Reviews: ${stats.feedbackList.size}"
            binding.exchangeText.text = "Exchanges: ${stats.totalExchanges}"
            binding.successRateText.text = "Success Rate: ${stats.successRate.toInt()}%"

            // Most frequent tag (from your original code)
            val allTags = stats.feedbackList.values.flatMap { it.tags }
            val mostFrequentTag = allTags.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            binding.tagTextView.text = mostFrequentTag?.label ?: "No Popular Tag"


            // Display Block Status
            when {
                stats.isPermanentlyBlocked -> {
                    binding.tvUserBlockStatus.text = "Status: PERMANENTLY BLOCKED\nReason: ${stats.blockReason ?: "N/A"}"
                    binding.tvUserBlockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red)) // Define red_error
                }
                stats.isTemporarilyBlocked -> {
                    val expiryDate = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                        .format(Date(stats.blockExpiryTimestamp))
                    binding.tvUserBlockStatus.text = "Status: TEMP. BLOCKED until $expiryDate\nReason: ${stats.blockReason ?: "N/A"}"
                    binding.tvUserBlockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_orange)) // Define orange_warning
                }
                else -> {
                    binding.tvUserBlockStatus.text = "Status: Active"
                    binding.tvUserBlockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green)) // Define green_success
                }
            }
            binding.tvUserBlockStatus.visibility = View.VISIBLE

            // Setup "View User Reports" button
            val reportCount = stats.reportCount
            binding.btnViewUserReports.text = "View User Reports ($reportCount)"
            binding.btnViewUserReports.visibility = View.VISIBLE
            binding.btnViewUserReports.setOnClickListener {
                user.userid?.let { onViewUserReportsClicked(it) }
            }

        } else {
            // Handle case where user.ratings is null
            binding.ratingBar.rating = 0f
            binding.ratingValue.text = "0 / 5"
            binding.reviewCountText.text = "Reviews: 0"
            binding.exchangeText.text = "Exchanges: 0"
            binding.successRateText.text = "Success Rate: 0%"
            binding.tagTextView.text = "No Rating Data"
            binding.tvUserBlockStatus.text = "Status: Active (No Rating Data)"
            binding.tvUserBlockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
            binding.tvUserBlockStatus.visibility = View.VISIBLE

            binding.btnViewUserReports.text = "View User Reports (0)" // Still allow navigation
            binding.btnViewUserReports.visibility = View.VISIBLE
            binding.btnViewUserReports.setOnClickListener {
                user.userid?.let { onViewUserReportsClicked(it) }
            }
        }
    }

    private fun onViewUserReportsClicked(userIdForProfile: String) {
        // Navigate to AdminPendingReportsFragment, passing userId to filter reports for this user
        // Pass null for statusFilter to indicate showing ALL reports for this user
        val action = UserProfileForAdminDirections
            .actionUserProfileForAdminToAdminPendingReportsFragment(userIdForProfile)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.adapter = null // Good practice
        _binding = null
    }
}










//package com.example.signuplogina.fragments
//
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.navigation.fragment.findNavController
//import com.bumptech.glide.Glide
//import com.example.signuplogina.R
//import com.example.signuplogina.User
//import com.example.signuplogina.adapter.UserDetailsPagerAdapter
//import com.example.signuplogina.databinding.FragmentUserProfileForAdminBinding
//import com.google.android.material.tabs.TabLayoutMediator
//
//class UserProfileForAdmin : Fragment() {
//
//    private var _binding: FragmentUserProfileForAdminBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var userItemsPagerAdapter: UserDetailsPagerAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentUserProfileForAdminBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
////
//
//
//    // In UserProfileForAdmin.kt
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val user = arguments?.getParcelable<User>("user")
//        user?.let { loadUserProfile(it) }
//
//        // Pass the specific user's ID to the PagerAdapter
//        userItemsPagerAdapter = UserDetailsPagerAdapter(this, user?.userid ?: "") // Handle potential null userid
//        binding.viewPager.adapter = userItemsPagerAdapter
//
//        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
//            tab.text = when (position) {
//                0 -> "Pending"    // **** NEW TAB ****
//                1 -> "Approved"
//                2 -> "Rejected"
//                else -> ""
//            }
//        }.attach()
//    }
//
//    // Inside UserProfileForAdminFragment.kt, when "View User's Reports" button is clicked:
//    private fun onViewUserReportsClicked(userIdForProfile: String) {
//        val action = UserProfileForAdminDirections
//            .actionUserProfileForAdminToAdminPendingReportsFragment(userIdForProfile) // Define this nav action
//        findNavController().navigate(action)
//    }
//
//    private fun loadUserProfile(user: User) {
//        binding.userName.text = user.fullName
//        Glide.with(this)
//            .load(user.imageUrl)
//            .placeholder(R.drawable.ic_profile)
//            .into(binding.userImage)
//
//        val stats = user.ratings
//        if (stats != null) {
//            binding.ratingBar.rating = stats.averageRating
//            binding.ratingValue.text = "${stats.averageRating} / 5"
//            binding.reviewCountText.text = "Reviews: ${stats.feedbackList.size}"
//            binding.exchangeText.text = "Exchanges: ${stats.totalExchanges}"
//            binding.successRateText.text = "Success Rate: ${stats.successRate.toInt()}%"
//
//            val allTags = stats.feedbackList.values.flatMap { it.tags }
//            val mostFrequentTag = allTags.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
//            binding.tagTextView.text = mostFrequentTag?.label ?: "No Tag"
//        } else {
//            Toast.makeText(requireContext(), "No ratings found", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
