package com.example.signuplogina.mvvm

import android.util.Log
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*

class BidRepository {

    private val bidsRef = FirebaseDatabase.getInstance().getReference("Bids")
    private val itemsRef = FirebaseDatabase.getInstance().getReference("Items")
    private val usersRef = FirebaseDatabase.getInstance().getReference("Users")
    private val TAG = "BidRepository"

    fun getBidById(bidId: String, onSuccess: (Bid) -> Unit, onFailure: (String) -> Unit) {
        if (bidId.isBlank()) {
            onFailure("Bid ID is blank.")
            return
        }
        bidsRef.child(bidId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val bid = snapshot.getValue(Bid::class.java)
                    if (bid != null) {
                        bid.bidId = snapshot.key ?: bidId // Ensure bidId is set
                        onSuccess(bid)
                    } else {
                        onFailure("Bid not found or data is null for ID: $bidId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bid $bidId", e)
                    onFailure("Error parsing bid data: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getBidById cancelled for $bidId: ${error.message}")
                onFailure(error.message)
            }
        })
    }

    /**
     * Fetches bids for a specific item (item being bid ON) and then populates
     * details for the bidder and the offered items (all of them).
     * This is useful for displaying bids received on an item.
     */
    fun fetchBidsWithFullDetails(
        itemId: String, // The ID of the item that received the bids
        onComplete: (List<Bid>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        bidsRef.orderByChild("itemId").equalTo(itemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onComplete(emptyList())
                        return
                    }

                    val bidsToProcess = mutableListOf<Bid>()
                    snapshot.children.forEach { child ->
                        child.getValue(Bid::class.java)?.let { bid ->
                            bid.bidId = child.key ?: bid.bidId
                            bidsToProcess.add(bid)
                        }
                    }

                    if (bidsToProcess.isEmpty()) {
                        onComplete(emptyList())
                        return
                    }

                    val enrichedBids = mutableListOf<Bid>()
                    var processedCount = 0

                    bidsToProcess.forEach { bid ->
                        // Concurrently fetch bidder name and all offered items details
                        val bidderNameTask = usersRef.child(bid.bidderId).child("username")
                            .get() // Assuming "username"

                        val offeredItemDetailTasks = bid.offeredItemIds
                            .filter { it.isNotBlank() }
                            .map { offeredId -> itemsRef.child(offeredId).get() }

                        val allDetailTasks = mutableListOf(bidderNameTask)
                        allDetailTasks.addAll(offeredItemDetailTasks)


                        Tasks.whenAllSuccess<DataSnapshot>(allDetailTasks)
                            .addOnSuccessListener { results ->
                                // First result is bidder name
                                bid.bidderName =
                                    results[0].getValue(String::class.java) ?: "Unknown Bidder"

                                // Subsequent results are offered items
                                val fetchedOfferedItems = mutableListOf<Item>()
                                for (i in 1 until results.size) {
                                    results[i].getValue(Item::class.java)?.let { itemDetail ->
                                        itemDetail.id = results[i].key ?: bid.offeredItemIds[i - 1]
                                        fetchedOfferedItems.add(itemDetail)
                                    }
                                }
                                bid.offeredItemsDetails =
                                    fetchedOfferedItems // Populate the @IgnoredOnParcel field
                                enrichedBids.add(bid)
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    TAG,
                                    "Error fetching details for bid ${bid.bidId}: ${e.message}"
                                )
                                // Optionally add bid even if details fail, or handle error
                                enrichedBids.add(bid) // Add bid with partial/no details
                            }
                            .addOnCompleteListener {
                                processedCount++
                                if (processedCount == bidsToProcess.size) {
                                    onComplete(enrichedBids)
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "fetchBidsWithFullDetails cancelled: ${error.message}")
                    onFailure(error.message)
                }
            })
    }



    fun updateUserBidsForTempBlock(userId: String, reasonForStall: String, onComplete: (Boolean) -> Unit) {
        val newBidStatus = "stalled_owner_temp_blocked"
        val activeStatuses = listOf("pending", "started", "agreement_reached")

        var opsCompleted = 0
        var allQueriesSuccessful = true
        val batchUpdates = mutableMapOf<String, Any?>()

        val processSnapshot = { snapshot: DataSnapshot ->
            snapshot.children.forEach { bidSnap ->
                val bid = bidSnap.getValue(Bid::class.java)
                bidSnap.key?.let { bidId ->
                    if (bid != null && activeStatuses.contains(bid.status.lowercase())) {
                        batchUpdates["/$bidId/previousStatusBeforeBlock"] = bid.status
                        batchUpdates["/$bidId/status"] = newBidStatus
                        batchUpdates["/$bidId/canceledReason"] = reasonForStall
                    }
                }
            }
        }

        val onQueryFinished = { success: Boolean ->
            opsCompleted++
            if (!success) allQueriesSuccessful = false
            if (opsCompleted == 2) { // Both queries (bidder and receiver) have finished
                if (batchUpdates.isNotEmpty()) {
                    bidsRef.updateChildren(batchUpdates)
                        .addOnSuccessListener {
                            Log.i(TAG, "Bids involving user $userId updated for temporary block.")
                            onComplete(true) }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to update bids for user $userId temp block: ${e.message}")
                            onComplete(false)
                        }
                } else {
                    Log.d(TAG, "No active bids found involving $userId to stall for temp block.")
                    onComplete(allQueriesSuccessful) // True if queries ran fine even if no updates
                }
            }
        }

        // Query for bids where user is bidder
        bidsRef.orderByChild("bidderId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { processSnapshot(s); onQueryFinished(true) }
            override fun onCancelled(e: DatabaseError) { Log.e(TAG, "Bidder query for temp block failed: ${e.message}"); onQueryFinished(false) }
        })
        // Query for bids where user is receiver
        bidsRef.orderByChild("receiverId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { processSnapshot(s); onQueryFinished(true) }
            override fun onCancelled(e: DatabaseError) { Log.e(TAG, "Receiver query for temp block failed: ${e.message}"); onQueryFinished(false) }
        })
    }

    // --- WHEN USER'S TEMPORARY BLOCK EXPIRES OR IS LIFTED ---
    fun reactivateUserBidsIfStalled(userId: String, onComplete: (Boolean) -> Unit) {
        val stalledStatus = "stalled_owner_temp_blocked"
        val batchUpdates = mutableMapOf<String, Any?>()
        var opsCompleted = 0
        var allQueriesSuccessful = true

        val processSnapshot = { snapshot: DataSnapshot ->
            snapshot.children.forEach { bidSnap ->
                val bid = bidSnap.getValue(Bid::class.java)
                bidSnap.key?.let { bidId ->
                    if (bid != null && bid.status.equals(stalledStatus, ignoreCase = true)) {
                        batchUpdates["/$bidId/status"] = bid.previousStatusBeforeBlock ?: "pending"
                        batchUpdates["/$bidId/previousStatusBeforeBlock"] = null
                        batchUpdates["/$bidId/canceledReason"] = null
                    }
                }
            }
        }
        val onQueryFinished = { success: Boolean ->
            opsCompleted++
            if(!success) allQueriesSuccessful = false
            if (opsCompleted == 2) {
                if (batchUpdates.isNotEmpty()) {
                    bidsRef.updateChildren(batchUpdates)
                        .addOnSuccessListener {
                            Log.i(TAG, "Stalled bids involving user $userId reactivated.")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to reactivate stalled bids for user $userId: ${e.message}")
                            onComplete(false)
                        }
                } else {
                    Log.d(TAG, "No stalled bids found involving $userId to reactivate.")
                    onComplete(allQueriesSuccessful)
                }
            }
        }
        bidsRef.orderByChild("bidderId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { processSnapshot(s); onQueryFinished(true) }
            override fun onCancelled(e: DatabaseError) { Log.e(TAG, "Bidder query for reactivate failed: ${e.message}"); onQueryFinished(false) }
        })
        bidsRef.orderByChild("receiverId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { processSnapshot(s); onQueryFinished(true) }
            override fun onCancelled(e: DatabaseError) { Log.e(TAG, "Receiver query for reactivate failed: ${e.message}"); onQueryFinished(false) }
        })
    }

    // --- WHEN A USER IS PERMANENTLY BLOCKED ---
    fun cancelUserBids(userId: String, reasonForCancel: String, onComplete: (Boolean) -> Unit) {
        val updatesToApply = mapOf<String, Any?>(
            "status" to "canceled_by_admin_user_blocked",
            "canceledReason" to reasonForCancel,
            "previousStatusBeforeBlock" to null // Clear any prior temp block stash
        )
        val activeAndStalledStatuses = listOf("pending", "started", "agreement_reached", "stalled_owner_temp_blocked")

        var opsCompleted = 0
        var allQueriesSuccessful = true
        val batchUpdates = mutableMapOf<String, Any?>()

        val processSnapshot = { snapshot: DataSnapshot ->
            snapshot.children.forEach { bidSnap ->
                val bid = bidSnap.getValue(Bid::class.java)
                if (bid != null && activeAndStalledStatuses.contains(bid.status.lowercase())) {
                    bidSnap.key?.let { bidId ->
                        updatesToApply.forEach { (k, v) -> batchUpdates["/$bidId/$k"] = v }
                    }
                }
            }
        }

        val onQueryFinished = { success: Boolean ->
            opsCompleted++
            if(!success) allQueriesSuccessful = false
            if (opsCompleted == 2) {
                if (batchUpdates.isNotEmpty()) {
                    bidsRef.updateChildren(batchUpdates)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(allQueriesSuccessful)
                }
            }
        }

        bidsRef.orderByChild("bidderId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { processSnapshot(s); onQueryFinished(true) }
            override fun onCancelled(e: DatabaseError) { Log.e(TAG, "Bidder query for perm block cancel failed: ${e.message}"); onQueryFinished(false) }
        })
        bidsRef.orderByChild("receiverId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { processSnapshot(s); onQueryFinished(true) }
            override fun onCancelled(e: DatabaseError) { Log.e(TAG, "Receiver query for perm block cancel failed: ${e.message}"); onQueryFinished(false) }
        })
    }


//    ///////////////////////////////////////////////
//    ///////////////////////////////////////////

//    fun updateUserBidsForTempBlock(userId: String, reasonForStall: String, onComplete: (Boolean) -> Unit) {
//        val newBidStatus = "stalled_owner_temp_blocked"
//        val updates = mapOf<String, Any?>(
//            "status" to newBidStatus,
//            "canceledReason" to reasonForStall // Using canceledReason to store stall info
//        )
//        updateActiveBidsInvolvingUser(userId, updates, onComplete)
//    }

    // For permanent block, bids are fully canceled
//    fun cancelUserBids(userId: String, reasonForCancel: String, onComplete: (Boolean) -> Unit) {
//        val updates = mapOf<String, Any?>(
//            "status" to "canceled_by_admin_user_blocked", // More specific status
//            "canceledReason" to reasonForCancel,
//            "isCancelledDueToItem" to false // Or a new field like "isCancelledDueToUserBlock"
//        )
//        updateActiveBidsInvolvingUser(userId, updates, onComplete)
//    }

//    fun reactivateUserBidsIfStalled(userId: String, onComplete: (Boolean) -> Unit) {
//        val stalledStatus = "stalled_owner_temp_blocked"
//        // Determine what the bid status should revert to.
//        // This is tricky. For simplicity, reverting to "pending".
//        // A more robust solution might store the pre-stall status.
//        val reactivatedStatus = "pending"
//        val updates = mapOf<String, Any?>(
//            "status" to reactivatedStatus,
//            "canceledReason" to null // Clear the stall reason
//        )
//        updateBidsByStatusAndUser(userId, stalledStatus, updates, onComplete)
//    }

    // Helper to update ACTIVE bids involving a user (bidder or receiver)
    private fun updateActiveBidsInvolvingUser(userId: String, updatesToApply: Map<String, Any?>, onComplete: (Boolean) -> Unit) {
        var operationsCompleted = 0
        var successfulOperations = 0
        val totalQueries = 2 // One for bidder, one for receiver

        val checkCompletion = {
            operationsCompleted++
            if (operationsCompleted == totalQueries) {
                onComplete(successfulOperations == totalQueries) // Or successfulOperations > 0 if partial success is ok
            }
        }

        val activeStatuses = listOf("pending", "started", "agreement_reached")

        // Query for bids where user is bidder
        bidsRef.orderByChild("bidderId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { checkCompletion(); return }
                val batchUpdates = mutableMapOf<String, Any?>()
                snapshot.children.forEach { bidSnap ->
                    val bid = bidSnap.getValue(Bid::class.java)
                    if (bid != null && activeStatuses.contains(bid.status)) {
                        bidSnap.key?.let { bidId ->
                            updatesToApply.forEach { (key, value) -> batchUpdates["$bidId/$key"] = value }
                        }
                    }
                }
                if (batchUpdates.isNotEmpty()) {
                    bidsRef.updateChildren(batchUpdates).addOnCompleteListener { checkCompletion() }
                } else {
                    checkCompletion()
                }
            }
            override fun onCancelled(error: DatabaseError) { Log.e(TAG, "Bidder query failed: ${error.message}"); checkCompletion(); }
        })

        // Query for bids where user is receiver
        bidsRef.orderByChild("receiverId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { checkCompletion(); return }
                val batchUpdates = mutableMapOf<String, Any?>()
                snapshot.children.forEach { bidSnap ->
                    val bid = bidSnap.getValue(Bid::class.java)
                    if (bid != null && activeStatuses.contains(bid.status)) {
                        bidSnap.key?.let { bidId ->
                            updatesToApply.forEach { (key, value) -> batchUpdates["$bidId/$key"] = value }
                        }
                    }
                }
                if (batchUpdates.isNotEmpty()) {
                    bidsRef.updateChildren(batchUpdates).addOnCompleteListener { checkCompletion() }
                } else {
                    checkCompletion()
                }
            }
            override fun onCancelled(error: DatabaseError) { Log.e(TAG, "Receiver query failed: ${error.message}"); checkCompletion(); }
        })
    }

    // Helper to update bids of a specific user IF they are in a specific currentStatus
    private fun updateBidsByStatusAndUser(userId: String, currentExpectedStatus: String, updatesToApply: Map<String, Any?>, onComplete: (Boolean) -> Unit) {
        var operationsCompleted = 0
        var successfulOperations = 0
        val totalQueries = 2

        val checkCompletion = {
            operationsCompleted++
            if (operationsCompleted == totalQueries) {
                onComplete(successfulOperations == totalQueries)
            }
        }
        // Bids where user is bidder and status matches
        bidsRef.orderByChild("bidderId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val batchUpdates = mutableMapOf<String, Any?>()
                    snapshot.children.forEach { bidSnap ->
                        val bid = bidSnap.getValue(Bid::class.java)
                        if (bid != null && bid.status == currentExpectedStatus) {
                            bidSnap.key?.let { bidId ->
                                updatesToApply.forEach { (key, value) -> batchUpdates["$bidId/$key"] = value }
                            }
                        }
                    }
                    if (batchUpdates.isNotEmpty()) bidsRef.updateChildren(batchUpdates).addOnCompleteListener{ checkCompletion() } else checkCompletion()
                }
                override fun onCancelled(error: DatabaseError) { checkCompletion() }
            })
        // Bids where user is receiver and status matches
        bidsRef.orderByChild("receiverId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val batchUpdates = mutableMapOf<String, Any?>()
                    snapshot.children.forEach { bidSnap ->
                        val bid = bidSnap.getValue(Bid::class.java)
                        if (bid != null && bid.status == currentExpectedStatus) {
                            bidSnap.key?.let { bidId ->
                                updatesToApply.forEach { (key, value) -> batchUpdates["$bidId/$key"] = value }
                            }
                        }
                    }
                    if (batchUpdates.isNotEmpty()) bidsRef.updateChildren(batchUpdates).addOnCompleteListener{ checkCompletion() } else checkCompletion()
                }
                override fun onCancelled(error: DatabaseError) { checkCompletion() }
            })
    }


    /**
     * Fetches raw bids for an item (item being bid ON) without populating extra details.
     * This is kept from your original code.
     */
    fun fetchBidsForItem(itemId: String, onResult: (List<Bid>) -> Unit) {
        val query = bidsRef.orderByChild("itemId").equalTo(itemId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bids = mutableListOf<Bid>()
                for (child in snapshot.children) {
                    child.getValue(Bid::class.java)?.let { bid ->
                        bid.bidId = child.key ?: bid.bidId
                        bids.add(bid)
                    }
                }
                onResult(bids)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "fetchBidsForItem cancelled: ${error.message}")
                onResult(emptyList())
            }
        })
    }

    // The old fetchBidsWithOfferedItems and fetchOfferedItemFromBid are no longer suitable
    // because they assume a single offeredItemId.
    // fetchBidsWithFullDetails above replaces their functionality in a more comprehensive way.

    // Your getAllBidIdsForExchangeChat seems fine as it operates on a different DB path ("Conversations")
    // and only extracts keys (bidIds), so it's not directly affected by the Bid model change.
    fun getAllBidIdsForExchangeChat(
        currentUserId: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val dbRef = FirebaseDatabase.getInstance().reference
            .child("Conversations")
            .child("ExchangeChats")
            .child(currentUserId)

        val bidIds = mutableListOf<String>()
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (otherUserSnapshot in snapshot.children) {
                    for (bidSnapshot in otherUserSnapshot.children) {
                        bidSnapshot.key?.let { bidIds.add(it) }
                    }
                }
                onSuccess(bidIds.distinct()) // Ensure distinct IDs
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.message)
            }
        })
    }

    // Inside BidRepository.kt
// ... (bidsRef, itemsRef, usersRef, TAG, getBidById)

    fun getOnHoldBidsForItemNonSuspending(
        requestedItemId: String,
        onResult: (List<Bid>) -> Unit
    ) {
        if (requestedItemId.isBlank()) {
            onResult(emptyList())
            return
        }
        bidsRef.orderByChild("itemId").equalTo(requestedItemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val onHoldBids = mutableListOf<Bid>()
                    if (snapshot.exists()) {
                        for (childSnapshot in snapshot.children) {
                            try {
                                val bid = childSnapshot.getValue(Bid::class.java)
                                if (bid != null && bid.status.equals(
                                        "on_hold_item_negotiation",
                                        ignoreCase = true
                                    )
                                ) {
                                    bid.bidId = childSnapshot.key ?: bid.bidId
                                    onHoldBids.add(bid)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Error parsing other holding bid: ${childSnapshot.key}",
                                    e
                                )
                            }
                        }
                    }
                    onResult(onHoldBids)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        TAG,
                        "Fetching other honlding bids for item $requestedItemId cancelled: ${error.message}"
                    )
                    onResult(emptyList())
                }
            })
    }

    // Non-suspending version using callback (ViewModel will need to handle nesting or convert to Flow/suspend)
    fun getOtherPendingBidsForItemNonSuspending(
        requestedItemId: String,
        excludeBidId: String, // The bid that is being accepted/started
        onResult: (List<Bid>) -> Unit
    ) {
        if (requestedItemId.isBlank()) {
            onResult(emptyList())
            return
        }
        bidsRef.orderByChild("itemId").equalTo(requestedItemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val otherBids = mutableListOf<Bid>()
                    if (snapshot.exists()) {
                        for (childSnapshot in snapshot.children) {
                            try {
                                val bid = childSnapshot.getValue(Bid::class.java)
                                if (bid != null &&
                                    childSnapshot.key != excludeBidId && // Exclude the current bid
                                    bid.status == "pending"
                                ) {           // Only consider pending bids
                                    bid.bidId =
                                        childSnapshot.key ?: bid.bidId // Ensure bidId is set
                                    otherBids.add(bid)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Error parsing other pending bid: ${childSnapshot.key}",
                                    e
                                )
                            }
                        }
                    }
                    onResult(otherBids)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        TAG,
                        "Fetching other pending bids for item $requestedItemId cancelled: ${error.message}"
                    )
                    onResult(emptyList())
                }
            })
    }

    // OR, if you prefer and can manage it within the ViewModel's coroutine:
// Suspending version (cleaner to use with await() in ViewModel)
//    suspend fun getOtherPendingBidsForItemSuspending(requestedItemId: String, excludeBidId: String): List<Bid> {
//        if (requestedItemId.isBlank()) {
//            return emptyList()
//        }
//        return try {
//            val snapshot = bidsRef.orderByChild("itemId").equalTo(requestedItemId).get().await()
//            val otherBids = mutableListOf<Bid>()
//            if (snapshot.exists()) {
//                snapshot.children.forEach { child ->
//                    try {
//                        val bid = child.getValue(Bid::class.java)
//                        if (bid != null && child.key != excludeBidId && bid.status == "pending") {
//                            bid.bidId = child.key ?: bid.bidId
//                            otherBids.add(bid)
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error parsing other pending bid in suspend fun: ${child.key}", e)
//                    }
//                }
//            }
//            otherBids
//        } catch (e: Exception) {
//            Log.e(TAG, "Suspending fetch for other pending bids failed for item $requestedItemId: ${e.message}")
//            emptyList()
//        }
//    }

}


//package com.example.signuplogina.mvvm
//
//import android.util.Log
//import com.example.signuplogina.Bid
//import com.example.signuplogina.Item
//import com.google.firebase.database.*
//
//class BidRepository {
//
//    private val database = FirebaseDatabase.getInstance().getReference("Bids")
//
//    fun getBidById(bidId: String, onSuccess: (Bid) -> Unit, onFailure: (String) -> Unit) {
//        database.child(bidId).addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val bid = snapshot.getValue(Bid::class.java)
//                if (bid != null) {
//                    onSuccess(bid)
//                } else {
//                    onFailure("Bid not found")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                onFailure(error.message)
//            }
//        })
//    }
//
//
//
//    fun fetchBidsWithOfferedItems(
//        itemId: String,
//        onComplete: (List<Bid>) -> Unit
//    ) {
//        val db = FirebaseDatabase.getInstance().reference
//        val bidsRef = db.child("Bids")
//        val itemsRef = db.child("Items")
//        val usersRef = db.child("Users")
//
//        bidsRef.orderByChild("itemId").equalTo(itemId)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val bids = mutableListOf<Bid>()
//
//                    for (child in snapshot.children) {
//                        val bid = child.getValue(Bid::class.java)
//                        bid?.let { b ->
//                            // Fetch offered item data
//                            itemsRef.child(b.offeredItemId).get()
//                                .addOnSuccessListener { itemSnap ->
//                                    val item = itemSnap.getValue(Item::class.java)
//                                    b.offeredItem = item
//
//                                    // Fetch bidder name data after the item data
//                                    usersRef.child(b.bidderId).child("name").get()
//                                        .addOnSuccessListener { nameSnap ->
//                                            b.bidderName = nameSnap.getValue(String::class.java) ?: ""
//                                            bids.add(b)
//                                        }
//                                }
//                        }
//                    }
//
//                    // Notify completion once all bids are processed
//                    // This should be done after all bidders' names and offered items have been added
//                    // Depending on your logic, you might want to add a counter to ensure all data is fetched
//                    if (bids.size == snapshot.childrenCount.toInt()) {
//                        onComplete(bids)
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    onComplete(emptyList())
//                }
//            })
//    }
//
//
//    fun fetchBidsForItem(itemId: String, onResult: (List<Bid>) -> Unit) {
//        val bidsRef = FirebaseDatabase.getInstance().getReference("Bids")
//        val query = bidsRef.orderByChild("itemId").equalTo(itemId)
//
//        query.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val bids = mutableListOf<Bid>()
//                for (child in snapshot.children) {
//                    val bid = child.getValue(Bid::class.java)
//                    bid?.let { bids.add(it) }
//                }
//                onResult(bids)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("BidRepo", "fetchBidsForItem cancelled: ${error.message}")
//                onResult(emptyList())
//            }
//        })
//    }
//
//
//// fetch offered item details from the bid
//    fun fetchOfferedItemFromBid(
//        bid: Bid,
//        onResult: (Item?) -> Unit
//    ) {
//        val itemRef = FirebaseDatabase.getInstance()
//            .getReference("Items")
//            .child(bid.offeredItemId)
//
//        itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val item = snapshot.getValue(Item::class.java)
//                onResult(item)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("fetchOfferedItemFromBid", "Error: ${error.message}")
//                onResult(null)
//            }
//        })
//    }
//
//    fun getAllBidIdsForExchangeChat(
//        currentUserId: String,
//        onSuccess: (List<String>) -> Unit,  // Callback that returns the list of bidIds
//        onFailure: (String) -> Unit         // Callback to handle failure
//    ) {
//        val dbRef = FirebaseDatabase.getInstance().reference
//            .child("Conversations")
//            .child("ExchangeChats")
//            .child(currentUserId)  // Target the current user's exchange chat
//
//        // List to store all the bidIds
//        val bidIds = mutableListOf<String>()
//
//        // Firebase listener to fetch data
//        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                // Loop through each child of currentUserId (each otherUserId in the exchange chat)
//                for (otherUserSnapshot in snapshot.children) {
//                    // Loop through each bidId under the otherUserId
//                    for (bidSnapshot in otherUserSnapshot.children) {
//                        // Add the bidId to the list
//                        val bidId = bidSnapshot.key
//                        if (bidId != null) {
//                            bidIds.add(bidId)  // Add bidId to the list
//                        }
//                    }
//                }
//
//                // Callback with the list of bidIds
//                onSuccess(bidIds)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // In case of error, call the failure callback with the error message
//                onFailure(error.message)
//            }
//        })
//    }
//
//}
