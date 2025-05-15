package com.example.signuplogina.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.signuplogina.Item
import com.example.signuplogina.adapter.UserDetailsPagerAdapter // For status constants
import com.google.firebase.database.*

class ItemViewModel : ViewModel() {

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _fetchError = MutableLiveData<String?>()
    val fetchError: LiveData<String?> = _fetchError

    private val _actionFeedback = MutableLiveData<String?>()
    val actionFeedback: LiveData<String?> = _actionFeedback

    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Items")
    private val itemsRepo = ItemsRepo() // Assuming you have ItemsRepo for updates
    private val TAG = "ItemViewModel" // Generic TAG

    // --- Listener Management ---
    private var currentItemsListener: ValueEventListener? = null
    private var currentItemsQuery: Query? = null

    /**
     * Attaches a real-time listener to fetch items based on userId (optional, for admin all items view)
     * and a specific status filter ("pending", "approved", "rejected").
     *
     * @param targetUserId The ID of the user whose items to fetch. If null or blank AND isAdminView is true,
     *                     fetches all items matching the statusFilter.
     * @param statusFilter The status of items to fetch (e.g., "pending", "approved", "rejected").
     * @param isAdminView If true and targetUserId is blank, indicates fetching all items of statusFilter.
     *                    If true and targetUserId is not blank, fetches specific user's items for admin.
     */
    fun attachItemListener(targetUserId: String?, statusFilter: String, isAdminView: Boolean) {
        removeCurrentListener() // Remove any previous listener
        _isLoading.value = true
        _fetchError.value = null
        Log.d(TAG, "Attach Listener: TargetUser='${targetUserId?.takeIf { it.isNotBlank() } ?: "ALL (Admin)"}', Status='$statusFilter', IsAdminView='$isAdminView'")

        val effectiveUserId = targetUserId?.takeIf { it.isNotBlank() }

        currentItemsQuery = if (isAdminView && effectiveUserId == null) {
            // Admin viewing ALL items of a certain status
            Log.d(TAG, "Querying ALL items by status: $statusFilter")
            databaseRef.orderByChild("status").equalTo(statusFilter)
        } else if (effectiveUserId != null) {
            // Admin viewing specific user's items by status OR User viewing their own items
            Log.d(TAG, "Querying items for User '$effectiveUserId' by status '$statusFilter'")
            // This ideally uses a composite index "userId_status"
            // Example: databaseRef.orderByChild("userId_status").equalTo("${effectiveUserId}_${statusFilter}")
            // For now, using less efficient client-side filtering after fetching by userId
            databaseRef.orderByChild("userId").equalTo(effectiveUserId)
        } else {
            Log.e(TAG, "Invalid parameters for attachItemListener. Cannot determine query.")
            _isLoading.value = false
            _fetchError.value = "Cannot load items due to missing user context."
            _items.value = emptyList()
            return
        }

        currentItemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemList = mutableListOf<Item>()
                Log.d(TAG, "[Listener] DataChange for User='${effectiveUserId ?: "ALL"}', Status='$statusFilter'. Children: ${snapshot.childrenCount}")
                for (itemSnapshot in snapshot.children) {
                    try {
                        itemSnapshot.getValue(Item::class.java)?.let { item ->
                            item.id = itemSnapshot.key ?: ""
                            // Apply status filter client-side if the query was only by userId
                            if (effectiveUserId != null && !(isAdminView && targetUserId.isNullOrBlank())) { // If query was by userId
                                if (item.status.equals(statusFilter, ignoreCase = true)) {
                                    itemList.add(item)
                                }
                            } else { // Query was already by status (for admin all items)
                                itemList.add(item)
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error parsing item: ${itemSnapshot.key}", e) }
                }
                Log.d(TAG, "[Listener] Filtered list size: ${itemList.size} for User='${effectiveUserId ?: "ALL"}', Status='$statusFilter'")
                _items.value = itemList.sortedByDescending { it.details.timestamp } // Sort for consistent display
                _isLoading.value = false
                _fetchError.value = if (itemList.isEmpty()) "No items found matching filter." else null
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "[Listener] Cancelled for User='${effectiveUserId ?: "ALL"}', Status='$statusFilter': ${error.message}")
                _fetchError.value = "Error fetching items: ${error.message}"
                _isLoading.value = false
                _items.value = emptyList()
            }
        }
        currentItemsQuery?.addValueEventListener(currentItemsListener!!)
    }

    // --- Admin Action Methods (using ItemsRepo) ---
    fun approveItem(itemId: String) {
        _isLoading.value = true // Indicate action in progress
        // When approving, item becomes available
        itemsRepo.updateItemStatus(itemId, UserDetailsPagerAdapter.STATUS_APPROVED, true, null) { success, msg ->
            _isLoading.value = false
            _actionFeedback.value = if (success) "Item '$itemId' approved." else "Failed to approve: $msg"
            if (!success) Log.e(TAG, "Approve failed for $itemId: $msg")
        }
    }

    fun rejectItem(itemId: String, reason: String? = null) {
        _isLoading.value = true
        // When rejecting, item becomes unavailable
        itemsRepo.updateItemStatus(itemId, UserDetailsPagerAdapter.STATUS_REJECTED, false, reason) { success, msg ->
            _isLoading.value = false
            _actionFeedback.value = if (success) "Item '$itemId' rejected." else "Failed to reject: $msg"
            if (!success) Log.e(TAG, "Reject failed for $itemId: $msg")
        }
    }

    // --- Listener Removal Helper ---
    private fun removeCurrentListener() {
        if (currentItemsListener != null && currentItemsQuery != null) {
            Log.d(TAG, "Removing current items listener from $currentItemsQuery")
            currentItemsQuery?.removeEventListener(currentItemsListener!!)
        }
        currentItemsListener = null
        currentItemsQuery = null
    }

    // --- ViewModel Cleanup ---
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared called, removing items listener.")
        removeCurrentListener()
    }

    // --- Feedback/Error Clearing ---
    fun clearActionFeedback() {
        _actionFeedback.value = null
    }
    fun clearFetchError() {
        _fetchError.value = null
    }
}







//
//package com.example.signuplogina.mvvm
//
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.example.signuplogina.Item
//import com.google.firebase.database.*
//
//// ViewModel specifically for handling lists of ONE USER's items
//class ItemViewModel : ViewModel() {
//
//    private val _items = MutableLiveData<List<Item>>() // User's items list
//    val items: LiveData<List<Item>> = _items
//
//    private val _isLoading = MutableLiveData<Boolean>(false) // Loading state for user items fetch
//    val isLoading: LiveData<Boolean> = _isLoading
//
//    private val _fetchError = MutableLiveData<String?>() // Error specific to fetching user items
//    val fetchError: LiveData<String?> = _fetchError
//
//    private val _actionFeedback = MutableLiveData<String?>() // Feedback for actions like reject
//    val actionFeedback: LiveData<String?> = _actionFeedback
//
//    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Items")
//    private val TAG = "UserItemViewModel" // Renamed TAG for clarity
//
//    // --- Listener Management ---
//    private var userItemsListener: ValueEventListener? = null
//    private var userItemsQuery: Query? = null
//
//    // --- Attach listener for USER items based on status ---
//    fun attachUserItemListener(userId: String, showRejected: Boolean) {
//        // Remove previous listener first
//        removeUserItemsListener()
//        _isLoading.value = true
//        _fetchError.value = null // Clear previous error
//        Log.d(TAG, "Attaching real-time listener for User ID: $userId, showRejected: $showRejected")
//
//        userItemsQuery = databaseRef.orderByChild("userId").equalTo(userId)
//
//        userItemsListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val itemList = mutableListOf<Item>()
//                val targetStatus = if (showRejected) "rejected" else "approved"
//                Log.d(TAG, "[Listener] User Items DataChange triggered for $userId. Processing ${snapshot.childrenCount} children for status: $targetStatus")
//                for (itemSnapshot in snapshot.children) {
//                    try {
//                        val item = itemSnapshot.getValue(Item::class.java)
//                        // Filter by status AFTER getting user's items
//                        if (item != null && item.status == targetStatus) {
//                            item.id = itemSnapshot.key ?: ""
//                            itemList.add(item)
//                        }
//                    } catch (e: Exception) { Log.e(TAG, "Error parsing user item: ${itemSnapshot.key}", e) }
//                }
//                Log.d(TAG, "[Listener] User Items updated: ${itemList.size} items for User ID: $userId, Status: $targetStatus")
//                _items.value = itemList // Update LiveData
//                _isLoading.value = false
//                _fetchError.value = null // Clear error on success
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "[Listener] User items fetch cancelled for User ID: $userId - ${error.message}")
//                _fetchError.value = "Error fetching user items: ${error.message}" // Set specific error
//                _isLoading.value = false
//            }
//        }
//        // Attach the persistent listener
//        userItemsQuery?.addValueEventListener(userItemsListener!!)
//    }
//
//    // --- rejectItem (posts feedback, listener updates list) ---
//    // This logic can be identical to AdminItemViewModel or you could potentially
//    // have different feedback messages if needed.
//    fun rejectItem(itemId: String) {
//        Log.d(TAG, "Attempting to update status to rejected for item: $itemId")
//        val statusPath = "status"
//        val updates = mapOf(statusPath to "rejected")
//
//        databaseRef.child(itemId)
//            .updateChildren(updates)
//            .addOnSuccessListener {
//                Log.i(TAG, "Item status updated to rejected successfully: $itemId")
//                _actionFeedback.value = "Success: Item status updated to rejected."
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Failed to update item status for $itemId", e)
//                _actionFeedback.value = "Error: Failed to update item status - ${e.message}"
//            }
//    }
//
//    // --- Listener Removal Helper ---
//    private fun removeUserItemsListener() {
//        if (userItemsListener != null && userItemsQuery != null) {
//            Log.d(TAG, "Removing user items listener from $userItemsQuery")
//            userItemsQuery?.removeEventListener(userItemsListener!!)
//        }
//        userItemsListener = null
//        userItemsQuery = null
//    }
//
//    // --- ViewModel Cleanup ---
//    override fun onCleared() {
//        super.onCleared()
//        Log.d(TAG, "onCleared called, removing user items listener.")
//        removeUserItemsListener() // Ensure listener is removed
//    }
//
//    // --- Feedback/Error Clearing ---
//    fun clearActionFeedback() {
//        _actionFeedback.value = null
//    }
//    fun clearFetchError() {
//        _fetchError.value = null
//    }
//}
//
//
//
//
