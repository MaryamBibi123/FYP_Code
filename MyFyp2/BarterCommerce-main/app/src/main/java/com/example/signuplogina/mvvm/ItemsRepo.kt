package com.example.signuplogina.mvvm

import android.util.Log
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
// import com.example.signuplogina.ItemDetails // Not needed if returning full Item
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ItemsRepo {

     val databaseRefPublic = FirebaseDatabase.getInstance().getReference("Items")
    private val TAG = "ItemsRepo"


    fun getItemById(itemId: String, onResult: (Item?) -> Unit) {
        if (itemId.isBlank()) {
            Log.w(TAG, "getItemById called with blank itemId.")
            onResult(null)
            return
        }
        databaseRefPublic.child(itemId).get()
            .addOnSuccessListener { snapshot ->
                try {
                    val item = snapshot.getValue(Item::class.java)
                    item?.id = snapshot.key ?: itemId // Ensure ID is set
                    onResult(item)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing item $itemId", e)
                    onResult(null)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch item $itemId: ${exception.message}")
                onResult(null)
            }
    }

    /**
     * Fetches multiple items based on a list of their IDs.
     * Useful for getting all offered items in a bid.
     */
    fun getMultipleItemsByIds(
        itemIds: List<String>,
        onSuccess: (List<Item>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (itemIds.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        val itemTasks = mutableListOf<Task<DataSnapshot>>()
        val validItemIds = itemIds.filter { it.isNotBlank() } // Filter out blank IDs

        if (validItemIds.isEmpty()){
            onSuccess(emptyList())
            return
        }

        for (id in validItemIds) {
            itemTasks.add(databaseRefPublic.child(id).get())
        }

        Tasks.whenAllSuccess<DataSnapshot>(itemTasks)
            .addOnSuccessListener { snapshots ->
                val itemsList = mutableListOf<Item>()
                snapshots.forEachIndexed { index, snapshot ->
                    try {
                        snapshot.getValue(Item::class.java)?.let { item ->
                            item.id = snapshot.key ?: validItemIds[index] // Ensure ID is set
                            itemsList.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing item with ID ${snapshot.key} during multi-fetch.", e)
                        // Optionally, you could call onFailure here or just skip the item
                    }
                }
                onSuccess(itemsList)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch one or more items in multi-fetch.", exception)
                onFailure(exception)
            }
    }


    /**
     * Specifically for the chat screen's "See Items" section:
     * Fetches the requested item AND all offered items for a given bid.
     * This is what your HomeChatExchangeFragment will likely use now.
     */

    fun updateItemStatus(
        itemId: String,
        newStatus: String,
        newAvailability: Boolean, // Make sure this is used
        rejectionReason: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val updates = mutableMapOf<String, Any?>(
            "status" to newStatus,
            "available" to newAvailability, // **** ENSURE THIS IS SET ****
            "rejectionTimestamp" to if (newStatus == "rejected") System.currentTimeMillis() else 0L // Or ServerValue.TIMESTAMP
        )
        // ... (rest of the method as before, handles rejectionReason)
        if (newStatus == "rejected") {
            updates["rejectionReason"] = rejectionReason ?: ""
        } else {
            // When approving or moving to pending, clear rejection reason and timestamp
            updates["rejectionReason"] = null
            updates["rejectionTimestamp"] = 0L
        }

        // If using composite index for admin query by userId_status, update it here
        // This requires fetching the item's userId first.
        databaseRefPublic.child(itemId).child("userId").get().addOnSuccessListener { userIdSnapshot ->
            val userId = userIdSnapshot.getValue(String::class.java)
            if (userId != null) {
                updates["userId_status"] = "${userId}_${newStatus}"
            }
            // Now update all children
            databaseRefPublic.child(itemId).updateChildren(updates)
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(false, e.message) }
        }.addOnFailureListener { e -> // Failed to get userId to update composite index
            Log.w(TAG, "Could not get userId for item $itemId to update composite index, updating without it. Error: ${e.message}")
            // Proceed to update without the composite key if fetching userId fails
            databaseRefPublic.child(itemId).updateChildren(updates)
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { ex -> onComplete(false, ex.message) }
        }
    }
    fun getExchangeChatItems(
        bid: Bid,
        onSuccess: (requestedItem: Item?, offeredItems: List<Item>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        var requestedItemTask: Task<DataSnapshot>? = null
        if (bid.itemId.isNotBlank()) {
            requestedItemTask = databaseRefPublic.child(bid.itemId).get()
        }

        val offeredItemsTasks = mutableListOf<Task<DataSnapshot>>()
        val validOfferedItemIds = bid.offeredItemIds.filter { it.isNotBlank() }
        if (validOfferedItemIds.isNotEmpty()) {
            for (offeredId in validOfferedItemIds) {
                offeredItemsTasks.add(databaseRefPublic.child(offeredId).get())
            }
        }

        val allTasks = mutableListOf<Task<DataSnapshot>>()
        requestedItemTask?.let { allTasks.add(it) }
        allTasks.addAll(offeredItemsTasks)

        if (allTasks.isEmpty()) {
            Log.w(TAG, "No valid items to fetch for exchange chat display (BidID: ${bid.bidId})")
            onSuccess(null, emptyList()) // No items to fetch
            return
        }

        Tasks.whenAllSuccess<DataSnapshot>(allTasks)
            .addOnSuccessListener { snapshots ->
                var requestedItemResult: Item? = null
                val offeredItemsResult = mutableListOf<Item>()
                var snapshotIndex = 0

                if (requestedItemTask != null && snapshots.isNotEmpty()) {
                    try {
                        snapshots[snapshotIndex].getValue(Item::class.java)?.let { item ->
                            item.id = snapshots[snapshotIndex].key ?: bid.itemId
                            requestedItemResult = item
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing requested item ${bid.itemId}", e)
                    }
                    snapshotIndex++
                }

                for (i in validOfferedItemIds.indices) {
                    if (snapshotIndex < snapshots.size) {
                        try {
                            snapshots[snapshotIndex].getValue(Item::class.java)?.let { item ->
                                item.id = snapshots[snapshotIndex].key ?: validOfferedItemIds[i]
                                offeredItemsResult.add(item)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing offered item ${validOfferedItemIds[i]}", e)
                        }
                        snapshotIndex++
                    }
                }
                onSuccess(requestedItemResult, offeredItemsResult)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch items for exchange chat (BidID: ${bid.bidId})", exception)
                onFailure(exception)
            }
    }


//        fun updateUserItemsForTempBlock(userId: String, onComplete: (Boolean) -> Unit) {
//            val updates = mapOf<String, Any>(
//                "available" to false,
//                "status" to "owner_temporarily_blocked", // Specific status
//                "exchangeState" to "on_hold_owner_blocked" // If item was in "none" or "in_negotiation"
//                // lockedByBidId might need to be cleared if part of a bid that gets stalled
//            )
//            updateItemsByOwnerWhereNotPermBlocked(userId, updates, onComplete)
//        }
//
//
//    //check that out
//        fun updateUserItemsForPermBlock(userId: String, onComplete: (Boolean) -> Unit) {
//            val updates = mapOf<String, Any>(
//                "available" to false,
//                "status" to "owner_permanently_blocked",
//                "exchangeState" to "archived_owner_blocked", // Or a more definitive "archived"
//                "lockedByBidId" to "null" // Definitely clear this
//            )
//            updateItemsByOwnerWhereNotPermBlocked(userId, updates, onComplete)
//            // If actual deletion is required, that's a separate, more complex operation.
//        }
//
//        fun unblockUserItems(userId: String, onComplete: (Boolean) -> Unit) {
//            // Revert items that were 'owner_temporarily_blocked'
//            val updates = mapOf<String, Any>(
//                "available" to true, // Assuming it becomes available again
//                "status" to "approved", // Default good status
//                "exchangeState" to "none" // Reset exchange state
//            )
//            updateItemsByOwnerIfStatus(userId, "owner_temporarily_blocked", updates, onComplete)
//        }
//
//        // Helper to update items by owner, but only if not already permanently blocked
//        private fun updateItemsByOwnerWhereNotPermBlocked(userId: String, updatesToApply: Map<String, Any>, onComplete: (Boolean) -> Unit) {
//            databaseRefPublic.orderByChild("userId").equalTo(userId)
//                .addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        if (!snapshot.exists()) { onComplete(true); return }
//                        val itemParentUpdates = mutableMapOf<String, Any?>()
//                        var allSuccessful = true
//                        snapshot.children.forEach { itemSnapshot ->
//                            val item = itemSnapshot.getValue(Item::class.java)
//                            if (item != null && item.status != "owner_permanently_blocked") { // Check
//                                itemSnapshot.key?.let { itemId ->
//                                    updatesToApply.forEach { (key, value) ->
//                                        itemParentUpdates["/$itemId/$key"] = value
//                                    }
//                                    // Update composite admin status key if you use one e.g. userId_status
//                                    // itemParentUpdates["/$itemId/userId_status"] = "${userId}_${updatesToApply["status"]}"
//                                }
//                            }
//                        }
//                        if (itemParentUpdates.isNotEmpty()) {
//                            databaseRefPublic.updateChildren(itemParentUpdates)
//                                .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
//                        } else {
//                            onComplete(true) // No items to update or all were perm blocked
//                        }
//                    }
//                    override fun onCancelled(error: DatabaseError) {
//                        Log.e(TAG, "updateItemsByOwnerWhereNotPermBlocked for $userId failed: ${error.message}")
//                        onComplete(false)
//                    }
//                })
//        }
//


    fun updateUserItemsForTempBlock(userId: String, onComplete: (Boolean) -> Unit) {
        databaseRefPublic.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "No items found for user $userId to temp block.")
                        onComplete(true)
                        return
                    }
                    val batchUpdates = mutableMapOf<String, Any?>()
                    snapshot.children.forEach { itemSnapshot ->
                        val item = itemSnapshot.getValue(Item::class.java)
                        itemSnapshot.key?.let { itemId ->
                            if (item != null &&
                                item.status != "owner_permanently_blocked" &&
                                item.status != "exchanged" &&
                                item.status != "archived_owner_blocked"
                            ) {
                                val stashedState = mapOf(
                                    "status" to item.status,
                                    "available" to item.available,
                                    "exchangeState" to item.exchangeState,
                                    "lockedByBidId" to item.lockedByBidId
                                )
                                batchUpdates["/$itemId/stashedStateBeforeTempBlock"] = stashedState

                                batchUpdates["/$itemId/status"] = "owner_temporarily_blocked"
                                batchUpdates["/$itemId/available"] = false
                                if (item.exchangeState == "in_negotiation" || item.exchangeState == "none") {
                                    batchUpdates["/$itemId/exchangeState"] = "on_hold_owner_blocked"
                                }
                            }
                        }
                    }
                    if (batchUpdates.isNotEmpty()) {
                        databaseRefPublic.updateChildren(batchUpdates)
                            .addOnSuccessListener {
                                Log.i(TAG, "Items for user $userId updated for temporary block.")
                                onComplete(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to update items for user $userId temp block: ${e.message}")
                                onComplete(false)
                            }
                    } else {
                        Log.d(TAG, "No applicable items found to update for user $userId temp block.")
                        onComplete(true)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "updateUserItemsForTempBlock query cancelled for $userId: ${error.message}")
                    onComplete(false)
                }
            })
    }

    // --- WHEN A USER'S TEMPORARY BLOCK EXPIRES OR IS LIFTED ---
    fun unblockUserItems(userId: String, onComplete: (Boolean) -> Unit) {
        databaseRefPublic.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "No items found for user $userId to unblock.")
                        onComplete(true)
                        return
                    }
                    val batchUpdates = mutableMapOf<String, Any?>()
                    snapshot.children.forEach { itemSnapshot ->
                        val item = itemSnapshot.getValue(Item::class.java)
                        itemSnapshot.key?.let { itemId ->
                            if (item != null && item.status == "owner_temporarily_blocked") {
                                val stashed = item.stashedStateBeforeTempBlock

                                batchUpdates["/$itemId/status"] = stashed?.status as? String ?: "pending"
                                batchUpdates["/$itemId/available"] = stashed?.available as? Boolean ?: ( (stashed?.status as? String ?: "pending") == "approved" ) // Default available if restored status is approved
                                batchUpdates["/$itemId/exchangeState"] = stashed?.exchangeState as? String ?: "none"
                                batchUpdates["/$itemId/lockedByBidId"] = stashed?.lockedByBidId // Can be null

                                batchUpdates["/$itemId/stashedStateBeforeTempBlock"] = null
                            }
                        }
                    }
                    if (batchUpdates.isNotEmpty()) {
                        databaseRefPublic.updateChildren(batchUpdates)
                            .addOnSuccessListener {
                                Log.i(TAG, "Items for user $userId restored after temporary block.")
                                onComplete(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to restore items for user $userId: ${e.message}")
                                onComplete(false)
                            }
                    } else {
                        Log.d(TAG, "No items found in 'owner_temporarily_blocked' state for user $userId.")
                        onComplete(true)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "unblockUserItems query cancelled for $userId: ${error.message}")
                    onComplete(false)
                }
            })
    }

    // --- WHEN A USER IS PERMANENTLY BLOCKED ---
    fun updateUserItemsForPermBlock(userId: String, onComplete: (Boolean) -> Unit) {
        val updatesToApply = mapOf<String, Any?>( // Use Any? to allow null
            "available" to false,
            "status" to "owner_permanently_blocked",
            "exchangeState" to "archived_owner_blocked",
            "lockedByBidId" to null,
            "stashedStateBeforeTempBlock" to null // Clear any prior temp block stash
        )
        updateItemsByOwner(userId, updatesToApply, onComplete)
    }

    // Generic helper to apply updates to all items of a user
    private fun updateItemsByOwner(userId: String, updatesToApply: Map<String, Any?>, onComplete: (Boolean) -> Unit) {
        databaseRefPublic.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "No items found for user $userId during updateItemsByOwner.")
                        onComplete(true)
                        return
                    }
                    val itemParentUpdates = mutableMapOf<String, Any?>()
                    snapshot.children.forEach { itemSnapshot ->
                        itemSnapshot.key?.let { itemId ->
                            updatesToApply.forEach { (key, value) ->
                                itemParentUpdates["/$itemId/$key"] = value
                            }
                        }
                    }
                    if (itemParentUpdates.isNotEmpty()) {
                        databaseRefPublic.updateChildren(itemParentUpdates)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.i(TAG, "updateItemsByOwner successful for user $userId")
                                } else {
                                    Log.e(TAG, "updateItemsByOwner failed for user $userId", task.exception)
                                }
                                onComplete(task.isSuccessful)
                            }
                    } else {
                        Log.d(TAG, "No items required updates for user $userId during updateItemsByOwner.")
                        onComplete(true)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "updateItemsByOwner query for $userId failed: ${error.message}")
                    onComplete(false)
                }
            })
    }
        // Helper to update items only if they match a current status (used for unblocking)
        private fun updateItemsByOwnerIfStatus(
            userId: String,
            currentExpectedStatus: String,
            updatesToApply: Map<String, Any>,
            onComplete: (Boolean) -> Unit
        ) {
            databaseRefPublic.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) { onComplete(true); return }
                        val itemParentUpdates = mutableMapOf<String, Any?>()
                        snapshot.children.forEach { itemSnapshot ->
                            val item = itemSnapshot.getValue(Item::class.java)
                            itemSnapshot.key?.let { itemId ->
                                if (item != null && item.status == currentExpectedStatus) {
                                    updatesToApply.forEach { (key, value) ->
                                        itemParentUpdates["/$itemId/$key"] = value
                                    }
                                    // itemParentUpdates["/$itemId/userId_status"] = "${userId}_${updatesToApply["status"]}"
                                }
                            }
                        }
                        if (itemParentUpdates.isNotEmpty()) {
                            databaseRefPublic.updateChildren(itemParentUpdates)
                                .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
                        } else {
                            onComplete(true)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) { onComplete(false) }
                })
        }
    }














//package com.example.signuplogina.mvvm
//
//import com.example.signuplogina.Bid
//import com.example.signuplogina.Item
//import com.example.signuplogina.ItemDetails
//import com.google.android.gms.tasks.Tasks
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.FirebaseDatabase
//
//class ItemsRepo {
//
//    private val databaseRef = FirebaseDatabase.getInstance().getReference("Items")
//// fetching the details of the requested and the offered item by using the bid (whole)
//    fun getItemsByIds(
//        bid: Bid,
//        onSuccess: (bidItem: ItemDetails, offeredItem: ItemDetails) -> Unit,
//        onFailure: (Exception) -> Unit
//    ) {
//        val itemId = bid.itemId
//        val offeredItemId = bid.offeredItemId
//
//        val itemTasks = listOf(
//            databaseRef.child(itemId).get(),
//            databaseRef.child(offeredItemId).get()
//        )
//
//        Tasks.whenAllSuccess<DataSnapshot>(itemTasks)
//            .addOnSuccessListener { snapshots ->
//                try {
//                    var bidItem = snapshots[0].getValue(Item::class.java)!!
//                    var bidItemDetails=bidItem.details
//                    val offeredItem = snapshots[1].getValue(Item::class.java)!!
//                    val offeredItemDetails=offeredItem.details
//                    onSuccess(bidItemDetails, offeredItemDetails)
//                } catch (e: Exception) {
//                    onFailure(e)
//                }
//            }
//            .addOnFailureListener { onFailure(it) }
//    }
//
//
//
//    fun getItemById(itemId: String, onResult: (Item?) -> Unit) {
//        val itemRef = FirebaseDatabase.getInstance().getReference("Items").child(itemId)
//        itemRef.get().addOnSuccessListener { snapshot ->
//            val item = snapshot.getValue(Item::class.java)
//            onResult(item)
//        }.addOnFailureListener {
//            onResult(null)
//        }
//    }
//
//
//
//}
