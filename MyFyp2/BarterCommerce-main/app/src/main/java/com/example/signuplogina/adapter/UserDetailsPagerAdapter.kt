package com.example.signuplogina.adapter // Or your correct package

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.signuplogina.fragments.ItemListFragment

class UserDetailsPagerAdapter(
    fragment: Fragment,
    private val userId: String // userId of the profile being viewed by admin
) : FragmentStateAdapter(fragment) {

    companion object {
        // Keep these constants for clarity
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
    }

    override fun getItemCount(): Int = 3 // **** CHANGED ****

    override fun createFragment(position: Int): Fragment {
        // userId here is ALWAYS the specific user whose profile the admin is viewing
        return when (position) {
            0 -> ItemListFragment.newInstance(
                userId = userId,
                itemStatusFilter = STATUS_PENDING,
                isAdminView = true
            )
            1 -> ItemListFragment.newInstance(
                userId = userId,
                itemStatusFilter = STATUS_APPROVED,
                isAdminView = true
            )
            2 -> ItemListFragment.newInstance(
                userId = userId,
                itemStatusFilter = STATUS_REJECTED,
                isAdminView = true
            )
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}







//package com.example.signuplogina.adapter
//
//import androidx.fragment.app.Fragment
//import androidx.viewpager2.adapter.FragmentStateAdapter
//import com.example.signuplogina.fragments.ItemListFragment
//
//class UserDetailsPagerAdapter(
//    fragment: Fragment,
//    private val userId: String
//) : FragmentStateAdapter(fragment) {
//
//    override fun getItemCount(): Int = 2
//
//    override fun createFragment(position: Int): Fragment {
//        return when (position) {
//            0 -> ItemListFragment.newInstance(userId, showRejected = false)
//            1 -> ItemListFragment.newInstance(userId, showRejected = true)
//            else -> throw IllegalArgumentException("Invalid position")
//        }
//    }
//}
