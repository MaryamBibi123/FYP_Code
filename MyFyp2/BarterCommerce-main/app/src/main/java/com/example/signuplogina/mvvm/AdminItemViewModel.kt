package com.example.signuplogina.mvvm // Or your correct package

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.signuplogina.Item
import com.example.signuplogina.adapter.UserDetailsPagerAdapter // For status constants
import com.google.firebase.database.*

class AdminItemViewModel : ViewModel() {

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _fetchError = MutableLiveData<String?>()
    val fetchError: LiveData<String?> = _fetchError

    private val _actionFeedback = MutableLiveData<String?>()
    val actionFeedback: LiveData<String?> = _actionFeedback

    private val itemsRepo = ItemsRepo() // Use your ItemsRepo
    private val TAG = "AdminItemViewModel"

    private var allItemsListener: ValueEventListener? = null
    private var allItemsQuery: Query? = null

    // Fetches ALL items filtered by status
    fun attachAllItemsListenerByStatus(statusFilter: String) {
        removeAllItemsListener() // Important: Remove previous listener
        _isLoading.value = true
        _fetchError.value = null
        Log.d(TAG, "Attaching listener for ALL items with status: $statusFilter")

        allItemsQuery = itemsRepo.databaseRefPublic.orderByChild("status").equalTo(statusFilter) // Use public ref if needed

        allItemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemList = mutableListOf<Item>()
                Log.d(TAG, "[Listener] ALL Items DataChange for status '$statusFilter'. Found ${snapshot.childrenCount} children.")
                for (itemSnapshot in snapshot.children) {
                    try {
                        itemSnapshot.getValue(Item::class.java)?.let { item ->
                            item.id = itemSnapshot.key ?: ""
                            itemList.add(item)
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error parsing item: ${itemSnapshot.key}", e) }
                }
                _items.value = itemList.sortedByDescending { it.details.timestamp }
                _isLoading.value = false
                if (itemList.isEmpty()) _fetchError.value = "No items found with status: $statusFilter"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "All items listener cancelled for status '$statusFilter': ${error.message}")
                _fetchError.value = "Error fetching items: ${error.message}"
                _isLoading.value = false
            }
        }
        allItemsQuery?.addValueEventListener(allItemsListener!!)
    }

    fun approveItem(itemId: String) {
        _isLoading.value = true // Indicate action in progress
        itemsRepo.updateItemStatus(itemId, UserDetailsPagerAdapter.STATUS_APPROVED, true, null) { success, msg ->
            _isLoading.value = false
            _actionFeedback.value = if (success) "Item '$itemId' approved." else "Failed to approve: $msg"
            if (!success) Log.e(TAG, "Approve failed for $itemId: $msg")
        }
    }

    fun rejectItem(itemId: String, reason: String? = null) {
        _isLoading.value = true
        itemsRepo.updateItemStatus(itemId, UserDetailsPagerAdapter.STATUS_REJECTED, false, reason) { success, msg ->
            _isLoading.value = false
            _actionFeedback.value = if (success) "Item '$itemId' rejected." else "Failed to reject: $msg"
            if (!success) Log.e(TAG, "Reject failed for $itemId: $msg")
        }
    }

    private fun removeAllItemsListener() {
        if (allItemsListener != null && allItemsQuery != null) {
            allItemsQuery?.removeEventListener(allItemsListener!!)
            Log.d(TAG, "Removed previous all items listener.")
        }
        allItemsListener = null
        allItemsQuery = null
    }

    override fun onCleared() {
        super.onCleared()
        removeAllItemsListener()
        Log.d(TAG, "ViewModel cleared.")
    }

    fun clearActionFeedback() { _actionFeedback.value = null }
    fun clearFetchError() { _fetchError.value = null }
}







//
// package com.example.signuplogina.mvvm
//
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.example.signuplogina.Item
//import com.google.firebase.database.*
//
//// ViewModel specifically for handling lists of ALL admin-viewable items
//class AdminItemViewModel : ViewModel() {
//
//    private val _items = MutableLiveData<List<Item>>()
//    val items: LiveData<List<Item>> = _items // Items list (all approved or all rejected)
//
//    private val _isLoading = MutableLiveData<Boolean>(false)
//    val isLoading: LiveData<Boolean> = _isLoading // Loading state for fetching all items
//
//    private val _fetchError = MutableLiveData<String?>() // Error specific to fetching all items
//    val fetchError: LiveData<String?> = _fetchError
//
//    private val _actionFeedback = MutableLiveData<String?>() // Feedback for actions like reject
//    val actionFeedback: LiveData<String?> = _actionFeedback
//
//    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Items")
//    private val TAG = "AdminItemViewModel"
//
//    // --- Listener Management ---
//    private var allItemsListener: ValueEventListener? = null
//    private var allItemsRef: DatabaseReference? = null
//
//    // --- Attach listener for ALL items based on status ---
//    fun attachAllItemsListener(showRejected: Boolean) {
//        // Remove previous listener first to avoid duplicates
//        removeAllItemsListener()
//        _isLoading.value = true
//        _fetchError.value = null // Clear previous fetch errors
//        Log.d(TAG, "Attaching real-time listener for ALL items. showRejected: $showRejected")
//
//        allItemsRef = databaseRef // Listen to the root "Items" node
//
//        allItemsListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val itemList = mutableListOf<Item>()
//                val targetStatus = if (showRejected) "rejected" else "approved"
//                Log.d(TAG, "[Listener] All Items DataChange triggered. Processing ${snapshot.childrenCount} children for status: $targetStatus")
//                for (itemSnapshot in snapshot.children) {
//                    try {
//                        val item = itemSnapshot.getValue(Item::class.java)
//                        // Filter ONLY by status after getting all items
//                        if (item != null && item.status == targetStatus) {
//                            item.id = itemSnapshot.key ?: "" // Assign the key as ID
//                            itemList.add(item)
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error parsing item from all items snapshot: ${itemSnapshot.key}", e)
//                    }
//                }
//                Log.d(TAG, "[Listener] All Items updated: ${itemList.size} total items matching status: $targetStatus")
//                _items.value = itemList // Update LiveData, triggers observers
//                _isLoading.value = false
//                _fetchError.value = null // Clear error on success
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "[Listener] All items fetch cancelled: ${error.message}")
//                _fetchError.value = "Error fetching all items: ${error.message}" // Set fetch error
//                _isLoading.value = false
//            }
//        }
//        // Attach the persistent listener
//        allItemsRef?.addValueEventListener(allItemsListener!!)
//    }
//
//    // --- rejectItem (posts feedback, listener updates list) ---
//    fun rejectItem(itemId: String) {
//        Log.d(TAG, "Attempting to update status to rejected for item: $itemId")
//        val statusPath = "status"
//        val updates = mapOf(statusPath to "rejected")
//
//        databaseRef.child(itemId)
//            .updateChildren(updates)
//            .addOnSuccessListener {
//                Log.i(TAG, "Item status updated to rejected successfully: $itemId")
//                _actionFeedback.value = "Success: Item status updated to rejected." // Post feedback
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Failed to update item status for $itemId", e)
//                _actionFeedback.value = "Error: Failed to update item status - ${e.message}" // Post feedback
//            }
//    }
//
//    // --- Listener Removal Helper ---
//    private fun removeAllItemsListener() {
//        if (allItemsListener != null && allItemsRef != null) {
//            Log.d(TAG, "Removing all items listener from $allItemsRef")
//            allItemsRef?.removeEventListener(allItemsListener!!)
//        }
//        allItemsListener = null
//        allItemsRef = null
//    }
//
//    // --- ViewModel Cleanup ---
//    override fun onCleared() {
//        super.onCleared()
//        Log.d(TAG, "onCleared called, removing all items listener.")
//        removeAllItemsListener() // Ensure listener is removed
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