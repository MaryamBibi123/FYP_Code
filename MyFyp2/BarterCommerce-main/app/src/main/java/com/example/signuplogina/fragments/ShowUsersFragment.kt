package com.example.signuplogina.fragments

import android.os.Bundle
import android.util.Log // Import Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible // For View.isVisible = true/false
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.User
import com.example.signuplogina.adapter.AdminUserAdapter
import com.example.signuplogina.databinding.FragmentShowUsersBinding
import com.example.signuplogina.mvvm.UserViewModel
import com.example.signuplogina.mvvm.UsersRepo // Keep if onBlockUserClicked uses it directly

class ShowUsersFragment : Fragment() {

    private var _binding: FragmentShowUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: AdminUserAdapter

    // Define admin emails (can be moved to a constants file or BuildConfig later)
    private val adminEmails = listOf("areebaeman524@gmail.com", "mbibi2949@gmail.com")
    private val TAG = "ShowUsersFragment" // Add a TAG

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupRecyclerView()
        observeUsers()

        // Initial call to fetch users
        viewModel.fetchUsersFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = AdminUserAdapter(
            onSeeDetailsClicked = { user ->
                navigateToUserProfileForAdmin(user)
            },
            onBlockUserClicked = { user ->
                // Consider moving this UsersRepo().updateUserStatus call to the ViewModel
                UsersRepo().updateUserStatus(user.userid.toString(), "blocked") { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "${user.fullName} has been blocked.", Toast.LENGTH_SHORT).show()
                        // Optionally, refresh the list or update the item in the adapter
                        viewModel.fetchUsersFromFirebase() // Refresh list
                    } else {
                        Toast.makeText(requireContext(), "Failed to block user. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.userRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userRecyclerView.adapter = adapter
    }

    private fun navigateToUserProfileForAdmin(user: User) {
        try {
            val action = ShowUsersFragmentDirections
                .actionShowUsersFragmentToUserProfileForAdmin(user)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to UserProfileForAdmin failed", e)
            Toast.makeText(context, "Could not open user profile.", Toast.LENGTH_SHORT).show()
        }
    }

    // MODIFIED observeUsers()
    private fun observeUsers() {
        // No need to call viewModel.fetchUsersFromFirebase() here if called once in onViewCreated

        viewModel.getUserList().observe(viewLifecycleOwner) { allUsersList ->
            if (allUsersList == null) {
                Log.w(TAG, "Received null user list from ViewModel.")
                binding.tvNoUsers?.text = "Error loading users." // Example text
                binding.tvNoUsers?.isVisible = true
                adapter.submitList(emptyList()) // Assuming AdminUserAdapter uses ListAdapter
                return@observe
            }

            // Filter out admin users before submitting to the adapter
            val nonAdminUsers = allUsersList.filter { user ->
                val isNotAdmin = !adminEmails.contains(user.useremail) // Assuming User has an 'email' field
                if (!isNotAdmin) {
                    Log.d(TAG, "Filtering out admin user: ${user.useremail}")
                }
                isNotAdmin
            }

            Log.d(TAG, "Observed user list. All: ${allUsersList.size}, Non-Admins: ${nonAdminUsers.size}")
            adapter.submitList(nonAdminUsers) // Submit the filtered list

            // Update empty state visibility based on the filtered list
            if (nonAdminUsers.isEmpty()) {
                binding.tvNoUsers?.text = "No other users found." // Or your preferred empty message
                binding.tvNoUsers?.isVisible = true
            } else {
                binding.tvNoUsers?.isVisible = false
            }
        }

        // Optional: Observe loading and error states from ViewModel if you add them
        // viewModel.isLoading.observe(viewLifecycleOwner) { /* ... */ }
        // viewModel.error.observe(viewLifecycleOwner) { /* ... */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.userRecyclerView.adapter = null // Good practice to clear adapter
        _binding = null
    }
}






//package com.example.signuplogina.fragments
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.core.os.bundleOf
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.signuplogina.R
//import com.example.signuplogina.User
//import com.example.signuplogina.adapter.AdminUserAdapter
//import com.example.signuplogina.databinding.FragmentShowUsersBinding
//import com.example.signuplogina.mvvm.UserViewModel
//import com.example.signuplogina.mvvm.UsersRepo
//
//class ShowUsersFragment : Fragment() {
//
//    private var _binding: FragmentShowUsersBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: UserViewModel by viewModels()
//    private lateinit var adapter: AdminUserAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentShowUsersBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupRecyclerView()
//        observeUsers()
//    }
//
//    private fun setupRecyclerView() {
//
//        adapter = AdminUserAdapter(
//            onSeeDetailsClicked = { user ->
//                navigateToUserProfileForAdmin(user)
//            },
//            onBlockUserClicked = { user ->
//                UsersRepo().updateUserStatus(user.userid.toString(), "blocked") { success ->
//                    if (success) {
//                        Toast.makeText(requireContext(), "${user.fullName} has been blocked.", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(requireContext(), "Failed to block user. Try again.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        )
//
//        binding.userRecyclerView.layoutManager = LinearLayoutManager(requireContext())
//        binding.userRecyclerView.adapter = adapter
//    }
//
//    private fun navigateToUserProfileForAdmin(user: User) {
//        val action = ShowUsersFragmentDirections
//            .actionShowUsersFragmentToUserProfileForAdmin(user)
//        findNavController().navigate(action)
//
////        val bundle = Bundle().apply {  }(
////            "user" to user
////        )
////        findNavController().navigate(
////            R.id.action_showUsersFragment_to_userProfileForAdmin,
////            bundle
////        )
//    }
////used in show users fragment , to show al the user acept admin
//    private fun observeUsers() {
//        viewModel.fetchUsersFromFirebase()
//
//        viewModel.getUserList().observe(viewLifecycleOwner) { userList ->
//            adapter.submitList(userList)
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
