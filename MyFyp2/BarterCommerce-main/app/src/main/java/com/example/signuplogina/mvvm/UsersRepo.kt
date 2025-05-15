package com.example.signuplogina.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.example.signuplogina.Utils
import com.example.signuplogina.User
import com.example.signuplogina.modal.BlockRecord
import com.example.signuplogina.modal.UserRatingStats
import com.google.firebase.database.*

class UsersRepo {

    private val usersRef = FirebaseDatabase.getInstance().getReference("Users")
    private val exchangeRef =
        FirebaseDatabase.getInstance().getReference("Conversations/ExchangeChats")

    private val itemsRepo = ItemsRepo()
    private val bidsRepo = BidRepository()
    private val TAG = "UsersRepo"

    // 🔹 Normal chat – Fetch all users excluding the current user
    fun getAllUsersForChat(): LiveData<List<User>> {
        val users = MutableLiveData<List<User>>()
        val currentUserId = Utils.getUidLoggedIn()

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersList = mutableListOf<User>()
                snapshot.children.forEach { data ->
                    val user = data.getValue(User::class.java)
                    if (user != null && user.userid != currentUserId) {
                        usersList.add(user)
                    }
                }
                users.value = usersList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "getAllUsersForChat error: ${error.message}")
            }
        })

        return users
    }



    fun getAllUsersFromFirebase(callback: (List<User>) -> Unit) {
        val currentUserId = Utils.getUidLoggedIn()

        FirebaseDatabase.getInstance().getReference("Users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usersList = mutableListOf<User>()
                    snapshot.children.forEach { data ->
                        val user = data.getValue(User::class.java)
                        if (user != null && user.userid != currentUserId) {
                            usersList.add(user)
                        }
                    }
                    callback(usersList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "getAllUsersFromFirebase error: ${error.message}")
                    callback(emptyList())
                }
            })
    }



    fun clearUserBlockStatus(userId: String, onComplete: (Boolean) -> Unit) {
        val userRatingsRef = usersRef.child(userId).child("ratings")
        val updates = mapOf<String, Any?>(
            "isTemporarilyBlocked" to false,
            "isPermanentlyBlocked" to false, // If an admin unblocks, this might also be set to false
            "blockExpiryTimestamp" to 0L,
            "blockReason" to null            // CLEAR THE ACTIVE BLOCK REASON
        )
        userRatingsRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.i(TAG, "Active block flags and reason cleared for user $userId in UserRatingStats.")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to clear block status for $userId: ${e.message}", e)
                onComplete(false)
            }
    }

    // Orchestrator when a temporary block expires automatically (called from MainActivity)
    fun handleExpiredTemporaryBlock(
        userId: String,
        onComplete: (unblockSuccess: Boolean, userAllowedToProceed: Boolean) -> Unit
    ) {
        val userRatingsRef = usersRef.child(userId).child("ratings")

        userRatingsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.getValue(UserRatingStats::class.java)
                if (stats == null || !stats.isTemporarilyBlocked || stats.isPermanentlyBlocked) {
                    Log.d(TAG, "handleExpiredTempBlock: No action for user $userId. Stats: $stats")
                    return Transaction.success(currentData)
                }
                if (System.currentTimeMillis() >= stats.blockExpiryTimestamp && stats.blockExpiryTimestamp != 0L) {
                    Log.i(TAG, "handleExpiredTempBlock: Temp block expired for user $userId. Updating stats.")
                    stats.isTemporarilyBlocked = false
                    stats.blockExpiryTimestamp = 0L
                    // blockReason is cleared by clearUserBlockStatus called in onComplete
                    currentData.value = stats
                    return Transaction.success(currentData)
                } else {
                    Log.d(TAG, "handleExpiredTempBlock: Temp block for user $userId not yet expired.")
                    return Transaction.success(currentData)
                }
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error == null && committed) {
                    val updatedStats = snapshot?.getValue(UserRatingStats::class.java)
                    // Check if the transaction actually unblocked (isTemporarilyBlocked is now false)
                    if (updatedStats != null && !updatedStats.isTemporarilyBlocked && !updatedStats.isPermanentlyBlocked) {
                        Log.i(TAG, "User $userId temp block expired and UserRatingStats updated. Proceeding with unblocking consequences.")

                        var itemsRestored = false
                        var bidsReactivated = false
                        var globalFlagCleared = false
                        var convosEnabled = false
                        var finalFlagsClearedInStats = false // For clearUserBlockStatus
                        var consequencesPending = 5

                        val checkConsequencesCompletion = {
                            if (consequencesPending == 0) {
                                val overallUnblockSuccess = itemsRestored && bidsReactivated && globalFlagCleared && convosEnabled && finalFlagsClearedInStats
                                Log.i(TAG, "All unblocking consequences for $userId processed. Overall success: $overallUnblockSuccess")
                                onComplete(true, overallUnblockSuccess)
                            }
                        }

                        itemsRepo.unblockUserItems(userId) { success -> itemsRestored = success; consequencesPending--; checkConsequencesCompletion() }
                        bidsRepo.reactivateUserBidsIfStalled(userId) { success -> bidsReactivated = success; consequencesPending--; checkConsequencesCompletion() }
                        markUserAsGloballyBlocked(userId, false) { success -> globalFlagCleared = success; consequencesPending--; checkConsequencesCompletion() }
                        enableConversationsForUser(userId) { success -> convosEnabled = success; consequencesPending--; checkConsequencesCompletion() }
                        clearUserBlockStatus(userId) { success -> finalFlagsClearedInStats = success; consequencesPending--; checkConsequencesCompletion() } // Ensure reason is cleared

                    } else if (updatedStats != null && (updatedStats.isTemporarilyBlocked || updatedStats.isPermanentlyBlocked)) {
                        Log.d(TAG, "User $userId still considered blocked after transaction (temp not expired or perm block).")
                        onComplete(false, false)
                    } else {
                        Log.d(TAG, "User $userId was not temporarily blocked or no stats node found after transaction.")
                        onComplete(false, true) // Not relevant to unblock, allow proceed if not otherwise blocked
                    }
                } else {
                    Log.e(TAG, "handleExpiredTemporaryBlock transaction failed or not committed for $userId: ${error?.message}")
                    onComplete(false, false)
                }
            }
        })
    }


//////////////////////////////////////
    // 🔹 Exchange chat – Fetch only users the current user is exchanging with
    fun getExchangeChatUsers(): LiveData<List<User>> {
        val users = MutableLiveData<List<User>>()
        val currentUserId = Utils.getUidLoggedIn()
        val exchangeUserRef = exchangeRef.child(currentUserId)

        exchangeUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val exchangeUserIds = mutableListOf<String>()
                snapshot.children.forEach { data ->
                    data.key?.let { exchangeUserIds.add(it) }
                }

                val userList = mutableListOf<User>()
                usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        userSnapshot.children.forEach { userSnap ->
                            val user = userSnap.getValue(User::class.java)
                            if (user != null && exchangeUserIds.contains(user.userid)) {
                                userList.add(user)
                            }
                        }
                        users.postValue(userList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseError", "User fetch error: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "ExchangeConversations error: ${error.message}")
            }
        })

        return users
    }

    fun getUserInfo(userId: String, callback: (name: String, imageUrl: String) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("fullName").getValue(String::class.java) ?: "Unknown"
//                val imageUrl = snapshot.child("https://www.pngarts.com/files/6/User-Avatar-in-Suit-PNG.png").getValue(String::class.java) ?: ""
                val imageUrl = "https://www.pngarts.com/files/6/User-Avatar-in-Suit-PNG.png"

                callback(userName, imageUrl)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Unknown", "")
            }
        })
    }

    fun getUserItems(userId: String, onResult: (List<Item>) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        val itemsRef = FirebaseDatabase.getInstance().getReference("Items")

        userRef.child("items").get().addOnSuccessListener { snapshot ->
            val itemIds = snapshot.children.mapNotNull { it.key }

            if (itemIds.isEmpty()) {
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val items = mutableListOf<Item>()
            var loadedCount = 0

            for (itemId in itemIds) {
                itemsRef.child(itemId).get().addOnSuccessListener { itemSnapshot ->
                    itemSnapshot.getValue(Item::class.java)?.let {
                        items.add(it)
                    }

                    loadedCount++
                    if (loadedCount == itemIds.size) {
                        onResult(items)
                    }
                }.addOnFailureListener {
                    loadedCount++
                    if (loadedCount == itemIds.size) {
                        onResult(items)
                    }
                }
            }
        }.addOnFailureListener {
            onResult(emptyList())
        }
    }

    fun getUserItemsWithTwoOrMoreBids(userId: String, onResult: (List<Item>) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        val itemsRef = FirebaseDatabase.getInstance().getReference("Items")

        userRef.child("items").get().addOnSuccessListener { snapshot ->
            val itemIds = snapshot.children.mapNotNull { it.key }

            if (itemIds.isEmpty()) {
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val items = mutableListOf<Item>()
            var loadedCount = 0

            for (itemId in itemIds) {
                itemsRef.child(itemId).get().addOnSuccessListener { itemSnapshot ->
                    val item = itemSnapshot.getValue(Item::class.java)
                    val bidsCount = itemSnapshot.child("bids").childrenCount

                    if (item != null && bidsCount >= 2) {
                        items.add(item)
                    }

                    loadedCount++
                    if (loadedCount == itemIds.size) {
                        onResult(items)
                    }
                }.addOnFailureListener {
                    loadedCount++
                    if (loadedCount == itemIds.size) {
                        onResult(items)
                    }
                }
            }
        }.addOnFailureListener {
            onResult(emptyList())
        }
    }

// UsersRepo.kt

// ... existing methods ...

    fun applyUserBlock(
        userId: String,
        isTemporary: Boolean,
        isPermanent: Boolean,
        expiryTimestamp: Long, // 0 if permanent or not temporary
        reason: String,
        blockRecord: BlockRecord,
        onComplete: (Boolean) -> Unit
    ) {
        val userRatingsRef = usersRef.child(userId).child("ratings")
        userRatingsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stats = currentData.getValue(UserRatingStats::class.java) ?: UserRatingStats()
                stats.isTemporarilyBlocked = isTemporary
                stats.isPermanentlyBlocked = isPermanent
                stats.blockExpiryTimestamp = if (isTemporary) expiryTimestamp else 0L
                stats.blockReason = reason
                val updatedHistory = stats.blockHistory.toMutableList()
                updatedHistory.add(0, blockRecord) // Add to the beginning of the list
                stats.blockHistory = updatedHistory.take(10) // Keep last 10 records for example

                currentData.value = stats
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e(
                        "UsersRepo",
                        "applyUserBlock failed: ${error.message}",
                        error.toException()
                    )
                }
                onComplete(committed && error == null)
            }
        })
    }

//    fun clearUserBlockStatus(userId: String, onComplete: (Boolean) -> Unit) {
//        val userRatingsRef = usersRef.child(userId).child("ratings")
//        val updates = mapOf(
//            "isTemporarilyBlocked" to false,
//            "isPermanentlyBlocked" to false,
//            "blockExpiryTimestamp" to 0L,
//            "blockReason" to null
//            // Note: blockHistory remains for record keeping
//        )
//        userRatingsRef.updateChildren(updates)
//            .addOnSuccessListener { onComplete(true) }
//            .addOnFailureListener { e ->
//                Log.e("UsersRepo", "clearUserBlockStatus failed", e)
//                onComplete(false)
//            }
//    }

    // You already have disableConversationsForBlockedUser
// Add enableConversationsForUser
    fun enableConversationsForUser(userId: String, onComplete: ((Boolean) -> Unit)? = null) {
        val db = FirebaseDatabase.getInstance().reference
        val updateValue = mapOf("isBlocked" to false)

        // 1. Regular Chats
        db.child("Conversations").child("chats").child(userId).get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { conversation ->
                    val otherUserId = conversation.key ?: return@forEach
                    db.child("Conversations").child("chats").child(otherUserId).child(userId)
                        .updateChildren(updateValue)
                    db.child("Conversations").child("chats").child(userId).child(otherUserId)
                        .updateChildren(updateValue)
                }
            }
        // 2. Exchange Chats
        db.child("Conversations").child("ExchangeChats").child(userId).get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { otherUserNode ->
                    val otherUserId = otherUserNode.key ?: return@forEach
                    otherUserNode.children.forEach { bidNode ->
                        val bidId = bidNode.key ?: return@forEach
                        db.child("Conversations").child("ExchangeChats").child(otherUserId)
                            .child(userId).child(bidId).updateChildren(updateValue)
                        db.child("Conversations").child("ExchangeChats").child(userId)
                            .child(otherUserId).child(bidId).updateChildren(updateValue)
                    }
                }
                onComplete?.invoke(true) // Simplified completion
            }.addOnFailureListener { onComplete?.invoke(false) }
    }

    // To mark in top-level /BlockedUsers for quick check in MainActivity
    fun markUserAsGloballyBlocked(
        userId: String,
        isBlocked: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val ref = FirebaseDatabase.getInstance().getReference("BlockedUsers").child(userId)
        if (isBlocked) {
            ref.setValue(true).addOnCompleteListener { task -> onComplete(task.isSuccessful) }
        } else {
            ref.removeValue().addOnCompleteListener { task -> onComplete(task.isSuccessful) }
        }
    }


    // get user details by item id
    fun getUserDetailsByItemId(itemId: String, callback: (User?) -> Unit) {
        if (itemId.isBlank()) {
            callback(null)
            return
        }

        val itemRef = FirebaseDatabase.getInstance().getReference("Items").child(itemId)

        itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    callback(null)
                    return
                }
                val userId = snapshot.child("userId").getValue(String::class.java)
                if (userId != null && userId.isNotBlank()) {
                    getUserDetailsById(userId, callback)
                } else {
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "FirebaseFetch",
                    "Database error fetching item $itemId: ${error.message}",
                    error.toException()
                )
                callback(null)
            }
        })
    }

    fun getUserDetailsById(userId: String, callback: (User?) -> Unit) {
        if (userId.isBlank()) {
            callback(null)
            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                try {
                    val user = snapshot.getValue(User::class.java)
                    callback(user)
                } catch (e: DatabaseException) {
                    Log.e("FirebaseFetch", "Error deserializing user data for $userId", e)
                    callback(null)
                }
            } else {
                callback(null)
            }
        }.addOnFailureListener { exception ->
            Log.e("FirebaseFetch", "Failed to fetch user details for userId: $userId", exception)
            callback(null)
        }
    }


//    fun handleExpiredTemporaryBlock(
//        userId: String,
//        onComplete: (unblockSuccess: Boolean, userAllowedToProceed: Boolean) -> Unit
//    ) {
//        val userRatingsRef = usersRef.child(userId).child("ratings")
//
//        // Use a transaction to ensure atomicity when checking expiry and updating
//        userRatingsRef.runTransaction(object : Transaction.Handler {
//            override fun doTransaction(currentData: MutableData): Transaction.Result {
//                val stats = currentData.getValue(UserRatingStats::class.java)
//                if (stats == null || !stats.isTemporarilyBlocked || stats.isPermanentlyBlocked) {
//                    // Not temporarily blocked, or is permanently blocked, or no stats node.
//                    // Let the main onStart logic handle (or this means user is fine if stats is null and not perm blocked)
//                    return Transaction.success(currentData) // No change needed by this specific function
//                }
//
//                if (System.currentTimeMillis() >= stats.blockExpiryTimestamp && stats.blockExpiryTimestamp != 0L) {
//                    // Temporary block has expired! Clear it.
//                    stats.isTemporarilyBlocked = false
//                    stats.blockExpiryTimestamp = 0L
//                    // stats.blockReason = "Temporary block expired." // Optionally update reason
//                    // blockHistory remains as a record.
//                    currentData.value = stats
//                    return Transaction.success(currentData)
//                } else {
//                    // Still temporarily blocked (not expired yet) or timestamp was 0 (should not happen for active temp block)
//                    return Transaction.success(currentData) // No change, still blocked
//                }
//            }
//
//            override fun onComplete(
//                error: DatabaseError?,
//                committed: Boolean,
//                snapshot: DataSnapshot?
//            ) {
//                if (error != null) {
//                    Log.e(
//                        TAG,
//                        "Transaction failed for expired block check on user $userId: ${error.message}"
//                    )
//                    onComplete(false, false) // Unblock failed, user should not proceed
//                    return
//                }
//
//                if (committed) {
//                    val updatedStats = snapshot?.getValue(UserRatingStats::class.java)
//                    if (updatedStats != null && !updatedStats.isTemporarilyBlocked && !updatedStats.isPermanentlyBlocked) {
//                        // Successfully unblocked via transaction because it expired
//                        Log.i(
//                            TAG,
//                            "User $userId automatically unblocked as temporary block expired."
//                        )
//                        // Now perform consequential unblocking actions
//                        itemsRepo.unblockUserItems(userId) {
//                            Log.d(
//                                TAG,
//                                "Auto-unblock: Items for $userId unblocked: $it"
//                            )
//                        }
//                        bidsRepo.reactivateUserBidsIfStalled(userId) {
//                            Log.d(
//                                TAG,
//                                "Auto-unblock: Stalled bids for $userId reactivated: $it"
//                            )
//                        }
//                        markUserAsGloballyBlocked(userId, false) {} // Clear from /BlockedUsers
//                        enableConversationsForUser(userId) {
//                            Log.d(
//                                TAG,
//                                "Auto-unblock: Conversations enabled: $it"
//                            )
//                        }
//                        onComplete(true, true) // Unblocked successfully, user can proceed
//                    } else if (updatedStats != null && (updatedStats.isTemporarilyBlocked || updatedStats.isPermanentlyBlocked)) {
//                        // Still blocked (either permanent or temporary not yet expired)
//                        onComplete(false, false) // Not unblocked, user cannot proceed
//                    } else {
//                        // No stats node or some other edge case, assume user is okay if not explicitly blocked elsewhere
//                        onComplete(false, true)
//                    }
//                } else {
//                    // Transaction not committed, possibly due to contention.
//                    // Treat as if block status hasn't changed for safety.
//                    Log.w(TAG, "Expired block check transaction not committed for user $userId.")
//                    onComplete(false, false) // Assume still blocked for safety
//                }
//            }
//        })
//    }
//



    fun updateUserStatus(userId: String, status: String, onComplete: (Boolean) -> Unit) {
        if (userId.isBlank()) {
            onComplete(false)
            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userRef.child("statusByAdmin").setValue(status)
            .addOnSuccessListener {
                // If status is "blocked", call blockUserCompletely
                if (status.equals("blocked", ignoreCase = true)) {
                    blockUserCompletely(userId) { blockSuccess ->
                        onComplete(blockSuccess)
                    }
                } else {
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpdateUserStatus", "Failed to update user status", e)
                onComplete(false)
            }
    }

    fun blockUserCompletely(userId: String, onComplete: (Boolean) -> Unit) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        userRef.child("statusByAdmin").setValue("blocked")
            .addOnSuccessListener {
                blockUserItems(userId)
                blockUserBids(userId)
                disableConversationsForBlockedUser(userId) {
                    Log.d("TAG", "Conversations disabled status for perm block: $it")

                }
                FirebaseDatabase.getInstance().getReference("BlockedUsers").child(userId).setValue(true)
                onComplete(true)

            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

//    fun disableConversationsForBlockedUser(userId: String) {
//        val db = FirebaseDatabase.getInstance().reference
//
//        // 1. Regular Chats
//        db.child("Conversations").child("chats").child(userId).get().addOnSuccessListener { snapshot ->
//            snapshot.children.forEach { conversation ->
//                val otherUserId = conversation.key ?: return@forEach
//                db.child("Conversations").child("chats").child(otherUserId).child(userId).child("isBlocked")
//                    .setValue(true)
//                db.child("Conversations").child("chats").child(userId).child(otherUserId).child("isBlocked")
//                    .setValue(true)
//            }
//        }
//
//        // 2. Exchange Chats
//        db.child("Conversations").child("ExchangeChats").child(userId).get().addOnSuccessListener { snapshot ->
//            snapshot.children.forEach { otherUserNode ->
//                val otherUserId = otherUserNode.key ?: return@forEach
//                otherUserNode.children.forEach { bidNode ->
//                    val bidId = bidNode.key ?: return@forEach
//
//                    db.child("Conversations").child("ExchangeChats").child(otherUserId).child(userId).child(bidId)
//                        .child("isBlocked").setValue(true)
//                    db.child("Conversations").child("ExchangeChats").child(userId).child(otherUserId).child(bidId)
//                        .child("isBlocked").setValue(true)
//                }
//            }
//        }
//    }

    fun disableConversationsForBlockedUser(userId: String, onComplete: (Boolean) -> Unit) {
        val db = FirebaseDatabase.getInstance().reference
        var regularChatsSuccess = false
        var exchangeChatsSuccess = false
        var operationsPending = 2 // Two main operations (regular chats, exchange chats)

        val checkCompletion = {
            if (operationsPending == 0) {
                onComplete(regularChatsSuccess && exchangeChatsSuccess)
            }
        }

        // 1. Regular Chats
        db.child("Conversations").child("chats").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    regularChatsSuccess = true // No regular chats to disable
                    operationsPending--
                    checkCompletion()
                    return@addOnSuccessListener
                }
                val updates = mutableMapOf<String, Any>()
                snapshot.children.forEach { conversation ->
                    val otherUserId = conversation.key
                    if (otherUserId != null) {
                        updates["/Conversations/chats/$otherUserId/$userId/isBlocked"] = true
                        updates["/Conversations/chats/$userId/$otherUserId/isBlocked"] = true
                    }
                }
                if (updates.isNotEmpty()) {
                    db.updateChildren(updates)
                        .addOnSuccessListener {
                            regularChatsSuccess = true
                            Log.d("UsersRepo", "Regular chats disabled for user $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("UsersRepo", "Failed to disable regular chats for user $userId", e)
                            regularChatsSuccess = false
                        }
                        .addOnCompleteListener {
                            operationsPending--
                            checkCompletion()
                        }
                } else {
                    regularChatsSuccess = true // No specific updates to make
                    operationsPending--
                    checkCompletion()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UsersRepo", "Failed to get regular chats for user $userId", e)
                regularChatsSuccess = false
                operationsPending--
                checkCompletion()
            }

        // 2. Exchange Chats
        db.child("Conversations").child("ExchangeChats").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    exchangeChatsSuccess = true // No exchange chats to disable
                    operationsPending--
                    checkCompletion()
                    return@addOnSuccessListener
                }
                val updates = mutableMapOf<String, Any>()
                snapshot.children.forEach { otherUserNode ->
                    val otherUserId = otherUserNode.key
                    if (otherUserId != null) {
                        otherUserNode.children.forEach { bidNode ->
                            val bidId = bidNode.key
                            if (bidId != null) {
                                updates["/Conversations/ExchangeChats/$otherUserId/$userId/$bidId/isBlocked"] =
                                    true
                                updates["/Conversations/ExchangeChats/$userId/$otherUserId/$bidId/isBlocked"] =
                                    true
                            }
                        }
                    }
                }
                if (updates.isNotEmpty()) {
                    db.updateChildren(updates)
                        .addOnSuccessListener {
                            exchangeChatsSuccess = true
                            Log.d("UsersRepo", "Exchange chats disabled for user $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("UsersRepo", "Failed to disable exchange chats for user $userId", e)
                            exchangeChatsSuccess = false
                        }
                        .addOnCompleteListener {
                            operationsPending--
                            checkCompletion()
                        }
                } else {
                    exchangeChatsSuccess = true // No specific updates to make
                    operationsPending--
                    checkCompletion()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UsersRepo", "Failed to get exchange chats for user $userId", e)
                exchangeChatsSuccess = false
                operationsPending--
                checkCompletion()
            }
    }


//    fun blockUserItems(userId: String) {
//        val itemsRef = FirebaseDatabase.getInstance().getReference("Items")
//        itemsRef.orderByChild("userId").equalTo(userId).get()
//            .addOnSuccessListener { snapshot ->
//                for (itemSnapshot in snapshot.children) {
//                    itemSnapshot.ref.child("statusByAdmin").setValue("blockedByAdmin")
//                    itemSnapshot.ref.child("isAvailable").setValue(false)
//                }
//            }
//    }


    fun blockUserItems(userId: String) {
        val itemsRef = FirebaseDatabase.getInstance().getReference("Items")
        itemsRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (itemSnapshot in snapshot.children) {
                            itemSnapshot.ref.child("status").setValue("blockedByAdmin")
                            itemSnapshot.ref.child("available").setValue(false)
                        }
                        Log.d("BlockUserItems", "Blocked items for userId: $userId")
                    } else {
                        Log.w("BlockUserItems", "No items found for userId: $userId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BlockUserItems", "Failed to fetch items: ${error.message}")
                }
            })
    }

    fun blockUserBids(userId: String) {
        val bidsRef = FirebaseDatabase.getInstance().getReference("Bids")
        bidsRef.get().addOnSuccessListener { snapshot ->
            for (bidSnapshot in snapshot.children) {
                val bid = bidSnapshot.getValue(Bid::class.java)
                if (bid != null && (bid.bidderId == userId || bid.receiverId == userId)) {
                    bidSnapshot.ref.child("status").setValue("canceledByAdmin")
                    bidSnapshot.ref.child("canceledReason").setValue("User has been blocked")
                    bidSnapshot.ref.child("isCancelledDueToItem").setValue(true)
                }
            }
        }
    }





}
