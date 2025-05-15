package com.example.signuplogina.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.example.signuplogina.Utils
import com.example.signuplogina.modal.PollModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class PollViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _polls = MutableLiveData<List<PollModel>>()
    val polls: LiveData<List<PollModel>> = _polls
    val currentUserId=Utils.getUidLoggedIn()

    private val _tradeBattles = MutableLiveData<List<TradeBattleModel>>()
    val tradeBattles: LiveData<List<TradeBattleModel>> = _tradeBattles



    private val userRepo = UsersRepo()
    private val bidRepo = BidRepository()

    private val _userItems = MutableLiveData<List<Item>>()
    val userItems: LiveData<List<Item>> = _userItems

    private val _bids = MutableLiveData<List<Bid>>()
    val bids: LiveData<List<Bid>> = _bids
    init {
        loadPolls()
        loadTradeBattles()
    }



    // In your ViewModel
    fun handleRatingChange(listerId:String,battleId: String, bid: BidModel, offeredBidId: String, newRating: Int) {
        Log.d("NEW RATING", "Handling rating change for bid $offeredBidId: $newRating")
        val userId = Utils.getUidLoggedIn()

        // 👉 Add this condition early
        if (userId == bid.userId || userId == listerId) {
            Log.d("Voting", "User is not allowed to vote on their own bid or battle.")
            return
        }
        val userVoteRef = database.reference.child("Polls/TradeBattle")
            .child(battleId)
            .child("votes")
            .child(userId)
            .child(offeredBidId)

        userVoteRef.get().addOnSuccessListener { snapshot ->
            val previousVote = snapshot.getValue(Vote::class.java)
            val previousRating = previousVote?.rating ?: 0 // 0 if no previous vote

            if (previousRating == newRating) {
                Log.d("Rating", "User clicked the same button, no change.")
                return@addOnSuccessListener // Nothing to do
            }

            // Store the new vote first
            storeNewVoteRecord(battleId, offeredBidId, userId, newRating) { success ->
                if (success) {
                    Log.d("Rating", "Vote record stored. Now updating bid points.")
                    // Now update the aggregate bid points based on the change
                    updateBidAggregatePoints(battleId, offeredBidId, previousRating, newRating)
                } else {
                    Log.e("Rating", "Failed to store the new vote record.")
                    // Handle failure - maybe show an error to the user
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Rating", "Error retrieving user vote: ${exception.message}")
            // Handle failure
        }
    }

    // Stores only the individual user's vote record
    private fun storeNewVoteRecord(battleId: String, offeredBidId: String, userId: String, newRating: Int, callback: (Boolean) -> Unit) {
        val userVote = Vote(
            bidId = offeredBidId,
            userId = userId,
            rating = newRating,
            timestamp = System.currentTimeMillis()
        )
        database.reference.child("Polls/TradeBattle")
            .child(battleId)
            .child("votes")
            .child(userId)
            .child(offeredBidId)
            .setValue(userVote)
            .addOnSuccessListener {
                Log.d("Vote", "New vote record stored successfully.")
                callback(true)
            }
            .addOnFailureListener {
                Log.e("Vote", "Failed to store new vote record.")
                callback(false)
            }
    }

    // Updates the bid's aggregate ratings and totalPoints in Firebase
// This should ideally use a Transaction for robustness
    private fun updateBidAggregatePoints(battleId: String, offeredBidId: String, previousRating: Int, newRating: Int) {
        val bidRef = database.reference.child("Polls/TradeBattle")
            .child(battleId)
            .child("bids")
            .child(offeredBidId)

        // *** Using a Transaction is Highly Recommended Here ***
        bidRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bid = currentData.getValue(BidModel::class.java)
                    ?: // If bid doesn't exist for some reason, abort.
                    // Or you could potentially initialize it here if that makes sense.
                    return Transaction.abort()


                // Adjust ratings based on the change
                // Decrement previous rating count (if there was one)
                when (previousRating) {
                    1 -> bid.ratings.fair = (bid.ratings.fair - 1).coerceAtLeast(0)
                    2 -> bid.ratings.good = (bid.ratings.good - 1).coerceAtLeast(0)
                    3 -> bid.ratings.best = (bid.ratings.best - 1).coerceAtLeast(0)
                }

                // Increment new rating count
                when (newRating) {
                    1 -> bid.ratings.fair += 1
                    2 -> bid.ratings.good += 1
                    3 -> bid.ratings.best += 1
                }

                // Recalculate total points
                bid.totalPoints = (bid.ratings.fair * 1) + (bid.ratings.good * 2) + (bid.ratings.best * 3)

                Log.d("Transaction", "Updating bid: Fair=${bid.ratings.fair}, Good=${bid.ratings.good}, Best=${bid.ratings.best}, Total=${bid.totalPoints}")

                // Set the updated bid data back
                currentData.value = bid
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("Vote", "Firebase transaction failed: ${error.message}")
                } else if (committed) {
                    Log.d("Vote", "Bid points updated successfully via transaction.")
                    // Here you might trigger a refresh or rely on listeners
                } else {
                    Log.w("Vote", "Firebase transaction aborted (maybe bid was null or concurrent modification).")
                }
            }
        })
    }


    fun loadTradeBattles() {
        database.getReference("Polls/TradeBattle")
            .orderByChild("status").equalTo("active") // Only active battles
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<TradeBattleModel>()
                    for (battleSnapshot in snapshot.children) {
                        battleSnapshot.getValue(TradeBattleModel::class.java)?.let {
                            list.add(it)
                        }
                    }
                    _tradeBattles.postValue(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    // handle error
                }
            })
    }


    private val _offeredItems = MutableLiveData<List<Item>>()
    val offeredItems: LiveData<List<Item>> = _offeredItems

    fun loadItemsWithMultipleBids(userId: String) {
        Log.d("loading+","loadItemsWithMultipleBids")

        userRepo.getUserItemsWithTwoOrMoreBids(userId) { items ->
            _userItems.postValue(items)
            Log.d("loading+","Items: $userItems.")

        }
    }
// fetch the bids for an item and then fetch the ITem deatils of the offered items all and saved

    //comenting for later
//    fun loadBidsWithOfferedItems(itemId: String) {
//        bidRepo.fetchBidsForItem(itemId) { bidsList ->
//            _bids.postValue(bidsList)
//
//            if (bidsList.isNotEmpty()) {
//                val offeredItemsList = mutableListOf<Item>()
//                var count = 0
//
//                bidsList.forEach { bid ->
//
//                    bidRepo.fetchBidsWithFullDetails(bid) { offeredItem ->
//                        offeredItemsList.add(offeredItem ?: Item())
//                        count++
//
//                        if (count == bidsList.size) {
//                            _offeredItems.postValue(offeredItemsList)
//
//                        }
//                    }
//                }
//            } else {
//                _offeredItems.postValue(emptyList())
//            }
//        }
//    }

    fun addPoll(poll: PollModel) {
        val userId = auth.currentUser?.uid ?: return
        val pollRef = database.getReference("Polls/General/$userId").push()
        val pollWithId = poll.copy(id = pollRef.key ?: "")
        pollRef.setValue(pollWithId)
    }



    fun voteInPoll(
        poll: PollModel,
        selectedOption: Int,
        category: String
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val pollId = poll.id
        val uploaderId = poll.userId

        val dbRef = FirebaseDatabase.getInstance().reference
        val pollPath = "Polls/$category/Uploader/$uploaderId/$pollId"
        val voterPath = "Polls/$category/Voter/$pollId/$userId"

        dbRef.child(voterPath).get().addOnSuccessListener { snapshot ->
            val previousVote = snapshot.getValue(Int::class.java)

            if (previousVote == selectedOption) return@addOnSuccessListener

            dbRef.child(voterPath).setValue(selectedOption)

            val updates = hashMapOf<String, Any?>()
            if (previousVote != null) {
                updates["votesOption$previousVote"] = ServerValue.increment(-1)
            }
            updates["votesOption$selectedOption"] = ServerValue.increment(1)

            if (previousVote == null) {
                updates["totalVotes"] = ServerValue.increment(1)
            }

            dbRef.child(pollPath).updateChildren(updates)
        }
    }

    private fun loadPolls() {
        val generalPollRef = database.getReference("Polls/General/Uploader")

        generalPollRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pollList = mutableListOf<PollModel>()
                for (userSnapshot in snapshot.children) {
                    for (pollSnapshot in userSnapshot.children) {
                        val poll = pollSnapshot.getValue(PollModel::class.java)
                        if (poll != null) {
                            pollList.add(poll)
                        }
                    }
                }
//                _polls.value = pollList.sortedByDescending { it.timestamp }
                val sortedPolls = pollList.sortedWith(
                    compareByDescending<PollModel> { it.totalVotes }
                        .thenByDescending { it.timestamp }
                )

                // Expose the sorted list to the UI
                _polls.value = sortedPolls
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PollViewModel", "Failed to load polls: ${error.message}")
            }
        })
    }
}
