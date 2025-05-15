package com.example.signuplogina.mvvm

import android.util.Log
import androidx.lifecycle.*
import com.example.signuplogina.User // Assuming User is imported
import com.example.signuplogina.modal.Report
import com.example.signuplogina.modal.BlockRecord
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
// Ensure your repository classes are imported if they are in a different package
// For example:
// import com.example.signuplogina.repository.UsersRepo
// import com.example.signuplogina.repository.ReportsRepo
// import com.example.signuplogina.repository.ItemsRepo
// import com.example.signuplogina.repository.BidRepository


class AdminViewModel : ViewModel() {

    private val usersRepo = UsersRepo()
    private val reportsRepo = ReportsRepo() // Make sure this class is defined
    private val itemsRepo = ItemsRepo()     // Make sure this class is defined
    private val bidsRepo = BidRepository()  // Make sure this class is defined

    private val TAG = "AdminViewModel"

    // --- LiveData declarations (as you have them) ---
    private val _selectedUserDetails = MutableLiveData<User?>()
    val selectedUserDetails: LiveData<User?> get() = _selectedUserDetails

    private val _reportsForTargetUser = MutableLiveData<List<Report>?>()
    val reportsForTargetUser: LiveData<List<Report>?> get() = _reportsForTargetUser

    private val _allPendingReports = MutableLiveData<List<Report>?>()
    val allPendingReports: LiveData<List<Report>?> get() = _allPendingReports

    private val _userActionOutcome = MutableLiveData<UserActionOutcome?>()
    val userActionOutcome: LiveData<UserActionOutcome?> get() = _userActionOutcome

    private val _reportActionOutcome = MutableLiveData<UserActionOutcome?>()
    val reportActionOutcome: LiveData<UserActionOutcome?> get() = _reportActionOutcome

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private fun getCurrentAdminId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    // --- User Profile Related (fetchUserDetails, fetchReportsForUser - as you have them) ---
    fun fetchUserDetails(userId: String) {
        _isLoading.postValue(true)
        usersRepo.getUserDetailsById(userId) { user ->
            _selectedUserDetails.postValue(user)
            _isLoading.postValue(false)
        }
    }

    fun fetchReportsForUser(userId: String) { // Renamed for clarity from your code
        _isLoading.postValue(true)
        reportsRepo.getReportsForUser(userId) { reports ->
            _reportsForTargetUser.postValue(reports)
            _isLoading.postValue(false)
        }
    }


    // --- Report Actions & User Blocking ---
    fun temporarilyBlockUser(
        targetUserId: String,
        targetUserFullName: String,
        durationDays: Int,
        adminProvidedReason: String,
        associatedReportId: String?
    ) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val adminId = getCurrentAdminId()
            val expiryTimestamp = System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000L)
            val blockRecord = BlockRecord(
                blockType = "temporary_${durationDays}days",
                reason = adminProvidedReason,
                blockedByAdminId = adminId,
                blockTimestamp = System.currentTimeMillis(),
                expiryTimestamp = expiryTimestamp
            )

            usersRepo.applyUserBlock(targetUserId, true, false, expiryTimestamp, adminProvidedReason, blockRecord) { success ->
                if (success) {
                    // --- Start Consequential Actions ---
                    var itemsSuccess = false
                    var bidsSuccess = false
                    var convosSuccess = false
                    var consequentialOpsPending = 3 // items, bids, convos

                    val checkConsequentialCompletion = {
                        if (consequentialOpsPending == 0) {
                            val allConsequentialSuccess = itemsSuccess && bidsSuccess && convosSuccess
                            Log.d(TAG, "Temp Block: Consequential actions completed. Overall success: $allConsequentialSuccess")
                            associatedReportId?.let { updateReportStatus(it, "resolved_temp_blocked", "User temporarily blocked for $durationDays days. Reason: $adminProvidedReason") }
                            _userActionOutcome.postValue(UserActionOutcome(true, "$targetUserFullName temp. blocked. Reason: $adminProvidedReason"))
                            _isLoading.postValue(false)
                        }
                    }

                    itemsRepo.updateUserItemsForTempBlock(targetUserId) {
                        itemsSuccess = it
                        Log.d(TAG, "Items temp block status: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    bidsRepo.updateUserBidsForTempBlock(targetUserId, "User temporarily blocked: $adminProvidedReason") {
                        bidsSuccess = it
                        Log.d(TAG, "Bids temp block status: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    // CORRECTED CALL with callback
                    usersRepo.disableConversationsForBlockedUser(targetUserId) {
                        convosSuccess = it
                        Log.d(TAG, "Conversations disabled status for temp block: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    // --- End Consequential Actions ---
                } else { // applyUserBlock failed
                    _userActionOutcome.postValue(UserActionOutcome(false, "Failed to temp. block $targetUserFullName."))
                    _isLoading.postValue(false)
                }
            }
        }
    }

    fun permanentlyBlockUser(
        targetUserId: String,
        targetUserFullName: String,
        adminProvidedReason: String,
        associatedReportId: String?
    ) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            val adminId = getCurrentAdminId()
            val blockRecord = BlockRecord(
                blockType = "permanent",
                reason = adminProvidedReason,
                blockedByAdminId = adminId,
                blockTimestamp = System.currentTimeMillis()
            )
            usersRepo.applyUserBlock(targetUserId, false, true, 0L, adminProvidedReason, blockRecord) { success ->
                if (success) {
                    // --- Start Consequential Actions ---
                    var itemsSuccess = false
                    var bidsSuccess = false
                    var convosSuccess = false
                    var globalMarkSuccess = false
                    var consequentialOpsPending = 4 // items, bids, convos, globalMark

                    val checkConsequentialCompletion = {
                        if (consequentialOpsPending == 0) {
                            val allConsequentialSuccess = itemsSuccess && bidsSuccess && convosSuccess && globalMarkSuccess
                            Log.d(TAG, "Perm Block: Consequential actions completed. Overall success: $allConsequentialSuccess")
                            associatedReportId?.let { updateReportStatus(it, "resolved_perm_blocked", "User permanently blocked. Reason: $adminProvidedReason") }
                            _userActionOutcome.postValue(UserActionOutcome(true, "$targetUserFullName permanently blocked. Reason: $adminProvidedReason"))
                            _isLoading.postValue(false)
                        }
                    }

                    itemsRepo.updateUserItemsForPermBlock(targetUserId) {
                        itemsSuccess = it
                        Log.d(TAG, "Items perm block status: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    bidsRepo.cancelUserBids(targetUserId, "User permanently blocked: $adminProvidedReason") {
                        bidsSuccess = it
                        Log.d(TAG, "Bids perm cancel status: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    // CORRECTED CALL with callback
                    usersRepo.disableConversationsForBlockedUser(targetUserId) {
                        convosSuccess = it
                        Log.d(TAG, "Conversations disabled status for perm block: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    usersRepo.markUserAsGloballyBlocked(targetUserId, true) {
                        globalMarkSuccess = it
                        Log.d(TAG, "Globally marked for perm block: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    // --- End Consequential Actions ---
                } else { // applyUserBlock failed
                    _userActionOutcome.postValue(UserActionOutcome(false, "Failed to perm. block $targetUserFullName."))
                    _isLoading.postValue(false)
                }
            }
        }
    }

    fun unblockUser(targetUserId: String, targetUserFullName: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            usersRepo.clearUserBlockStatus(targetUserId) { success ->
                if (success) {
                    // --- Start Consequential Actions for Unblock ---
                    var itemsSuccess = false
                    var bidsSuccess = false
                    var convosSuccess = false
                    var globalMarkSuccess = false
                    var consequentialOpsPending = 4

                    val checkConsequentialCompletion = {
                        if (consequentialOpsPending == 0) {
                            val allConsequentialSuccess = itemsSuccess && bidsSuccess && convosSuccess && globalMarkSuccess
                            Log.d(TAG, "Unblock: Consequential actions completed. Overall success: $allConsequentialSuccess")
                            _userActionOutcome.postValue(UserActionOutcome(true, "$targetUserFullName has been unblocked."))
                            _isLoading.postValue(false)
                        }
                    }

                    itemsRepo.unblockUserItems(targetUserId) {
                        itemsSuccess = it
                        Log.d(TAG, "Items unblocked status: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    bidsRepo.reactivateUserBidsIfStalled(targetUserId) {
                        bidsSuccess = it
                        Log.d(TAG, "Stalled bids reactivated status: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    usersRepo.markUserAsGloballyBlocked(targetUserId, false) {
                        globalMarkSuccess = it
                        Log.d(TAG, "Globally unmarked: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    usersRepo.enableConversationsForUser(targetUserId) {
                        convosSuccess = it
                        Log.d(TAG, "Conversations enabled: $it")
                        consequentialOpsPending--
                        checkConsequentialCompletion()
                    }
                    // --- End Consequential Actions for Unblock ---
                } else { // clearUserBlockStatus failed
                    _userActionOutcome.postValue(UserActionOutcome(false, "Failed to unblock $targetUserFullName."))
                    _isLoading.postValue(false)
                }
            }
        }
    }


    // --- Methods for AdminPendingReportsFragment (fetchAllPendingReports, fetchReportsForTargetUser, updateReportStatus - as you have them) ---
    fun fetchAllPendingReports() {
        _isLoading.postValue(true)
        reportsRepo.getPendingReports { reports ->
            _allPendingReports.postValue(reports)
            _isLoading.postValue(false)
        }
    }

    fun fetchReportsForTargetUser(userId: String, statusFilter: String? = null) { // Name updated to match usage
        _isLoading.postValue(true)
        reportsRepo.getReportsForUserFilteredByStatus(userId, statusFilter) { reports ->
            _reportsForTargetUser.postValue(reports)
            _isLoading.postValue(false)
        }
    }

    fun updateReportStatus(reportId: String, newStatus: String, adminNotes: String?) {
        _isLoading.postValue(true)
        val updates = mutableMapOf<String, Any?>(
            "status" to newStatus,
            "adminActionTakenTimestamp" to System.currentTimeMillis()
        )
        adminNotes?.let { if(it.isNotBlank()) updates["adminNotes"] = it }

        reportsRepo.updateReport(reportId, updates) { success ->
            if (success) {
                _reportActionOutcome.postValue(UserActionOutcome(true, "Report $reportId status: $newStatus."))
            } else {
                _reportActionOutcome.postValue(UserActionOutcome(false, "Failed to update report $reportId."))
            }
            _isLoading.postValue(false)
        }
    }

    fun clearUserActionOutcome() { _userActionOutcome.value = null }
    fun clearReportActionOutcome() { _reportActionOutcome.value = null }

    data class UserActionOutcome(val success: Boolean, val message: String?)
}





//
// AdminViewModel.kt
//package com.example.signuplogina.mvvm
//
//import android.util.Log
//import androidx.lifecycle.*
//import com.example.signuplogina.User
//import com.example.signuplogina.modal.Report
//import com.example.signuplogina.modal.BlockRecord
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.launch
//import com.example.signuplogina.mvvm.* // Assuming you create these
//
//class AdminViewModel : ViewModel() {
//
//    private val usersRepo = UsersRepo()
//    private val reportsRepo = ReportsRepo()
//    private val itemsRepo = ItemsRepo()
//    private val bidsRepo = BidRepository() // Assuming BidRepository for bids
//
//    private val TAG = "AdminViewModel"
//
//    private val _selectedUserDetails = MutableLiveData<User?>()
//    val selectedUserDetails: LiveData<User?> get() = _selectedUserDetails
//
//    private val _reportsForTargetUser = MutableLiveData<List<Report>?>() // For specific user, any status
//    val reportsForTargetUser: LiveData<List<Report>?> get() = _reportsForTargetUser
//
//    private val _allPendingReports = MutableLiveData<List<Report>?>() // All users, pending_review status
//    val allPendingReports: LiveData<List<Report>?> get() = _allPendingReports
//
//    private val _userActionOutcome = MutableLiveData<UserActionOutcome?>()
//    val userActionOutcome: LiveData<UserActionOutcome?> get() = _userActionOutcome
//
//    private val _reportActionOutcome = MutableLiveData<UserActionOutcome?>()
//    val reportActionOutcome: LiveData<UserActionOutcome?> get() = _reportActionOutcome
//
//    private val _isLoading = MutableLiveData<Boolean>()
//    val isLoading: LiveData<Boolean> get() = _isLoading
//
//    private fun getCurrentAdminId(): String? = FirebaseAuth.getInstance().currentUser?.uid
//
//    // --- User Profile Related ---
//    fun fetchUserDetails(userId: String) {
//        _isLoading.postValue(true)
//        usersRepo.getUserDetailsById(userId) { user ->
//            _selectedUserDetails.postValue(user)
//            _isLoading.postValue(false)
//        }
//    }
//
//    fun fetchReportsForUser(userId: String) {
//        _isLoading.postValue(true)
//        reportsRepo.getReportsForUser(userId) { reports -> // Gets ALL reports for this user
//            _reportsForTargetUser.postValue(reports)
//            _isLoading.postValue(false)
//        }
//    }
//
//    // --- Report Actions & User Blocking ---
//    fun temporarilyBlockUser(
//        targetUserId: String,
//        targetUserFullName: String,
//        durationDays: Int,
//        adminProvidedReason: String,
//        associatedReportId: String? // Report that triggered this block
//    ) {
//        _isLoading.postValue(true)
//        viewModelScope.launch {
//            val adminId = getCurrentAdminId()
//            val expiryTimestamp = System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000L)
//            val blockRecord = BlockRecord(
//                blockType = "temporary_${durationDays}days",
//                reason = adminProvidedReason,
//                blockedByAdminId = adminId,
//                blockTimestamp = System.currentTimeMillis(),
//                expiryTimestamp = expiryTimestamp
//            )
//
//            usersRepo.applyUserBlock(targetUserId, true, false, expiryTimestamp, adminProvidedReason, blockRecord) { success ->
//                if (success) {
//                    itemsRepo.updateUserItemsForTempBlock(targetUserId) { Log.d(TAG, "Items temp block status: $it") }
//                    bidsRepo.updateUserBidsForTempBlock(targetUserId, "User temporarily blocked: $adminProvidedReason") { Log.d(TAG, "Bids temp block status: $it") }
//                  // may need to impelemt calback or success etc
//                    usersRepo.disableConversationsForBlockedUser(targetUserId){ conversationSuccess ->
//                        Log.d(TAG, "Conversations disabled status: $conversationSuccess")
//                        // ... logic to handle completion ...
//                    }
//                    associatedReportId?.let { updateReportStatus(it, "resolved_temp_blocked", "User temporarily blocked for $durationDays days. Reason: $adminProvidedReason") }
//                    _userActionOutcome.postValue(UserActionOutcome(true, "$targetUserFullName temp. blocked. Reason: $adminProvidedReason"))
//                } else {
//                    _userActionOutcome.postValue(UserActionOutcome(false, "Failed to temp. block $targetUserFullName."))
//                }
//                _isLoading.postValue(false)
//            }
//        }
//    }
//
//    fun permanentlyBlockUser(
//        targetUserId: String,
//        targetUserFullName: String,
//        adminProvidedReason: String,
//        associatedReportId: String?
//    ) {
//        _isLoading.postValue(true)
//        viewModelScope.launch {
//            val adminId = getCurrentAdminId()
//            val blockRecord = BlockRecord(
//                blockType = "permanent",
//                reason = adminProvidedReason,
//                blockedByAdminId = adminId,
//                blockTimestamp = System.currentTimeMillis()
//            )
//            usersRepo.applyUserBlock(targetUserId, false, true, 0L, adminProvidedReason, blockRecord) { success ->
//                if (success) {
//                    itemsRepo.updateUserItemsForPermBlock(targetUserId) { Log.d(TAG, "Items perm block status: $it") } // Or delete
//                    bidsRepo.cancelUserBids(targetUserId, "User permanently blocked: $adminProvidedReason") { Log.d(TAG, "Bids perm cancel status: $it") }
//                    usersRepo.disableConversationsForBlockedUser(targetUserId)
//                    usersRepo.markUserAsGloballyBlocked(targetUserId, true) {}
//                    associatedReportId?.let { updateReportStatus(it, "resolved_perm_blocked", "User permanently blocked. Reason: $adminProvidedReason") }
//                    _userActionOutcome.postValue(UserActionOutcome(true, "$targetUserFullName permanently blocked. Reason: $adminProvidedReason"))
//                } else {
//                    _userActionOutcome.postValue(UserActionOutcome(false, "Failed to perm. block $targetUserFullName."))
//                }
//                _isLoading.postValue(false)
//            }
//        }
//    }
//
//    fun unblockUser(targetUserId: String, targetUserFullName: String) {
//        _isLoading.postValue(true)
//        viewModelScope.launch {
//            usersRepo.clearUserBlockStatus(targetUserId) { success ->
//                if (success) {
//                    itemsRepo.unblockUserItems(targetUserId) { Log.d(TAG, "Items unblocked status: $it") }
//                    bidsRepo.reactivateUserBidsIfStalled(targetUserId) { Log.d(TAG, "Stalled bids reactivated status: $it") }
//                    usersRepo.markUserAsGloballyBlocked(targetUserId, false) {}
//                    usersRepo.enableConversationsForUser(targetUserId) { Log.d(TAG, "Conversations enabled: $it") }
//                    _userActionOutcome.postValue(UserActionOutcome(true, "$targetUserFullName has been unblocked."))
//                } else {
//                    _userActionOutcome.postValue(UserActionOutcome(false, "Failed to unblock $targetUserFullName."))
//                }
//                _isLoading.postValue(false)
//            }
//        }
//    }
//
//
//    // --- Methods for AdminPendingReportsFragment ---
//    fun fetchAllPendingReports() {
//        _isLoading.postValue(true)
//        reportsRepo.getPendingReports { reports ->
//            _allPendingReports.postValue(reports)
//            _isLoading.postValue(false)
//        }
//    }
//
//    fun fetchReportsForTargetUser(userId: String, statusFilter: String? = null) {
//        _isLoading.postValue(true)
//        reportsRepo.getReportsForUserFilteredByStatus(userId, statusFilter) { reports ->
//            _reportsForTargetUser.postValue(reports) // Use the specific LiveData
//            _isLoading.postValue(false)
//        }
//    }
//
//
//    fun updateReportStatus(reportId: String, newStatus: String, adminNotes: String?) {
//        _isLoading.postValue(true)
//        val updates = mutableMapOf<String, Any?>(
//            "status" to newStatus,
//            "adminActionTakenTimestamp" to System.currentTimeMillis()
//        )
//        adminNotes?.let { if(it.isNotBlank()) updates["adminNotes"] = it }
//
//        reportsRepo.updateReport(reportId, updates) { success ->
//            if (success) {
//                _reportActionOutcome.postValue(UserActionOutcome(true, "Report $reportId status: $newStatus."))
//                // The fragment that called this should re-fetch its list to reflect the change
//            } else {
//                _reportActionOutcome.postValue(UserActionOutcome(false, "Failed to update report $reportId."))
//            }
//            _isLoading.postValue(false)
//        }
//    }
//
//    fun clearUserActionOutcome() { _userActionOutcome.value = null }
//    fun clearReportActionOutcome() { _reportActionOutcome.value = null }
//
//    data class UserActionOutcome(val success: Boolean, val message: String?)
//}