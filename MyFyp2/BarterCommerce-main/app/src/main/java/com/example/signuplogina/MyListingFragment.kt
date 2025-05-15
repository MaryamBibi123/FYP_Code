package com.example.signuplogina

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.signuplogina.R

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Or viewModels({requireParentFragment()})
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.*
import com.example.signuplogina.ChildBidAdapter
import com.example.signuplogina.MyListingAdapter
import com.example.signuplogina.databinding.FragmentMyListingBinding
import com.example.signuplogina.fragments.UserProfileFragment
import com.example.signuplogina.modal.RecentChats
import com.example.signuplogina.mvvm.BidRepository // Keep if used directly
import com.example.signuplogina.mvvm.ExchangeAppViewModel // ViewModel for initiating exchange
import com.google.firebase.database.*

class MyListingFragment : Fragment() {

    private var _binding: FragmentMyListingBinding? = null
    private val binding get() = _binding!!

    private lateinit var myListingAdapter: MyListingAdapter
    private val itemListFromDb = mutableListOf<Item>()
    private val displayedItemList = mutableListOf<Item>()

    private lateinit var userIdToDisplayListingsFor: String
    private val currentLoggedInUserId = Utils.getUidLoggedIn()

    private val databaseRef = FirebaseDatabase.getInstance().reference
    private val TAG = "MyListingFragment"
    private var isViewingOwnListings: Boolean = false

    // ViewModel instance - adjust scoping as needed
    private val viewModel: ExchangeAppViewModel by activityViewModels()
    private lateinit var bidsRepo: BidRepository // For fetching full Bid object

    companion object {
        private const val ARG_USER_ID = "userId"
        @JvmStatic
        fun newInstance(userId: String?): MyListingFragment {
            val fragment = MyListingFragment()
            userId?.let {
                val args = Bundle(); args.putString(ARG_USER_ID, it); fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated. Args: ${arguments}")

        bidsRepo = BidRepository() // Initialize

        val passedUserId = arguments?.getString(ARG_USER_ID)
        userIdToDisplayListingsFor = passedUserId ?: currentLoggedInUserId ?: run {
            Log.e(TAG, "User ID missing."); Toast.makeText(context, "User ID not found.", Toast.LENGTH_SHORT).show()
            binding.tvNoListings?.isVisible = true; binding.tvNoListings?.text = "User not identified."
            return
        }
        isViewingOwnListings = (userIdToDisplayListingsFor == currentLoggedInUserId)
        Log.d(TAG, "Listings for: $userIdToDisplayListingsFor. Is own: $isViewingOwnListings")

        setupRecyclerView()
        fetchMyListings(userIdToDisplayListingsFor)
        observeViewModelForExchangeInitiation() // Call observer setup
    }

    private fun setupRecyclerView() {
        myListingAdapter = MyListingAdapter(
            items = displayedItemList,
            isOwnerViewing = isViewingOwnListings,
            onItemClicked = { item -> navigateToItemDetailsFragment(item, isViewingOwnListings) },
            onBidReceivedClicked = { item, childRecyclerView, expandableLayout, button ->
                if (isViewingOwnListings && item.available && item.status.equals("approved", ignoreCase = true)) {
                    handleBidReceivedClick(item, childRecyclerView, expandableLayout, button)
                } else { Log.w(TAG, "View Bids clicked on invalid item: ${item.id}") }
            }
        )
        binding.rvMyListings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyListings.adapter = myListingAdapter
    }

    private fun fetchMyListings(userId: String) {
        Log.d(TAG, "Fetching listings for user: $userId")
        binding.progressBarListings?.visibility = View.VISIBLE
        binding.tvNoListings?.visibility = View.GONE

        val query = databaseRef.child("Items").orderByChild("userId").equalTo(userId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBarListings?.visibility = View.GONE
                val tempFetchedItems = mutableListOf<Item>()
                if (snapshot.exists()) {
                    snapshot.children.forEach { s -> s.getValue(Item::class.java)?.let { i -> i.id = s.key ?: ""; tempFetchedItems.add(i) } }
                }
                itemListFromDb.clear(); itemListFromDb.addAll(tempFetchedItems)
                filterAndDisplayItems()
            }
            override fun onCancelled(error: DatabaseError) {
                binding.progressBarListings?.visibility = View.GONE; Log.e(TAG, "Fetch listings error: ${error.message}")
                if(_binding != null) { Toast.makeText(context, "Failed to load listings.", Toast.LENGTH_SHORT).show(); binding.tvNoListings?.text = "Failed to load listings."; binding.tvNoListings?.isVisible = true }
            }
        })
    }

    private fun filterAndDisplayItems() {
        if (_binding == null) return
        displayedItemList.clear()
        if (isViewingOwnListings) {
            displayedItemList.addAll(itemListFromDb)
        } else {
            displayedItemList.addAll(itemListFromDb.filter { it.status.equals("approved", ignoreCase = true) && it.available })
        }
        displayedItemList.sortByDescending { it.details.timestamp }
        myListingAdapter.updateList(displayedItemList)
        binding.tvNoListings?.isVisible = displayedItemList.isEmpty()
        if(displayedItemList.isEmpty()) binding.tvNoListings?.text = if (isViewingOwnListings) "You have no items listed." else "No active items from this user."
    }

    private fun navigateToItemDetailsFragment(item: Item, isOwnListingCurrently: Boolean) {
        if (!isOwnListingCurrently && (!item.available || !item.status.equals("approved", ignoreCase = true))) {
            Toast.makeText(context, "Item not available for details.", Toast.LENGTH_SHORT).show(); return
        }
        val bundle = Bundle().apply {
            putParcelable("ITEM_DETAILS", item); putBoolean("IS_FROM_MY_LISTINGS", isOwnListingCurrently)
        }
        try {
            val navController = findNavController()
            if (parentFragment is BidsFragment) navController.navigate(R.id.action_bidsFragment_to_itemDetailsFragment, bundle)
            else if (parentFragment is UserProfileFragment) navController.navigate(R.id.action_userProfileFragment_to_ItemDetailsFragment, bundle)
            else Log.e(TAG, "Unknown host for ItemDetails nav: ${parentFragment?.javaClass?.simpleName}")
        } catch (e: Exception) { Log.e(TAG, "Nav to ItemDetails failed.", e) }
    }

    private fun handleBidReceivedClick(
        itemBeingBidOn: Item, childRecyclerView: RecyclerView, expandableLayout: View, buttonView: Button
    ) {
        val isCurrentlyVisible = expandableLayout.visibility == View.VISIBLE
        val context = requireContext()
        expandableLayout.visibility = if (isCurrentlyVisible) View.GONE else View.VISIBLE
        buttonView.text = if (isCurrentlyVisible) context.getString(R.string.view_bids_button) else context.getString(R.string.hide_bids_button)

        if (expandableLayout.visibility == View.VISIBLE) {
            childRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            fetchBidsForItem(itemBeingBidOn.id) { bidPairs ->
                if (_binding == null) return@fetchBidsForItem
                val childAdapter = ChildBidAdapter(
                    bidPairs.ifEmpty { emptyList() },
                    onViewOfferDetailsClicked = { bid ->
                        navigateToOfferedItemsDetails(bid, true, itemBeingBidOn.id)
                    },
                    onManageBidClicked = { bidIdFromAdapter, _ -> // firstOfferedItemName might not be needed now
                        Log.d(TAG, "'Manage Bid' clicked for bidId: $bidIdFromAdapter")
                        showGeneralLoading(true) // Show general loading
                        buttonView.isEnabled = false // Temporarily disable the "View Bids" button

                        bidsRepo.getBidById(bidIdFromAdapter,
                            onSuccess = { fullBidObject ->
                                if (_binding == null) { showGeneralLoading(false); buttonView.isEnabled = true; return@getBidById }
                                if (fullBidObject.status == "pending") {
                                    val dialogBuilder = AlertDialog.Builder(requireContext())
                                    dialogBuilder.setTitle("Start Exchange Chat")
                                    val input = EditText(requireContext())
                                    val defaultRoomName = "Chat for '${itemBeingBidOn.details.productName}'"
                                    input.setText(defaultRoomName)
                                    dialogBuilder.setView(input)
                                    dialogBuilder.setPositiveButton("Start Chat") { dialog, _ ->
                                        val roomName = input.text.toString().trim()
                                        if (roomName.isNotEmpty()) {
                                            viewModel.initiateExchange(fullBidObject, roomName)
                                        } else {
                                            Toast.makeText(context, "Room name required.", Toast.LENGTH_SHORT).show()
                                            showGeneralLoading(false); buttonView.isEnabled = true
                                        }
                                        dialog.dismiss()
                                    }
                                    dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                                        showGeneralLoading(false); buttonView.isEnabled = true; dialog.cancel()
                                    }
                                    dialogBuilder.setOnDismissListener {
                                        if(viewModel.isLoadingInitiation.value == false) {
                                            showGeneralLoading(false); buttonView.isEnabled = true
                                        }
                                    }
                                    dialogBuilder.show()
                                } else if (fullBidObject.status == "started" || fullBidObject.status == "agreement_reached") {
                                    Log.d(TAG, "Bid ${fullBidObject.bidId} already started. Navigating to chat.")
                                    viewModel.createRoomIfNotExistsInternal(fullBidObject, "Existing Exchange") { success, actualRoomName, friendDetails ->
                                        showGeneralLoading(false); buttonView.isEnabled = true
                                        if (success && friendDetails != null) {
                                            val recentChat = RecentChats(bidId = fullBidObject.bidId, roomName = actualRoomName, friendid = friendDetails.first, name = friendDetails.second, friendsimage = friendDetails.third)
                                            navigateToExchangeRoomWithRecentChat(recentChat)
                                        } else { Toast.makeText(context, "Could not open existing chat.", Toast.LENGTH_SHORT).show() }
                                    }
                                } else {
                                    showGeneralLoading(false); buttonView.isEnabled = true
                                    Toast.makeText(context, "Cannot manage bid (Status: ${fullBidObject.status}).", Toast.LENGTH_LONG).show()
                                }
                            },
                            onFailure = { error ->
                                if(_binding != null) {showGeneralLoading(false); buttonView.isEnabled = true; Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()}
                            }
                        )
                    },
                    isCurrentUserListingOwner = isViewingOwnListings
                )
                childRecyclerView.adapter = childAdapter
                if(bidPairs.isEmpty()) Toast.makeText(context, "No active bids.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModelForExchangeInitiation() {
        viewModel.isLoadingInitiation.observe(viewLifecycleOwner) { isLoading ->
            // This observer is primarily for fragment-wide loading indication if initiateExchange is lengthy.
            // The showGeneralLoading calls within onManageBidClicked provide more immediate feedback.
            Log.d(TAG, "VM isLoadingInitiation: $isLoading")
            // showGeneralLoading(isLoading) // You could use this if you remove local showGeneralLoading calls
        }

        // In MyListingFragment.kt -> observeExchangeInitiation()

        viewModel.exchangeInitiationResult.observe(viewLifecycleOwner) { pairData ->
            // This lambda is already guaranteed to be called only once per new event
            // because _exchangeInitiationResult is a SingleLiveEvent.
            // 'pairData' is the actual Pair<Boolean, ExchangeInitiationData?>.

            showGeneralLoading(false) // Always hide general loading after an attempt

            val (success, data) = pairData // Destructure the pair

            if (success && data != null) {
                Log.d(TAG, "Exchange initiated. Navigating. Bid: ${data.bidId}")
                val recentChat = RecentChats(
                    bidId = data.bidId, roomName = data.actualRoomName, friendid = data.friendIdForChat,
                    name = data.friendName, friendsimage = data.friendImage
                    // Initialize other RecentChats fields as needed
                )
                navigateToExchangeRoomWithRecentChat(recentChat)
            } else {
                Log.e(TAG, "Exchange initiation failed (ViewModel signal). Check initiationError LiveData for details.")
                // The specific error message is typically observed from a separate LiveData like viewModel.initiationError
            }
        }

        viewModel.initiationError.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearInitiationError()
                showGeneralLoading(false) // Hide general loading on error
            }
        }
    }

    private fun showGeneralLoading(isLoading: Boolean) {
        if (_binding != null) {
            // Assuming you have a ProgressBar in fragment_my_listing.xml with id: progressBarOverallMyListing
            binding.progressBarListings?.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d(TAG, "General loading (MyListing): $isLoading")
        }
    }

    private fun navigateToExchangeRoomWithRecentChat(recentChat: RecentChats) {
        Log.d(TAG, "Navigating to Exchange Room: ${recentChat.bidId}")
        if (!isAdded || context == null) return
        try {
            val action = BidsFragmentDirections.actionBidsFragmentToHomeExchangeFragment(recentChat.bidId,recentChat.roomName)
            parentFragment?.findNavController()?.navigate(action) ?: findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "Nav to exchange room failed.", e)
            Toast.makeText(context, "Error navigating to exchange.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchBidsForItem(itemId: String, callback: (List<Pair<Bid, String>>) -> Unit) {
        val query = databaseRef.child("Bids").orderByChild("itemId").equalTo(itemId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bidPairs = mutableListOf<Pair<Bid, String>>()
                if (snapshot.exists()) {
                    snapshot.children.forEach { s ->
                        s.getValue(Bid::class.java)?.let { b ->
                            if (b.status == "pending" || b.status == "start") {
                                b.bidId = s.key ?: ""; bidPairs.add(Pair(b, b.bidId))
                            }
                        }
                    }
                }
                callback(bidPairs)
            }
            override fun onCancelled(error: DatabaseError) { callback(emptyList()); Log.e(TAG, "Fetch bids error: ${error.message}") }
        })
    }

    private fun navigateToOfferedItemsDetails(bid: Bid, isCurrentUserTheBidReceiver: Boolean, requestedItemId: String) {
        if (context == null || !isAdded) return
        try {
            val action = BidsFragmentDirections.actionBidsFragmentToOfferedItemsDetailsFragment(
                bid.bidId, bid.offeredItemIds.toTypedArray(), isCurrentUserTheBidReceiver, requestedItemId, !isViewingOwnListings
            )
            parentFragment?.findNavController()?.navigate(action) ?: findNavController().navigate(action)
        } catch(e: Exception) { Log.e(TAG, "Nav to OfferedItemsDetails failed", e) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        binding.rvMyListings.adapter = null
        _binding = null
    }
}




//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.core.view.isVisible // For View.isVisible
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.signuplogina.*
//import com.example.signuplogina.ChildBidAdapter
//import com.example.signuplogina.MyListingAdapter
//import com.example.signuplogina.databinding.FragmentMyListingBinding
//import com.example.signuplogina.fragments.UserProfileFragment
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//
//class MyListingFragment : Fragment() {
//
//    private var _binding: FragmentMyListingBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var myListingAdapter: MyListingAdapter
//    private val itemListFromDb = mutableListOf<Item>() // Holds all items for the user from DB
//    private val displayedItemList =
//        mutableListOf<Item>() // Holds items to actually display after filtering
//
//    private lateinit var userIdToDisplayListingsFor: String
//    private val currentLoggedInUserId = Utils.getUidLoggedIn()
//
//    private val databaseRef = FirebaseDatabase.getInstance().reference
//    private val TAG = "MyListingFragment"
//
//    // Flag to determine if the current logged-in user is viewing THEIR OWN listings.
//    private var isViewingOwnListings: Boolean = false
//
//    companion object {
//        private const val ARG_USER_ID = "userId"
//
//        @JvmStatic
//        fun newInstance(userId: String?): MyListingFragment { // Allow nullable for current user's tab
//            val fragment = MyListingFragment()
//            userId?.let { // Only add arguments if a userId is actually passed
//                val args = Bundle()
//                args.putString(ARG_USER_ID, it)
//                fragment.arguments = args
//            }
//            return fragment
//        }
//    }
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentMyListingBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d(TAG, "onViewCreated called. Arguments: ${arguments}")
//
//        val passedUserId = arguments?.getString(ARG_USER_ID) // Use constant
//        userIdToDisplayListingsFor = passedUserId ?: currentLoggedInUserId
//
//        isViewingOwnListings = (userIdToDisplayListingsFor == currentLoggedInUserId)
//        Log.d(
//            TAG,
//            "Displaying listings for user ID: $userIdToDisplayListingsFor. Is viewing own: $isViewingOwnListings"
//        )
//
//        setupRecyclerView() // Call this before fetchMyListings
//        fetchMyListings(userIdToDisplayListingsFor)
//    }
//
//    private fun setupRecyclerView() {
//        Log.d(TAG, "Setting up My Listings RecyclerView")
//        // Pass isViewingOwnListings to the adapter
//        myListingAdapter = MyListingAdapter(
//            items = displayedItemList, // Adapter will work with the displayedItemList
//            isOwnerViewing = isViewingOwnListings, // Pass the crucial flag
//            onItemClicked = { item ->
//                navigateToItemDetailsFragment(item, isViewingOwnListings)
//            },
//            onBidReceivedClicked = { item, childRecyclerView, expandableLayout, button ->
//                // This button is only active if isOwnerViewing=true AND item is approved/available
//                // The adapter's bind logic should already handle enabling/disabling the button itself.
//                // This callback is only invoked if the button was clickable.
//                if (isViewingOwnListings) { // Double check, though adapter should gate this
//                    handleBidReceivedClick(item, childRecyclerView, expandableLayout, button)
//                } else {
//                    Log.w(
//                        TAG,
//                        "onBidReceivedClicked triggered for non-owned item, should not happen if button is hidden by adapter."
//                    )
//                }
//            }
//        )
//        binding.rvMyListings.layoutManager = LinearLayoutManager(requireContext())
//        binding.rvMyListings.adapter = myListingAdapter
//        Log.d(TAG, "My Listings RecyclerView setup complete.")
//    }
//
//    private fun fetchMyListings(userId: String) {
//        Log.d(TAG, "Fetching listings for user: $userId. Viewing own: $isViewingOwnListings")
//        binding.progressBarListings?.visibility = View.VISIBLE // Add ID progressBarListings to XML
//        binding.tvNoListings?.visibility = View.GONE
//
//        val itemsRef = databaseRef.child("Items")
//        val query = itemsRef.orderByChild("userId").equalTo(userId)
//
//        query.addListenerForSingleValueEvent(object : ValueEventListener {
//            @SuppressLint("NotifyDataSetChanged")
//            override fun onDataChange(snapshot: DataSnapshot) {
//                binding.progressBarListings?.visibility = View.GONE
//                val tempFetchedItems = mutableListOf<Item>()
//                if (snapshot.exists()) {
//                    for (itemSnapshot in snapshot.children) {
//                        try {
//                            itemSnapshot.getValue(Item::class.java)?.let { item ->
//                                item.id = itemSnapshot.key ?: ""
//                                tempFetchedItems.add(item)
//                            }
//                        } catch (e: Exception) {
//                            Log.e(TAG, "Error parsing listing item ${itemSnapshot.key}", e)
//                        }
//                    }
//                } else {
//                    Log.d(TAG, "No listings found in DB for user $userId")
//                }
//
//                // Clear previous lists and apply filter
//                itemListFromDb.clear()
//                itemListFromDb.addAll(tempFetchedItems)
//                filterAndDisplayItems()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                binding.progressBarListings?.visibility = View.GONE
//                Log.e(TAG, "Error fetching listings: ${error.message}")
//                if (_binding != null) {
//                    Toast.makeText(context, "Failed to load listings.", Toast.LENGTH_SHORT).show()
//                    binding.tvNoListings?.text = "Failed to load listings."
//                    binding.tvNoListings?.isVisible = true
//                }
//            }
//        })
//    }
//
//    private fun filterAndDisplayItems() {
//        if (_binding == null) return
//
//        displayedItemList.clear()
//        if (isViewingOwnListings) {
//            // Owner sees all their items
//            displayedItemList.addAll(itemListFromDb)
//            Log.d(TAG, "Owner viewing: Displaying all ${itemListFromDb.size} fetched items.")
//        } else {
//            // Other users see only approved and available items
//            val filtered = itemListFromDb.filter { item ->
//                val condition = item.status.equals("approved", ignoreCase = true) && item.available
//                if (!condition) Log.d(
//                    TAG,
//                    "Other user viewing: SKIPPED item ${item.id} (Status: ${item.status}, Available: ${item.available})"
//                )
//                condition
//            }
//            displayedItemList.addAll(filtered)
//            Log.d(TAG, "Other user viewing: Displaying ${filtered.size} approved/available items.")
//        }
//
//        displayedItemList.sortByDescending { it.details.timestamp }
//        myListingAdapter.updateList(displayedItemList) // Update adapter with the final list
//
//        if (displayedItemList.isEmpty()) {
//            binding.tvNoListings?.text =
//                if (isViewingOwnListings) "You haven't listed any items yet." else "This user has no active listings."
//            binding.tvNoListings?.isVisible = true
//        } else {
//            binding.tvNoListings?.isVisible = false
//        }
//    }
//
//
//    private fun navigateToItemDetailsFragment(item: Item, isOwnListingCurrently: Boolean) {
//        if (!isOwnListingCurrently && (!item.available || !item.status.equals(
//                "approved",
//                ignoreCase = true
//            ))
//        ) {
//            Log.w(
//                TAG,
//                "Attempted to navigate to details of unavailable/non-approved item of another user: ${item.id}"
//            )
//            Toast.makeText(
//                context,
//                "This item is currently not available for viewing details.",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//
//        Log.d(
//            TAG,
//            "Navigating to details for item: ${item.id}, isOwnListingCurrently: $isOwnListingCurrently"
//        )
//        val bundle = Bundle().apply {
//            putParcelable("ITEM_DETAILS", item)
//            putBoolean("IS_FROM_MY_LISTINGS", isOwnListingCurrently)
//        }
//
//        try {
//            val navController = findNavController()
//            // Determine the correct navigation action based on where MyListingFragment is hosted
//            if (parentFragment is BidsFragment) {
//                navController.navigate(R.id.action_bidsFragment_to_itemDetailsFragment, bundle)
//            } else if (parentFragment is UserProfileFragment) {
//                navController.navigate(
//                    R.id.action_userProfileFragment_to_ItemDetailsFragment,
//                    bundle
//                )
//            } else {
//                Log.e(
//                    TAG,
//                    "Cannot determine navigation path for ItemDetails. Parent: ${parentFragment?.javaClass?.simpleName}"
//                )
//                // Fallback or default navigation if MyListingFragment could be a top-level destination itself
//                // This requires action_myListingFragment_to_itemDetailsFragment to be defined:
//                // navController.navigate(R.id.action_myListingFragment_to_itemDetailsFragment, bundle)
//                Toast.makeText(context, "Navigation error.", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Navigation to ItemDetails failed.", e)
//            Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun handleBidReceivedClick(
//        itemBeingBidOn: Item,
//        childRecyclerView: RecyclerView,
//        expandableLayout: View,
//        button: Button
//    ) {
//        val isVisible = expandableLayout.visibility == View.VISIBLE
//        val context = requireContext() // Safe access
//
//        expandableLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
//        button.text =
//            if (isVisible) context.getString(R.string.view_bids_button) else context.getString(R.string.hide_bids_button)
//
//        if (expandableLayout.visibility == View.VISIBLE) { // Check if expanding now
//            childRecyclerView.layoutManager =
//                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//            Log.d(TAG, "Fetching bids for item ${itemBeingBidOn.id}")
//
//            // isViewingOwnListings is already correctly set in onViewCreated
//            fetchBidsForItem(itemBeingBidOn.id) { bidPairs ->
//                if (_binding == null) return@fetchBidsForItem
//                if (bidPairs.isEmpty()) {
//                    Toast.makeText(context, "No active bids received yet.", Toast.LENGTH_SHORT)
//                        .show()
//                    childRecyclerView.adapter = ChildBidAdapter(emptyList(), {}, { _, _ -> }, false)
//                } else {
//                    val adapter = ChildBidAdapter(
//                        bidPairs,
//                        onViewOfferDetailsClicked = { bid ->
//                            // When owner views bids on their item, they ARE the receiver
//                            navigateToOfferedItemsDetails(bid, true, itemBeingBidOn.id)
//                        },
//                        onManageBidClicked = { bidId, firstOfferedItemName ->
//                            // Show dialog for creating exchange room
//                            val dialogBuilder =
//                                AlertDialog.Builder(requireContext()) // ... your dialog setup ...
//                            dialogBuilder.setTitle("Start Exchange Chat")
//                            val input = EditText(requireContext())
//                            input.hint = "Chat regarding '$firstOfferedItemName'"
//                            input.setText("Chat for my item: ${itemBeingBidOn.details.productName}")
//                            dialogBuilder.setView(input)
//                            dialogBuilder.setPositiveButton("Start Chat") { dialog, _ ->
//                                val roomName = input.text.toString().trim()
//                                if (roomName.isNotEmpty()) {
//                                    navigateToExchangeRoom(bidId, roomName)
//                                }
//                                dialog.dismiss()
//                            }
//                            dialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
//                            dialogBuilder.show()
//                        },
//                        isCurrentUserListingOwner = isViewingOwnListings // True, because this is owner's item
//                    )
//                    childRecyclerView.adapter = adapter
//                }
//            }
//        }
//    }
//
//
//        private fun navigateToExchangeRoom(bidId: String, roomName: String) {
//        Log.d(TAG, "Navigating to Exchange Room for bid $bidId, room: $roomName")
//        try {
//            // IMPORTANT: Adjust this action based on where MyListingFragment is actually hosted
//            // This assumes it's hosted under a fragment identified by R.id.bidsFragment
//            val action =
//                BidsFragmentDirections.actionBidsFragmentToHomeExchangeFragment(bidId, roomName)
//            findNavController().navigate(action)
//        } catch (e: Exception) {
//            Log.e(TAG, "Navigation to exchange room failed.", e)
//            Toast.makeText(context, "Error navigating to exchange.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
////    // --- UPDATED: fetchBidsForItem to query /Bids ---
//    private fun fetchBidsForItem(
//        itemId: String,
//        callback: (List<Pair<Bid, String>>) -> Unit
//    ) {
//        val bidsRef = databaseRef.child("Bids") // Use rootRef.child("Bids")
//        // Query for bids WHERE the 'itemId' field matches the listing's itemId
//        val query = bidsRef.orderByChild("itemId").equalTo(itemId)
//
//        query.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val bidPairs = mutableListOf<Pair<Bid, String>>()
//                if (!snapshot.exists()) {
//                    Log.d(TAG, "No bids found directly under /Bids for itemId $itemId")
//                    callback(emptyList())
//                    return
//                }
//                Log.d(TAG, "Found ${snapshot.childrenCount} bids under /Bids for itemId $itemId")
//                snapshot.children.forEach { bidSnapshot ->
//                    try {
//                        val bid = bidSnapshot.getValue(Bid::class.java)
//                        val bidId = bidSnapshot.key
//                        if (bid != null && bidId != null) {
//                            // Filter for relevant bid statuses (e.g., only active ones)
//                            if (bid.status == "pending" || bid.status == "start") {
//                                bid.bidId = bidId // Ensure bidId is populated
//                                bidPairs.add(Pair(bid, bidId))
//                            }
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error parsing bid ${bidSnapshot.key} for item $itemId", e)
//                    }
//                }
//                callback(bidPairs)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "Error fetching bids for item $itemId: ${error.message}")
//                callback(emptyList())
//            }
//        })
//    }
////    // --- End UPDATED fetchBidsForItem ---
////
//    private fun navigateToOfferedItemsDetails(
//        bid: Bid,
//        isCurrentUserTheBidReceiver: Boolean,
//        requestedItemId: String
//    ) {
//        if (context == null || !isAdded) return
//        try {
//            // This action must originate from the NavHost that contains MyListingFragment
//            // e.g., if MyListingFragment is in a ViewPager hosted by BidsFragment
//            val action = BidsFragmentDirections.actionBidsFragmentToOfferedItemsDetailsFragment(
//                bidId = bid.bidId,
//                offeredItemIds = bid.offeredItemIds.toTypedArray(),
//                isCurrentUserBidReceiver = isCurrentUserTheBidReceiver,
//                requestedItemId = requestedItemId,
//                isFromOtherUserProfile = !isViewingOwnListings // Pass context if viewing other's profile's item's bids
//            )
//            // Navigate using the NavController of the PARENT fragment (BidsFragment or UserProfileFragment)
//            parentFragment?.findNavController()?.navigate(action)
//                ?: findNavController().navigate(action) // Fallback (might not be correct if nested)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Nav to OfferedItemsDetails failed", e)
//            Toast.makeText(context, "Could not view offer details", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//}
//
//
//
