package com.example.signuplogina.mvvm

import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signuplogina.R
import kotlinx.coroutines.withContext

import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.example.signuplogina.MyApplication
import com.example.signuplogina.SharedPrefs
import com.example.signuplogina.Utils
import com.example.signuplogina.modal.Messages
import com.example.signuplogina.modal.RecentChats
import com.example.signuplogina.User
import com.example.signuplogina.fragments.SingleLiveEvent
import com.example.signuplogina.modal.ExchangeAgreement
import com.example.signuplogina.modal.Feedback
import com.example.signuplogina.modal.Report
import com.example.signuplogina.modal.UserRatingStats
import com.example.signuplogina.notifications.entity.NotificationData
import com.example.signuplogina.notifications.entity.PushNotification
import com.example.signuplogina.notifications.entity.Token
import com.example.signuplogina.notifications.network.RetrofitInstance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


data class BannerState(
    val isVisible: Boolean = false,
    val message: String = "",
    val backgroundColorRes: Int = Color.TRANSPARENT, // Default transparent
    val disablesAllInteractions: Boolean = false
)

class ExchangeAppViewModel : ViewModel() {


    companion object {
        private const val TAG = "ExchangeAppViewModel"

    }

    val message = MutableLiveData<String>()
    val name = MutableLiveData<String>()
    val imageUrl = MutableLiveData<String>()
    var bidId: String? = null
    val usersRepo = UsersRepo()
    val messageRepo = MessageRepo()
    val chatlistRepo = ChatListRepo()
    val bidRepo = BidRepository()
    val itemsRepo = ItemsRepo()


    private val database = FirebaseDatabase.getInstance().reference
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        _isLoadingChatItems.postValue(false)// is this correct?/

    }


    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _opponentUserDetailsForBanner = MutableLiveData<User?>()
    private val _bidDetailsForBanner = MutableLiveData<Bid?>()
    private val _chatPathBlockStatusForBanner = MutableLiveData<Boolean>()

    // NEW MediatorLiveData for the banner state
    private val _effectiveBannerState = MediatorLiveData<BannerState>()
    val effectiveBannerState: LiveData<BannerState> get() = _effectiveBannerState

    // Firebase Listeners for real-time updates relevant to the banner
    private var opponentDetailsValueListener: ValueEventListener? = null
    private var opponentUserRef: DatabaseReference? = null
    private var chatBlockStatusListener: ValueEventListener? = null
    private var chatBlockStatusRef: DatabaseReference? = null
    private var bidStatusListener: ValueEventListener? = null
    private var bidRefForBanner: DatabaseReference? =
        null // Different from this.bidId which is for createRoom


    init {
        getCurrentUser()

        // Setup the MediatorLiveData sources
        _effectiveBannerState.addSource(_opponentUserDetailsForBanner) { updateBannerLogic() }
        _effectiveBannerState.addSource(_chatPathBlockStatusForBanner) { updateBannerLogic() }
        _effectiveBannerState.addSource(_bidDetailsForBanner) { updateBannerLogic() }
    }

    // NEW method to be called from Fragment's onViewCreated
    fun loadDataForBannerAndInteraction(
        currentUserId: String,
        opponentId: String,
        bidIdForState: String
    ) {
        if (opponentId.isBlank() || currentUserId.isBlank() || bidIdForState.isBlank()) {
            Log.e("ExchnageAppViewModelBanner", "Cannot load banner data: Critical IDs missing.")
            _effectiveBannerState.postValue(
                BannerState(
                    true,
                    "Error: Invalid exchange context.",
                    R.color.overlay_rejected_background,
                    true
                )
            )
            return
        }
        clearBannerListeners() // Clear any previous listeners

        // 1. Fetch/Listen to Opponent's User Details
        opponentUserRef = database.child("Users")
            .child(opponentId) // 'database' is your existing FirebaseDatabase.getInstance().reference
        opponentDetailsValueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _opponentUserDetailsForBanner.postValue(snapshot.getValue(User::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "ExchnageAppViewModelBanner",
                    "Failed to load opponent user details for banner: ${error.message}"
                )
                _opponentUserDetailsForBanner.postValue(null)
            }
        }
        opponentUserRef?.addValueEventListener(opponentDetailsValueListener!!)

        // 2. Listen to the specific chat path's "isBlocked" status
        chatBlockStatusRef =
            database.child("Conversations/ExchangeChats/$currentUserId/$opponentId/$bidIdForState/isBlocked")
        chatBlockStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _chatPathBlockStatusForBanner.postValue(
                    snapshot.getValue(Boolean::class.java) ?: false
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(
                    "ExchnageAppViewModelBanner",
                    "Failed to listen to chat path block status for banner: ${error.message}"
                )
                _chatPathBlockStatusForBanner.postValue(false)
            }
        }
        chatBlockStatusRef?.addValueEventListener(chatBlockStatusListener!!)

        // 3. Fetch/Listen to Bid status
        bidRefForBanner = database.child("Bids").child(bidIdForState)
        bidStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _bidDetailsForBanner.postValue(snapshot.getValue(Bid::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "ExchnageAppViewModelBanner",
                    "Failed to load bid details for banner: ${error.message}"
                )
                _bidDetailsForBanner.postValue(null)
            }
        }
        bidRefForBanner?.addValueEventListener(bidStatusListener!!)
    }

    // NEW method containing the core logic for determining banner state
    private fun updateBannerLogic() {
        val opponent = _opponentUserDetailsForBanner.value
        val isChatPathBlocked = _chatPathBlockStatusForBanner.value ?: false
        val currentBid = _bidDetailsForBanner.value

        var message: String? = null
        var colorRes: Int = R.color.colorPrimaryDark // A default neutral banner color
        var disableAllInteractions = false

        // Priority 1: Opponent's account block status
        if (opponent?.ratings != null) {
            val ratings = opponent.ratings
            when {
                ratings.isPermanentlyBlocked -> {
//                    message = "OPPONENT PERMANENTLY BLOCKED. Reason: ${ratings.blockReason ?: "N/A"}. Exchange cannot proceed."
                    message = "OPPONENT PERMANENTLY BLOCKED. Exchange cannot proceed."
                    colorRes = R.color.overlay_rejected_background
                    disableAllInteractions = true
                }

                ratings.isTemporarilyBlocked && (System.currentTimeMillis() < ratings.blockExpiryTimestamp || ratings.blockExpiryTimestamp == 0L /*Should be set if temp blocked*/) -> {
                    val expiry = if (ratings.blockExpiryTimestamp != 0L) SimpleDateFormat(
                        "MMM dd, yyyy",
                        Locale.US
                    ).format(Date(ratings.blockExpiryTimestamp)) else "Indefinite"
//                    message = "OPPONENT TEMPORARILY RESTRICTED until $expiry. Reason: ${ratings.blockReason ?: "N/A"}. Exchange on hold."
                    message = "OPPONENT TEMPORARILY RESTRICTED until $expiry. Exchange on hold."
                    colorRes = R.color.light_orange
                    disableAllInteractions = true
                }
                // Case for expired temporary block (user should be auto-unblocked by MainActivity onStart, but data might lag)
                ratings.isTemporarilyBlocked && System.currentTimeMillis() >= ratings.blockExpiryTimestamp && ratings.blockExpiryTimestamp != 0L -> {
                    message =
                        "Opponent's restriction recently ended. Refreshing status..." // Or simply don't show a block banner
                    colorRes = R.color.dark_green // Or neutral
                    // disableAllInteractions remains false unless other conditions trigger it
                }
            }
        } else if (opponent?.statusByAdmin == "blocked" && message == null) { // Legacy or simpler block check
            message = "OPPONENT ACCOUNT RESTRICTED. Exchange cannot proceed."
            colorRes = R.color.light_orange
            disableAllInteractions = true
        }

        // Priority 2: Bid Status (if no user block already disables everything)
        if (!disableAllInteractions && currentBid != null) {
            when (currentBid.status.lowercase()) {
                "completed" -> {
                    message = "This exchange has been completed."
                    colorRes = R.color.dark_green
                    disableAllInteractions = true
                }

                "canceledbyadmin", "canceled_by_admin_user_blocked",
                "rejectedbyreceiver", "withdrawbybidder", "canceledbysystem_item_unavailable" -> {
                    message =
                        "EXCHANGE CANCELED. ${currentBid.canceledReason?.let { "Reason: $it" } ?: ""}"
                    colorRes = R.color.light_gray
                    disableAllInteractions = true
                }

                "stalled_owner_temp_blocked" -> {
                    message =
                        "EXCHANGE STALLED: ${currentBid.canceledReason ?: "Participant restricted."}"
                    colorRes = R.color.light_orange
                    disableAllInteractions = true
                }
                // "pending", "started", "agreement_reached" are active states, no banner unless overridden by user block.
            }
        }

        // Priority 3: Chat Path Specific Block (if no user/bid block already disables everything)
        // This implies a direct disabling of the conversation path itself, perhaps by an admin feature not yet built,
        // or as a consequence of disableConversationsForBlockedUser if the UserRatingStats listener is slow.
        if (isChatPathBlocked && !disableAllInteractions) {
            message =
                (message?.let { "$it\n" } ?: "") + "Communication for this exchange is disabled."
            if (message == null || colorRes == R.color.colorPrimaryDark) colorRes =
                R.color.light_gray // Don't override stronger colors
            disableAllInteractions =
                true // If chat path is blocked, assume all interactions for this exchange are off.
        }

        if (message != null) {
            _effectiveBannerState.postValue(
                BannerState(
                    true,
                    message,
                    colorRes,
                    disableAllInteractions
                )
            )
        } else {
            // No specific blocking banner needed, interactions depend on normal bid/agreement flow
            _effectiveBannerState.postValue(BannerState(false, "", R.color.transparent, false))
        }
    }

    // NEW method to clear listeners
    fun clearBannerListeners() {
        opponentDetailsValueListener?.let { opponentUserRef?.removeEventListener(it) }
        chatBlockStatusListener?.let { chatBlockStatusRef?.removeEventListener(it) }
        bidStatusListener?.let { bidRefForBanner?.removeEventListener(it) }
        Log.d("ExchnageAppViewModelBanner", "Banner listeners cleared.")
    }


    private val _chatItemsError =
        SingleLiveEvent<String?>() // Use SingleLiveEvent for one-time error messages
    val chatItemsError: LiveData<String> =
        _chatItemsError as LiveData<String> // Expose as LiveData<String>

    private val _isLoadingInitiation = MutableLiveData<Boolean>(false) // Default to not loading
    val isLoadingInitiation: LiveData<Boolean> = _isLoadingInitiation

    // 2. initiationError: To communicate specific errors that occur during initiation.
    private val _initiationError = MutableLiveData<String?>() // Nullable, null means no error
    val initiationError: LiveData<String?> = _initiationError


    private val _isLoadingCancellation = MutableLiveData<Boolean>(false)
    val isLoadingCancellation: LiveData<Boolean> = _isLoadingCancellation

    private val _cancellationResult = SingleLiveEvent<Pair<Boolean, String?>>()
    val cancellationResult: LiveData<Pair<Boolean, String?>> = _cancellationResult


    // LiveData for UI feedback after submission
    private val _feedbackSubmissionResult = MutableLiveData<FeedbackSubmissionResult?>()
    val feedbackSubmissionResult: LiveData<FeedbackSubmissionResult?> get() = _feedbackSubmissionResult

    fun clearFeedbackSubmissionResult() {
        _feedbackSubmissionResult.value = null
    }

    fun finalizeCompletedExchange(bidId: String) {
        if (bidId.isBlank()) {
            Log.e(TAG, "finalizeCompletedExchange: bidId is blank.")
            // Optionally post an error to a LiveData
            return
        }
        Log.d(TAG, "Finalizing completed exchange for bid: $bidId")
        _isLoading.postValue(true) // Indicate loading/processing

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                // 1. Fetch the Bid object
                val bidSnapshot = database.child("Bids").child(bidId).get().await()
                val bid = bidSnapshot.getValue(Bid::class.java)

                if (bid == null) {
                    Log.e(TAG, "Bid $bidId not found during finalization.")
                    withContext(Dispatchers.Main) { _isLoading.postValue(false) }
                    // Optionally post error to LiveData
                    return@launch
                }

                // Check if already processed to prevent redundant updates (optional but good)
                if (bid.status == "completed") {
                    Log.i(
                        TAG,
                        "Bid $bidId already marked as completed. No further action needed for items."
                    )
                    withContext(Dispatchers.Main) { _isLoading.postValue(false) }
                    // Ensure banner is updated if it wasn't reflecting completion
                    _bidDetailsForBanner.postValue(bid) // Trigger banner update
                    return@launch
                }


                val updates = mutableMapOf<String, Any?>()

                // 2. Update Bid status in /Bids/
                updates["/Bids/$bidId/status"] = "completed"

                // 3. Update Requested Item (itemId in Bid object)
                if (bid.itemId.isNotBlank()) {
                    updates["/Items/${bid.itemId}/exchangeState"] = "exchanged"
                    updates["/Items/${bid.itemId}/available"] = false
                    updates["/Items/${bid.itemId}/lockedByBidId"] = null
                }

                // 4. Update Offered Items
                bid.offeredItemIds.forEach { offeredItemId ->
                    if (offeredItemId.isNotBlank()) {
                        updates["/Items/$offeredItemId/exchangeState"] = "exchanged"
                        updates["/Items/$offeredItemId/available"] = false
                        updates["/Items/$offeredItemId/lockedByBidId"] = null
                    }
                }

                // 5. Ensure ExchangeAgreement status is also "completed" (double check or set)
                //    FirebaseAgreementManager already sets this, but good to ensure consistency if called directly.
                updates["/ExchangeAgreements/$bidId/status"] = "completed"


                database.updateChildren(updates).await() // Perform batch update
                Log.i(TAG, "Main entities (Bid, Items, Agreement) for $bidId updated to completed/exchanged.")

                // Update UserRatingStats for totalExchanges and lastExchangeTimestamp
                if (bid.bidderId.isNotBlank()) {
                    updateUserStatsAfterExchange(bid.bidderId, System.currentTimeMillis())
                }
                if (bid.receiverId.isNotBlank()) {
                    updateUserStatsAfterExchange(bid.receiverId, System.currentTimeMillis())
                }
                Log.i(
                    TAG,
                    "Successfully finalized exchange: Bid $bidId and items updated to exchanged/completed."
                )

                // Post updated bid to trigger banner update if it's listening to _bidDetailsForBanner
                _bidDetailsForBanner.postValue(bid.copy(status = "completed"))


                // Optionally, trigger a feedback message for UI if needed via another LiveData
                // _userActionOutcome.postValue(UserActionOutcome(true, "Exchange completed successfully!"))

            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing completed exchange for bid $bidId: ${e.message}", e)
                // Optionally post error to LiveData
                // _userActionOutcome.postValue(UserActionOutcome(false, "Failed to finalize exchange: ${e.message}"))
            } finally {
                withContext(Dispatchers.Main) { _isLoading.postValue(false) }
            }
        }
    }

    private fun updateUserStatsAfterExchange(userId: String, exchangeCompletionTime: Long) {
        if (userId.isBlank()) return
        val userRatingsRef = database.child("Users").child(userId).child("ratings")
        userRatingsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val stats = mutableData.getValue(UserRatingStats::class.java)
                    ?: UserRatingStats()

                stats.totalExchanges = (stats.totalExchanges ?: 0) + 1 // Correct
                stats.lastExchangeTimestamp = exchangeCompletionTime    // Correct

                mutableData.value = stats
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (databaseError != null) {
                    Log.e(TAG, "Failed to update totalExchanges/lastTimestamp for user $userId: ${databaseError.message}")
                } else if (committed) {
                    Log.i(TAG, "totalExchanges/lastTimestamp updated for user $userId.")
                } else {
                    Log.w(TAG, "totalExchanges/lastTimestamp transaction not committed for $userId.")
                }
            }
        })
    }
    fun saveFeedbackAndPotentialReport(
        ratedOrReportedUserId: String,
        bidId: String,
        feedbackData: Feedback, // Contains all info: rating, tags, isReport, description
        isAlsoAReport: Boolean, // This is redundant if feedbackData.isReport is reliable
        reporterUserId: String,
        currentAgreement: ExchangeAgreement // Pass the current agreement state
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val userRatingsRef =
                database.child("Users").child(ratedOrReportedUserId).child("ratings")
            var reportIdForAgreement: String? = null

            // --- Step 1: If it's a report, create and save the Report object first ---
            if (feedbackData.isReport) {
                val reportsRef = database.child("reports")
                val newReportId = reportsRef.push().key
                if (newReportId == null) {
                    Log.e("transactionfailerating6", "Failed to generate report ID for bid $bidId")
                    _feedbackSubmissionResult.postValue(
                        FeedbackSubmissionResult(
                            false,
                            "Error submitting report (ID gen failed).",
                            true
                        )
                    )
                    return@launch
                }
                reportIdForAgreement = newReportId // Store for updating agreement

                val newReport = Report(
                    reportId = newReportId,
                    reporterUserId = reporterUserId,
                    reportedUserId = ratedOrReportedUserId,
                    bidId = bidId,
                    reportReasonTags = feedbackData.tags.map { it.label }, // Store string labels
                    reportDescription = feedbackData.reportDescription,
                    timestamp = feedbackData.timestamp,
                    status = "pending_review"
                )

                try {
                    reportsRef.child(newReportId).setValue(newReport)
                        .await() // Using await for sequential logic
                    Log.i("transactionfailerating3", "Report $newReportId saved for bid $bidId.")
                    // Optional: Update denormalized /userReports/ and /pendingReports/ here
                    database.child("userReports").child(ratedOrReportedUserId).child(newReportId)
                        .setValue(true).await()
                    database.child("pendingReports").child(newReportId).setValue(true).await()

                } catch (e: Exception) {
                    Log.e(
                        "transactionfailerating2",
                        "Failed to save report $newReportId: ${e.message}",
                        e
                    )
                    _feedbackSubmissionResult.postValue(
                        FeedbackSubmissionResult(
                            false,
                            "Error submitting report.",
                            true
                        )
                    )
                    return@launch
                }
            }

            // --- Step 2: Update UserRatingStats (Transaction) ---
//            userRatingsRef.runTransaction(object : Transaction.Handler {
//                override fun doTransaction(mutableData: MutableData): Transaction.Result {
//                    val stats = mutableData.getValue(UserRatingStats::class.java)
//                        ?: UserRatingStats() // Initialize if null
//
//                    // Add/Update feedback in the list
//                    val updatedFeedbackList = stats.feedbackList.toMutableMap()
//                    updatedFeedbackList[bidId] = feedbackData // Use bidId as key
//                    stats.feedbackList = updatedFeedbackList
//
//                    // Update overall rating stats (only if not solely a report with 0 rating, or if you always count the implicit rating)
//                    if (feedbackData.rating > 0) { // Assuming report implies at least 1 star
//                        // Recalculate totalStars and averageRating robustly
//                        var newTotalStars = 0
//                        var validRatingsCount = 0
//                        stats.feedbackList.values.forEach { fb ->
//                            if (fb.rating > 0) { // Only consider actual ratings for average
//                                newTotalStars += fb.rating
//                                validRatingsCount++
//                            }
//                        }
//                        stats.totalStarsReceived = newTotalStars
//                        stats.averageRating = if (validRatingsCount > 0) {
//                            newTotalStars.toFloat() / validRatingsCount
//                        } else {
//                            0f
//                        }
//                        // Update totalExchanges - be careful not to double count if both users rate
//                        // This might need more complex logic based on ExchangeAgreement status
//                        // For simplicity, let's assume an exchange is counted when feedback is given
//                        if (stats.feedbackList.containsKey(bidId) && stats.feedbackList[bidId]?.rating ?: 0 > 0) {
//                            // This logic for totalExchanges might need refinement.
//                            // It's better to increment totalExchanges when the ExchangeAgreement status becomes "completed".
//                            // For now, this is a placeholder.
//                        }
//
//                    }
//
//                    if (feedbackData.isReport) {
//                        stats.reportCount = (stats.reportCount ?: 0) + 1
//                    }
            userRatingsRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val stats = mutableData.getValue(UserRatingStats::class.java)
                        ?: UserRatingStats()

                    // Add/Update this specific feedback entry
                    val updatedFeedbackList = stats.feedbackList.toMutableMap()
                    updatedFeedbackList[bidId] = feedbackData
                    stats.feedbackList = updatedFeedbackList

                    // --- Recalculate Rating Statistics ---
                    var currentTotalStars = 0
                    var validRatingsCount = 0
                    var currentHighestRating = 0
                    var currentLowestRating = 5
                    val freshRatingBreakdown = mutableMapOf("1_star" to 0, "2_star" to 0, "3_star" to 0, "4_star" to 0, "5_star" to 0)
                    var firstValidRatingFound = false

                    stats.feedbackList.values.forEach { receivedFeedback ->
                        if (receivedFeedback.rating in 1..5) {
                            currentTotalStars += receivedFeedback.rating
                            validRatingsCount++

                            if (!firstValidRatingFound) {
                                currentHighestRating = receivedFeedback.rating
                                currentLowestRating = receivedFeedback.rating
                                firstValidRatingFound = true
                            } else {
                                if (receivedFeedback.rating > currentHighestRating) {
                                    currentHighestRating = receivedFeedback.rating
                                }
                                if (receivedFeedback.rating < currentLowestRating) {
                                    currentLowestRating = receivedFeedback.rating
                                }
                            }
                            val key = "${receivedFeedback.rating}_star"
                            freshRatingBreakdown[key] = (freshRatingBreakdown[key] ?: 0) + 1
                        }
                    }

                    stats.totalStarsReceived = currentTotalStars
                    stats.averageRating = if (validRatingsCount > 0) currentTotalStars.toFloat() / validRatingsCount else 0f
                    stats.ratingBreakdown = freshRatingBreakdown
                    stats.highestRating = if (validRatingsCount > 0) currentHighestRating else 0
                    stats.lowestRating = if (validRatingsCount > 0) currentLowestRating else 5

                    // --- Update reportCount ---
                    if (feedbackData.isReport) {
                        stats.reportCount = (stats.reportCount ?: 0) + 1
                    }
                    mutableData.value = stats
                    return Transaction.success(mutableData)
                }

                override fun onComplete(
                    databaseError: DatabaseError?,
                    committed: Boolean,
                    dataSnapshot: DataSnapshot?
                ) {
                    if (databaseError != null) {
                        Log.e(
                            "transactionfailerating",
                            "UserRatingStats transaction failed for bid $bidId: ${databaseError.message}"
                        )
                        _feedbackSubmissionResult.postValue(
                            FeedbackSubmissionResult(
                                false,
                                "Failed to update user stats.",
                                feedbackData.isReport
                            )
                        )
                    } else if (committed) {
                        Log.i(
                            "transactionfailerating1",
                            "UserRatingStats updated for user $ratedOrReportedUserId due to bid $bidId."
                        )
                        // Proceed to update ExchangeAgreement only after stats are committed
                        updateExchangeAgreementAfterFeedback(
                            bidId,
                            reporterUserId,
                            feedbackData.isReport,
                            reportIdForAgreement,
                            currentAgreement
                        )
                    } else {
                        _feedbackSubmissionResult.postValue(
                            FeedbackSubmissionResult(
                                false,
                                "Could not save rating/report. Please try again.",
                                feedbackData.isReport
                            )
                        )
                    }
                }
            })
        }
    }

    private fun updateExchangeAgreementAfterFeedback(
        bidId: String,
        reporterOrRaterId: String,
        wasReported: Boolean,
        reportId: String?, // Null if not a report
        initialAgreementState: ExchangeAgreement // Use the state passed from fragment
    ) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val agreementRef = database.child("ExchangeAgreements").child(bidId)
            // Use a transaction for ExchangeAgreement as well to avoid race conditions
            // if both users try to update simultaneously (though less critical than stats).
            agreementRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val agreement = mutableData.getValue(ExchangeAgreement::class.java)
                        ?: initialAgreementState // Fallback to initial state if null from DB (should not happen ideally)
                        ?: return Transaction.abort() // Critical: if agreement is null, abort.

                    if (reporterOrRaterId == agreement.bidderId) {
                        agreement.ratingGivenByBidder = true
                        if (wasReported) {
                            agreement.reportFiledByBidder = true
                            agreement.bidderReportId = reportId
                        }
                    } else if (reporterOrRaterId == agreement.receiverId) {
                        agreement.ratingGivenByReceiver = true
                        if (wasReported) {
                            agreement.reportFiledByReceiver = true
                            agreement.receiverReportId = reportId
                        }
                    } else {
                        Log.w(
                            "transactionfailerating4",
                            "Reporter/Rater ID $reporterOrRaterId does not match bidder/receiver for bid $bidId"
                        )
                        // Do not modify if ID doesn't match, but don't abort transaction for other valid updates.
                    }
                    mutableData.value = agreement
                    return Transaction.success(mutableData)
                }

                override fun onComplete(
                    databaseError: DatabaseError?,
                    committed: Boolean,
                    dataSnapshot: DataSnapshot?
                ) {
                    if (databaseError != null) {
                        Log.e(
                            "transactionfailerating5",
                            "ExchangeAgreement update failed for bid $bidId: ${databaseError.message}"
                        )
                        _feedbackSubmissionResult.postValue(
                            FeedbackSubmissionResult(
                                false,
                                "Failed to finalize exchange details.",
                                wasReported
                            )
                        )
                    } else if (committed) {
                        Log.i(
                            "transactionfailerating6",
                            "ExchangeAgreement updated for bid $bidId after feedback/report."
                        )
                        _feedbackSubmissionResult.postValue(
                            FeedbackSubmissionResult(
                                true,
                                if (wasReported) "Report submitted successfully." else "Rating submitted successfully.",
                                wasReported
                            )
                        )
                    } else {
                        // Transaction not committed, likely due to stale data or contention
                        _feedbackSubmissionResult.postValue(
                            FeedbackSubmissionResult(
                                false,
                                "Could not update exchange status. Please try again.",
                                wasReported
                            )
                        )
                    }
                }
            })
        }
    }

    // Helper data class for LiveData result
    data class FeedbackSubmissionResult(
        val success: Boolean,
        val message: String,
        val isReport: Boolean
    )


    fun cancelActiveExchange(bidToCancel: Bid, cancellingUserId: String) {
        val currentBidStatus = bidToCancel.status
        if (currentBidStatus != "started" && currentBidStatus != "agreement_reached") {
            Log.w(
                "ViewModelCancel",
                "Cannot cancel bid ${bidToCancel.bidId}, status is $currentBidStatus."
            )
            _cancellationResult.postValue(
                Pair(
                    false,
                    "Exchange cannot be canceled at this stage."
                )
            ) // Corrected post
            return
        }

        _isLoadingCancellation.postValue(true)

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                val newStatus: String
                val canceledReason: String

                if (cancellingUserId == bidToCancel.bidderId) {
                    newStatus = "canceledByBidder"
                    canceledReason = "Exchange canceled by bidder."
                } else if (cancellingUserId == bidToCancel.receiverId) {
                    newStatus = "canceledByReceiver"
                    canceledReason = "Exchange canceled by item owner."
                } else {
                    Log.e(
                        "ViewModelCancel",
                        "User $cancellingUserId is not part of bid ${bidToCancel.bidId}."
                    )
                    withContext(Dispatchers.Main) {
                        _isLoadingCancellation.postValue(false)
                        _cancellationResult.postValue(
                            Pair(
                                false,
                                "Unauthorized cancellation."
                            )
                        ) // Corrected post
                    }
                    return@launch
                }

                val updates = mutableMapOf<String, Any?>()
                updates["Bids/${bidToCancel.bidId}/status"] = newStatus
                updates["Bids/${bidToCancel.bidId}/canceledReason"] = canceledReason
                updates["Bids/${bidToCancel.bidId}/isCancelledDueToItem"] = false

                // Release items involved in the bid being canceled
                val itemsToRelease = mutableListOf(bidToCancel.itemId)
                itemsToRelease.addAll(bidToCancel.offeredItemIds)
                for (itemIdToRelease in itemsToRelease.distinct().filter { it.isNotBlank() }) {
                    val itemSnapshot = database.child("Items").child(itemIdToRelease).get().await()
                    val item = itemSnapshot.getValue(Item::class.java)
                    if (item != null && item.lockedByBidId == bidToCancel.bidId) {
                        updates["Items/$itemIdToRelease/available"] = (item.status == "approved")
                        updates["Items/$itemIdToRelease/exchangeState"] = "none"
                        updates["Items/$itemIdToRelease/lockedByBidId"] = null
                        Log.d(
                            "ViewModelCancel",
                            "Prepared release for item $itemIdToRelease from bid ${bidToCancel.bidId}"
                        )
                    }
                }

                // Fetch and prepare updates for other bids that were on hold FOR THIS ITEM
                if (bidToCancel.itemId.isNotBlank()) {
                    // This callback structure is necessary because getOnHoldBidsForItemNonSuspending is async
                    bidRepo.getOnHoldBidsForItemNonSuspending( // Assumes this is the correct repo method
                        requestedItemId = bidToCancel.itemId,
                        // No need to exclude bidToCancel.bidId if we are fetching by "on_hold_item_negotiation" status
                        onResult = { otherBidsPreviouslyOnHold ->
                            if (otherBidsPreviouslyOnHold.isNotEmpty()) {
                                otherBidsPreviouslyOnHold.forEach { otherBid ->
                                    // Double check it's not the one we are already cancelling, just in case
                                    if (otherBid.bidId != bidToCancel.bidId) {
                                        updates["Bids/${otherBid.bidId}/status"] =
                                            "pending" // Revert to pending
                                        Log.d(
                                            "ViewModelCancel",
                                            "Prepared revert for on-hold bid ${otherBid.bidId} to pending."
                                        )
                                        // TODO: Notify bidder of otherBid that item is available again
                                    }
                                }
                            }

                            // All updates are now in the map, execute them in a new coroutine context
                            // to ensure they happen after the callback and on IO dispatcher
                            viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
                                try {
                                    database.updateChildren(updates).await()
                                    Log.i(
                                        "ViewModelCancel",
                                        "Bid ${bidToCancel.bidId} canceled. Items and other bids updated."
                                    )
                                    withContext(Dispatchers.Main) {
                                        _isLoadingCancellation.postValue(false)
                                        _cancellationResult.postValue(
                                            Pair(
                                                true,
                                                "Exchange has been canceled."
                                            )
                                        )
                                        // TODO: Notify the OTHER party of this cancellation.
                                    }
                                } catch (dbUpdateException: Exception) {
                                    Log.e(
                                        "ViewModelCancel",
                                        "Error executing final DB updates for cancellation ${bidToCancel.bidId}: ${dbUpdateException.message}",
                                        dbUpdateException
                                    )
                                    withContext(Dispatchers.Main) {
                                        _isLoadingCancellation.postValue(false)
                                        _cancellationResult.postValue(
                                            Pair(
                                                false,
                                                "Failed to fully process cancellation: ${dbUpdateException.message}"
                                            )
                                        )
                                    }
                                }
                            }
                        } // End of onResult for getOnHoldBidsForItemNonSuspending
                    )
                } else {
                    // No requested item ID on bidToCancel, or no other bids to update.
                    // Just update the main bid and its items.
                    database.updateChildren(updates).await()
                    Log.i(
                        "ViewModelCancel",
                        "Bid ${bidToCancel.bidId} (no itemId or no other bids) canceled. Items released."
                    )
                    withContext(Dispatchers.Main) {
                        _isLoadingCancellation.postValue(false)
                        _cancellationResult.postValue(Pair(true, "Exchange has been canceled."))
                        // TODO: Notify the OTHER party.
                    }
                }
            } catch (e: Exception) { // Catch for outer try (e.g., item fetching errors before other bids cb)
                Log.e(
                    "ViewModelCancel",
                    "Error in cancelActiveExchange for ${bidToCancel.bidId}: ${e.message}",
                    e
                )
                withContext(Dispatchers.Main) {
                    _isLoadingCancellation.postValue(false)
                    _cancellationResult.postValue(
                        Pair(
                            false,
                            "Failed to cancel exchange: ${e.message}"
                        )
                    )
                }
            }
        }
    }


    fun clearChatItemsError() {
        _chatItemsError.value = null // Or postValue(null) if from background
    }

    data class ExchangeInitiationData(
        val bidId: String,           // The ID of the bid that was started
        val actualRoomName: String,  // The name of the chat room (could be existing or new)
        val friendIdForChat: String, // The ID of the other user in the chat
        val friendName: String,      // The name of the other user
        val friendImage: String      // The image URL of the other user
    )

    private val _exchangeInitiationResult =
        SingleLiveEvent<Pair<Boolean, ExchangeInitiationData?>>()
    val exchangeInitiationResult: LiveData<Pair<Boolean, ExchangeInitiationData?>> =
        _exchangeInitiationResult // Expose as LiveData


    fun clearInitiationError() {
        _initiationError.postValue(null)
    }


    fun getUsers(): LiveData<List<User>> {

        ///.differnt Type
        return usersRepo.getExchangeChatUsers()
    }

    fun saveRatingToUser(userId: String, feedback: Feedback, bidId: String) {
        val userRatingRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(userId)
            .child("ratings")

        userRatingRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.getValue(UserRatingStats::class.java) ?: UserRatingStats()

                // Update general stats
                stats.totalExchanges += 1
                stats.totalStarsReceived += feedback.rating
                stats.averageRating = stats.totalStarsReceived.toFloat() / stats.totalExchanges
                stats.highestRating = maxOf(stats.highestRating, feedback.rating)
                stats.lowestRating = minOf(stats.lowestRating, feedback.rating)
                stats.lastExchangeTimestamp = feedback.timestamp

                // Update breakdown
                val breakdown = stats.ratingBreakdown.toMutableMap()
                val key = "${feedback.rating}_star"
                breakdown[key] = (breakdown[key] ?: 0) + 1
                stats.ratingBreakdown = breakdown

                // Update feedback map by bidId
                val updatedFeedbackMap = stats.feedbackList.toMutableMap()
                updatedFeedbackMap[bidId] = feedback
                stats.feedbackList = updatedFeedbackMap

                currentData.value = stats
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (committed) {
                    Log.d("Rating", "Rating and feedback successfully updated for bid: $bidId")
                } else {
                    Log.e("Rating", "Rating update failed: ${error?.message}")
                }
            }
        })
    }


    // Inside ExchangeAppViewModel.kt

// ... (Existing LiveData: _exchangeInitiationResult, _isLoadingInitiation, _initiationError) ...
// ... (Existing Repositories: bidRepo, itemsRepo, usersRepo) ...
// ... (Existing database reference: private val database = FirebaseDatabase.getInstance().reference) ...
// ... (Existing coroutineExceptionHandler) ...

    // Inside ExchangeAppViewModel.kt

// ... (Ensure _isLoadingInitiation, _initiationError, and _exchangeInitiationResult LiveData are defined as SingleLiveEvent for _exchangeInitiationResult)
// private val _isLoadingInitiation = MutableLiveData<Boolean>(false)
// private val _initiationError = MutableLiveData<String?>()
// private val _exchangeInitiationResult = SingleLiveEvent<Pair<Boolean, ExchangeInitiationData?>>() // Correct type

    fun initiateExchange(bidToStart: Bid, desiredRoomName: String) {
        if (bidToStart.status != "pending") {
            Log.w(
                "ViewModel",
                "Cannot initiate: Bid ${bidToStart.bidId} is not 'pending' (status: ${bidToStart.status})."
            )
            _initiationError.postValue("This offer is no longer pending.")
            _exchangeInitiationResult.postValue(Pair(false, null)) // Corrected
            return
        }

        _isLoadingInitiation.postValue(true)
        _initiationError.postValue(null)

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                val requestedItemSnapshot =
                    database.child("Items").child(bidToStart.itemId).get().await()
                val requestedItemCurrent = requestedItemSnapshot.getValue(Item::class.java)

                if (requestedItemCurrent == null ||
                    !requestedItemCurrent.available ||
                    requestedItemCurrent.status != "approved" ||
                    requestedItemCurrent.exchangeState != "none"
                ) {
                    Log.w(
                        "ViewModel",
                        "Requested item ${bidToStart.itemId} is not ready for negotiation. Aborting."
                    )
                    withContext(Dispatchers.Main) {
                        _isLoadingInitiation.postValue(false)
                        _initiationError.postValue("The requested item is no longer available or is in another exchange.")
                        _exchangeInitiationResult.postValue(Pair(false, null)) // Corrected
                    }
                    return@launch
                }

                var allOfferedItemsReady = true
                for (offeredId in bidToStart.offeredItemIds.filter { it.isNotBlank() }) {
                    val offeredItemSnapshot = database.child("Items").child(offeredId).get().await()
                    val offeredItem = offeredItemSnapshot.getValue(Item::class.java)
                    if (offeredItem == null ||
                        !offeredItem.available ||
                        offeredItem.status != "approved" ||
                        offeredItem.exchangeState != "none"
                    ) {
                        allOfferedItemsReady = false
                        Log.w(
                            "ViewModel",
                            "Offered item $offeredId is not ready for negotiation. Aborting."
                        )
                        break
                    }
                }

                if (!allOfferedItemsReady) {
                    withContext(Dispatchers.Main) {
                        _isLoadingInitiation.postValue(false)
                        _initiationError.postValue("One or more offered items are no longer available or are in another exchange.")
                        _exchangeInitiationResult.postValue(Pair(false, null)) // Corrected
                    }
                    return@launch
                }

                val updates = mutableMapOf<String, Any?>()
                updates["Bids/${bidToStart.bidId}/status"] = "started"
                updates["Items/${bidToStart.itemId}/available"] = false
                updates["Items/${bidToStart.itemId}/exchangeState"] = "in_negotiation"
                updates["Items/${bidToStart.itemId}/lockedByBidId"] = bidToStart.bidId

                bidToStart.offeredItemIds.forEach { offeredId ->
                    if (offeredId.isNotBlank()) {
                        updates["Items/$offeredId/available"] = false
                        updates["Items/$offeredId/exchangeState"] = "in_negotiation"
                        updates["Items/$offeredId/lockedByBidId"] = bidToStart.bidId
                    }
                }

                bidRepo.getOtherPendingBidsForItemNonSuspending(
                    bidToStart.itemId,
                    bidToStart.bidId
                ) { otherPendingBids ->
                    if (otherPendingBids.isNotEmpty()) {
                        otherPendingBids.forEach { otherBid ->
                            updates["Bids/${otherBid.bidId}/status"] = "on_hold_item_negotiation"
                        }
                    }

                    viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
                        try {
                            database.updateChildren(updates).await()
                            Log.i(
                                "ViewModel",
                                "Statuses updated for bid ${bidToStart.bidId} and related items/bids."
                            )

                            createRoomIfNotExistsInternal(
                                bidToStart,
                                desiredRoomName
                            ) { success, actualRoomName, friendDetails ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    _isLoadingInitiation.postValue(false)
                                    if (success && friendDetails != null) {
                                        val initiationData = ExchangeInitiationData(
                                            bidId = bidToStart.bidId,
                                            actualRoomName = actualRoomName,
                                            friendIdForChat = friendDetails.first,
                                            friendName = friendDetails.second,
                                            friendImage = friendDetails.third
                                        )
                                        _exchangeInitiationResult.postValue(
                                            Pair(
                                                true,
                                                initiationData
                                            )
                                        ) // Corrected
                                    } else {
                                        _initiationError.postValue("Failed to prepare the exchange chat room.")
                                        _exchangeInitiationResult.postValue(
                                            Pair(
                                                false,
                                                null
                                            )
                                        ) // Corrected
                                        Log.e(
                                            "ViewModel",
                                            "Room creation failed for bid ${bidToStart.bidId}."
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "ViewModel",
                                "Error executing updates or room creation for bid ${bidToStart.bidId}: ${e.message}",
                                e
                            )
                            withContext(Dispatchers.Main) {
                                _isLoadingInitiation.postValue(false)
                                _initiationError.postValue("An error occurred: ${e.message}")
                                _exchangeInitiationResult.postValue(Pair(false, null)) // Corrected
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "ViewModel",
                    "Outer exception in initiateExchange for bid ${bidToStart.bidId}: ${e.message}",
                    e
                )
                withContext(Dispatchers.Main) {
                    _isLoadingInitiation.postValue(false)
                    _initiationError.postValue("An error occurred: ${e.message}")
                    _exchangeInitiationResult.postValue(Pair(false, null)) // Corrected
                }
            }
        }
    }

    internal fun createRoomIfNotExistsInternal(
        bid: Bid,
        roomName: String,
        onComplete: (success: Boolean, actualRoomName: String, friendDetails: Triple<String, String, String>?) -> Unit
        // friendDetails: Triple(friendId, friendName, friendImage)
    ) {
        val currentAuthUserId = Utils.getUidLoggedIn()
        val senderIdForConv = bid.bidderId // The one who placed the bid
        val receiverIdForConv = bid.receiverId // The one who owns the requested item

        // Determine who the "friend" is from the perspective of the current logged-in user
        val friendId =
            if (currentAuthUserId == senderIdForConv) receiverIdForConv else senderIdForConv

        if (senderIdForConv.isBlank() || receiverIdForConv.isBlank() || bid.bidId.isBlank()) {
            onComplete(false, roomName, null)
            return
        }

        val db = FirebaseDatabase.getInstance().reference
        val user1Path =
            if (currentAuthUserId == senderIdForConv) senderIdForConv else receiverIdForConv
        val user2Path = friendId

        val chatRoomRefUser1View = db.child("Conversations").child("ExchangeChats").child(user1Path)
            .child(user2Path).child(bid.bidId)

        chatRoomRefUser1View.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val actualRoomNameFromDb =
                    snapshot.child("roomName").getValue(String::class.java) ?: roomName
                val friendNameFromDb = snapshot.child("name").getValue(String::class.java)
                val friendImageFromDb = snapshot.child("friendsimage").getValue(String::class.java)


                if (!snapshot.exists()) {
                    Log.d("ViewModel", "Room does not exist for bid ${bid.bidId}, creating...")
                    // Fetch details for BOTH users to populate RecentChats for each other
                    usersRepo.getUserInfo(senderIdForConv) { sName, sImage ->
                        usersRepo.getUserInfo(receiverIdForConv) { rName, rImage ->
                            val chatTime = System.currentTimeMillis()
                            val sNameVal = sName ?: "User"
                            val sImageVal = sImage ?: ""
                            val rNameVal = rName ?: "User"
                            val rImageVal = rImage ?: ""

                            val senderPovRecentChat = mapOf(
                                "bidId" to bid.bidId,
                                "friendid" to receiverIdForConv,
                                "time" to chatTime,
                                "sender" to senderIdForConv,
                                "message" to "Exchange started!",
                                "friendsimage" to rImageVal,
                                "name" to rNameVal,
                                "person" to "you",
                                "roomName" to actualRoomNameFromDb
                            )
                            val receiverPovRecentChat = mapOf(
                                "bidId" to bid.bidId,
                                "friendid" to senderIdForConv,
                                "time" to chatTime,
                                "sender" to senderIdForConv,
                                "message" to "Exchange started!",
                                "friendsimage" to sImageVal,
                                "name" to sNameVal,
                                "person" to sNameVal,
                                "roomName" to actualRoomNameFromDb
                            )

                            val updates = hashMapOf<String, Any>(
                                "Conversations/ExchangeChats/$senderIdForConv/$receiverIdForConv/${bid.bidId}" to senderPovRecentChat,
                                "Conversations/ExchangeChats/$receiverIdForConv/$senderIdForConv/${bid.bidId}" to receiverPovRecentChat
                            )
                            db.updateChildren(updates).addOnCompleteListener { task ->
                                val friendDetailsToPass =
                                    if (currentAuthUserId == senderIdForConv) Triple(
                                        receiverIdForConv,
                                        rNameVal,
                                        rImageVal
                                    ) else Triple(senderIdForConv, sNameVal, sImageVal)
                                onComplete(
                                    task.isSuccessful,
                                    actualRoomNameFromDb,
                                    friendDetailsToPass
                                )
                            }
                        }
                    }
                } else {
                    Log.d(
                        "ViewModel",
                        "Room already exists for bid ${bid.bidId}. Friend Name: $friendNameFromDb"
                    )
                    val friendDetailsToPass =
                        Triple(friendId, friendNameFromDb ?: "Partner", friendImageFromDb ?: "")
                    onComplete(true, actualRoomNameFromDb, friendDetailsToPass)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onComplete(false, roomName, null)
            }
        })
    }


    private val _bidDetails = MutableLiveData<Bid?>()
    val bidDetails: LiveData<Bid?> = _bidDetails

// ... (existing coroutineExceptionHandler, init, other methods)

    // --- NEW: Method to fetch/update current bid details ---
    fun fetchBidDetailsForMenu(bidIdToFetch: String) {
        // Only fetch if different from current or if current is null
        if (bidDetails.value?.bidId == bidIdToFetch && bidDetails.value != null) {
            Log.d(
                "bidDetailsInViewExchange",
                "Bid details for $bidIdToFetch already present in ViewModel."
            )
            return
        }
        Log.d("bidDetailsInViewExchange1", "Fetching bid details for menu update: $bidIdToFetch")
        bidRepo.getBidById(
            bidIdToFetch,
            onSuccess = { bid ->
                _bidDetails.postValue(bid)
            },
            onFailure = { error ->
                Log.e("bidDetailsInViewExchange2", "Failed to fetch bid details for menu: $error")
                _bidDetails.postValue(null) // Post null on failure
            }
        )
    }

    // --- NEW: Method for internal update of bid details if needed by other VM functions ---
    fun updateCurrentBidDetails(bid: Bid?) { // Can be called if another part of VM updates the bid
        _bidDetails.postValue(bid)
    }

    fun createExchangeRoomIfNotExists(bidId: String, roomName: String, onComplete: () -> Unit) {
        this.bidId = bidId
        bidRepo.getBidById(bidId, onSuccess = { bid ->

            val senderId = bid.bidderId
            val receiverId = bid.receiverId
            val db = FirebaseDatabase.getInstance().reference
            val chatRoomRef = db.child("Conversations").child("ExchangeChats").child(senderId)
                .child(receiverId).child(bidId)

            chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        usersRepo.getUserInfo(senderId) { senderName, senderImage ->
                            usersRepo.getUserInfo(receiverId) { receiverName, receiverImage ->

                                val chatTime = System.currentTimeMillis()

                                val senderConversation = mapOf(
                                    "bidId" to bidId,
                                    "friendid" to receiverId,
                                    "time" to chatTime,
                                    "sender" to senderId,
                                    "message" to "",
                                    "friendsimage" to receiverImage,
                                    "name" to receiverName,
                                    "person" to "you",
                                    "roomName" to roomName
                                )

                                val receiverConversation = mapOf(
                                    "bidId" to bidId,
                                    "friendid" to senderId,
                                    "time" to chatTime,
                                    "sender" to senderId,
                                    "message" to "",
                                    "friendsimage" to senderImage,
                                    "name" to senderName,
                                    "person" to senderName,
                                    "roomName" to roomName
                                )

                                db.child("Conversations").child("ExchangeChats").child(senderId)
                                    .child(receiverId).child(bidId).setValue(senderConversation)

                                db.child("Conversations").child("ExchangeChats").child(receiverId)
                                    .child(senderId).child(bidId).setValue(receiverConversation)
                                    .addOnCompleteListener {
                                        onComplete() // ✅ notify when done
                                    }
                            }
                        }
                    } else {
                        Log.d("ExchangeRoom", "Room already exists. Skipping creation.")
                        onComplete() // ✅ call anyway
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ExchangeRoom", "Error checking room existence: ${error.message}")
                    onComplete() // ✅ still call to prevent blocking
                }
            })
        }, onFailure = {
            onComplete() // still call it
        })
    }

    // Inside ExchangeAppViewModel.kt


    private val _exchangeChatItems = MutableLiveData<List<Item>?>() // Nullable initially
    val exchangeChatItems: LiveData<List<Item>?> = _exchangeChatItems

    private val _isLoadingChatItems = MutableLiveData<Boolean>()
    val isLoadingChatItems: LiveData<Boolean> = _isLoadingChatItems

    // Keep track of the bid ID whose items are currently loaded
    private var currentLoadedBidId: String? = null

    fun loadExchangeItemsForChat(bidId: String, forceReload: Boolean = false) {
        // Only fetch if the bidId changed or if forceReload is true, or if data is not yet loaded
        if (!forceReload && bidId == currentLoadedBidId && _exchangeChatItems.value != null) {
            Log.d("ViewModel", "Exchange items for bid $bidId already loaded.")
            return
        }

        Log.d("ViewModel", "Loading exchange items for bid $bidId...")
        _isLoadingChatItems.postValue(true)
        _exchangeChatItems.postValue(null) // Clear previous items while loading new ones
        currentLoadedBidId = bidId // Store the bid ID being loaded

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) { // Use IO + handler
            try {
                // 1. Get the Bid
                // You might need a suspending version of getBidById or use Tasks.await() if not already done
                // For simplicity, assuming bidsRepo.getBidById is synchronous or handled correctly
                bidRepo.getBidById(
                    bidId,
                    onSuccess = { bid ->
                        // 2. Get the associated items
                        itemsRepo.getExchangeChatItems(
                            bid,
                            onSuccess = { requestedItem, offeredItemsList ->
                                val allItemsInvolved = mutableListOf<Item>()
                                requestedItem?.let { allItemsInvolved.add(it) }
                                allItemsInvolved.addAll(offeredItemsList)

                                // Filter for available items
                                val availableItems = allItemsInvolved.filter { item ->
                                    item.available && item.status.equals(
                                        "approved",
                                        ignoreCase = true
                                    )
                                }

                                _exchangeChatItems.postValue(availableItems) // Update LiveData
                                _isLoadingChatItems.postValue(false)
                                Log.d(
                                    "ViewModel",
                                    "Loaded ${availableItems.size} available items for bid $bidId."
                                )
                            },
                            onFailure = { exception ->
                                Log.e(
                                    "ViewModel",
                                    "Failed to fetch items for bid $bidId: ${exception.message}"
                                )
                                _exchangeChatItems.postValue(emptyList()) // Post empty list on failure
                                _isLoadingChatItems.postValue(false)
                            }
                        )
                    },
                    onFailure = { errorMsg ->
                        Log.e(
                            "ViewModel",
                            "Failed to fetch bid $bidId for loading items: $errorMsg"
                        )
                        _exchangeChatItems.postValue(emptyList()) // Post empty list on failure
                        _isLoadingChatItems.postValue(false)
                    }
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Exception during loadExchangeItemsForChat: ${e.message}")
                _exchangeChatItems.postValue(emptyList())
                _isLoadingChatItems.postValue(false)
            }
        }
    }


    fun sendMessage(
        sender: String,
        receiver: String,
        bidId: String,
        friendname: String,
        friendimage: String
    ) =
        viewModelScope.launch(Dispatchers.IO) {

            val context = MyApplication.instance.applicationContext
//            Log.e("send message", "send message inchatviewmodel")

            val chatTime = System.currentTimeMillis()
            val msgMap = mapOf(
                "sender" to sender,
                "receiver" to receiver,
                "message" to message.value!!,
                "time" to chatTime,
                "bidId" to bidId// we can also specify "" here
            )
            //ch
            val chatRoomId = "$bidId-${listOf(sender, receiver).sorted().joinToString("")}"

            // Store the actual message
            database.child("Messages").child("ExchangeChats").child(chatRoomId).child("messages")
                .child(chatTime.toString()).setValue(msgMap).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        message.postValue("")

                        val sharedPrefs = SharedPrefs()
                        val senderName = sharedPrefs.getValue("username") ?: "Sender"
                        val senderImage = imageUrl.value ?: "default_img_url"

                        // Correct structure: Conversation / senderId / receiverId
                        val senderConversation = mapOf(
                            "bidId" to bidId,
                            "friendid" to receiver,
                            "time" to chatTime,
                            "sender" to sender,
                            "message" to msgMap["message"]!!,
                            "friendsimage" to friendimage,
                            "name" to friendname,
                            "person" to "you"
                        )

                        val receiverConversation = mapOf(
                            "bidId" to bidId,
                            "friendid" to sender,
                            "time" to chatTime,
                            "sender" to sender,
                            "message" to msgMap["message"]!!,
                            "friendsimage" to senderImage,
                            "name" to senderName,
                            "person" to senderName
                        )

                        // ✅ Correct Firebase paths
                        database.child("Conversations").child("ExchangeChats").child(sender)
                            .child(receiver).child(
                                bidId.toString()
                            ).updateChildren(senderConversation)
//                        Log.e("receiverid", "receiverid $receiver")
                        database.child("Conversations").child("ExchangeChats").child(receiver)
                            .child(sender).child(
                                bidId.toString()
                            ).updateChildren(receiverConversation)

                        // ✅ Push Notification
                        database.child("Tokens").child(receiver).get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.getValue(Token::class.java)?.let { tokenObj ->
                                    val message = msgMap["message"] as? String ?: "New message"
                                    val userName = senderName.split(" ").firstOrNull() ?: "User"

                                    val notification = PushNotification(
                                        NotificationData(userName, message), tokenObj.token ?: ""
                                    )
                                    sendNotification(notification)
                                }
                            }
                    }
                }
        }


    fun getMessages(friend: String, bidId: String): LiveData<List<Messages>> {
        return messageRepo.getExchangeMessages(friend, bidId)
    }

    fun getRecentUsersAllChats(): LiveData<List<RecentChats>> {
        Log.e("Auth for user", "Auth Calling")

        return chatlistRepo.getAllExchangeChatsForCurrentUser()
    }

    fun sendNotification(notification: PushNotification) = viewModelScope.launch {
        try {
            RetrofitInstance.api.postNotification(notification)
        } catch (e: Exception) {
            Log.e("ViewModelError", e.toString())
        }
    }

    fun getCurrentUser() = viewModelScope.launch(Dispatchers.IO) {
//        val context = MyApplication.instance.applicationContext
        val userId = Utils.getUidLoggedIn()
//Updation
        database.child("Users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(User::class.java)?.let { user ->
                    name.postValue(user.fullName!!)
                    imageUrl.postValue(user.imageUrl!!)

                    SharedPrefs().setValue("username", user.fullName!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    fun updateProfile() = viewModelScope.launch(Dispatchers.IO) {
        val context = MyApplication.instance.applicationContext
        val userId = Utils.getUidLoggedIn()

        val updateMap = mapOf(
            "username" to name.value!!, "imageUrl" to imageUrl.value!!
        )

        database.child("Users").child(userId).updateChildren(updateMap).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
            }
        }

        val sharedPrefs = SharedPrefs()
        val friendId = sharedPrefs.getValue("friendid") ?: return@launch

        val updateChatMap = mapOf(
            "friendsimage" to imageUrl.value!!, "name" to name.value!!, "person" to name.value!!
        )

        database.child("Conversation$friendId").child(userId).updateChildren(updateChatMap)
        database.child("Conversation$userId").child(friendId).child("person").setValue("you")
    }
}


//listners
//package com.example.signuplogina.mvvm
//
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//// Import necessary classes from your project
//import com.example.signuplogina.*
//import com.example.signuplogina.fragments.HomeChatExchangeFragmentArgs
//import com.example.signuplogina.modal.Messages
//import com.example.signuplogina.modal.RecentChats
//import com.example.signuplogina.modal.Feedback
//import com.example.signuplogina.modal.UserRatingStats
//import com.example.signuplogina.notifications.entity.NotificationData
//import com.example.signuplogina.notifications.entity.PushNotification
//import com.example.signuplogina.notifications.entity.Token
//import com.example.signuplogina.notifications.network.RetrofitInstance
//import com.google.firebase.database.*
//import kotlinx.coroutines.CoroutineExceptionHandler
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.util.*
//
//// Simplified ViewModel without real-time listeners for bid/item state
//class ExchangeAppViewModel : ViewModel() {
//
//    // --- LiveData for UI binding and basic info ---
//    val message = MutableLiveData<String>() // For message input binding
//    val name = MutableLiveData<String>() // Current User Name
//    val imageUrl = MutableLiveData<String>() // Current User Image Url
//
//    // Feedback for actions like confirm, send failure etc.
//    private val _actionFeedback = MutableLiveData<String?>()
//    val actionFeedback: LiveData<String?> = _actionFeedback
//
//    // --- Repositories (Used by Fragment or Actions) ---
//    // Fragment will use these directly or via ViewModel methods
//    val usersRepo = UsersRepo()
//    val messageRepo = MessageRepo()
//    val chatlistRepo = ChatListRepo()
//    val bidRepo = BidRepository() // Needed for getBidById used by fragment
//    val itemsRepo = ItemsRepo() // Needed for getItemsByIds used by fragment
//    var bidId: String? = null
//
//    // --- Firebase References ---
//    private val dbRootRef = FirebaseDatabase.getInstance().reference
//    private val bidsRef = dbRootRef.child("Bids")
//    private val itemsRef = dbRootRef.child("Items")
//    private val TAG = "ExchangeAppViewModel"
//            val database = FirebaseDatabase.getInstance().reference
//
//    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
//        _actionFeedback.postValue("Error: ${throwable.localizedMessage}")
//        Log.e(TAG, "Coroutine Exception: ", throwable)
//        throwable.printStackTrace()
//    }
//
//    init {
//        getCurrentUser() // Fetch current user info on init
//    }
//
//    // --- Actions performed by ViewModel ---
//
//    // Example: Confirm Exchange (Now needs validity passed from Fragment)
//    fun confirmExchange(bidId: String, currentBidStatus: String?) {
//        // Check passed status before proceeding
//        val validBidStatuses = listOf("pending", "start")
//        if (!validBidStatuses.contains(currentBidStatus)) {
//            Log.e(TAG, "Confirm Exchange blocked by ViewModel: Status was $currentBidStatus")
//            _actionFeedback.postValue("Error: Exchange is no longer active, cannot confirm.")
//            return
//        }
//
//        Log.d(TAG, "Attempting to confirm exchange for bid $bidId (ViewModel)")
//        val updates = mapOf(
//            "status" to "completed",
//            "completionTimestamp" to ServerValue.TIMESTAMP // Optional
//        )
//        bidsRef.child(bidId).updateChildren(updates)
//            .addOnSuccessListener {
//                Log.i(TAG, "Exchange confirmed successfully for bid $bidId")
//                _actionFeedback.postValue("Success: Exchange confirmed!")
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Failed to confirm exchange for bid $bidId", e)
//                _actionFeedback.postValue("Error: Failed to confirm exchange - ${e.message}")
//            }
//    }
//
//
//
//    // Helper for updating conversation metadata (called from sendMessage)
//    private fun updateConversationMetadata(
//        senderId: String, receiverId: String, bidId: String, lastMessage: String, timestamp: Long,
//        friendName: String, friendImage: String
//    ) {
//        // ... (Implementation from Response #59) ...
//        val currentUserName = name.value ?: "You"
//        val currentUserImage = imageUrl.value ?: ""
//        val senderConversation = mapOf(
//            "bidId" to bidId, "friendid" to receiverId, "time" to timestamp, "sender" to senderId,
//            "message" to lastMessage, "friendsimage" to friendImage, "name" to friendName, "person" to "you",
//            "roomName" to (args.recentchats.roomName ?: "Exchange Room") // Need args here? Or pass roomName to sendMessage
//        )
//        val receiverConversation = mapOf(
//            "bidId" to bidId, "friendid" to senderId, "time" to timestamp, "sender" to senderId,
//            "message" to lastMessage, "friendsimage" to currentUserImage, "name" to currentUserName, "person" to currentUserName,
//            "roomName" to (args.recentchats.roomName ?: "Exchange Room") // Need args here?
//        )
//        val conversationPathSender = "Conversations/ExchangeChats/$senderId/$receiverId/$bidId"
//        val conversationPathReceiver = "Conversations/ExchangeChats/$receiverId/$senderId/$bidId"
//        val updates = mapOf(conversationPathSender to senderConversation, conversationPathReceiver to receiverConversation)
//        dbRootRef.updateChildren(updates)
//            .addOnSuccessListener { Log.d(TAG, "Conversation metadata updated.") }
//            .addOnFailureListener { e -> Log.e(TAG, "Failed to update conversation metadata", e) }
//    }
//
//    // Helper for sending notifications (called from sendMessage)
//    private fun sendNotificationLogic(receiverId: String, messageText: String){
//        dbRootRef.child("Tokens").child(receiverId).get()
//            .addOnSuccessListener { snapshot ->
//                snapshot.getValue(Token::class.java)?.let { tokenObj ->
//                    val senderName = name.value ?: "User" // Use fetched current user name
//                    val userName = senderName.split(" ").firstOrNull() ?: "User"
//                    val notification = PushNotification(
//                        NotificationData(userName, messageText), tokenObj.token ?: ""
//                    )
//                    sendNotification(notification)
//                }
//            }
//    }
//
//
//    // --- Feedback Clearing ---
//    fun clearActionFeedback() {
//        _actionFeedback.value = null
//    }
//
//    // --- Keep Existing Functions Unmodified (If needed by Fragment directly) ---
//
//        fun getUsers(): LiveData<List<User>> {
//
//        ///.differnt Type
//        return usersRepo.getExchangeChatUsers()
//    }
//    fun saveRatingToUser(userId: String, feedback: Feedback, bidId: String) {
//        val userRatingRef = FirebaseDatabase.getInstance()
//            .getReference("Users")
//            .child(userId)
//            .child("ratings")
//
//        userRatingRef.runTransaction(object : Transaction.Handler {
//            override fun doTransaction(currentData: MutableData): Transaction.Result {
//                val stats = currentData.getValue(UserRatingStats::class.java) ?: UserRatingStats()
//
//                // Update general stats
//                stats.totalExchanges += 1
//                stats.totalStarsReceived += feedback.rating
//                stats.averageRating = stats.totalStarsReceived.toFloat() / stats.totalExchanges
//                stats.highestRating = maxOf(stats.highestRating, feedback.rating)
//                stats.lowestRating = minOf(stats.lowestRating, feedback.rating)
//                stats.lastExchangeTimestamp = feedback.timestamp
//
//                // Update breakdown
//                val breakdown = stats.ratingBreakdown.toMutableMap()
//                val key = "${feedback.rating}_star"
//                breakdown[key] = (breakdown[key] ?: 0) + 1
//                stats.ratingBreakdown = breakdown
//
//                // Update feedback map by bidId
//                val updatedFeedbackMap = stats.feedbackList.toMutableMap()
//                updatedFeedbackMap[bidId] = feedback
//                stats.feedbackList = updatedFeedbackMap
//
//                currentData.value = stats
//                return Transaction.success(currentData)
//            }
//
//            override fun onComplete(
//                error: DatabaseError?,
//                committed: Boolean,
//                currentData: DataSnapshot?
//            ) {
//                if (committed) {
//                    Log.d("Rating", "Rating and feedback successfully updated for bid: $bidId")
//                } else {
//                    Log.e("Rating", "Rating update failed: ${error?.message}")
//                }
//            }
//        })
//    }
//
//    fun createExchangeRoomIfNotExists(bidId: String, roomName: String, onComplete: () -> Unit) {
//        this.bidId = bidId
//        bidRepo.getBidById(bidId, onSuccess = { bid ->
//
//            val senderId = bid.bidderId
//            val receiverId = bid.receiverId
//            val db = FirebaseDatabase.getInstance().reference
//            val chatRoomRef = db.child("Conversations").child("ExchangeChats").child(senderId)
//                .child(receiverId).child(bidId)
//
//            chatRoomRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (!snapshot.exists()) {
//                        usersRepo.getUserInfo(senderId) { senderName, senderImage ->
//                            usersRepo.getUserInfo(receiverId) { receiverName, receiverImage ->
//
//                                val chatTime = System.currentTimeMillis()
//
//                                val senderConversation = mapOf(
//                                    "bidId" to bidId,
//                                    "friendid" to receiverId,
//                                    "time" to chatTime,
//                                    "sender" to senderId,
//                                    "message" to "",
//                                    "friendsimage" to receiverImage,
//                                    "name" to receiverName,
//                                    "person" to "you",
//                                    "roomName" to roomName
//                                )
//
//                                val receiverConversation = mapOf(
//                                    "bidId" to bidId,
//                                    "friendid" to senderId,
//                                    "time" to chatTime,
//                                    "sender" to senderId,
//                                    "message" to "",
//                                    "friendsimage" to senderImage,
//                                    "name" to senderName,
//                                    "person" to senderName,
//                                    "roomName" to roomName
//                                )
//
//                                db.child("Conversations").child("ExchangeChats").child(senderId)
//                                    .child(receiverId).child(bidId).setValue(senderConversation)
//
//                                db.child("Conversations").child("ExchangeChats").child(receiverId)
//                                    .child(senderId).child(bidId).setValue(receiverConversation)
//                                    .addOnCompleteListener {
//                                        onComplete() // ✅ notify when done
//                                    }
//                            }
//                        }
//                    } else {
//                        Log.d("ExchangeRoom", "Room already exists. Skipping creation.")
//                        onComplete() // ✅ call anyway
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("ExchangeRoom", "Error checking room existence: ${error.message}")
//                    onComplete() // ✅ still call to prevent blocking
//                }
//            })
//        }, onFailure = {
//            onComplete() // still call it
//        })
//    }
//
//
//    fun sendMessage(
//        sender: String,
//        receiver: String,
//        bidId: String,
//        friendname: String,
//        friendimage: String
//    ) =
//        viewModelScope.launch(Dispatchers.IO) {
//
//            val context = MyApplication.instance.applicationContext
////            Log.e("send message", "send message inchatviewmodel")
//
//            val chatTime = System.currentTimeMillis()
//            val msgMap = mapOf(
//                "sender" to sender,
//                "receiver" to receiver,
//                "message" to message.value!!,
//                "time" to chatTime,
//                "bidId" to bidId// we can also specify "" here
//            )
//            //ch
//            val chatRoomId = "$bidId-${listOf(sender, receiver).sorted().joinToString("")}"
//
//            // Store the actual message
//            database.child("Messages").child("ExchangeChats").child(chatRoomId).child("messages")
//                .child(chatTime.toString()).setValue(msgMap).addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        message.postValue("")
//
//                        val sharedPrefs = SharedPrefs()
//                        val senderName = sharedPrefs.getValue("username") ?: "Sender"
//                        val senderImage = imageUrl.value ?: "default_img_url"
//
//                        // Correct structure: Conversation / senderId / receiverId
//                        val senderConversation = mapOf(
//                            "bidId" to bidId,
//                            "friendid" to receiver,
//                            "time" to chatTime,
//                            "sender" to sender,
//                            "message" to msgMap["message"]!!,
//                            "friendsimage" to friendimage,
//                            "name" to friendname,
//                            "person" to "you"
//                        )
//
//                        val receiverConversation = mapOf(
//                            "bidId" to bidId,
//                            "friendid" to sender,
//                            "time" to chatTime,
//                            "sender" to sender,
//                            "message" to msgMap["message"]!!,
//                            "friendsimage" to senderImage,
//                            "name" to senderName,
//                            "person" to senderName
//                        )
//
//                        // ✅ Correct Firebase paths
//                        database.child("Conversations").child("ExchangeChats").child(sender)
//                            .child(receiver).child(
//                                bidId.toString()
//                            ).updateChildren(senderConversation)
////                        Log.e("receiverid", "receiverid $receiver")
//                        database.child("Conversations").child("ExchangeChats").child(receiver)
//                            .child(sender).child(
//                                bidId.toString()
//                            ).updateChildren(receiverConversation)
//
//                        // ✅ Push Notification
//                        database.child("Tokens").child(receiver).get()
//                            .addOnSuccessListener { snapshot ->
//                                snapshot.getValue(Token::class.java)?.let { tokenObj ->
//                                    val message = msgMap["message"] as? String ?: "New message"
//                                    val userName = senderName.split(" ").firstOrNull() ?: "User"
//
//                                    val notification = PushNotification(
//                                        NotificationData(userName, message), tokenObj.token ?: ""
//                                    )
//                                    sendNotification(notification)
//                                }
//                            }
//                    }
//                }
//        }
//
//
//    fun getMessages(friend: String, bidId: String): LiveData<List<Messages>> {
//        return messageRepo.getExchangeMessages(friend, bidId)
//    }
//
//    fun getRecentUsersAllChats(): LiveData<List<RecentChats>> {
//        Log.e("Auth for user", "Auth Calling")
//
//        return chatlistRepo.getAllExchangeChatsForCurrentUser()
//    }
//
//    fun sendNotification(notification: PushNotification) = viewModelScope.launch {
//        try {
//            RetrofitInstance.api.postNotification(notification)
//        } catch (e: Exception) {
//            Log.e("ViewModelError", e.toString())
//        }
//    }
//
//    fun getCurrentUser() = viewModelScope.launch(Dispatchers.IO) {
////        val context = MyApplication.instance.applicationContext
//        val userId = Utils.getUidLoggedIn()
////Updation
//        database.child("Users").child(userId).addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                snapshot.getValue(User::class.java)?.let { user ->
//                    name.postValue(user.fullName!!)
//                    imageUrl.postValue(user.imageUrl!!)
//
//                    SharedPrefs().setValue("username", user.fullName!!)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("FirebaseError", error.message)
//            }
//        })
//    }
//
//    fun updateProfile() = viewModelScope.launch(Dispatchers.IO) {
//        val context = MyApplication.instance.applicationContext
//        val userId = Utils.getUidLoggedIn()
//
//        val updateMap = mapOf(
//            "username" to name.value!!, "imageUrl" to imageUrl.value!!
//        )
//
//        database.child("Users").child(userId).updateChildren(updateMap).addOnCompleteListener {
//            if (it.isSuccessful) {
//                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        val sharedPrefs = SharedPrefs()
//        val friendId = sharedPrefs.getValue("friendid") ?: return@launch
//
//        val updateChatMap = mapOf(
//            "friendsimage" to imageUrl.value!!, "name" to name.value!!, "person" to name.value!!
//        )
//
//        database.child("Conversation$friendId").child(userId).updateChildren(updateChatMap)
//        database.child("Conversation$userId").child(friendId).child("person").setValue("you")
//    }
//
//    // Need args in ViewModel? This is generally discouraged. Pass necessary data to functions.
//    // If roomName is needed by updateConversationMetadata, pass it to sendMessage.
//    private val args: HomeChatExchangeFragmentArgs by lazy {
//        // This is a hacky way to access args here, generally avoid.
//        // Consider restructuring how roomName is obtained for metadata update.
//        val bundle = Bundle() // Need a way to get the actual args bundle here if required
//        HomeChatExchangeFragmentArgs.fromBundle(bundle)
//    }
//
//
//    // ViewModel cleanup - no listeners to remove in this version
//    override fun onCleared() {
//        super.onCleared()
//        Log.d(TAG, "onCleared called.")
//    }
//}
//
//
//
//
//
//
