package com.example.signuplogina.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity // For setting up toolbar as action bar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.signuplogina.R
import com.example.signuplogina.adapter.UserDetailsPagerAdapter // For status constants
import com.example.signuplogina.databinding.FragmentAdminItemHostBinding // Your binding class
import com.google.android.material.tabs.TabLayoutMediator

class AdminItemHostFragment : Fragment() {

    private var _binding: FragmentAdminItemHostBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "AdminItemHostFrag"
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentAdminItemHostBinding.inflate(inflater, container, false)
            return binding.root
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupToolbar()
        setupViewPagerAndTabs()
    }

    private fun setupToolbar() {
        // If you want to use the Toolbar as the ActionBar for menu items, etc.
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarItems)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowHomeEnabled(true)
        // Handle navigation icon click
        binding.toolbarItems.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        // Title is set in XML: app:title="@string/admin_items_title"
        // Or you can set it dynamically:
        // binding.toolbarItems.title = getString(R.string.admin_items_title)
    }

    private fun setupViewPagerAndTabs() {
        Log.d(TAG, "Setting up ViewPager and Tabs for Admin Host")
        val viewPagerAdapter = AdminItemsViewPagerAdapter(this) // Use the inner adapter
        binding.viewPagerItems.adapter = viewPagerAdapter

        // Attach TabLayoutMediator
        TabLayoutMediator(binding.tabLayoutItems, binding.viewPagerItems) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_all_pending)   // "Pending"
                1 -> getString(R.string.tab_all_approved)  // "Approved"
                2 -> getString(R.string.tab_all_rejected)  // "Rejected"
                else -> null
            }
        }.attach()
        Log.d(TAG, "Admin Host ViewPager and Tabs setup complete")
    }

    // --- ViewPager Adapter for Admin Global Item Lists ---
    private inner class AdminItemsViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3 // Three tabs: Pending, Approved, Rejected

        override fun createFragment(position: Int): Fragment {
            Log.d(TAG, "Creating AdminItemListFragment for global view, position $position")
            // Instantiate AdminItemListFragment, passing the correct status filter
            // For these global admin views, userId in AdminItemListFragment's newInstance will be "" or null
            // And isAdminView will be true.
            return when (position) {
                0 -> AdminItemListFragment.newInstance(itemStatusFilter = UserDetailsPagerAdapter.STATUS_PENDING)
                1 -> AdminItemListFragment.newInstance(itemStatusFilter = UserDetailsPagerAdapter.STATUS_APPROVED)
                2 -> AdminItemListFragment.newInstance(itemStatusFilter = UserDetailsPagerAdapter.STATUS_REJECTED)
                else -> throw IllegalStateException("Invalid position for AdminItemsViewPagerAdapter: $position")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        // It's good practice to null out the ViewPager's adapter to help with memory management,
        // though ViewPager2 handles lifecycle somewhat better than the old ViewPager.
        binding.viewPagerItems.adapter = null
        _binding = null
    }
}



//package com.example.signuplogina.fragments
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import androidx.viewpager2.adapter.FragmentStateAdapter
//import com.example.signuplogina.R
//import com.example.signuplogina.databinding.FragmentAdminItemHostBinding
//import com.google.android.material.tabs.TabLayoutMediator
//
//class AdminItemHostFragment : Fragment() {
//
//    private var _binding: FragmentAdminItemHostBinding? = null
//    private val binding get() = _binding!!
//
//    companion object {
//        private const val TAG = "AdminItemsHostFrag"
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentAdminItemHostBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "onViewCreated")
//
//        // Setup Toolbar (optional, if you want a specific title here)
//        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.admin_items_title) // Add string resource
//
//        setupViewPagerAndTabs()
//    }
//
//    private fun setupViewPagerAndTabs() {
//        Log.d(TAG, "Setting up ViewPager and Tabs")
//        val viewPagerAdapter = ViewPagerAdapter(this)
//        binding.viewPagerItems.adapter = viewPagerAdapter // Use ID from new layout
//
//        // Attach TabLayoutMediator
//        TabLayoutMediator(binding.tabLayoutItems, binding.viewPagerItems) { tab, position -> // Use IDs from new layout
//            tab.text = when (position) {
//                0 -> getString(R.string.tab_all_approved) // Use specific strings
//                1 -> getString(R.string.tab_all_rejected) // Use specific strings
//                else -> null
//            }
//        }.attach()
//        Log.d(TAG, "ViewPager and Tabs setup complete")
//    }
//
//    // --- ViewPager Adapter ---
//    private inner class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
//        override fun getItemCount(): Int = 2 // Still two tabs
//
//        override fun createFragment(position: Int): Fragment {
//            Log.d(TAG, "Creating AdminItemListFragment for position $position")
//            // Instantiate the NEW AdminItemListFragment
//            return when (position) {
//                0 -> AdminItemListFragment.newInstance(showRejected = false) // All Approved
//                1 -> AdminItemListFragment.newInstance(showRejected = true)  // All Rejected
//                else -> throw IllegalStateException("Invalid position: $position")
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        Log.d(TAG, "onDestroyView")
//        _binding = null
//    }
//}