
package com.example.signuplogina

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class StashedItemState(
    val status: String? = null,
    val available: Boolean? = null,
    val exchangeState: String? = null,
    val lockedByBidId: String? = null // Can be null
) : Parcelable {
    // No-argument constructor for Firebase deserialization
    constructor() : this(null, null, null, null)
}

@Parcelize
data class Item(
    var id: String = "", // Firebase-generated item ID
    val userId: String = "", // Owner's user ID
    val details: ItemDetails = ItemDetails(), // Embedded item details

    // Bid Tracking
    val bidsPlaced: Map<String, Boolean> = mapOf(),   // Bid IDs where this item was offered
    val bidsReceived: Map<String, Boolean> = mapOf(), // Bid IDs received for this item

    // Admin Moderation
    var status: String = "pending", //, "approved", "rejected", "removed"
    var rejectionReason: String? = "",
    var rejectionTimestamp: Long = 0L, // Set when rejected

    // Flags
    //check is available
    var available: Boolean = false, // true if approaved false if rejected

    var exchangeState: String = "none", // Values: "none", "in_negotiation", "exchanged"
    // "none" means it's not currently locked in an active exchange process

    // --- NEW: Tracks which Bid has this item in an active negotiation ---
    var lockedByBidId: String? = null,
    var stashedStateBeforeTempBlock: StashedItemState? = null,



    ) : Parcelable

@Parcelize
data class ItemDetails(
    val productName: String = "",
    val description: String = "",
    val category: String = "",
    val condition: String = "",
    val price: Double = 0.0,
    val availability: Int = 0,
    val imageUrls: List<String> = listOf(),
    var timestamp: Long = 0L,
    var isReported: Boolean = false,
    var reportCount: Int = 0
) : Parcelable








//package com.example.signuplogina
//
//import android.os.Parcelable
//import kotlinx.android.parcel.Parcelize
//
//@Parcelize
//data class Item(
//    var id: String = "", // Firebase-generated ID
//    val userId: String = "", // User ID
//    val details: ItemDetails = ItemDetails(),// Default empty ItemDetails object
//
//
//) : Parcelable
//
//@Parcelize // Add this line to make ItemDetails parceble
//data class ItemDetails(
//    val productName: String = "",
//    val description: String = "",
//    val category: String = "",
//    val condition: String = "",
//    val price: Double = 0.0,
//    val availability: Int = 0,
//    val imageUrls: List<String> = listOf(), // Using listOf() for immutability
//    var timestamp: Long = 0L,
//    var status: String = "approved",
//    var isReported: Boolean = false, // <-- New field: Has any user reported this item?
//    var reportCount: Int = 0         // <-- New field: How many users reported this item?
//) : Parcelable
