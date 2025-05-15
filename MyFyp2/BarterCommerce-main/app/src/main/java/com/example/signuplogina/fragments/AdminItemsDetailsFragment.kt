package com.example.signuplogina.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.signuplogina.Item // Your Item data class
import com.example.signuplogina.R
import com.example.signuplogina.adapter.ImageAdapter
// *** IMPORT THE NEW BINDING CLASS ***
import com.example.signuplogina.databinding.FragmentAdminItemsDetailsBinding // Change this line
import com.example.signuplogina.mvvm.ItemViewModel
import com.example.signuplogina.mvvm.UserViewModel
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class AdminItemsDetailsFragment : Fragment() {

    // *** USE THE NEW BINDING CLASS ***
    private var _binding: FragmentAdminItemsDetailsBinding? = null
    private val binding get() = _binding!!

    private val TAG = "AdminItemDetailFragment"
    private val args: AdminItemsDetailsFragmentArgs by navArgs()
    private val itemViewModel: ItemViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var imageAdapter: ImageAdapter
    private lateinit var dotsIndicator: DotsIndicator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // *** INFLATE USING THE NEW BINDING CLASS ***
        _binding = FragmentAdminItemsDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = args.item
        Log.d(TAG, "Displaying details for item: ${item.id}, Status: ${item.status}")

        binding.productName.text = item.details.productName
        binding.productDescriptionText.text = item.details.description
        binding.productCategoryText.text = item.details.category
        binding.productConditionText.text = item.details.condition
        binding.likeButton.visibility = View.GONE
        dotsIndicator = binding.dotsIndicator
        val imageUrls = item.details.imageUrls ?: emptyList()
        if (imageUrls.isNotEmpty()) {
            imageAdapter = ImageAdapter(imageUrls, false)
            binding.productImageCarousel.apply {
                adapter = imageAdapter
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                if (imageUrls.size > 1) {
                    dotsIndicator.visibility = View.VISIBLE
                    dotsIndicator.setViewPager2(this)
                } else {
                    dotsIndicator.visibility = View.GONE
                }
            }
        } else {
            Log.w(TAG, "No image URLs found for item ${item.id}")
            binding.productImageCarousel.visibility = View.GONE
            dotsIndicator.visibility = View.GONE
        }

        setupDescriptionToggle()

        binding.arrowBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupAdminButtons(item)
        observeViewModel()
    }

    private fun setupDescriptionToggle() {
        val toggleLayout = binding.toggleDescriptionLayout
        val arrowIcon = binding.arrowIcon
        val descriptionText = binding.productDescriptionText
        descriptionText.visibility = View.GONE
        arrowIcon.setImageResource(R.drawable.arrow_back)
        toggleLayout.setOnClickListener {
            if (descriptionText.visibility == View.GONE) {
                descriptionText.visibility = View.VISIBLE
                arrowIcon.setImageResource(R.drawable.ic_arrow_right)
            } else {
                descriptionText.visibility = View.GONE
                arrowIcon.setImageResource(R.drawable.arrow_back)
            }
        }
    }


    // Inside AdminItemsDetailsFragment.kt

    private fun setupAdminButtons(item: Item) {
        val isPending = item.status.equals("pending", ignoreCase = true)
        // val isRejected = item.status.equals("rejected", ignoreCase = true) // No longer needed for this button's logic
        // val isApproved = item.status.equals("approved", ignoreCase = true) // For clarity

        // --- Button 1: See Seller (Always visible if userId exists) ---
        if (item.userId.isNotEmpty()) {
            binding.exploreSellerProfileButton.visibility = View.VISIBLE
            binding.exploreSellerProfileButton.setOnClickListener {
                Log.d(TAG, "See Seller clicked for userId: ${item.userId}")
                userViewModel.fetchUserById(item.userId) { sellerUser -> // Assuming userViewModel is available
                    if (sellerUser != null) {
                        try {
                            val action = AdminItemsDetailsFragmentDirections
                                .actionAdminItemDetailFragmentToUserProfileForAdmin(sellerUser)
                            findNavController().navigate(action)
                        } catch (e: Exception) {
                            Log.e(TAG, "Navigation to UserProfileForAdmin failed.", e)
                            Toast.makeText(requireContext(), "Could not open seller profile.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Could not load seller profile.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to fetch user data for userId: ${item.userId}")
                    }
                }
            }
        } else {
            binding.exploreSellerProfileButton.visibility = View.GONE
            Log.w(TAG, "Item ${item.id} has no userId, hiding 'See Seller' button.")
        }

        // --- Button 2: "Reject Item" (was removeItemButton) ---
        // This button should ONLY be visible if the item's status is "pending".
        // If you also need an "Approve" button here, you'd add it similarly.

        if (isPending) {
            binding.removeItemButton.visibility = View.VISIBLE
//            binding.removeItemButton.text = "Reject" // e.g., "Reject Item"
            binding.removeItemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red)) // Example color
            binding.removeItemButton.setOnClickListener {
                Log.d(TAG, "Reject Item button clicked from details for PENDING item: ${item.id}")
                showRejectConfirmationDialog(item) // Renamed for clarity
            }

            // OPTIONAL: Add an "Approve" button if you want it on the details screen too
            binding.approveItemButton?.visibility = View.VISIBLE // Assuming you add this button to your XML
            binding.approveItemButton?.setOnClickListener {
                Log.d(TAG, "Approve Item button clicked from details for PENDING item: ${item.id}")
                showApproveConfirmationDialog(item)
            }

        } else {
            // If item is NOT pending (i.e., approved, rejected, or any other status),
            // the "Reject" action (via removeItemButton) is not available from this screen.
            binding.removeItemButton.visibility = View.GONE
            binding.approveItemButton?.visibility = View.GONE // Hide approve button too
        }
    }

    // Renamed showRemoveConfirmationDialog to be more specific
    private fun showRejectConfirmationDialog(item: Item) {
        val editTextReason = EditText(requireContext()).apply {
            hint = "Reason for rejection (optional)"
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_rejection_title)) // "Confirm Rejection"
            .setMessage(getString(R.string.confirm_rejection_message, item.details.productName))
            .setView(editTextReason)
            .setPositiveButton("Reject") { dialog, _ -> // e.g., "Reject"
                Log.d(TAG, "Confirmed rejection for item: ${item.id}")
                val reason = editTextReason.text.toString().trim()
                itemViewModel.rejectItem(item.id, if (reason.isNotEmpty()) reason else null)
                // Navigation back will be handled by actionFeedback observer
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> // e.g., "Cancel"
                Log.d(TAG, "Cancelled rejection for item: ${item.id}")
                dialog.dismiss()
            }
            .show()
    }

    // OPTIONAL: Add a confirmation dialog for approving
    private fun showApproveConfirmationDialog(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Approval")
            .setMessage("Are you sure you want to approve '${item.details.productName}'?")
            .setPositiveButton("Approve") { dialog, _ ->
                Log.d(TAG, "Confirmed approval for item: ${item.id}")
                itemViewModel.approveItem(item.id)
                // Navigation back can be handled by actionFeedback observer
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
//    private fun setupAdminButtons(item: Item) {
//        val isRejected = item.status.equals("rejected", ignoreCase = true)
//        val isPending = item.status.equals("pending", ignoreCase = true)
//
//        // --- Button 1: See Seller ---
//        binding.exploreSellerProfileButton.visibility = View.VISIBLE
//        binding.exploreSellerProfileButton.setOnClickListener {
//            if (item.userId.isNotEmpty()) {
//                Log.d(TAG, "See Seller clicked for userId: ${item.userId}")
//                userViewModel.fetchUserById(item.userId) { sellerUser ->
//                    if (sellerUser != null) {
//                        val action = AdminItemsDetailsFragmentDirections
//                            .actionAdminItemDetailFragmentToUserProfileForAdmin(sellerUser)
//                        findNavController().navigate(action)
//                    } else {
//                        Toast.makeText(requireContext(), "Could not load seller profile.", Toast.LENGTH_SHORT).show()
//                        Log.e(TAG, "Failed to fetch user data for userId: ${item.userId}")
//                    }
//                }
//            } else {
//                Toast.makeText(requireContext(), "Seller information not available.", Toast.LENGTH_SHORT).show()
//                Log.w(TAG, "Item ${item.id} has no userId.")
//            }
//        }
//
//        // --- Button 2: Remove Item ---
//        if (!isRejected) {
//            // *** REFERENCE THE NEW BUTTON ID ***
//            binding.removeItemButton.visibility = View.VISIBLE
//            binding.removeItemButton.setOnClickListener {
//                Log.d(TAG, "Remove Item clicked for item: ${item.id}")
//                showRemoveConfirmationDialog(item)
//            }
//        }
//
//        else if (isPending) {
//            // *** REFERENCE THE NEW BUTTON ID ***
//            binding.removeItemButton.visibility = View.GONE
//        }
//        else {
//            // *** REFERENCE THE NEW BUTTON ID ***
//            binding.removeItemButton.visibility = View.GONE
//        }
//
//    }

//    private fun showRemoveConfirmationDialog(item: Item) {
//        AlertDialog.Builder(requireContext())
//            .setTitle(R.string.confirm_rejection_title)
//            .setMessage(getString(R.string.confirm_rejection_message, item.details.productName))
//            .setPositiveButton(R.string.rejected) { dialog, _ ->
//                Log.d(TAG, "Confirmed removal for item: ${item.id}")
//                Toast.makeText(requireContext(), getString(R.string.rejecting_item_toast, item.details.productName), Toast.LENGTH_SHORT).show()
//                itemViewModel.rejectItem(item.id)
//                dialog.dismiss()
//            }
//            .setNegativeButton(R.string.cancel) { dialog, _ ->
//                Log.d(TAG, "Cancelled removal for item: ${item.id}")
//                dialog.dismiss()
//            }
//            .show()
//    }


    private fun observeViewModel() {
        // *** OBSERVE actionFeedback for rejectItem results ***
        itemViewModel.actionFeedback.observe(viewLifecycleOwner) { feedbackMsg ->
            // Handle both success ("Success: ...") and failure ("Error: Failed to update...") messages
            if (feedbackMsg != null) {
                Toast.makeText(requireContext(), feedbackMsg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Action Feedback received: $feedbackMsg")
                if (feedbackMsg.startsWith("Success:", ignoreCase = true)) {
                    // Navigate back only on successful rejection
                    findNavController().navigateUp()
                }
                // Clear the feedback message after handling it
                itemViewModel.clearActionFeedback()
            }
        }}
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}