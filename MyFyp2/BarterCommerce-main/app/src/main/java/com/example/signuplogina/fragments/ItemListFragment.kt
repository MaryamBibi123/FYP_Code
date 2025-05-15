package com.example.signuplogina.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText // Import EditText for reject reason
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Use this import
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.signuplogina.Item
import com.example.signuplogina.R
import com.example.signuplogina.adapter.ItemListAdapter
import com.example.signuplogina.adapter.UserDetailsPagerAdapter // For status constants
import com.example.signuplogina.databinding.FragmentItemListBinding
import com.example.signuplogina.mvvm.ItemViewModel // This should be the ViewModel for user-specific items

class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    // Assuming ItemViewModel is designed to fetch items for a specific user OR all items based on filter
    private val viewModel: ItemViewModel by viewModels() // Or AdminItemViewModel if that's what you use here

    private lateinit var adapter: ItemListAdapter
    private lateinit var userId: String // User whose items are being shown (can be empty for "all items" view)
    private lateinit var itemStatusFilter: String // "pending", "approved", "rejected"
    private var isAdminView: Boolean = false     // True if admin is viewing

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_ITEM_STATUS_FILTER = "item_status_filter"
        private const val ARG_IS_ADMIN_VIEW = "is_admin_view"
        private const val TAG = "ItemListFragment"

        fun newInstance(
            userId: String,
            itemStatusFilter: String,
            isAdminView: Boolean
        ): ItemListFragment {
            return ItemListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_ITEM_STATUS_FILTER, itemStatusFilter)
                    putBoolean(ARG_IS_ADMIN_VIEW, isAdminView)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID) ?: ""
            itemStatusFilter = it.getString(ARG_ITEM_STATUS_FILTER) ?: UserDetailsPagerAdapter.STATUS_APPROVED // Default
            isAdminView = it.getBoolean(ARG_IS_ADMIN_VIEW, false)
            Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter, Admin:$isAdminView] onCreate")
        }
        if (userId.isEmpty() && !isAdminView) { // User ID is crucial unless admin is viewing all items
            Log.e(TAG, "[Filter:$itemStatusFilter, Admin:$isAdminView] User ID is missing for non-global admin view!")
            // Toast.makeText(requireContext(), "User ID missing.", Toast.LENGTH_LONG).show() // Consider if toast is needed here
        }
    }

    override fun onCreateView(   inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle? ): View {
        Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter, Admin:$isAdminView] onCreateView")
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter, Admin:$isAdminView] onViewCreated.")

        setupRecyclerView()
        observeViewModel()

        // Trigger fetch: If isAdminView is true, userId can be empty to signify "all users" for that status.
        // ItemViewModel's attach listener method needs to handle this logic.
        if (isAdminView || userId.isNotEmpty()) {
            Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter, Admin:$isAdminView] Attaching listener.")
            // Assuming your ItemViewModel.attachUserItemListener is modified or you have a new one for admin
            viewModel.attachItemListener(userId, itemStatusFilter, isAdminView) // New or modified VM method
        } else {
            binding.emptyTextView.text = "Cannot load items: User ID missing."
            binding.emptyTextView.isVisible = true
            binding.itemsRecyclerView.isVisible = false
            binding.progressBar.isVisible = false
        }
    }

    private fun setupRecyclerView() {
        if (!::adapter.isInitialized || binding.itemsRecyclerView.adapter == null) {
            Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter, Admin:$isAdminView] Setting up RecyclerView.")
            adapter = ItemListAdapter(
                currentDisplayStatus = itemStatusFilter,
                isAdminView = isAdminView,
                onItemClicked = { item -> handleItemClick(item) },
                onAdminApproveClicked = if (isAdminView) { item -> handleApproveItemClick(item) } else null,
                onAdminRejectClicked = if (isAdminView) { item -> handleRejectItemClick(item) } else null,
                onAdminReApproveClicked = if (isAdminView) { item -> handleReApproveItemClick(item) } else null,
                onUserRemoveItemClicked = if (!isAdminView) {item -> handleUserRemoveItem(item)} else null // Example
            )
            val spanCount = 2 // Or 1 for a list view
            binding.itemsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
            binding.itemsRecyclerView.adapter = adapter
        }
    }

    private fun observeViewModel() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter] Items updated: ${items?.size ?: 0} items.")
            adapter.submitList(items)
            updateEmptyStateVisibility(items.isNullOrEmpty())
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter] Loading state: $isLoading")
            updateEmptyStateVisibility(viewModel.items.value.isNullOrEmpty())
        }
        viewModel.fetchError.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Log.e(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter] Fetch Error: $errorMsg")
                binding.emptyTextView.text = errorMsg; binding.emptyTextView.isVisible = true
                binding.itemsRecyclerView.isVisible = false; binding.progressBar.isVisible = false
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearFetchError()
            }
        }
        viewModel.actionFeedback.observe(viewLifecycleOwner) { feedbackMsg ->
            if (feedbackMsg != null) {
                Toast.makeText(requireContext(), feedbackMsg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter] Action Feedback: $feedbackMsg")
                viewModel.clearActionFeedback()
            }
        }
    }

    private fun updateEmptyStateVisibility(isListEmpty: Boolean) { /* ... as before ... */
        if (_binding == null) return
        val isLoading = viewModel.isLoading.value ?: false
        val fetchError = viewModel.fetchError.value

        binding.progressBar.isVisible = isLoading

        if (isLoading) {
            binding.itemsRecyclerView.isVisible = false
            binding.emptyTextView.isVisible = false
        } else if (fetchError != null) {
            binding.itemsRecyclerView.isVisible = false
            binding.emptyTextView.text = fetchError
            binding.emptyTextView.isVisible = true
        } else {
            binding.itemsRecyclerView.isVisible = !isListEmpty
            binding.emptyTextView.isVisible = isListEmpty
            if (isListEmpty) {
//                binding.emptyTextView.text = getString(R.string.no_items_found_for_filter) // Use a more specific string
            }
        }
    }

    private fun handleItemClick(item: Item) {
        Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter] Item clicked: ${item.id}")
        try {
            // Navigate to AdminItemDetailFragment, needs UserProfileForAdminDirections if that's the host
            // Or a global action if ItemListFragment can be hosted elsewhere
            val hostFragment = parentFragment // ViewPager2 -> PagerAdapter's Host -> NavHost's Host
            if (hostFragment is UserProfileForAdmin) {
                val action = UserProfileForAdminDirections.actionUserProfileForAdminToAdminItemDetailFragment(item)
                findNavController().navigate(action)
            }

                //check for errors
//            else if (parentFragment is AdminItemHostFragment) { // Example if hosted by a main admin panel
//                val action = AdminItemHostFragmentDirections.action_adminItemsHostFragment_to_adminItemDetailFragment(item)
//                findNavController().navigate(action)
//            }
            else {
                Log.e(TAG, "Could not determine correct navigation action for item details.")
                Toast.makeText(context, "Cannot open item details.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to AdminItemDetail failed for item ${item.id}", e)
            Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleApproveItemClick(item: Item) {
        Log.d(TAG, "[Admin Action] Approve item: ${item.id}")
        viewModel.approveItem(item.id) // Assuming ItemViewModel has this
    }

    private fun handleRejectItemClick(item: Item) { // Renamed
        Log.d(TAG, "[Admin Action] Reject item: ${item.id}")
        val editTextReason = EditText(requireContext()).apply { hint = "Rejection reason (optional)" }
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Item")
            .setMessage("Reject '${item.details.productName}'?")
            .setView(editTextReason)
            .setPositiveButton("Reject") { _, _ ->
                val reason = editTextReason.text.toString().trim()
                viewModel.rejectItem(item.id, if (reason.isNotEmpty()) reason else null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun handleReApproveItemClick(item: Item) {
        Log.d(TAG, "[Admin Action] Re-Approve item: ${item.id}")
        viewModel.approveItem(item.id) // Re-uses approve logic which should set status to "approved" and available to true
    }
    private fun handleUserRemoveItem(item:Item){
        Log.d(TAG, "[User Action] Request to remove item: ${item.id}")
        // viewModel.userRemoveItem(item.id) // Example if user can remove their own items
        Toast.makeText(context, "User remove action for ${item.details.productName}", Toast.LENGTH_SHORT).show();
    }


    override fun onDestroyView() { /* ... as before ... */
        super.onDestroyView()
        Log.d(TAG, "[User:${userId.takeIf { id->id.isNotEmpty() }?.take(4) ?: "ALL"}, Filter:$itemStatusFilter, Admin:$isAdminView] onDestroyView")
        binding.itemsRecyclerView.adapter = null
        _binding = null
    }
}






//
//package com.example.signuplogina.fragments
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.core.view.isVisible
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.GridLayoutManager
//import com.example.signuplogina.Item
//import com.example.signuplogina.R
//import com.example.signuplogina.adapter.ItemListAdapter
//import com.example.signuplogina.databinding.FragmentItemListBinding
//import com.example.signuplogina.mvvm.ItemViewModel // *** Use the specific User Item ViewModel ***
//
//class ItemListFragment : Fragment() {
//
//    private var _binding: FragmentItemListBinding? = null
//    private val binding get() = _binding!!
//
//    // *** Inject the correct ViewModel ***
//    private val viewModel: ItemViewModel by viewModels()
//
//    private lateinit var adapter: ItemListAdapter
//    private lateinit var userId: String
//    private var showRejected: Boolean = false
//
//    companion object {
//        private const val ARG_USER_ID = "user_id"
//        private const val ARG_SHOW_REJECTED = "show_rejected"
//        private const val TAG = "UserItemListFragment"
//
//        fun newInstance(userId: String, showRejected: Boolean): ItemListFragment {
//            // ... (newInstance remains the same) ...
//            return ItemListFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_USER_ID, userId)
//                    putBoolean(ARG_SHOW_REJECTED, showRejected)
//                }
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            userId = it.getString(ARG_USER_ID) ?: ""
//            showRejected = it.getBoolean(ARG_SHOW_REJECTED, false)
//            Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] onCreate: userId=$userId, showRejected=$showRejected")
//        }
//        if (userId.isEmpty()) {
//            Log.e(TAG, "[User - ${if(showRejected) "Rejected" else "Approved"}] User ID is missing!")
//            Toast.makeText(requireContext(), "Required User ID is missing.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] onCreateView")
//        _binding = FragmentItemListBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] onViewCreated.")
//
//        setupRecyclerView()
//        observeViewModel()
//
//        // Trigger the initial fetch / attach listener
//        if (userId.isNotEmpty()) {
//            Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Attaching listener from onViewCreated")
//            viewModel.attachUserItemListener(userId, showRejected) // Use correct VM method
//        } else {
//            binding.emptyTextView.text = "Cannot load items: User ID missing."
//            binding.emptyTextView.isVisible = true
//            binding.itemsRecyclerView.isVisible = false
//            binding.progressBar.isVisible = false
//        }
//    }
//
//    // --- REMOVED onResume ---
//// In ItemListFragment.kt (the one used by UserDetailsPagerAdapter)
//    private fun setupRecyclerView() {
//        if (!::adapter.isInitialized || binding.itemsRecyclerView.adapter == null) {
//            Log.d(TAG, "[User:${userId.take(4)}, Status:$itemStatusFilter, Admin:$isAdminView] Setting up RecyclerView.")
//            adapter = ItemListAdapter(
//                currentDisplayStatus = itemStatusFilter, // Passed from UserDetailsPagerAdapter
//                isAdminView = isAdminView,               // Passed from UserDetailsPagerAdapter (should be true)
//                onItemClicked = { item -> handleItemClick(item) },
//                onAdminApproveClicked = { item -> handleApproveItemClick(item) }, // Implement these handlers
//                onAdminRejectClicked = { item -> handleRejectItemClick(item) },   // Implement these handlers
//                onAdminReApproveClicked = { item -> handleUnblockItemClick(item) } // Implement these handlers
//                // onUserRemoveItemClicked = null // User doesn't remove their items via admin view of their profile
//            )
//            val spanCount = 2
//            binding.itemsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
//            binding.itemsRecyclerView.adapter = adapter
//        }
//    }
//
//    private fun observeViewModel() {
//        // Observe items list
//        viewModel.items.observe(viewLifecycleOwner) { items ->
//            Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] USER Items updated via Listener: ${items?.size ?: 0} items received.")
//            adapter.submitList(items)
//            updateEmptyStateVisibility(items.isNullOrEmpty())
//        }
//
//        // Observe loading state
//        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
//            Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] USER Items Loading state changed: $isLoading")
//            updateEmptyStateVisibility(viewModel.items.value.isNullOrEmpty())
//        }
//
//        // Observe Fetch Errors
//        viewModel.fetchError.observe(viewLifecycleOwner) { errorMsg ->
//            if (errorMsg != null) {
//                Log.e(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Fetch Error observed: $errorMsg")
//                binding.emptyTextView.text = errorMsg
//                binding.emptyTextView.isVisible = true
//                binding.itemsRecyclerView.isVisible = false
//                binding.progressBar.isVisible = false
//                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
//                viewModel.clearFetchError()
//            }
//        }
//
//        // Observe Action Feedback
//        viewModel.actionFeedback.observe(viewLifecycleOwner) { feedbackMsg ->
//            if (feedbackMsg != null) {
//                Toast.makeText(requireContext(), feedbackMsg, Toast.LENGTH_SHORT).show()
//                Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Action Feedback: $feedbackMsg")
//                if (binding.emptyTextView.isVisible && binding.emptyTextView.text.toString().startsWith("Error fetching")) {
//                    updateEmptyStateVisibility(viewModel.items.value.isNullOrEmpty())
//                }
//                viewModel.clearActionFeedback()
//            }
//        }
//    }
//
//    private fun updateEmptyStateVisibility(isListEmpty: Boolean) {
//        if (_binding == null) return
//        val isLoading = viewModel.isLoading.value ?: false
//        val fetchError = viewModel.fetchError.value
//
//        binding.progressBar.isVisible = isLoading
//
//        if (isLoading) {
//            binding.itemsRecyclerView.isVisible = false
//            binding.emptyTextView.isVisible = false
//        } else if (fetchError != null) {
//            binding.itemsRecyclerView.isVisible = false
//            binding.emptyTextView.text = fetchError
//            binding.emptyTextView.isVisible = true
//        } else {
//            binding.itemsRecyclerView.isVisible = !isListEmpty
//            binding.emptyTextView.isVisible = isListEmpty
//            if (isListEmpty) {
//                binding.emptyTextView.text = getString(R.string.no_items_found)
//            }
//        }
//    }
//
//
//    // handleItemClick remains the same
//    private fun handleItemClick(item: Item) {
//        Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Item clicked: ${item.id} - ${item.details.productName}")
//        try {
//            val action = UserProfileForAdminDirections
//                .actionUserProfileForAdminToAdminItemDetailFragment(item)
//            findNavController().navigate(action)
//            Log.d(TAG, "Navigating to AdminItemDetailFragment for item: ${item.id}")
//        } catch (e: Exception) {
//            Log.e(TAG, "Navigation failed for item ${item.id}", e)
//            Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // handleRemoveItemClick remains the same
//    private fun handleRemoveItemClick(item: Item) {
//        Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Reject button clicked in LIST for item: ${item.id}")
//        if (item.status.equals("rejected", ignoreCase = true)) {
//            Log.w(TAG, "Reject button clicked in list for an item that is already rejected? ID: ${item.id}")
//            return
//        }
//        AlertDialog.Builder(requireContext())
//            .setTitle(R.string.confirm_rejection_title)
//            .setMessage(getString(R.string.confirm_rejection_message, item.details.productName))
//            .setPositiveButton(R.string.rejected) { dialog, _ ->
//                Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Confirmed rejection from LIST for item: ${item.id}")
//                viewModel.rejectItem(item.id)
//                dialog.dismiss()
//            }
//            .setNegativeButton(R.string.cancel) { dialog, _ ->
//                Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] Cancelled rejection from LIST for item: ${item.id}")
//                dialog.dismiss()
//            }
//            .show()
//    }
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        Log.d(TAG, "[User ${userId.take(4)} - ${if(showRejected) "Rejected" else "Approved"}] onDestroyView")
//        binding.itemsRecyclerView.adapter = null
//        _binding = null
//        // Listener removal handled by ViewModel's onCleared
//    }
//}
//







