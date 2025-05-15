package com.example.signuplogina.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.signuplogina.Item
import com.example.signuplogina.R
import com.example.signuplogina.adapter.ItemListAdapter
import com.example.signuplogina.adapter.UserDetailsPagerAdapter
import com.example.signuplogina.databinding.FragmentItemListBinding
import com.example.signuplogina.mvvm.AdminItemViewModel // *** Use the specific Admin ViewModel ***

class AdminItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    // *** Inject the correct ViewModel ***
    private val viewModel: AdminItemViewModel by viewModels()

    private lateinit var adapter: ItemListAdapter
    private var showRejected: Boolean = false

//    companion object {
//        private const val ARG_SHOW_REJECTED = "show_rejected"
//        private const val TAG = "AdminItemListFragment"
//
//        fun newInstance(showRejected: Boolean): AdminItemListFragment {
//            // ... (newInstance remains the same) ...
//            return AdminItemListFragment().apply {
//                arguments = Bundle().apply {
//                    putBoolean(ARG_SHOW_REJECTED, showRejected)
//                }
//            }
//        }
//    }
companion object {
    // private const val ARG_SHOW_REJECTED = "show_rejected" // OLD
    private const val ARG_ITEM_STATUS_FILTER_GLOBAL = "item_status_filter_global" // NEW
    private const val TAG = "AdminItemListFragment"

    fun newInstance(itemStatusFilter: String): AdminItemListFragment { // Renamed from showRejected
        return AdminItemListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ITEM_STATUS_FILTER_GLOBAL, itemStatusFilter)
            }
        }
    }
}
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            showRejected = it.getBoolean(ARG_SHOW_REJECTED, false)
//            Log.d(TAG, "onCreate: showRejected=$showRejected")
//        }
//    }

    private lateinit var itemStatusFilter: String // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // showRejected = it.getBoolean(ARG_SHOW_REJECTED, false) // OLD
            itemStatusFilter = it.getString(ARG_ITEM_STATUS_FILTER_GLOBAL) ?: UserDetailsPagerAdapter.STATUS_PENDING // Default to pending if not passed
            Log.d(TAG, "onCreate: itemStatusFilter=$itemStatusFilter")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onCreateView")
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onViewCreated.")
//
//        setupRecyclerView()
//        observeViewModel()
//
//        // Trigger the initial fetch / attach listener
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Attaching listener from onViewCreated")
//        viewModel.attachAllItemsListener(showRejected) // Use the correct VM method
//    }

    // In AdminItemListFragment.kt
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "[Filter:$itemStatusFilter] onViewCreated.")

        setupRecyclerView() // Pass status filter here
        observeViewModel()

        Log.d(TAG, "[Filter:$itemStatusFilter] Attaching listener from onViewCreated")
        // **** PASS itemStatusFilter to ViewModel fetch method for ALL items ****
        viewModel.attachAllItemsListenerByStatus(itemStatusFilter) // New/modified VM method
    }
    // --- REMOVED onResume ---

//    private fun setupRecyclerView() {
//        if (!::adapter.isInitialized || binding.itemsRecyclerView.adapter == null) {
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Setting up RecyclerView. showRejected: $showRejected")
//            adapter = ItemListAdapter(
//                isShowingRejected = showRejected,
//                onItemClicked = { item -> handleItemClick(item) },
//                onRemoveClicked = { item -> handleRemoveItemClick(item) }
//            )
//            val spanCount = 2
//            binding.itemsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
//            binding.itemsRecyclerView.adapter = adapter
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] RecyclerView setup complete.")
//        }
//    }
// In AdminItemListFragment.kt
// In AdminItemListFragment.kt

private fun setupRecyclerView() {
    if (!::adapter.isInitialized || binding.itemsRecyclerView.adapter == null) {
        Log.d(TAG, "[Filter:$itemStatusFilter] Setting up RecyclerView.")
        adapter = ItemListAdapter(
            currentDisplayStatus = itemStatusFilter, // Pass the status this tab is showing
            isAdminView = true,                      // This is always true for AdminItemListFragment
            onItemClicked = { item -> handleItemClick(item) },
            onAdminApproveClicked = { item -> handleAdminApproveItem(item) },
            onAdminRejectClicked = { item -> handleAdminRejectItem(item) },
            onAdminReApproveClicked = { item -> handleAdminUnblockItem(item) }
            // onUserRemoveItemClicked = null // Not applicable for global admin view
        )
        val spanCount = 2
        binding.itemsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.itemsRecyclerView.adapter = adapter
    }
}

    // Implement new click handlers in AdminItemListFragment
    private fun handleAdminApproveItem(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Approve Item")
            .setMessage("Approve '${item.details.productName}'?")
            .setPositiveButton("Approve") { _, _ -> viewModel.approveItem(item.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleAdminRejectItem(item: Item) { // Renamed
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
    private fun handleAdminUnblockItem(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Re-approve Item")
            .setMessage("Re-approve '${item.details.productName}'? This will make it 'approved' and 'available'.")
            .setPositiveButton("Re-approve") { _, _ ->
                viewModel.approveItem(item.id) // Re-uses approve logic
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun observeViewModel() {
        // Observe items list (now driven by listener)
        viewModel.items.observe(viewLifecycleOwner) { items ->
            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] ALL Items updated via Listener: ${items?.size ?: 0} items received.")
            adapter.submitList(items)
            updateEmptyStateVisibility(items.isNullOrEmpty())
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] ALL Items Loading state changed: $isLoading")
            updateEmptyStateVisibility(viewModel.items.value.isNullOrEmpty())
        }

        // Observe Fetch Errors
        viewModel.fetchError.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Log.e(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Fetch Error observed: $errorMsg")
                binding.emptyTextView.text = errorMsg // Show fetch error
                binding.emptyTextView.isVisible = true
                binding.itemsRecyclerView.isVisible = false
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearFetchError() // Clear error after showing
            }
        }

        // Observe Action Feedback (e.g., from rejectItem)
        viewModel.actionFeedback.observe(viewLifecycleOwner) { feedbackMsg ->
            if (feedbackMsg != null) {
                Toast.makeText(requireContext(), feedbackMsg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Action Feedback: $feedbackMsg")
                // Check if the error text view is showing a fetch error, if so, clear it
                if (binding.emptyTextView.isVisible && binding.emptyTextView.text.toString().startsWith("Error fetching")) {
                    updateEmptyStateVisibility(viewModel.items.value.isNullOrEmpty())
                }
                viewModel.clearActionFeedback() // Clear feedback after showing
            }
        }
    }

    private fun updateEmptyStateVisibility(isListEmpty: Boolean) {
        if (_binding == null) return
        val isLoading = viewModel.isLoading.value ?: false
        val fetchError = viewModel.fetchError.value // Check if there's a fetch error

        binding.progressBar.isVisible = isLoading

        if (isLoading) {
            binding.itemsRecyclerView.isVisible = false
            binding.emptyTextView.isVisible = false
        } else if (fetchError != null) {
            // Prioritize showing fetch error
            binding.itemsRecyclerView.isVisible = false
            binding.emptyTextView.text = fetchError
            binding.emptyTextView.isVisible = true
        } else {
            // Not loading, no fetch error: show list or empty text
            binding.itemsRecyclerView.isVisible = !isListEmpty
            binding.emptyTextView.isVisible = isListEmpty
            if (isListEmpty) {
                binding.emptyTextView.text = getString(R.string.no_items_found)
            }
        }
    }

    // handleItemClick remains the same (using navigation by ID)
    private fun handleItemClick(item: Item) {
        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Item clicked: ${item.id} - ${item.details.productName}")
        try {
            val bundle = Bundle().apply { putParcelable("item", item) }
            // Use requireParentFragment if needed, otherwise direct findNavController might work here too
            requireParentFragment().findNavController().navigate(R.id.adminItemDetailFragment, bundle)
            Log.d(TAG, "Navigating to AdminItemDetailFragment for item: ${item.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed for item ${item.id}", e)
            Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
        }
    }

    // handleRemoveItemClick remains the same (calling rejectItem)
    private fun handleRemoveItemClick(item: Item) {
        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Reject button clicked in LIST for item: ${item.id}")
        if (item.status.equals("rejected", ignoreCase = true)) {
            Log.w(TAG, "Reject button clicked in list for an item that is already rejected? ID: ${item.id}")
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_rejection_title)
            .setMessage(getString(R.string.confirm_rejection_message, item.details.productName))
            .setPositiveButton(R.string.rejected) { dialog, _ ->
                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Confirmed rejection from LIST for item: ${item.id}")
                // Don't show toast here, rely on actionFeedback observer
                viewModel.rejectItem(item.id)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Cancelled rejection from LIST for item: ${item.id}")
                dialog.dismiss()
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onDestroyView")
        binding.itemsRecyclerView.adapter = null
        _binding = null
        // ViewModel's onCleared will handle listener removal
    }
}












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
//import com.example.signuplogina.mvvm.ItemViewModel
//
//class AdminItemListFragment : Fragment() {
//
//    private var _binding: FragmentItemListBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: ItemViewModel by viewModels()
//    private lateinit var adapter: ItemListAdapter
//    private var showRejected: Boolean = false
//    private var isInitialLoad = true
//
//    companion object {
//        private const val ARG_SHOW_REJECTED = "show_rejected"
//        private const val TAG = "AdminItemListFragment"
//
//        fun newInstance(showRejected: Boolean): AdminItemListFragment {
//            return AdminItemListFragment().apply {
//                arguments = Bundle().apply {
//                    putBoolean(ARG_SHOW_REJECTED, showRejected)
//                }
//            }
//        }
//    }
//
//    // ... (onCreate, onCreateView, onViewCreated, onResume, setupRecyclerView remain the same) ...
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            showRejected = it.getBoolean(ARG_SHOW_REJECTED, false)
//            Log.d(TAG, "onCreate: showRejected=$showRejected")
//        }
//        isInitialLoad = true
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onCreateView")
//        _binding = FragmentItemListBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onViewCreated. isInitialLoad: $isInitialLoad")
//
//        setupRecyclerView()
//        observeViewModel()
//
//        if (isInitialLoad) {
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Triggering initial fetch from onViewCreated")
//            viewModel.fetchAllItemsByStatus(showRejected)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onResume. isInitialLoad: $isInitialLoad")
//        if (!isInitialLoad) {
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Triggering refresh fetch from onResume")
//            if (viewModel.isLoadingAllItems.value != true) {
//                viewModel.fetchAllItemsByStatus(showRejected)
//            } else {
//                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Skipping fetch in onResume as already loading")
//            }
//        }
//        isInitialLoad = false
//    }
//
//
//    private fun setupRecyclerView() {
//        if (!::adapter.isInitialized) {
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Setting up RecyclerView. showRejected: $showRejected")
//            adapter = ItemListAdapter(
//                isShowingRejected = showRejected,
//                onItemClicked = { item -> handleItemClick(item) },
//                onRemoveClicked = { item -> handleRemoveItemClick(item) }
//            )
//            val spanCount = 2
//            binding.itemsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
//            binding.itemsRecyclerView.adapter = adapter
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] RecyclerView setup complete.")
//        }
//    }
//
//
//    private fun observeViewModel() {
//        if (!viewModel.allItems.hasObservers()) {
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Observing ViewModel for ALL items")
//
//            viewModel.allItems.observe(viewLifecycleOwner) { items ->
//                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] ALL Items updated: ${items?.size ?: 0} items received.")
//                adapter.submitList(items) {
//                    Log.d(TAG,"[${if(showRejected) "Rejected" else "Approved"}] Diff calculation finished for ALL items.")
//                }
//                updateEmptyStateVisibility(items.isNullOrEmpty())
//            }
//
//            viewModel.isLoadingAllItems.observe(viewLifecycleOwner) { isLoading ->
//                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] ALL Items Loading state changed: $isLoading")
//                // Progress bar handled by updateEmptyStateVisibility
//                updateEmptyStateVisibility(viewModel.allItems.value.isNullOrEmpty())
//            }
//
//            // --- Modified Error Observer ---
//            viewModel.error.observe(viewLifecycleOwner) { feedbackMsg ->
//                if (feedbackMsg != null) {
//                    Toast.makeText(requireContext(), feedbackMsg, Toast.LENGTH_SHORT).show() // Show feedback
//
//                    if (feedbackMsg.startsWith("Error:", ignoreCase = true)) {
//                        // If it's an actual error, show it in the empty text view
//                        Log.e(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Error observed: $feedbackMsg")
//                        binding.emptyTextView.text = feedbackMsg // Show the error message
//                        binding.emptyTextView.isVisible = true
//                        binding.itemsRecyclerView.isVisible = false
//                        binding.progressBar.isVisible = false
//                    } else if (feedbackMsg.startsWith("Success:", ignoreCase = true)) {
//                        // If it's the success message from rejecting an item...
//                        Log.i(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Success feedback observed: $feedbackMsg - Triggering refresh.")
//                        // *** TRIGGER REFRESH of the current list ***
//                        // Check loading state again just before fetching
//                        if (viewModel.isLoadingAllItems.value != true) {
//                            viewModel.fetchAllItemsByStatus(showRejected)
//                        } else {
//                            Log.w(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Success observed, but skipping refresh as already loading.")
//                        }
//                    }
//
//                    // Clear the message in the ViewModel so it's only handled once
//                    viewModel.clearError()
//                }
//            }
//            // --- End of Modified Error Observer ---
//
//        } else {
//            Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] ViewModel's allItems stream already being observed.")
//        }
//    }
//
//    private fun updateEmptyStateVisibility(isListEmpty: Boolean) {
//        if (_binding == null) return
//        val isLoading = viewModel.isLoadingAllItems.value ?: false
//        binding.progressBar.isVisible = isLoading // Update progress bar visibility
//
//        // Don't show empty text if there was an error message displayed there
//        val showingError = binding.emptyTextView.isVisible && binding.emptyTextView.text.toString().startsWith("Error:")
//
//        if (isLoading) {
//            binding.itemsRecyclerView.isVisible = false
//            binding.emptyTextView.isVisible = false
//        } else if (showingError) {
//            // Keep error visible, hide list
//            binding.itemsRecyclerView.isVisible = false
//            binding.emptyTextView.isVisible = true
//        }
//        else {
//            // Not loading, not showing error: show list or empty text
//            binding.itemsRecyclerView.isVisible = !isListEmpty
//            binding.emptyTextView.isVisible = isListEmpty
//            if (isListEmpty) {
//                binding.emptyTextView.text = getString(R.string.no_items_found) // Reset text
//            }
//        }
//    }
//
//
//    private fun handleItemClick(item: Item) {
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Item clicked: ${item.id} - ${item.details.productName}")
//        // Toast.makeText(requireContext(), "Clicked: ${item.details.productName}", Toast.LENGTH_SHORT).show()
//        try {
//            val bundle = Bundle().apply { putParcelable("item", item) }
//            requireParentFragment().findNavController().navigate(R.id.adminItemDetailFragment, bundle)
//            Log.d(TAG, "Navigating to AdminItemDetailFragment for item: ${item.id}")
//        } catch (e: Exception) {
//            Log.e(TAG, "Navigation failed for item ${item.id}", e)
//            Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // Renamed for clarity, matches ViewModel action
//    private fun handleRemoveItemClick(item: Item) {
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Reject button clicked in LIST for item: ${item.id}")
//        if (item.details.status.equals("rejected", ignoreCase = true)) {
//            Log.w(TAG, "Reject button clicked in list for an item that is already rejected? ID: ${item.id}")
//            return
//        }
//
//        AlertDialog.Builder(requireContext())
//            .setTitle(R.string.confirm_rejection_title)
//            .setMessage(getString(R.string.confirm_rejection_message, item.details.productName))
//            .setPositiveButton(R.string.rejected) { dialog, _ ->
//                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Confirmed rejection from LIST for item: ${item.id}")
//                Toast.makeText(requireContext(), getString(R.string.rejecting_item_toast, item.details.productName), Toast.LENGTH_SHORT).show()
//                viewModel.rejectItem(item.id)
//                dialog.dismiss()
//            }
//            .setNegativeButton(R.string.cancel) { dialog, _ ->
//                Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] Cancelled rejection from LIST for item: ${item.id}")
//                dialog.dismiss()
//            }
//            .show()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        Log.d(TAG, "[${if(showRejected) "Rejected" else "Approved"}] onDestroyView")
//        binding.itemsRecyclerView.adapter = null
//        _binding = null
//    }
//}