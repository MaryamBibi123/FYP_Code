package com.example.signuplogina

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.databinding.FragmentBidsPlacedBinding
import com.example.signuplogina.fragments.UserProfileFragment // For parentFragment check
import com.example.signuplogina.BidsFragmentDirections // For NavDirections
import com.example.signuplogina.fragments.UserProfileFragmentDirections
import com.example.signuplogina.mvvm.BidManagementViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BidsPlacedFragment : Fragment() {

    private var _binding: FragmentBidsPlacedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BidManagementViewModel by viewModels()
    private lateinit var adapter: BidsPlacedAdapter

    private val TAG = "BidsPlacedFragment"
    private var currentLoggedInUserId: String? = null
    // This will be the ID of the user whose bids are being displayed.
    // It's determined once and used consistently.
    private lateinit var effectiveUserIdForDisplay: String

    companion object {
        private const val ARG_USER_ID = "userId" // Ensure BidsPagerAdapter uses this exact key

        @JvmStatic
        fun newInstance(userId: String?): BidsPlacedFragment {
            val fragment = BidsPlacedFragment()
            Log.d("BidsPlacedFragment", "newInstance called with userId: $userId")
            // Only set arguments if userId is actually provided
            if (userId != null && userId.isNotBlank()) {
                val args = Bundle()
                args.putString(ARG_USER_ID, userId)
                fragment.arguments = args
                Log.d("BidsPlacedFragment", "newInstance: Arguments SET with userId: $userId")
            } else {
                Log.d("BidsPlacedFragment", "newInstance: No userId provided, arguments NOT set.")
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBidsPlacedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated BEGIN. Arguments bundle: ${arguments}")

        currentLoggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
        val passedUserIdFromArgs = arguments?.getString(ARG_USER_ID)

        Log.d(TAG, "onViewCreated: passedUserIdFromArgs = '$passedUserIdFromArgs', currentLoggedInUserId = '$currentLoggedInUserId'")

        if (passedUserIdFromArgs != null && passedUserIdFromArgs.isNotBlank()) {
            effectiveUserIdForDisplay = passedUserIdFromArgs
            Log.i(TAG, "Context: Will display bids for PASSED user: $effectiveUserIdForDisplay")
        } else if (currentLoggedInUserId != null) {
            effectiveUserIdForDisplay = currentLoggedInUserId!!
            Log.i(TAG, "Context: No valid passedID, will display bids for LOGGED-IN user: $effectiveUserIdForDisplay")
        } else {
            Log.e(TAG, "CRITICAL ERROR: No user ID available. Cannot fetch bids.")
            binding.tvNoBids.text = "User context error."
            binding.tvNoBids.isVisible = true
            return
        }

        setupRecyclerView()
        observeViewModel()

        Log.d(TAG, "Requesting ViewModel to fetch bids for user: $effectiveUserIdForDisplay")
        viewModel.fetchBidsPlacedByUser(effectiveUserIdForDisplay)
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView for user: $effectiveUserIdForDisplay. LoggedInUser: $currentLoggedInUserId")
        adapter = BidsPlacedAdapter(
            bids = emptyList(), // ViewModel will populate
            currentLoggedInUserId = this.currentLoggedInUserId, // Pass the actual logged-in user ID
            onReplaceClicked = { bid ->
                // Replace is only allowed if the *displayed bids* belong to the *logged-in user*
                if (effectiveUserIdForDisplay == currentLoggedInUserId) {
                    navigateToSelectProductFragment(bid)
                } else {
                    Toast.makeText(context, "You can only modify your own offers.", Toast.LENGTH_SHORT).show()
                }
            },
            onOfferedItemsClicked = { bid ->
                // isCurrentUserTheReceiver: true if the logged-in user is the receiver of THIS bid.
                val isCurrentUserTheReceiver = (bid.receiverId == currentLoggedInUserId)
                // isFromOtherUserProfileContext: true if we are currently displaying bids of a user who is NOT the logged-in user.
                val isViewingAnotherUsersProfileBids = (effectiveUserIdForDisplay != currentLoggedInUserId)
                Log.d(TAG, "onOfferedItemsClicked: Bid ID=${bid.bidId}, isCurrentUserTheReceiver=$isCurrentUserTheReceiver, isViewingAnotherUsersProfileBids=$isViewingAnotherUsersProfileBids")
                navigateToOfferedItemsDetails(bid, isCurrentUserTheReceiver, isViewingAnotherUsersProfileBids)
            },
            onWithdrawClicked = { bidToWithdraw ->
                // Withdraw is only allowed if the *displayed bids* belong to the *logged-in user*
                if (effectiveUserIdForDisplay == currentLoggedInUserId) {
                    showWithdrawConfirmationDialog(bidToWithdraw)
                } else {
                    Toast.makeText(context, "You can only withdraw your own offers.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvBidsPlaced.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBidsPlaced.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.placedBids.observe(viewLifecycleOwner) { bids ->
            Log.d(TAG, "Observed placedBids update from ViewModel with ${bids.size} bids for user $effectiveUserIdForDisplay.")
            adapter.updateBids(bids)
            binding.tvNoBids.isVisible = bids.isEmpty() && (viewModel.isLoading.value == false)
            if (bids.isEmpty() && viewModel.isLoading.value == false) {
                binding.tvNoBids.text = if (effectiveUserIdForDisplay == currentLoggedInUserId) "You haven't placed any bids." else "This user hasn't placed any bids."
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Observed isLoading update: $isLoading")
            binding.progressBarBidsPlaced?.isVisible = isLoading
            if (isLoading) {
                binding.tvNoBids.isVisible = false
            } else {
                if (viewModel.placedBids.value.isNullOrEmpty()){ // Check after loading finishes
                    binding.tvNoBids.isVisible = true
                }
            }
        }

        viewModel.feedbackMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                // Consider if viewModel should have a method to clear the message after it's shown
                // viewModel.clearFeedbackMessage()
            }
        }
    }

    private fun showWithdrawConfirmationDialog(bid: Bid) {
        // ... (as before)
        AlertDialog.Builder(requireContext())
            .setTitle("Withdraw Offer")
            .setMessage("Are you sure you want to withdraw your offer for '${bid.requestedItemDetails?.details?.productName ?: "this item"}'?")
            .setPositiveButton("Yes, Withdraw") { _, _ ->
                viewModel.withdrawPendingBid(bid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToSelectProductFragment(bidToReplace: Bid) {
        // ... (as before, ensure 'selectedProduct' is correctly populated if needed by SelectProductFragment)
        Log.d(TAG, "Navigating to SelectProduct for replacing bid: ${bidToReplace.bidId}")
        val bundle = Bundle().apply {
            putString("bidId", bidToReplace.bidId)
            putBoolean("isReplacing", true)
            putStringArray("initialOfferedItemIds", bidToReplace.offeredItemIds.toTypedArray())
            putParcelable("selectedProduct", bidToReplace.requestedItemDetails)
        }
        try {
            val actionId = if (parentFragment is BidsFragment) R.id.action_bidsFragment_to_selectProductFragment
//            else if (parentFragment is UserProfileFragment) R.id.action_userProfileFragment_to_selectProductFragment
            else { Log.e(TAG, "Unknown parent for BidsPlaced->SelectProduct nav."); return }
            findNavController().navigate(actionId, bundle)
        } catch (e: Exception) { Log.e(TAG, "Nav to SelectProduct failed", e) }
    }

    private fun navigateToOfferedItemsDetails(bid: Bid, isCurrentUserTheReceiver: Boolean, isFromOtherUserProfileContext: Boolean) {
        // ... (as before, ensure NavDirections and parentFragment checks are robust)
        Log.d(TAG, "navigateToOfferedItemsDetails: bid=${bid.bidId}, isReceiver=$isCurrentUserTheReceiver, fromOtherProfile=$isFromOtherUserProfileContext")
        if (context == null || !isAdded) return
        try {
            val action = when (parentFragment) {
                is BidsFragment -> BidsFragmentDirections.actionBidsFragmentToOfferedItemsDetailsFragment(bid.bidId, bid.offeredItemIds.toTypedArray(), isCurrentUserTheReceiver, bid.itemId, isFromOtherUserProfileContext)
                is UserProfileFragment -> UserProfileFragmentDirections.actionUserProfileFragmentToOfferedItemsDetailsFragment(bid.bidId, bid.offeredItemIds.toTypedArray(), isCurrentUserTheReceiver, bid.itemId, isFromOtherUserProfileContext)
                else -> { Log.e(TAG, "Unknown parent for BidsPlaced->OfferedDetails nav: ${parentFragment?.javaClass?.simpleName}"); return }
            }
            findNavController().navigate(action)
        } catch (e: Exception) {Log.e(TAG, "Nav to OfferedDetails failed.", e) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvBidsPlaced.adapter = null
        _binding = null
    }
}









//package com.example.signuplogina
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.signuplogina.databinding.FragmentBidsPlacedBinding
//import com.example.signuplogina.fragments.UserProfileFragment
//import com.example.signuplogina.fragments.UserProfileFragmentDirections
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//
//class BidsPlacedFragment : Fragment() {
//
//    private var _binding: FragmentBidsPlacedBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var adapter: BidsPlacedAdapter
//    private val bidList = mutableListOf<Bid>()
//    private val TAG = "BidsPlacedFragment" // Added TAG
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentBidsPlacedBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "onViewCreated. Arguments from PagerAdapter: ${arguments}") // Log arguments
//
//        // --- THIS IS THE SECTION TO CHANGE/CORRECT ---
//        val passedUserIdFromArgs = arguments?.getString("userId") // Key used by your BidsPagerAdapter
//        val currentLoggedInUserId = FirebaseAuth.getInstance().currentUser?.uid
//        val userIdToUse: String
//        var isViewingOwnBids: Boolean =false// NEW FLAG
//
//        if (passedUserIdFromArgs != null && passedUserIdFromArgs.isNotBlank()) {
//            // If a userId was explicitly passed via arguments (e.g., from UserProfileFragment's PagerAdapter)
//            userIdToUse = passedUserIdFromArgs
//            isViewingOwnBids = (passedUserIdFromArgs == currentLoggedInUserId)
//            Log.i(TAG, "Using passedUserIdFromArgs for fetching bids: $userIdToUse")
//        } else if (currentLoggedInUserId != null) {
//            // If no valid userId was passed, default to the currently logged-in user (for "My Bids Placed")
//            userIdToUse = currentLoggedInUserId
//            Log.i(TAG, "No valid passedUserId, using currentLoggedInUserId for fetching bids: $userIdToUse")
//        } else {
//            // Critical error: No user ID available at all.
//            Log.e(TAG, "User ID not available (neither passed nor logged in). Cannot fetch bids.")
//            binding.tvNoBids.text = "User not identified. Cannot load bids."
//            binding.tvNoBids.visibility = View.VISIBLE
//            return // Exit if no user ID can be determined
//        }
//        // --- END OF SECTION TO CHANGE ---
//
//        Log.d(TAG, "Final userIdToUse for fetching bids: $userIdToUse")
//
//        // Initialize adapter
//        adapter = BidsPlacedAdapter(
//            bids = bidList,
//            onReplaceClicked = { bidId ->
//                if (userIdToUse == currentLoggedInUserId) { // Assuming currentLoggedInUserId is defined
//                    navigateToSelectProductFragment(bidId)
//                } else {
//                    Toast.makeText(context, "You can only modify your own offers.", Toast.LENGTH_SHORT).show()
//                }
//            },
//            onOfferedItemsClicked = { bid ->
//                val isCurrentUserTheReceiverOfThisSpecificBid = (bid.receiverId == currentLoggedInUserId)
//                // Pass the 'isViewingOwnBids' flag to control actions on the next screen
//                navigateToOfferedItemsDetails(bid, isCurrentUserTheReceiverOfThisSpecificBid, !isViewingOwnBids)
//            }
////            onOfferedItemsClicked = { bid -> // This is the lambda you provide
////                // Determine if the LOGGED-IN user is the RECEIVER of THIS specific bid.
////                val isCurrentUserTheReceiverOfThisBid = (bid.receiverId == currentLoggedInUserId) // Assuming currentLoggedInUserId
////                Log.d(TAG, "onOfferedItemsClicked: bidId=${bid.bidId}, bid.receiverId=${bid.receiverId}, currentLoggedInUserId=$currentLoggedInUserId")
////                navigateToOfferedItemsDetails(bid, isCurrentUserTheReceiverOfThisBid)
////            }
//        )
//
//        binding.rvBidsPlaced.layoutManager = LinearLayoutManager(requireContext())
//        binding.rvBidsPlaced.adapter = adapter
//
//        fetchBidsPlaced(userIdToUse)
//    }
//
//    // Add this new function for navigating to offered item details
//// (Ensure NavDirections class and action ID are correct for your graph)
//    private fun navigateToOfferedItemsDetails(
//        bid: Bid,
//        isLoggedUserTheBidReceiver: Boolean,
//        isFromOtherUserProfileContext: Boolean // **** NEW FLAG ****
//    ) {
//        Log.d(TAG, "Navigating to OfferedItemsDetails for bid: ${bid.bidId}, isReceiver: $isLoggedUserTheBidReceiver, fromOtherProfile: $isFromOtherUserProfileContext")
//        if (context == null || !isAdded) return
//
//        try {
//            val action = when (parentFragment) { // Or however you determine the NavDirections
//                is BidsFragment -> BidsFragmentDirections.actionBidsFragmentToOfferedItemsDetailsFragment(
//                    bidId = bid.bidId,
//                    offeredItemIds = bid.offeredItemIds.toTypedArray(),
//                    isCurrentUserBidReceiver = isLoggedUserTheBidReceiver,
//                    requestedItemId = bid.itemId,
//                    isFromOtherUserProfile = isFromOtherUserProfileContext // **** PASS NEW FLAG ****
//                )
//                is UserProfileFragment -> UserProfileFragmentDirections.actionUserProfileFragmentToOfferedItemsDetailsFragment(
//                    bidId = bid.bidId,
//                    offeredItemIds = bid.offeredItemIds.toTypedArray(),
//                    isCurrentUserBidReceiver = isLoggedUserTheBidReceiver,
//                    requestedItemId = bid.itemId,
//                    isFromOtherUserProfile = isFromOtherUserProfileContext // **** PASS NEW FLAG ****
//                )
//                else -> { /* ... error handling ... */ return }
//            }
//            findNavController().navigate(action)
//        } catch (e: Exception) {
//            // ... error handling ...
//        }
//    }//        super.onViewCreated(view, savedInstanceState)
////        Log.d(TAG, "onViewCreated")
////
////        val passedUserId = arguments?.getString("userId")
////        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
////        // Generally, BidsPlacedFragment shows the *logged-in* user's bids.
////        // Using passedUserId might be for viewing *another* user's placed bids?
////        // If it's always the logged-in user, simplify this:
////        val userIdToUse = currentUserId ?: run {
////            Log.e(TAG, "Current user ID is null. Cannot fetch bids.")
////            binding.tvNoBids.visibility = View.VISIBLE // Show no bids message
////            return // Exit if no user logged in
////        }
////
////        // Initialize adapter - removed userId parameter as adapter checks current user internally
////        adapter = BidsPlacedAdapter(
////            bidList,
////            onReplaceClicked = { bidId -> navigateToSelectProductFragment(bidId) }
////            // userId = userIdToUse // Removed - adapter checks FirebaseAuth.getInstance() now
////        )
////
////        binding.rvBidsPlaced.layoutManager = LinearLayoutManager(requireContext())
////        binding.rvBidsPlaced.adapter = adapter
////
////        fetchBidsPlaced(userIdToUse)
//
//
//    private fun navigateToSelectProductFragment(bidId: String) {
//        Log.d(TAG, "Navigating to SelectProduct for replacing bid: $bidId")
//        // Find the bid object to potentially pass existing offered items
//        val bidToReplace = bidList.find { it.bidId == bidId }
//
//        val bundle = Bundle().apply {
//            putString("bidId", bidId)
//            // Pass the item being bid ON (requested item) from the bid if needed by SelectProductFragment
//            // You might need to fetch its details first if not already available
//            // putParcelable("selectedProduct", /* Fetch or get requested item for bidToReplace */)
//            putBoolean("isReplacing", true)
//            // Pass existing offered IDs to pre-select in SelectProductFragment
//            putStringArray("initialOfferedItemIds", bidToReplace?.offeredItemIds?.toTypedArray())
//        }
//
//        try {
//            // Ensure this navigation action ID is correct for your graph
//            findNavController().navigate(
//                R.id.action_bidsFragment_to_selectProductFragment, // Adjust if needed
//                bundle
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "Navigation to SelectProductFragment failed", e)
//            Toast.makeText(context,"Could not start offer replacement.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun fetchBidsPlaced(userId: String) {
//        Log.d(TAG, "Fetching bids placed by user: $userId")
//        binding.tvNoBids.visibility = View.GONE // Hide initially
//        // Consider adding a ProgressBar
//        // binding.progressBarBidsPlaced.visibility = View.VISIBLE
//
//        val userBidsRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("bids").child("placed")
//
//        userBidsRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val fetchedBidIds = snapshot.children.mapNotNull { it.key }
//                Log.d(TAG, "Found ${fetchedBidIds.size} placed bid IDs for user $userId")
//
//                if (fetchedBidIds.isEmpty()) {
//                    bidList.clear()
//                    adapter.notifyDataSetChanged()
//                    binding.tvNoBids.visibility = View.VISIBLE
//                    // binding.progressBarBidsPlaced.visibility = View.GONE
//                    return
//                }
//
//                val bidsDataMap = mutableMapOf<String, Bid?>()
//                var fetchCounter = fetchedBidIds.size
//
//                fetchedBidIds.forEach { bidId ->
//                    val bidRef = FirebaseDatabase.getInstance().getReference("Bids").child(bidId)
//                    bidRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(bidSnapshot: DataSnapshot) {
//                            val bid = try {
//                                bidSnapshot.getValue(Bid::class.java)?.apply { this.bidId = bidId }
//                            } catch(e: Exception) {
//                                Log.e(TAG, "Error parsing bid $bidId", e)
//                                null
//                            }
//                            bidsDataMap[bidId] = bid // Store fetched bid (or null if error)
//
//                            fetchCounter--
//                            if (fetchCounter == 0) {
//                                // All fetches complete, process the results
//                                bidList.clear()
//                                // Add valid bids in the original order (optional)
//                                fetchedBidIds.forEach { id ->
//                                    bidsDataMap[id]?.let { validBid -> bidList.add(validBid) }
//                                }
//                                bidList.sortByDescending { it.timestamp } // Sort by time, newest first
//                                adapter.notifyDataSetChanged()
//                                binding.tvNoBids.visibility = if (bidList.isEmpty()) View.VISIBLE else View.GONE
//                                // binding.progressBarBidsPlaced.visibility = View.GONE
//                                Log.d(TAG, "Finished processing all fetched bids. Displaying ${bidList.size}.")
//                            }
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {
//                            Log.e(TAG, "Failed to fetch details for bid $bidId: ${error.message}")
//                            bidsDataMap[bidId] = null // Mark as failed
//                            fetchCounter--
//                            if (fetchCounter == 0) {
//                                // Process results even if some failed
//                                bidList.clear()
//                                fetchedBidIds.forEach { id ->
//                                    bidsDataMap[id]?.let { validBid -> bidList.add(validBid) }
//                                }
//                                bidList.sortByDescending { it.timestamp }
//                                adapter.notifyDataSetChanged()
//                                binding.tvNoBids.visibility = if (bidList.isEmpty()) View.VISIBLE else View.GONE
//                                // binding.progressBarBidsPlaced.visibility = View.GONE
//                                Log.d(TAG, "Finished processing bids with some errors. Displaying ${bidList.size}.")
//                            }
//                        }
//                    })
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "Failed to fetch placed bid IDs: ${error.message}")
//                Toast.makeText(context, "Failed to load bids.", Toast.LENGTH_SHORT).show()
//                binding.tvNoBids.visibility = View.VISIBLE
//                // binding.progressBarBidsPlaced.visibility = View.GONE
//            }
//        })
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        binding.rvBidsPlaced.adapter = null // Clear adapter reference
//        _binding = null
//    }
//}
//
//
//
//
