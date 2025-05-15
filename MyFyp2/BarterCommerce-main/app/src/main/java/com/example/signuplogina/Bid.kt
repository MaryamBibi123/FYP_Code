package com.example.signuplogina // Or your actual package

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bid(
    var bidId: String = "",
    val bidderId: String = "",
    val receiverId: String = "",
    val itemId: String = "", // Item being requested (the uploader's item)

    // --- MAJOR CHANGE HERE ---
    val offeredItemIds: List<String> = listOf(), // IDs of items being offered by the bidder (NEW)
    // val offeredItemId: String = "", // OLD - This should be REMOVED or COMMENTED OUT

    val timestamp: Long = 0L,
    var status: String = "pending", // "start","pending", "completed", "rejectedByReceiver","withdrawByBidder" "canceledByAdmin",
    val canceledReason: String? = null,
    val isCancelledDueToItem: Boolean = false,
    var previousStatusBeforeBlock:String?=null,// newly added

    // --- For UI purposes, potentially populated after fetching ---
    @IgnoredOnParcel
    var requestedItemDetails: Item? = null, // Details of the item being bid ON

    @IgnoredOnParcel
    var offeredItemsDetails: List<Item> = listOf(), // Details of ALL items being offered

    @IgnoredOnParcel
    var bidderName: String = "" // Optional: used only in UI
) : Parcelable



//
//
//package com.example.signuplogina
//
//import android.os.Parcelable
//import kotlinx.parcelize.IgnoredOnParcel
//import kotlinx.parcelize.Parcelize
//
//@Parcelize
//data class Bid(
//    var bidId: String = "",
//    val bidderId: String = "",
//    val receiverId: String = "",
//    val itemId: String = "", // Item being requested
//    val offeredItemId: String = "", // Item being offered
//    val timestamp: Long = 0L,
//    val status: String = "pending", // "start","pending", "completed", "rejectedByReceiver","withdrawByBidder" "canceledByAdmin",
//    val canceledReason: String? = null,
//    val isCancelledDueToItem: Boolean = false,
//
//    @IgnoredOnParcel
//    var offeredItem: Item? = null, // Optional: used only in UI
//
//    @IgnoredOnParcel
//    var bidderName: String = "" // Optional: used only in UI
//) : Parcelable
//
//
//
//
//
//
