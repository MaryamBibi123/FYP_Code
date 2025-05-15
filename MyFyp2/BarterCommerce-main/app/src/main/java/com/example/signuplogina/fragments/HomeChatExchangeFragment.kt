package com.example.signuplogina.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Switch
import androidx.navigation.fragment.findNavController
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
// import androidx.appcompat.widget.Toolbar // Using MaterialToolbar potentially via binding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.signuplogina.* // Import Item, Bid, Utils, R etc.
import com.example.signuplogina.adapter.ExchangeItemsAdapter
import com.example.signuplogina.adapter.MessageAdapter
import com.example.signuplogina.databinding.FragmentExchangeChatHomeBinding
import com.example.signuplogina.modal.ExchangeAgreement
import com.example.signuplogina.modal.Feedback
import com.example.signuplogina.modal.FeedbackTags
import com.example.signuplogina.modal.Messages
import com.example.signuplogina.mvvm.BidRepository
import com.example.signuplogina.mvvm.ExchangeAppViewModel
import com.example.signuplogina.mvvm.FirebaseAgreementManager
import com.example.signuplogina.mvvm.ItemsRepo
import com.example.signuplogina.mvvm.MessageRepo
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.FirebaseDatabase
// No need for these if Repos handle DB access
// import com.google.firebase.database.DataSnapshot
// import com.google.firebase.database.DatabaseError
// import com.google.firebase.database.FirebaseDatabase
// import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.IllegalArgumentException // For navigation error

class HomeChatExchangeFragment : Fragment() {

    lateinit var args: HomeChatExchangeFragmentArgs
    private var _binding: FragmentExchangeChatHomeBinding? = null
    private val binding get() = _binding!!
    lateinit var viewModel: ExchangeAppViewModel
    lateinit var messageAdapter: MessageAdapter // Changed name
    lateinit var currentUser: String
    private var isItemsVisible = false

    lateinit var roomId: String
    lateinit var itemsRepo: ItemsRepo
    lateinit var bidsRepo: BidRepository
    lateinit var messageRepo: MessageRepo
    lateinit var firebaseAgreement: FirebaseAgreementManager
    lateinit var bidId: String
    lateinit var receiverId: String
    var agreement: ExchangeAgreement? = null
    private val TAG = "HomeChatExchangeFrag"
    private var isDataLoaded = false // Flag to fetch items only once

    // --- UI State Variables (Re-added) ---
    private var interactionEnabled = false // Internal flag managed by enable/disableInteraction
    private var bannerIsForcingDisable = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_exchange_chat_home, container, false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        // Initialize Repos and Firebase Manager first
        firebaseAgreement = FirebaseAgreementManager()
        itemsRepo = ItemsRepo()
        bidsRepo = BidRepository()
        messageRepo = MessageRepo()
        viewModel = ViewModelProvider(this)[ExchangeAppViewModel::class.java]
        binding.viewModel = viewModel
        observeBannerState()

        observeRatingReportSubmission()
        // --- Get arguments and user ID ---
        args = HomeChatExchangeFragmentArgs.fromBundle(requireArguments())
        currentUser = Utils.getUidLoggedIn()
        bidId = args.recentchats.bidId ?: run { /* Handle missing bidId */
            Log.e(TAG, "Bid ID null")
            Toast.makeText(context, "Error: Bid ID missing.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }
        receiverId = args.recentchats.friendid.orEmpty()
        if (receiverId.isEmpty()) { /* Handle missing receiverId */
            Log.e(TAG, "Receiver ID null")
            Toast.makeText(context, "Error: Participant missing.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }
        roomId = "$bidId-${listOf(currentUser, receiverId).sorted().joinToString("")}"
        Log.d(TAG, "Init Bid: $bidId, Room: $roomId, Receiver: $receiverId")



        if (currentUser.isNotBlank() && receiverId.isNotBlank() && bidId.isNotBlank()) {
            viewModel.loadDataForBannerAndInteraction(currentUser, receiverId, bidId)
        } else {
            Log.e(TAG, "Critical IDs for banner/exchange are missing!")
            binding.blockStatusBannerCard?.visibility = View.VISIBLE
            binding.tvBlockStatusBannerMessage?.text = "Error: Invalid exchange context."
            binding.blockStatusBannerCard?.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.overlay_rejected_background
                )
            )
            if (interactionEnabled) {
                disableInteraction()
            }
            return
        }

        // --- Initial Bid Status Check ---
//        checkInitialBidStatus(bidId) { isValid ->
//            if (_binding == null) return@checkInitialBidStatus // View destroyed check
//
//            if (isValid) {
//                Log.d(TAG, "Bid valid. Setup UI.")
//                setupToolbar(view)
//                setupItemsExpansion() // Use Corrected Version
//                setupActionButtons()
//                setupMessageObserver()
//                checkAgreementAndRatingStatus()
//                viewModel.createExchangeRoomIfNotExists(
//                    bidId,
//                    args.recentchats.roomName.orEmpty()
//                ) { /* ... */ }
//            } else {
//                Log.w(TAG, "Bid invalid. UI interaction disabled.")
//                // handleInvalidBidState (called by checkInitialBidStatus) should have disabled UI
//                setupToolbar(view) // Still setup toolbar to show user info and allow back nav
//                binding.cardSeeItems.visibility = View.GONE // Hide "See Items" if invalid
//            }

        checkAgreementAndRatingStatus()

        checkInitialBidStatus(bidId) { isBidIntrinsicallyValid ->
            if (_binding == null) return@checkInitialBidStatus

            Log.d(
                TAG,
                "checkInitialBidStatus onComplete. isBidIntrinsicallyValid: $isBidIntrinsicallyValid, bannerIsForcingDisable: $bannerIsForcingDisable"
            )

            if (bannerIsForcingDisable) {
                // Banner says disable, so ensure interactions are off, regardless of bid validity.
                // Your `disableInteraction()` was already called by the banner observer.
                Log.d(TAG, "Banner is forcing disable. Interactions remain disabled.")
                // Setup toolbar and hide "See Items" if bid was also intrinsically invalid
                if (!isBidIntrinsicallyValid) {
                    setupToolbar(view)
                    binding.cardSeeItems.visibility = View.GONE
                } else {
                    setupToolbar(view) // Still setup toolbar
                }
            } else {
                // Banner is NOT forcing disable. Rely on bid's own validity.
                if (isBidIntrinsicallyValid) {
                    Log.d(
                        TAG,
                        "Bid is valid AND banner allows. Setting up full UI and enabling interactions."
                    )
                    // The enableInteraction() call is already inside your original checkInitialBidStatus logic for a valid bid.
                    // We just need to ensure all setup calls happen here.
                    setupToolbar(view)
                    setupItemsExpansion()
                    setupActionButtons()
                    setupMessageObserver()
                    checkAgreementAndRatingStatus()
                    viewModel.createExchangeRoomIfNotExists(
                        bidId,
                        args.recentchats.roomName.orEmpty()
                    ) { /* ... */ }
                    // Ensure enableInteraction() was effectively called by checkInitialBidStatus if not already.
                    if (!interactionEnabled) { // If checkInitialBidStatus said valid but UI not enabled yet
                        enableInteraction()
                    }
                } else {
                    Log.w(
                        TAG,
                        "Bid is intrinsically invalid AND banner allows. UI remains disabled."
                    )
                    // handleInvalidBidState() inside checkInitialBidStatus already called disableInteraction().
                    setupToolbar(view)
                    binding.cardSeeItems.visibility = View.GONE
                }
            }
        }
    }

@SuppressLint("ResourceType")
private fun observeBannerState() {
    viewModel.effectiveBannerState.observe(viewLifecycleOwner) { bannerState ->
        if (_binding == null) return@observe

        Log.d(TAG, "observeBannerState: $bannerState")

        if (bannerState.isVisible) {
            binding.blockStatusBannerCard.visibility = View.VISIBLE
            binding.tvBlockStatusBannerMessage.text = bannerState.message
            binding.blockStatusBannerCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), bannerState.backgroundColorRes)
            )
        } else {
            binding.blockStatusBannerCard.visibility = View.GONE
        }

        val oldBannerIsForcingDisable = bannerIsForcingDisable
        bannerIsForcingDisable = bannerState.disablesAllInteractions

        if (bannerIsForcingDisable) {
            if (interactionEnabled) { // If it was previously enabled
                Log.d(TAG, "Banner is forcing disableInteraction()")
                disableInteraction() // Your existing method
            }
        } else { // Banner is NOT forcing disable
            if (oldBannerIsForcingDisable) { // If banner *was* disabling but now isn't
                Log.d(TAG, "Banner restriction lifted. Re-evaluating based on 'interactionEnabled' flag ($interactionEnabled).")
                // Interactions now depend on the underlying state set by checkInitialBidStatus
                if (interactionEnabled) { // If the bid itself is active
                    enableInteraction() // Call your existing method to ensure UI reflects enabled state
                } else {
                    // If bid is not active (e.g. completed, cancelled), ensure it stays disabled
                    // This should have been handled by checkInitialBidStatus calling handleInvalidBidState.
                    // But as a safeguard:
                    if (binding.sendBtn.isEnabled) { // Check a representative UI element
                        disableInteraction()
                    }
                }
            }}}}
            // If banner was not forcing disable and still isn't, no change needed from banner's side.
            // The state is determined by checkInitialBidStatus.




    // --- Function to check initial bid status ---
    private fun checkInitialBidStatus(bidIdToCheck: String, onComplete: (Boolean) -> Unit) {
        Log.d(TAG, "Checking initial status for bid: $bidIdToCheck")
        showProgress(true) // Show progress
        binding.exchangeStatusTextView.visibility = View.GONE

        bidsRepo.getBidById(bidIdToCheck, onSuccess = { bid ->
            if (_binding == null) {
                Log.w(
                    TAG,
                    "View destroyed"
                ); showProgress(false); onComplete(false); return@getBidById
            }
            showProgress(false) // Hide progress
            val inactiveStatuses =
                listOf("canceledByAdmin", "withdrawByBidder", "rejectedByReceiver", "completed")
            if (inactiveStatuses.contains(bid.status)) {
                Log.w(TAG, "Bid $bidIdToCheck inactive (Status: ${bid.status}).")
                handleInvalidBidState("This exchange is no longer active (Status: ${bid.status}).")
                onComplete(false)
            } else {
                Log.i(TAG, "Bid $bidIdToCheck active (Status: ${bid.status}).")
                enableInteraction() // Enable UI
                onComplete(true)
            }
        }, onFailure = { errorMessage ->
            if (_binding == null) {
                Log.w(
                    TAG,
                    "View destroyed"
                ); showProgress(false); onComplete(false); return@getBidById
            }
            showProgress(false) // Hide progress
            Log.e(TAG, "Failed fetch bid status for $bidIdToCheck: $errorMessage")
            handleInvalidBidState("Error loading exchange details.")

            onComplete(false)
        })
    }

    // --- setupToolbar (Keep as is) ---
    private fun setupToolbar(view: View) {
        val circleImageView = view.findViewById<CircleImageView>(R.id.chatImageViewUser)
        val textViewName = view.findViewById<TextView>(R.id.chatUserName)
        // Check if binding and context are valid before Glide call
        if (_binding != null && context != null) {
            Glide.with(requireContext()).load(args.recentchats.friendsimage)
                .placeholder(R.drawable.person).dontAnimate().into(circleImageView)
            textViewName.text = args.recentchats.name
        }
        binding.chatBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // --- CORRECTED setupItemsExpansion ---
    private fun setupItemsExpansion() {
        binding.exchangeItemsRecyclerView.visibility = View.GONE
        isItemsVisible = false
        // isDataLoaded = false; // Reset if needed
        val arrow = binding.arrowIcon
        arrow.rotation = 0f
        binding.cardSeeItems.visibility = View.VISIBLE // Make sure it's initially visible

        binding.cardSeeItems.setOnClickListener {
            // Use internal interaction flag
            if (!interactionEnabled) {
                Toast.makeText(context, "Exchange is not active.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isItemsVisible = !isItemsVisible
            binding.exchangeItemsRecyclerView.visibility =
                if (isItemsVisible) View.VISIBLE else View.GONE
            arrow.animate().rotation(if (isItemsVisible) 90f else 0f).start()

            if (isItemsVisible && !isDataLoaded) {
                Log.d(TAG, "Fetching items for chat display expansion...")
                showItemLoadingProgress(true) // Show item loading progress

                bidsRepo.getBidById(bidId, onSuccess = { bid ->
                    if (_binding == null) {
                        showItemLoadingProgress(false); return@getBidById
                    }
                    Log.d("fetched Bids", "$bid")

                    itemsRepo.getExchangeChatItems(
                        bid,

                        onSuccess = { requestedItem, offeredItemsList ->
                            Log.d(TAG, "Fetched Items for Bid $bidId - Requested: ${requestedItem?.id}, Offered: ${offeredItemsList.map { it.id }}")

                            if (_binding != null) {
                                showItemLoadingProgress(false)
                                val allItemsInvolvedInThisBid = mutableListOf<Item>()
                                requestedItem?.let { allItemsInvolvedInThisBid.add(it) }
                                allItemsInvolvedInThisBid.addAll(offeredItemsList)


                                val itemsToActuallyDisplayInChat = allItemsInvolvedInThisBid.filter { item ->
                                    val isAdminStatusOkForDisplay = item.status.equals("approved", ignoreCase = true) ||
                                            item.status.equals("in_negotiation", ignoreCase = true) // If you use this on Item.status too
                                    // The critical part is that ExchangeItemsAdapter will look at item.exchangeState
                                    // and item.lockedByBidId (passed as currentBidIdForContext) to display it correctly.

                                    // More lenient filter for chat display: just ensure it's not hard-removed or rejected by admin.
                                    // The individual item cards will show their specific state.
                                    val isValidForChatDisplay = !listOf("rejected", "removed_by_owner", "removed_by_admin")
                                        .contains(item.status.lowercase())

                                    if (!isValidForChatDisplay) {
                                        Log.w(TAG, "Filtering out item ${item.id} from chat display due to its admin status: ${item.status}")
                                    }
                                    isValidForChatDisplay
                                }
                                Log.d(TAG, "Items to display in chat after initial filter: ${itemsToActuallyDisplayInChat.size}")


                                if (itemsToActuallyDisplayInChat.isNotEmpty()) {
                                    setupItemsRecyclerView(itemsToActuallyDisplayInChat)
                                    isDataLoaded = true // Mark as loaded
                                } else {
                                    Log.w(TAG, "No items to display in chat expansion after filtering for bid $bidId")
                                    handleNoAvailableItems()
                                }
                            }
                        },
// ... onFailure ...
                        onFailure = { exception ->
                            if (_binding != null) {
                                showItemLoadingProgress(false)
                                Log.e(TAG, "Failed fetch item details: ${exception.message}")
                                Toast.makeText(
                                    context,
                                    "Error loading item details.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                handleNoAvailableItems() // Treat failure as no items to show
                            }
                        }
                    )
                }, onFailure = { error ->
                    if (_binding != null) {
                        showItemLoadingProgress(false)
                        Log.e(TAG, "Failed fetch bid for items: $error")
                        Toast.makeText(context, "Error loading bid details.", Toast.LENGTH_SHORT)
                            .show()
                        handleNoAvailableItems() // Treat failure as no items to show
                    }
                })
            }
        }
    }

    // Helper to handle when no available items are found/loaded
    private fun handleNoAvailableItems() {
        Log.w(TAG, "No available items to display in chat expansion for bid $bidId.")
        Toast.makeText(context, "No available items to display currently.", Toast.LENGTH_SHORT)
            .show()
        binding.exchangeItemsRecyclerView.visibility = View.GONE
        isItemsVisible = false
        binding.arrowIcon.animate().rotation(0f).start()
        isDataLoaded = true // Mark attempt as done
    }

    // --- CORRECTED setupItemsRecyclerView ---
    // Inside HomeChatExchangeFragment.kt

    private fun setupItemsRecyclerView(itemsToDisplay: List<Item>) {
        if (_binding == null) return
        Log.d(
            TAG,
            "Setting up exchange items RecyclerView with ${itemsToDisplay.size} available items."
        )

        // **** Pass currentUserId to the adapter ****
        val itemsAdapter = ExchangeItemsAdapter(
            itemsToDisplay.toMutableList(),
            Utils.getUidLoggedIn(), // Pass the logged-in user's ID
            currentBidIdForContext = this.bidId,
        ) { clickedItem ->
            // Navigation logic remains the same
            try {
                val bundle = Bundle().apply {
                    putParcelable("ITEM_DETAILS", clickedItem)
                    putBoolean("IS_VIEWING_EXCHANGE_ITEM_CONTEXT", true)
                }
                findNavController().navigate(
                    R.id.action_homeChatExchangeFragment_to_itemDetailsFragment,
                    bundle
                )
            } catch (e: Exception) {
                // ... error handling ...
                Log.e(TAG, "Navigation to ItemDetailsFragment failed from chat items.", e)
                Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.exchangeItemsRecyclerView.apply {
            // **** CHANGE LayoutManager to VERTICAL ****
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = itemsAdapter
        }
    }


    // --- setupActionButtons (Use internal interaction flag) ---
    private fun setupActionButtons() {
        binding.sendBtn.setOnClickListener {
            if (!interactionEnabled) { /* Show Toast */ return@setOnClickListener
            }
            // ... rest of send logic ...
            if (receiverId.isBlank()) {
                Log.e(TAG, "Receiver ID Blank"); return@setOnClickListener
            }
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                viewModel.message.value = messageText
                viewModel.sendMessage(
                    currentUser,
                    receiverId,
                    bidId,
                    args.recentchats.name.orEmpty(),
                    args.recentchats.friendsimage.orEmpty()
                )
                binding.editTextMessage.text.clear()
            }
        }
        binding.btnAgreement.setOnClickListener {
            if (!interactionEnabled) { /* Show Toast */ return@setOnClickListener
            }
            // ... rest of agreement nav logic ...
            try {
                val action =
                    HomeChatExchangeFragmentDirections.actionHomeChatExchangeFragmentToExchangeAgreementFragment(
                        bidId,
                        args.recentchats
                    )
                findNavController().navigate(action)
            } catch (e: Exception) {
                Log.e(TAG, "Nav Agreement failed", e)
            }
        }
        binding.btnDone.setOnClickListener {
            if (!interactionEnabled) { /* Show Toast */ return@setOnClickListener
            }
            showDoneConfirmationDialog()
        }
        binding.btnPlanMeeting.setOnClickListener {
            if (!interactionEnabled) { /* Show Toast */ return@setOnClickListener
            }
            showMeetingBottomSheet()
        }
    }

    // --- setupMessageObserver (Keep as is, but rename initRecyclerView call) ---
    private fun setupMessageObserver() {
        Log.d(TAG, "Setting up message observer for friend $receiverId and bid $bidId")
        viewModel.getMessages(receiverId, bidId)
            .observe(viewLifecycleOwner, Observer { messages ->
                if (_binding != null) {
                    Log.d(TAG, "Received ${messages.size} messages")
                    initRecyclerViewMessages(messages.toMutableList()) // Renamed call
                }
            })
    }

    // Renamed method to avoid conflict
    private fun initRecyclerViewMessages(list: MutableList<Messages>) {
        if (_binding == null) return
        messageAdapter = MessageAdapter() // Initialize here if not done elsewhere
        val layoutManager = LinearLayoutManager(context)
        binding.messagesRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        messageAdapter.setList(list)
        binding.messagesRecyclerView.adapter = messageAdapter
    }


    // --- checkAgreementAndRatingStatus (Keep as is) ---
    private fun checkAgreementAndRatingStatus() {
        // ... (Your existing implementation - seems okay) ...
        firebaseAgreement.getExchangeAgreementRef(bidId) { fetchedAgreement ->
            if (_binding == null) return@getExchangeAgreementRef // Check view
            agreement = fetchedAgreement // Store fetched agreement
            if (agreement != null) {
                if (agreement!!.status.equals("completed", ignoreCase = true)) {
                    Log.d(TAG, "Agreement completed. Disabling UI.")
                    handleInvalidBidState("This exchange has been completed.")
                    checkAndPromptRating(bidId, agreement!!, currentUser)
                } else {
                    Log.d(TAG, "Agreement status: ${agreement!!.status}.")
                }
            } else {
                Log.d(TAG, "No agreement found for bid $bidId")
            }
        }
    }

    // --- UI State Helper Functions (Re-added/Verified) ---
    private fun handleInvalidBidState(message: String) {
        if (_binding == null) return
        Log.w(TAG, "Handling Invalid Bid State: $message")
        binding.exchangeStatusTextView.text = message
        binding.exchangeStatusTextView.visibility = View.VISIBLE
        disableInteraction()
    }

    private fun disableInteraction() {
        if (_binding == null) return
        Log.d(TAG, "Disabling Interactions")
        interactionEnabled = false // Set internal flag
        binding.sendBtn.isEnabled = false
        binding.editTextMessage.isEnabled = false
        binding.btnAgreement.isEnabled = false
        binding.btnDone.isEnabled = false
        binding.btnPlanMeeting.isEnabled = false
        binding.cardSeeItems.isClickable = false
        // Make visually disabled
        binding.layoutChatbox.alpha = 0.5f
        binding.btnAgreement.alpha = 0.5f
        binding.btnDone.alpha = 0.5f
        binding.btnPlanMeeting.alpha = 0.5f
        binding.cardSeeItems.alpha = 0.5f
        // Collapse items if they were visible
        if (isItemsVisible) {
            binding.exchangeItemsRecyclerView.visibility = View.GONE
            isItemsVisible = false
            binding.arrowIcon.animate().rotation(0f).start()
        }
    }

    private fun enableInteraction() {
        if (_binding == null) return
        // Enable only if agreement isn't completed
        if (agreement?.status?.equals("completed", ignoreCase = true) != true) {
            Log.d(TAG, "Enabling Interactions")
            interactionEnabled = true // Set internal flag
            binding.sendBtn.isEnabled = true
            binding.editTextMessage.isEnabled = true
            binding.btnAgreement.isEnabled = true
            binding.btnDone.isEnabled = true
            binding.btnPlanMeeting.isEnabled = true
            binding.cardSeeItems.isClickable = true
            // Restore visual state
            binding.layoutChatbox.alpha = 1.0f
            binding.btnAgreement.alpha = 1.0f
            binding.btnDone.alpha = 1.0f
            binding.btnPlanMeeting.alpha = 1.0f
            binding.cardSeeItems.alpha = 1.0f
            binding.exchangeStatusTextView.visibility = View.GONE // Hide status message
        } else {
            Log.d(TAG, "Interaction enable skipped, agreement completed.")
            // Ensure it remains disabled if called when agreement is completed
            disableInteraction()
            binding.exchangeStatusTextView.text = "This exchange has been completed."
            binding.exchangeStatusTextView.visibility = View.VISIBLE
        }
    }


    // --- Progress Bar Helper (Add this) ---
    private fun showProgress(show: Boolean) {
        // Add a ProgressBar with id 'progressBarOverall' to your layout if you want overall loading
        binding.progressBarOverall?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showItemLoadingProgress(show: Boolean) {
        // Add a ProgressBar with id 'progressBarItemsLoading' to your layout (e.g., near the items recyclerview)
        binding.progressBarItemsLoading?.visibility = if (show) View.VISIBLE else View.GONE
    }

    // --- Existing Methods (keep as is) ---
    // Helper to check if interaction should be enabled (based on current state)
    private fun isInteractionEnabled(): Boolean {
        // Interaction is enabled only if the bid check passed and agreement isn't completed
        val agreementCompleted = agreement?.status?.equals("completed", ignoreCase = true) == true
        // We might need a flag set by checkInitialBidStatus if agreement check runs first
        // For now, assume disableInteraction() was called if status was bad initially.
        return binding.sendBtn.isEnabled && !agreementCompleted // Check a representative enabled view
    }

//    fun checkAndPromptRating(bidId: String, agreement: ExchangeAgreement, currentUserId: String) {
//        if (agreement.bidderOtpVerified && agreement.receiverOtpVerified) {
//            //check here when the second user verifies otp this wot show , untilcome back on this screen
//            val hasUserAlreadyRated = if (currentUserId == agreement.bidderId)
//                agreement.ratingGivenByBidder else agreement.ratingGivenByReceiver
//
//            if (!hasUserAlreadyRated) {
//                showCustomRatingDialog(currentUserId, bidId, agreement)
//            }
//        }
//    }

    fun checkAndPromptRating(bidId: String, agreementToCheck: ExchangeAgreement, currentUserIdForCheck: String) {
        Log.d(TAG, "checkAndPromptRating: User: $currentUserIdForCheck, Bidder: ${agreementToCheck.bidderId}, Receiver: ${agreementToCheck.receiverId}")
        Log.d(TAG, "checkAndPromptRating: BidderOTP: ${agreementToCheck.bidderOtpVerified}, ReceiverOTP: ${agreementToCheck.receiverOtpVerified}")
        Log.d(TAG, "checkAndPromptRating: BidderRated: ${agreementToCheck.ratingGivenByBidder}, ReceiverRated: ${agreementToCheck.ratingGivenByReceiver}")

        if (agreementToCheck.bidderOtpVerified && agreementToCheck.receiverOtpVerified) {
            val hasUserAlreadyRated = if (currentUserIdForCheck == agreementToCheck.bidderId) {
                agreementToCheck.ratingGivenByBidder
            } else {
                agreementToCheck.ratingGivenByReceiver
            }
            Log.d(TAG, "checkAndPromptRating: hasUserAlreadyRated for $currentUserIdForCheck = $hasUserAlreadyRated")
            if (!hasUserAlreadyRated) {
                showCustomRatingDialog(currentUserIdForCheck, bidId, agreementToCheck)
            } else {
                Log.d(TAG, "User $currentUserIdForCheck has already rated or reported.")
            }
        } else {
            Log.d(TAG, "Not prompting rating: Both OTPs not verified yet.")
        }
    }


    fun showDoneConfirmationDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Complete the Bid?")
            .setMessage("You are about to mark this exchange as done. You will see the OTP once both users confirm.")
            .setPositiveButton("OK") { _, _ ->
                handleRevealRequest(bidId, currentUser)
            }.setNegativeButton("Cancel", null).show()
    }

    // Method to initialize RecyclerView

    private fun showMeetingBottomSheet() {
        messageRepo.fetchMeetingDetails(roomId) { location, date, time ->


            val safeLocation = location ?: ""
            val safeDate = date ?: ""
            val safeTime = time ?: ""
//            if (location == null || date == null || time == null) {
////
//                Toast.makeText(
//                    requireContext(), "Meeting details not available", Toast.LENGTH_SHORT
//                ).show()
//                return@fetchMeetingDetails
//            }
            val bottomSheet =
                MeetingBottomSheet.newInstance(safeLocation, safeDate, safeTime) { loc, d, t ->
                    val meetingStored = messageRepo.updateMeetingDetails(loc, d, t, roomId, bidId)
                    Toast.makeText(requireContext(), "Meet at $loc on $d $t", Toast.LENGTH_LONG)
                        .show()
                    if (meetingStored) {
                        Toast.makeText(requireContext(), "Meeting Updated", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            bottomSheet.show(parentFragmentManager, "MeetingBottomSheet")
        }
    }

//
    fun handleRevealRequest(string: String, value: Any) {

        val ref = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidId)

        ref.get().addOnSuccessListener { snapshot ->
            val agreement =
                snapshot.getValue(ExchangeAgreement::class.java) ?: return@addOnSuccessListener

            // Mark this user's press
            if (currentUser == agreement.bidderId) {
                agreement.isBidderPressedReveal = true
            } else if (currentUser == agreement.receiverId) {
                agreement.isReceiverPressedReveal = true
            }

            ref.setValue(agreement).addOnSuccessListener {
                if (agreement.isBidderPressedReveal && agreement.isReceiverPressedReveal) {
                    val otpToShow =
                        if (currentUser == agreement.bidderId) agreement.otpByReceiver else agreement.otpByBidder

                    showOtpDialog(otpToShow)

                    // ✅ Complete the bid
//                    agreement.status = "Completed"
                    ref.setValue(agreement)
                } else {
                    showWaitingDialog()
                }

            }
        }
    }

    private fun showOtpDialog(otp: String) {
        val context = requireContext()

        AlertDialog.Builder(context).setTitle("Partner's OTP")
            .setMessage("Use this OTP for verification: $otp")
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss() // Dismiss the OTP info dialog
                verityOtp() // Now call the verification dialog
            }.setCancelable(false).show()
    }

    private fun verityOtp() { // This is called after user clicks "Submit" in OTP entry dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_enter_otp, null)
        val editText = dialogView.findViewById<EditText>(R.id.et_enter_otp)

        AlertDialog.Builder(requireContext())
            .setTitle("Enter OTP")
            .setMessage("Please enter the OTP shared by your partner.")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val enteredOtp = editText.text.toString().trim()
                if (enteredOtp.isBlank()) {
                    Toast.makeText(context, "OTP cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Call the modified verifyOtp
                firebaseAgreement.verifyOtp(bidId, currentUser, enteredOtp) { isOtpSuccess, isExchangeNowCompleted, updatedAgreement ->
                    if (isOtpSuccess) {
                        Toast.makeText(context, "OTP Verified!", Toast.LENGTH_SHORT).show()

                        // Update the fragment's local agreement object if it exists
                        if (updatedAgreement != null) {
                            this.agreement = updatedAgreement
                        }

                        if (isExchangeNowCompleted) {
                            Log.d(TAG, "Exchange for bid $bidId is now fully completed by both OTPs.")
                            // Call ViewModel to update Bid and Item statuses
                            viewModel.finalizeCompletedExchange(bidId) // ViewModel will fetch necessary details

                            // Now, after telling ViewModel to finalize, prompt for rating/reporting
                            // Ensure `updatedAgreement` is not null if `checkAndPromptRating` needs it immediately
                            if (updatedAgreement != null) {
                                checkAndPromptRating(bidId, updatedAgreement, currentUser)
                            } else {
                                // Fallback or re-fetch agreement if needed for checkAndPromptRating
                                Log.w(TAG, "Updated agreement is null after completion signal, rating prompt might be affected.")
                                // You could re-fetch here if absolutely necessary for rating prompt:
                                // firebaseAgreementManager.getExchangeAgreementRef(bidId) { finalAgreement -> ... }
                            }
                            // UI should update based on bannerState from ViewModel reacting to "completed" status
                        } else {
                            Log.d(TAG, "OTP successful, but exchange not yet completed by both parties.")
                            // Refresh UI to show current state (e.g., one OTP verified)
                            // This might involve re-fetching agreement or updating UI based on `updatedAgreement`
                        }
                        refreshFragment() // Or a more targeted UI update based on `updatedAgreement`
                    } else {
                        Toast.makeText(context, "Invalid OTP! Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }
//    private fun verityOtp() {
//        val dialogView =
//            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_enter_otp, null)
//        val editText = dialogView.findViewById<EditText>(R.id.et_enter_otp)
//
//        AlertDialog.Builder(requireContext()).setTitle("Enter OTP")
//            .setMessage("Please enter the OTP shared by your partner.").setView(dialogView)
//            .setPositiveButton("Submit") { _, _ ->
//                val enteredOtp = editText.text.toString()
//                firebaseAgreement.verifyOtp(bidId, currentUser, enteredOtp) { isSuccess ->
//                    if (isSuccess) {
////                       checkAndPromptRating(bidId,agreement,currentUser)
//                        refreshFragment()
//
//                        Toast.makeText(
//                            context, "OTP Verified! Waiting for other user...", Toast.LENGTH_SHORT
//
//                        ).show()
//
//
//                    } else {
//                        Toast.makeText(context, "Invalid OTP!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }.setCancelable(false).show()
//
//    }


//    private fun checkAndPromptRating(bidId: String, currentAgreement: ExchangeAgreement, currentUserId: String) {
//        // Check if current user has already rated/reported for this agreement
//        val alreadyProcessedByBidder = (currentUserId == currentAgreement.bidderId && (currentAgreement.ratingGivenByBidder || currentAgreement.reportFiledByBidder))
//        val alreadyProcessedByReceiver = (currentUserId == currentAgreement.receiverId && (currentAgreement.ratingGivenByReceiver || currentAgreement.reportFiledByReceiver))
//
//        if (alreadyProcessedByBidder || alreadyProcessedByReceiver) {
//            Log.d(TAG, "Rating/Report already submitted for this exchange by user $currentUserId.")
//            // Optionally show a message or just do nothing
//            return
//        }
//
//        // If agreement is null, it means it wasn't fetched or an error occurred.
//        // The showCustomRatingDialog needs a non-null agreement.
//        if (this.agreement == null) { // Check the fragment's agreement property
//            Log.e(TAG, "Agreement object is null, cannot show rating dialog. Fetching...")
//            // You might need to re-fetch the agreement here if it can be null
//            // For now, assuming 'this.agreement' will be populated correctly before this is called robustly.
//            // If 'currentAgreement' param is always up-to-date, use that.
//            firebaseAgreement.getExchangeAgreementRef(bidId) { fetchedAgreement ->
//                if (fetchedAgreement != null) {
//                    this.agreement = fetchedAgreement // Update fragment's agreement
//                    showCustomRatingDialog(currentUserId, bidId, fetchedAgreement)
//                } else {
//                    Toast.makeText(context, "Could not load exchange details for rating.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            return
//        }
//        // If the fragment's agreement is available and up-to-date, use it
//        showCustomRatingDialog(currentUserId, bidId, this.agreement!!)
//    }


// HomeChatExchangeFragment.kt

    private fun showCustomRatingDialog(
        raterUserId: String,
        bidId: String,
        currentExchangeAgreement: ExchangeAgreement
    ) {
        if (context == null || !isAdded) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title) // Make sure this ID exists in your XML
        val positiveFeedbackLayout = dialogView.findViewById<LinearLayout>(R.id.layout_positive_feedback)
        val feedbackChipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_feedback)
        val reportDetailsLayout = dialogView.findViewById<LinearLayout>(R.id.layout_report_details)
        val reportReasonsChipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_report_reasons)
        val reportDescriptionEditText = dialogView.findViewById<EditText>(R.id.et_report_description)
        val fileReportSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switch_file_report)
        val submitBtn = dialogView.findViewById<Button>(R.id.btn_submit_rating)

        val userToRateId = if (raterUserId == currentExchangeAgreement.bidderId) currentExchangeAgreement.receiverId else currentExchangeAgreement.bidderId

        // Populate positive feedback chips
        FeedbackTags.getPositiveFeedbackTags().forEach { tag ->
            val chip = Chip(requireContext()).apply { text = tag.label; isCheckable = true; this.tag = tag }
            feedbackChipGroup.addView(chip)
        }

        // Populate report reason chips
        FeedbackTags.getReportReasonTags().forEach { tag ->
            val chip = Chip(requireContext()).apply { text = tag.label; isCheckable = true; this.tag = tag }
            reportReasonsChipGroup.addView(chip)
        }

        // Initial UI state (Rating mode)
        titleTextView.text = getString(R.string.dialog_title_rate_exchange)
        positiveFeedbackLayout.visibility = View.VISIBLE
        reportDetailsLayout.visibility = View.GONE
        submitBtn.text = getString(R.string.submit_rating)
        ratingBar.setIsIndicator(false)
        ratingBar.rating = 0f // Reset rating

        fileReportSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { // Report Mode
                titleTextView.text = getString(R.string.dialog_title_report_exchange)
                positiveFeedbackLayout.visibility = View.GONE
                reportDetailsLayout.visibility = View.VISIBLE
                submitBtn.text = getString(R.string.submit_report_and_rating)
                ratingBar.rating = 1f // Default low rating for reports
                ratingBar.setIsIndicator(true)
            } else { // Rating Mode
                titleTextView.text = getString(R.string.dialog_title_rate_exchange)
                positiveFeedbackLayout.visibility = View.VISIBLE
                reportDetailsLayout.visibility = View.GONE
                submitBtn.text = getString(R.string.submit_rating)
                ratingBar.setIsIndicator(false)
                // ratingBar.rating = 0f; // Optionally reset if user unchecks
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        submitBtn.setOnClickListener {
            val ratingValue = ratingBar.rating.toInt()
            val isReporting = fileReportSwitch.isChecked

            val activeChipGroup = if (isReporting) reportReasonsChipGroup else feedbackChipGroup
            val selectedTags = mutableListOf<FeedbackTags>()
            for (i in 0 until activeChipGroup.childCount) {
                val chip = activeChipGroup.getChildAt(i) as Chip
                if (chip.isChecked) {
                    (chip.tag as? FeedbackTags)?.let { selectedTags.add(it) }
                }
            }
            val reportDescriptionText = reportDescriptionEditText.text.toString().trim()

            if (!isReporting && ratingValue == 0) {
                Toast.makeText(requireContext(), getString(R.string.toast_please_select_rating), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isReporting && selectedTags.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.toast_please_select_report_reason), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val feedbackForSubmission = Feedback(
                reviewerId = raterUserId,
                tags = selectedTags,
                rating = ratingValue,
                timestamp = System.currentTimeMillis(),
                isReport = isReporting,
                reportDescription = if (isReporting) reportDescriptionText.ifEmpty { null } else null,
                reportedUserId = userToRateId
            )

            Log.d(TAG, "Submitting: $feedbackForSubmission")
            viewModel.saveFeedbackAndPotentialReport(
                ratedOrReportedUserId = userToRateId,
                bidId = bidId,
                feedbackData = feedbackForSubmission,
                isAlsoAReport = isReporting, // Can derive from feedbackForSubmission.isReport
                reporterUserId = raterUserId,
                currentAgreement = currentExchangeAgreement
            )
            dialog.dismiss()
        }
        dialog.show()
    }

    // Add observer for ViewModel feedback (e.g., in onViewCreated)
    private fun observeRatingReportSubmission() {
        viewModel.feedbackSubmissionResult.observe(viewLifecycleOwner) { result ->
            result?.let { (success, message) ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                if (success) {
                    // Optionally, navigate back or refresh UI to prevent re-submission
                    // For example, update the local 'agreement' object state.
                    val processedAgreement = agreement ?: return@observe
                    if (currentUser == processedAgreement.bidderId) {
                        processedAgreement.ratingGivenByBidder = true
                        if (result.isReport) processedAgreement.reportFiledByBidder = true // Assuming result has an isReport flag
                    } else {
                        processedAgreement.ratingGivenByReceiver = true
                        if (result.isReport) processedAgreement.reportFiledByReceiver = true
                    }
                    // findNavController().popBackStack() // Example
                }
                viewModel.clearFeedbackSubmissionResult() // Clear LiveData to prevent re-triggering
            }
        }
    }


    private fun showWaitingDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Waiting")
            .setMessage("Waiting for the other user to press Done.").setPositiveButton("OK", null)
            .show()
    }

    private fun refreshFragment() {
        val fragment = parentFragmentManager.findFragmentById(R.id.fragment_exchange_chat_home)
        fragment?.let {
            parentFragmentManager.beginTransaction()
                .detach(it)
                .attach(it)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        // Null out binding to prevent memory leaks and invalid access
        _binding = null
    }
}


// item 2 cards properly showing , not showing inthe above one
//package com.example.signuplogina.fragments
//
//import android.app.AlertDialog
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.RatingBar
//import androidx.navigation.fragment.findNavController
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.widget.Toolbar
//import androidx.databinding.DataBindingUtil
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.bumptech.glide.Glide
//import com.example.signuplogina.ItemDetails
//import com.example.signuplogina.MeetingBottomSheet
//import com.example.signuplogina.R
//import com.example.signuplogina.Utils
//import com.example.signuplogina.adapter.ExchangeItemsAdapter
//import com.example.signuplogina.adapter.MessageAdapter
//import com.example.signuplogina.databinding.FragmentChatfromHomeBinding
//import com.example.signuplogina.databinding.FragmentExchangeChatHomeBinding
//import com.example.signuplogina.modal.ExchangeAgreement
//import com.example.signuplogina.modal.Feedback
//import com.example.signuplogina.modal.FeedbackTags
//import com.example.signuplogina.modal.Messages
//import com.example.signuplogina.mvvm.BidRepository
//import com.example.signuplogina.mvvm.ChatAppViewModel
//import com.example.signuplogina.mvvm.ExchangeAppViewModel
//import com.example.signuplogina.mvvm.FirebaseAgreementManager
//import com.example.signuplogina.mvvm.ItemsRepo
//import com.example.signuplogina.mvvm.MessageRepo
//import com.google.android.material.chip.Chip
//import com.google.android.material.chip.ChipGroup
//import com.google.firebase.database.FirebaseDatabase
//
//import de.hdodenhof.circleimageview.CircleImageView
//
//class HomeChatExchangeFragment : Fragment() {
//
//    lateinit var args: HomeChatExchangeFragmentArgs
//    lateinit var binding: FragmentExchangeChatHomeBinding
//    lateinit var viewModel: ExchangeAppViewModel
//    lateinit var adapter: MessageAdapter
//    lateinit var currentUser: String
//    private var isItemsVisible = false
//    private var isDataLoaded = false
//    lateinit var roomId: String
//    lateinit var itemsRepo: ItemsRepo
//    lateinit var bidsRepo: BidRepository
//    lateinit var messageRepo: MessageRepo
//    lateinit var firebaseAgreement: FirebaseAgreementManager
//    lateinit var bidId: String//
//    lateinit var receiverId: String
//    lateinit var agreement: ExchangeAgreement
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        binding = DataBindingUtil.inflate(
//            inflater, R.layout.fragment_exchange_chat_home, container, false
//        )
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d("NavigationDebug", "🟢 ChatfromHOme Loaded")
//        firebaseAgreement = FirebaseAgreementManager()
//        args = HomeChatExchangeFragmentArgs.fromBundle(requireArguments())
//        currentUser = Utils.getUidLoggedIn()
//        bidId = args.recentchats.bidId!!
//
//        firebaseAgreement.getExchangeAgreementRef(bidId) { agreement ->
//            if (agreement != null) {
//                this.agreement=agreement
//            }
//            if (agreement != null) {
//                if(agreement.status.equals("completed")){
//                    binding.btnAgreement.isEnabled = false
//                    binding.btnDone.isEnabled=false
//                    binding.btnPlanMeeting.isEnabled=false
//                    binding.sendBtn.isEnabled=false
//
//                    checkAndPromptRating(bidId, agreement, currentUser)
//
//                }
//            } else {
//                Toast.makeText(context, "Agreement not found", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        receiverId = args.recentchats.friendid.orEmpty()
//        roomId = "$bidId-${
//            listOf(currentUser, receiverId).sorted().joinToString("")
//        }"// make sure that it is not null and the above
//        // Initializing the arguments, view model, and binding lifecycle owner
//        viewModel = ViewModelProvider(this).get(ExchangeAppViewModel::class.java)
//        binding.viewModel = viewModel
//        binding.lifecycleOwner = viewLifecycleOwner
//
//        itemsRepo = ItemsRepo()
//        bidsRepo = BidRepository()
//        messageRepo = MessageRepo()
//
//        // Set up toolbar with Glide for image and text view for user name
//        val circleImageView = view.findViewById<CircleImageView>(R.id.chatImageViewUser)
//        val textViewName = view.findViewById<TextView>(R.id.chatUserName)
//
//        Glide.with(view.context).load(args.recentchats.friendsimage!!)
//            .placeholder(R.drawable.person).dontAnimate().into(circleImageView)
//
//        textViewName.text = args.recentchats.name
//
//        binding.chatBackBtn.setOnClickListener {
//            findNavController().popBackStack()
//        }
////go to the agreemnt ,
//
//        binding.btnAgreement.setOnClickListener {
//            val action =
//                HomeChatExchangeFragmentDirections.actionHomeChatExchangeFragmentToExchangeAgreementFragment(
//                        bidId,
//                        args.recentchats
//                    )
//
//            findNavController().navigate(action)
//
//        }
//
//        binding.btnDone.setOnClickListener {
//            showDoneConfirmationDialog()
//        }
//
//
//        // Send message button click listener
//        binding.sendBtn.setOnClickListener {
//
//
//            if (receiverId.isBlank()) {
//                Log.e("SendMessage", "❌ receiverId is null or blank in ChatfromHome!")
//                return@setOnClickListener
//            }
//
//            viewModel.sendMessage(
//                Utils.getUidLoggedIn(),
//                receiverId,
//                bidId.toString(),
//                args.recentchats.name.orEmpty(),
//                args.recentchats.friendsimage.orEmpty()
//            )
//
//        }
//
//
//
//        binding.cardSeeItems.setOnClickListener {
//            isItemsVisible = !isItemsVisible
//            binding.exchangeItemsRecyclerView.visibility =
//                if (isItemsVisible) View.VISIBLE else View.GONE
//
//            // Optional: Animate the arrow
//            val arrow = binding.arrowIcon
//            arrow.animate().rotation(if (isItemsVisible) 90f else 0f).start()
//
//            // Fetch and display items when expanded for the first time
//            if (isItemsVisible && !isDataLoaded) {
//                val bidId = args.recentchats.bidId
//                bidsRepo.getBidById(bidId.toString(), onSuccess = { bid ->
//                    // ✅ Bid fetched successfully here
//                    Log.d("BidFetch", "Bid fetched: ${bid.itemId}, ${bid.offeredItemId}")
//
//                    // Now use this bid to fetch the related items
//                    itemsRepo.getItemsByIds(bid, onSuccess = { bidItemDetails, offeredItemDetails ->
//                        // ✅ You have both item details now
//                        val itemsList = listOf(bidItemDetails, offeredItemDetails)
//                        setupItemsRecyclerView(itemsList)
//                    }, onFailure = { error ->
//                        Log.e("ItemsRepo", "Failed to fetch items: ${error.message}")
//                    })
//
//                }, onFailure = { errorMessage ->
//                    // ❌ Handle failure to fetch Bid
//                    Log.e("BidRepo", "Failed to fetch bid: $errorMessage")
//                })
//            }
//        }
//
//        binding.btnPlanMeeting.setOnClickListener {
//            showMeetingBottomSheet()
//        }
//
//
//        // Observe the messages from the ViewModel
//        viewModel.getMessages(args.recentchats.friendid!!, args.recentchats.bidId!!)
//            .observe(viewLifecycleOwner, Observer {
//                initRecyclerView(it.toMutableList())
//            })
//    }
//
//
//    fun checkAndPromptRating(bidId: String, agreement: ExchangeAgreement, currentUserId: String) {
//        if (agreement.bidderOtpVerified && agreement.receiverOtpVerified) {
//            //check here when the second user verifies otp this wot show , untilcome back on this screen
//            val hasUserAlreadyRated = if (currentUserId == agreement.bidderId)
//                agreement.ratingGivenByBidder else agreement.ratingGivenByReceiver
//
//            if (!hasUserAlreadyRated) {
//                showCustomRatingDialog(currentUserId, bidId, agreement)
//            }
//        }
//    }
//    private fun showCustomRatingDialog(raterUserId: String, bidId: String, agreement: ExchangeAgreement) {
//        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rating, null)
//        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
//        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_feedback)
//        val submitBtn = dialogView.findViewById<Button>(R.id.btn_submit_rating)
//
//        // Dynamically add Chips
//        FeedbackTags.allTags.forEach { feedbackTag ->
//            val chip = Chip(requireContext()).apply {
//                text = feedbackTag.label
//                isCheckable = true
//                isClickable = true
//                tag = feedbackTag  // Now no conflict!
//            }
//            chipGroup.addView(chip)
//        }
//
//
//        val dialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//
//        submitBtn.setOnClickListener {
//            val rating = ratingBar.rating.toInt()
//            val selectedTags = mutableListOf<FeedbackTags>()
//
//            for (i in 0 until chipGroup.childCount) {
//                val chip = chipGroup.getChildAt(i) as Chip
//                if (chip.isChecked){
//                val tag = chip.tag as? FeedbackTags
//                if (tag != null) selectedTags.add(tag)
//            }}
//
//            if (rating > 0) {
//                val ratedUserId = if (raterUserId == agreement.bidderId) agreement.receiverId else agreement.bidderId
//                val feedback = Feedback(
//                    reviewerId = raterUserId,
//                    tags = selectedTags,
//                    rating = rating,
//                    timestamp = System.currentTimeMillis()
//                )
//
//                // Save feedback
//                viewModel.saveRatingToUser(ratedUserId, feedback,bidId)
//
//                // Mark rating complete in agreement
//                val ref = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidId)
//                val ratingField = if (raterUserId == agreement.bidderId) "ratingGivenByBidder" else "ratingGivenByReceiver"
//                ref.child(ratingField).setValue(true)
//
//                dialog.dismiss()
//            } else {
//                Toast.makeText(requireContext(), "Please select a rating.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        dialog.show()
//    }
//
//
//    fun showDoneConfirmationDialog() {
//        AlertDialog.Builder(requireContext()).setTitle("Complete the Bid?")
//            .setMessage("You are about to mark this exchange as done. You will see the OTP once both users confirm.")
//            .setPositiveButton("OK") { _, _ ->
//                handleRevealRequest(bidId, currentUser)
//            }.setNegativeButton("Cancel", null).show()
//    }
//
//    // Method to initialize RecyclerView
//    private fun initRecyclerView(list: MutableList<Messages>) {
//        // Set up the adapter
//        adapter = MessageAdapter()
//
//        // Initialize LayoutManager
//        val layoutManager = LinearLayoutManager(context)
//        binding.messagesRecyclerView.layoutManager = layoutManager
//        layoutManager.stackFromEnd = true
//
//        // Set the list of messages to the adapter
//        adapter.setList(list)
//
//        // Bind the adapter to the RecyclerView
//        binding.messagesRecyclerView.adapter = adapter
//    }
//
//    private fun showMeetingBottomSheet() {
//        messageRepo.fetchMeetingDetails(roomId) { location, date, time ->
//            val safeLocation = location ?: ""
//            val safeDate = date ?: ""
//            val safeTime = time ?: ""
////            if (location == null || date == null || time == null) {
//////
////                Toast.makeText(
////                    requireContext(), "Meeting details not available", Toast.LENGTH_SHORT
////                ).show()
////                return@fetchMeetingDetails
////            }
//            val bottomSheet = MeetingBottomSheet.newInstance(safeLocation, safeDate, safeTime) { loc, d, t ->
//                val meetingStored = messageRepo.updateMeetingDetails(loc, d, t, roomId, bidId)
//                Toast.makeText(requireContext(), "Meet at $loc on $d $t", Toast.LENGTH_LONG).show()
//                if (meetingStored) {
//                    Toast.makeText(requireContext(), "Meeting Updated", Toast.LENGTH_LONG).show()
//                }
//            }
//            bottomSheet.show(parentFragmentManager, "MeetingBottomSheet")
//        }
//    }
//
//
//    private fun HomeChatExchangeFragment.setupItemsRecyclerView(details: List<ItemDetails>) {
//        val adapter = ExchangeItemsAdapter(details.toMutableList()) { position ->
//            val clickedItem = details[position]
//            // Handle click: e.g., expand card, show dialog, etc.
//            Toast.makeText(
//                requireContext(), "Clicked: ${clickedItem.productName}", Toast.LENGTH_SHORT
//            ).show()
//        }
//
//        binding.exchangeItemsRecyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            this.adapter = adapter
//        }
//    }
//
//
//    fun handleRevealRequest(string: String, value: Any) {
//
//        val ref = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidId)
//
//        ref.get().addOnSuccessListener { snapshot ->
//            val agreement =
//                snapshot.getValue(ExchangeAgreement::class.java) ?: return@addOnSuccessListener
//
//            // Mark this user's press
//            if (currentUser == agreement.bidderId) {
//                agreement.isBidderPressedReveal = true
//            } else if (currentUser == agreement.receiverId) {
//                agreement.isReceiverPressedReveal = true
//            }
//
//            ref.setValue(agreement).addOnSuccessListener {
//                if (agreement.isBidderPressedReveal && agreement.isReceiverPressedReveal) {
//                    val otpToShow =
//                        if (currentUser == agreement.bidderId) agreement.otpByReceiver else agreement.otpByBidder
//
//                    showOtpDialog(otpToShow)
//
//                    // ✅ Complete the bid
////                    agreement.status = "Completed"
//                    ref.setValue(agreement)
//                } else {
//                    showWaitingDialog()
//                }
//
//            }
//        }
//    }
//
//    private fun showOtpDialog(otp: String) {
//        val context = requireContext()
//
//        AlertDialog.Builder(context).setTitle("Partner's OTP")
//            .setMessage("Use this OTP for verification: $otp")
//            .setPositiveButton("Got it") { dialog, _ ->
//                dialog.dismiss() // Dismiss the OTP info dialog
//                verityOtp() // Now call the verification dialog
//            }.setCancelable(false).show()
//    }
//
//
//    private fun verityOtp() {
//        val dialogView =
//            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_enter_otp, null)
//        val editText = dialogView.findViewById<EditText>(R.id.et_enter_otp)
//
//        AlertDialog.Builder(requireContext()).setTitle("Enter OTP")
//            .setMessage("Please enter the OTP shared by your partner.").setView(dialogView)
//            .setPositiveButton("Submit") { _, _ ->
//                val enteredOtp = editText.text.toString()
//                firebaseAgreement.verifyOtp(bidId, currentUser, enteredOtp) { isSuccess ->
//                    if (isSuccess) {
////                       checkAndPromptRating(bidId,agreement,currentUser)
//                    refreshFragment()
//
//                        Toast.makeText(
//                            context, "OTP Verified! Waiting for other user...", Toast.LENGTH_SHORT
//
//                        ).show()
//
//
//                    } else {
//                        Toast.makeText(context, "Invalid OTP!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }.setCancelable(false).show()
//
//    }
//
//    private fun showWaitingDialog() {
//        AlertDialog.Builder(requireContext()).setTitle("Waiting")
//            .setMessage("Waiting for the other user to press Done.").setPositiveButton("OK", null)
//            .show()
//    }
//    private fun refreshFragment(){
//        val fragment = parentFragmentManager.findFragmentById(R.id.fragment_exchange_chat_home)
//        fragment?.let {
//            parentFragmentManager.beginTransaction()
//                .detach(it)
//                .attach(it)
//                .commit()
//        }
//    }
//
//
//}
