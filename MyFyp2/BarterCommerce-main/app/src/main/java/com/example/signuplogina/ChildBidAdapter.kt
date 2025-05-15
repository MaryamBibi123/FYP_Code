
package com.example.signuplogina

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class ChildBidAdapter(
    private val bidList: List<Pair<Bid, String>>, // Bid object and its ID
    // NEW: Callback when user wants to see all offered items for a specific bid
    private val onViewOfferDetailsClicked: (bid: Bid) -> Unit,
    private val onManageBidClicked: (bidId: String, firstOfferedItemName: String) -> Unit, // For "Manage Bid"
    private val isCurrentUserListingOwner: Boolean // True if the logged-in user owns the item *receiving* these bids
) : RecyclerView.Adapter<ChildBidAdapter.BidViewHolder>() {

    // Cache for fetched data to avoid repeated DB calls for the same bid
    private val fetchedBidDataCache = mutableMapOf<String, CachedBidUIData>()

    data class CachedBidUIData(
        val bidderName: String = "Bidder",
        val firstOfferedItem: Item? = null,
        val isOfferStillValid: Boolean = true // Based on first item's availability
    )

    inner class BidViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val firstOfferedItemImageView: ImageView = itemView.findViewById(R.id.ivFirstOfferedItemPreview)
        val bidSummaryTextView: TextView = itemView.findViewById(R.id.tvBidSummary)
        val viewAllOfferedItemsTextView: TextView = itemView.findViewById(R.id.tvViewAllOfferedItems)
        val manageBidButton: Button = itemView.findViewById(R.id.btnManageBid)
        val unavailableOverlay: TextView = itemView.findViewById(R.id.unavailableOverlayBid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.child_bids, parent, false) // Use your updated layout name
        return BidViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val (bid, bidId) = bidList[position]

        // Reset views for recycling
        holder.firstOfferedItemImageView.setImageResource(R.drawable.ic_camera) // Placeholder
        holder.bidSummaryTextView.text = "Loading offer..."
        holder.manageBidButton.visibility = View.GONE
        holder.unavailableOverlay.visibility = View.GONE
        holder.itemView.alpha = 1.0f

        // Use cached data if available
        if (fetchedBidDataCache.containsKey(bidId)) {
            val cachedData = fetchedBidDataCache[bidId]!!
            bindDataToViews(holder, bid, bidId, cachedData)
        } else {
            // Fetch data: Bidder Name first, then First Offered Item
            fetchBidderName(bid.bidderId) { bidderName ->
                if (bid.offeredItemIds.isNotEmpty()) {
                    val firstOfferedItemId = bid.offeredItemIds[0]
                    fetchFirstOfferedItem(firstOfferedItemId) { firstItem ->
                        val isOfferValid = firstItem != null && firstItem.available &&
                                firstItem.status.equals("approved", ignoreCase = true)

                        val cachedData = CachedBidUIData(bidderName, firstItem, isOfferValid)
                        fetchedBidDataCache[bidId] = cachedData // Store in cache

                        if (holder.adapterPosition == position) { // Ensure holder is still for this position
                            bindDataToViews(holder, bid, bidId, cachedData)
                        }
                    }
                } else {
                    // No items offered - should ideally not happen
                    val cachedData = CachedBidUIData(bidderName, null, false)
                    fetchedBidDataCache[bidId] = cachedData
                    if (holder.adapterPosition == position) {
                        bindDataToViews(holder, bid, bidId, cachedData)
                    }
                }
            }
        }

        holder.viewAllOfferedItemsTextView.setOnClickListener {
            // Regardless of cached availability, let user view details.
            // Availability checks will happen on the details screen.
            onViewOfferDetailsClicked(bid)
        }
    }

    private fun bindDataToViews(holder: BidViewHolder, bid: Bid, bidId: String, data: CachedBidUIData) {
        val context = holder.itemView.context
        var summaryText: String

        if (data.firstOfferedItem != null) {
            holder.firstOfferedItemImageView.visibility = View.VISIBLE
            if (data.firstOfferedItem.details.imageUrls.isNotEmpty()) {
                Glide.with(context)
                    .load(data.firstOfferedItem.details.imageUrls[0])
                    .placeholder(R.drawable.ic_camera)
                    .error(R.drawable.baseline_error_24)
                    .centerCrop()
                    .into(holder.firstOfferedItemImageView)
            } else {
                holder.firstOfferedItemImageView.setImageResource(R.drawable.ic_camera)
            }

            val firstItemName = data.firstOfferedItem.details.productName
            summaryText = if (bid.offeredItemIds.size > 1) {
                val othersCount = bid.offeredItemIds.size - 1
                context.getString(R.string.offers_item_and_more_format, firstItemName, othersCount)
            } else {
                context.getString(R.string.offers_single_item_format, firstItemName)
            }
            summaryText = "${context.getString(R.string.bid_by_format, data.bidderName)}\n$summaryText"

        } else {
            holder.firstOfferedItemImageView.visibility = View.INVISIBLE // Or GONE, or placeholder
            summaryText = "${context.getString(R.string.bid_by_format, data.bidderName)}\n${context.getString(R.string.offers_x_items_format, bid.offeredItemIds.size)}"
            if (bid.offeredItemIds.isEmpty()) {
                summaryText = "${context.getString(R.string.bid_by_format, data.bidderName)}\nNo items offered."
            }
        }
        holder.bidSummaryTextView.text = summaryText


        if (data.isOfferStillValid && (bid.status == "pending" || bid.status == "start")) {
            holder.unavailableOverlay.visibility = View.GONE
            holder.itemView.alpha = 1.0f
            holder.viewAllOfferedItemsTextView.isEnabled = true


            if (isCurrentUserListingOwner) {
                holder.manageBidButton.visibility = View.VISIBLE
                holder.manageBidButton.isEnabled = true
                holder.manageBidButton.alpha = 1.0f
                holder.manageBidButton.setOnClickListener {
                    // The manage bid click now needs context about the bid/item
                    // For the dialog, we might use the first item's name as a reference
                    val referenceItemName = data.firstOfferedItem?.details?.productName ?: "this offer"
                    onManageBidClicked(bidId, referenceItemName)
                }
            } else {
                holder.manageBidButton.visibility = View.GONE
            }
        } else {
            // Offer is not valid (item unavailable or bid status not active)
            holder.unavailableOverlay.visibility = View.VISIBLE
            holder.itemView.alpha = 0.6f
            holder.viewAllOfferedItemsTextView.isEnabled = true // Still allow viewing details

            if(bid.status != "pending" && bid.status != "start"){
                holder.unavailableOverlay.text = "Status: ${bid.status.capitalize()}"
            } else {
                holder.unavailableOverlay.text = context.getString(R.string.offer_unavailable)
            }

            holder.manageBidButton.visibility = View.GONE // Can't manage inactive/invalid bid
        }
    }


    private fun fetchBidderName(bidderId: String, callback: (String) -> Unit) {
        if (bidderId.isEmpty()) {
            callback("Unknown Bidder")
            return
        }
        FirebaseDatabase.getInstance().getReference("Users").child(bidderId).child("username")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.getValue(String::class.java) ?: "Bidder")
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChildBidAdapter", "Error fetching bidder name: ${error.message}")
                    callback("Bidder (Error)")
                }
            })
    }

    private fun fetchFirstOfferedItem(itemId: String, callback: (Item?) -> Unit) {
        if (itemId.isEmpty()){
            callback(null)
            return
        }
        FirebaseDatabase.getInstance().getReference("Items").child(itemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val item = snapshot.getValue(Item::class.java)
                    item?.id = snapshot.key ?: ""
                    callback(item)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChildBidAdapter", "Error fetching first offered item: ${error.message}")
                    callback(null)
                }
            })
    }

    override fun getItemCount(): Int = bidList.size
}






//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.app.AlertDialog
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.signuplogina.Bid
//import com.example.signuplogina.Item
//import com.example.signuplogina.R // Make sure R is imported
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//
//class ChildBidAdapter(
//    private val bidList: List<Pair<Bid, String>>,
//    private val onOfferedClicked: (item: Item) -> Unit,
//    private val onExchangeClicked: (String, String) -> Unit,
//    private val isCurrentUser: Boolean // Flag to know if viewing own listing's bids
//) : RecyclerView.Adapter<ChildBidAdapter.BidViewHolder>() {
//
//    inner class BidViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        // Views from child_bids.xml
//        val productImage: ImageView = itemView.findViewById(R.id.child_icon)
//        val productName: TextView = itemView.findViewById(R.id.child_title)
//        val exchangeButton: Button = itemView.findViewById(R.id.GoToExchangeRoomButton)
//        // Find the overlay TextView (using the ID from the updated layout)
//        val unavailableOverlay: TextView = itemView.findViewById(R.id.unavailableOverlayBid)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
//        // Inflate the child_bids layout
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.child_bids, parent, false)
//        return BidViewHolder(view)
//    }
//
//    @SuppressLint("SetTextI18n")
//    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
//        val (bid, bidId) = bidList[position]
//        val offeredItemId = bid.offeredItemId // The ID of the item BEING OFFERED
//
//        // Show/hide exchange button based on who is viewing
//        holder.exchangeButton.visibility = if (isCurrentUser) View.VISIBLE else View.GONE
//
//        // Fetch offered item details from Firebase
//        val itemRef = FirebaseDatabase.getInstance().getReference("Items").child(offeredItemId)
//        itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                // Make sure holder is still valid (due to async nature)
//                if (holder.adapterPosition == RecyclerView.NO_POSITION) return
//
//                val item = snapshot.getValue(Item::class.java) // Fetch the full offered item object
//
//                if (item != null) {
//                    item.id = snapshot.key ?: "" // Assign item ID
//
//                    // --- Check OFFERED Item Availability ---
//                    val isOfferedItemAvailable = item.isAvailable && item.status.equals("approved", ignoreCase = true)
//                    Log.d("ChildBidAdapter", "Offered Item: ${item.id}, Status: ${item.status}, IsAvailable: ${item.isAvailable}, DeterminedAvailable: $isOfferedItemAvailable")
//                    // --- End Check ---
//
//                    // Set name always
//                    holder.productName.text = item.details.productName
//
//                    // Load image always
//                    if (item.details.imageUrls.isNotEmpty()) {
//                        Glide.with(holder.itemView.context)
//                            .load(item.details.imageUrls[0])
//                            .placeholder(R.drawable.ic_camera) // Use your placeholder
//                            .error(R.drawable.baseline_error_24) // Use your error drawable
//                            .centerCrop()
//                            .into(holder.productImage)
//                    } else {
//                        holder.productImage.setImageResource(R.drawable.ic_camera) // Default image
//                    }
//
//                    if (isOfferedItemAvailable) {
//                        // Offered item IS available
//                        holder.itemView.alpha = 1.0f
//                        holder.itemView.isClickable = true
//                        holder.itemView.isFocusable = true
//                        holder.itemView.setOnClickListener { onOfferedClicked(item) } // Enable click
//
//                        if (isCurrentUser) { // Only enable exchange button if it's the current user viewing their listing
//                            holder.exchangeButton.isEnabled = true
//                            holder.exchangeButton.alpha = 1.0f
//                            holder.exchangeButton.setOnClickListener {
//                                // Show Exchange Dialog Logic
//                                val context = holder.itemView.context
//                                val builder = AlertDialog.Builder(context)
//                                builder.setTitle("Create Exchange Room")
//                                val input = EditText(context)
//                                input.hint = "Exchange for ${item.details.productName}"
//                                input.setText("Exchange for ${item.details.productName}") // Pre-fill
//                                builder.setView(input)
//                                builder.setPositiveButton("Start Chat") { dialog, _ ->
//                                    val roomName = input.text.toString().trim()
//                                    if (roomName.isNotEmpty()) {
//                                        onExchangeClicked(bidId, roomName)
//                                    }
//                                    dialog.dismiss()
//                                }
//                                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
//                                builder.show()
//                            }
//                        }
//                        holder.unavailableOverlay.visibility = View.GONE // Hide overlay
//
//                    } else {
//                        // Offered item is NOT available
//                        holder.itemView.alpha = 0.5f // Dim the view
//                        holder.itemView.isClickable = false
//                        holder.itemView.isFocusable = false
//                        holder.itemView.setOnClickListener(null) // Disable click
//
//                        if (isCurrentUser) {
//                            holder.exchangeButton.isEnabled = false
//                            holder.exchangeButton.alpha = 0.5f
//                            holder.exchangeButton.setOnClickListener(null) // Disable exchange
//                        }
//                        // Optionally prepend text to indicate status
//                        holder.productName.text = holder.itemView.context.getString(R.string.offered_item_unavailable_text, item.details.productName)
//                        holder.unavailableOverlay.visibility = View.VISIBLE // Show overlay
//                    }
//                } else {
//                    // Offered Item fetch failed or deleted
//                    Log.w("ChildBidAdapter", "Offered item $offeredItemId not found for bid $bidId")
//                    holder.productName.text = holder.itemView.context.getString(R.string.item_not_found)
//                    holder.productImage.setImageResource(R.drawable.baseline_error_24) // Error indicator
//                    holder.itemView.alpha = 0.5f
//                    holder.itemView.isClickable = false
//                    holder.itemView.setOnClickListener(null)
//                    if (isCurrentUser) {
//                        holder.exchangeButton.isEnabled = false
//                        holder.exchangeButton.alpha = 0.5f
//                        holder.exchangeButton.setOnClickListener(null)
//                    }
//                    holder.unavailableOverlay.visibility = View.VISIBLE // Show unavailable
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("ChildBidAdapter", "Failed to fetch offered item ($offeredItemId) for bid $bidId: ${error.message}")
//                if (holder.adapterPosition != RecyclerView.NO_POSITION) { // Check holder validity
//                    holder.productName.text = holder.itemView.context.getString(R.string.item_fetch_error)
//                    holder.productImage.setImageResource(R.drawable.baseline_error_24)
//                    holder.itemView.alpha = 0.5f
//                    holder.itemView.isClickable = false
//                    holder.itemView.setOnClickListener(null)
//                    if (isCurrentUser) {
//                        holder.exchangeButton.isEnabled = false
//                        holder.exchangeButton.alpha = 0.5f
//                        holder.exchangeButton.setOnClickListener(null)
//                    }
//                    holder.unavailableOverlay.visibility = View.VISIBLE // Show unavailable
//                }
//            }
//        })
//    }
//
//    override fun getItemCount(): Int = bidList.size
//}
//
//
//
//
