package com.example.signuplogina.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OfferedItemsViewModel : ViewModel() {

    private val databaseRef = FirebaseDatabase.getInstance().reference

    private val _offeredItemsList = MutableLiveData<List<Item>>()
    val offeredItemsList: LiveData<List<Item>> = _offeredItemsList

    private val _bidDetails = MutableLiveData<Bid?>()
    val bidDetails: LiveData<Bid?> = _bidDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _bidUpdateStatus = MutableLiveData<Pair<Boolean, String>>() // Success, Message
    val bidUpdateStatus: LiveData<Pair<Boolean, String>> = _bidUpdateStatus

    private var requestedItemCache: Item? = null // Cache for the item being bid on


    fun loadBidAndItems(bidId: String, offeredItemIdsFromNav: List<String>, requestedItemIdFromNav: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Fetch Bid Details
                val bidSnapshot = databaseRef.child("Bids").child(bidId).get().await()
                val bid = bidSnapshot.getValue(Bid::class.java)
                bid?.bidId = bidId // Ensure bidId is set
                _bidDetails.postValue(bid)

                // Fetch Requested Item Details (for header info)
                if (requestedItemCache == null || requestedItemCache?.id != requestedItemIdFromNav) {
                    val requestedItemSnapshot = databaseRef.child("Items").child(requestedItemIdFromNav).get().await()
                    requestedItemCache = requestedItemSnapshot.getValue(Item::class.java)
                    requestedItemCache?.id = requestedItemIdFromNav
                }


                // Fetch Offered Items
                val itemsListResult = mutableListOf<Item>()
                if (offeredItemIdsFromNav.isNotEmpty()) {
                    for (itemId in offeredItemIdsFromNav) {
                        val itemSnapshot = databaseRef.child("Items").child(itemId).get().await()
                        itemSnapshot.getValue(Item::class.java)?.let {
                            it.id = itemSnapshot.key ?: ""
                            itemsListResult.add(it)
                        }
                    }
                }
                _offeredItemsList.postValue(itemsListResult)

            } catch (e: Exception) {
                _toastMessage.postValue("Error loading offer details: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun getRequestedItemDetails(): Item? = requestedItemCache


    fun acceptBid(bid: Bid) {
        if (bid.bidId.isEmpty()) {
            _toastMessage.value = "Invalid Bid ID."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any?>(
                    "Bids/${bid.bidId}/status" to "completed",
                    "Items/${bid.itemId}/available" to false,
//                    "Items/${bid.itemId}/status" to "exchanged"
                )
                bid.offeredItemIds.forEach { offeredId ->
                    updates["Items/$offeredId/available"] = false
                    updates["Items/$offeredId/status"] = "exchanged"
                }
                // TODO: Handle ownership transfer (update userId on items)
                // This might involve more complex logic, potentially cloud functions for atomicity

                databaseRef.updateChildren(updates).await()
                _bidUpdateStatus.postValue(Pair(true, "Offer accepted successfully!"))
            } catch (e: Exception) {
                _bidUpdateStatus.postValue(Pair(false, "Failed to accept offer: ${e.message}"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun rejectBid(bidId: String) {
        if (bidId.isEmpty()) {
            _toastMessage.value = "Invalid Bid ID."
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                databaseRef.child("Bids").child(bidId).child("status").setValue("rejectedByReceiver").await()
                _bidUpdateStatus.postValue(Pair(true, "Offer rejected."))
            } catch (e: Exception) {
                _bidUpdateStatus.postValue(Pair(false, "Failed to reject offer: ${e.message}"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun consumeToastMessage() {
        _toastMessage.value = null
    }
}