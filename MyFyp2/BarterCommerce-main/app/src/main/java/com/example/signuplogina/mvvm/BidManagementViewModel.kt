package com.example.signuplogina.mvvm

import android.util.Log
// import android.widget.Toast // ViewModels should not have Android framework dependencies like Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signuplogina.Bid
import com.example.signuplogina.Item // Assuming this might be needed for future bid actions
import com.example.signuplogina.fragments.SingleLiveEvent // Your SingleLiveEvent class
import com.example.signuplogina.Utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BidManagementViewModel : ViewModel() {

    private val bidRepo = BidRepository()
    // private val itemsRepo = ItemsRepo() // Keep if you plan to update item statuses from here
    private val database = FirebaseDatabase.getInstance().reference
    private val TAG = "BidManagementViewModel"

    private val _placedBids = MutableLiveData<List<Bid>>()
    val placedBids: LiveData<List<Bid>> = _placedBids

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // **** CORRECTED DECLARATION AND USAGE OF _feedbackMessage ****
    private val _feedbackMessage = SingleLiveEvent<String>() // _feedbackMessage IS the SingleLiveEvent
    val feedbackMessage: LiveData<String> = _feedbackMessage   // Expose as LiveData<String>

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine Exception: ${throwable.message}", throwable)
        _isLoading.postValue(false)
        _feedbackMessage.postValue("An unexpected error occurred.") // Post String directly
    }

    fun fetchBidsPlacedByUser(userId: String) {
        if (userId.isBlank()) {
            _placedBids.postValue(emptyList())
            return
        }
        _isLoading.postValue(true)
        val userBidsRef = database.child("Users").child(userId).child("bids").child("placed")
        userBidsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedBidIds = snapshot.children.mapNotNull { it.key }
                if (fetchedBidIds.isEmpty()) {
                    _placedBids.postValue(emptyList())
                    _isLoading.postValue(false)
                    return
                }
                val bidsDataList = mutableListOf<Bid>()
                var fetchCounter = fetchedBidIds.size

                fetchedBidIds.forEach { bidId ->
                    bidRepo.getBidById(bidId,
                        onSuccess = { bid ->
                            bidsDataList.add(bid)
                            fetchCounter--
                            if (fetchCounter == 0) {
                                _placedBids.postValue(bidsDataList.sortedByDescending { it.timestamp })
                                _isLoading.postValue(false)
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to fetch details for bid $bidId: $error")
                            fetchCounter--
                            if (fetchCounter == 0) {
                                _placedBids.postValue(bidsDataList.sortedByDescending { it.timestamp })
                                _isLoading.postValue(false)
                            }
                        }
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch placed bid IDs for $userId: ${error.message}")
                _feedbackMessage.postValue("Failed to load your bids.") // Post String directly
                _placedBids.postValue(emptyList())
                _isLoading.postValue(false)
            }
        })
    }

    fun withdrawPendingBid(bidToWithdraw: Bid) {
        val currentUserId = Utils.getUidLoggedIn()
        if (bidToWithdraw.bidderId != currentUserId) {
            _feedbackMessage.postValue("Error: You can only withdraw your own bids.") // Post String directly
            return
        }
        if (bidToWithdraw.status != "pending") {
            _feedbackMessage.postValue("Error: This bid is no longer pending and cannot be withdrawn.") // Post String directly
            return
        }

        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                val updates = mapOf("Bids/${bidToWithdraw.bidId}/status" to "withdrawByBidder")
                database.updateChildren(updates).await()

                Log.i(TAG, "Bid ${bidToWithdraw.bidId} withdrawn successfully by bidder.")
                withContext(Dispatchers.Main) {
                    _feedbackMessage.postValue("Your offer has been withdrawn.") // Post String directly
                    currentUserId?.let { fetchBidsPlacedByUser(it) } // Refresh list
                }
                // TODO: Notify the item receiver that the bid was withdrawn.
            } catch (e: Exception) {
                Log.e(TAG, "Error withdrawing bid ${bidToWithdraw.bidId}: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _feedbackMessage.postValue("Failed to withdraw offer: ${e.message}") // Post String directly
                }
            } finally {
                withContext(Dispatchers.Main) { _isLoading.postValue(false) }
            }
        }
    }

    // Optional: If you need to clear the event from the fragment after handling
    // fun clearFeedbackMessage() {
    //     // SingleLiveEvent's observe logic usually handles one-time delivery.
    //     // Explicit clearing is typically not needed unless specific re-triggering logic is desired.
    //     // If you were to clear, it would be:
    //     // _feedbackMessage.value = null // but be careful, as this is a new "event" of null
    // }
}