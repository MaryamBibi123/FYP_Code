package com.example.signuplogina

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class BidsPlacedAdapter(
    private var bids: List<Bid>, // Changed to var to allow internal update via a method
    private val currentLoggedInUserId: String?, // Pass the logged-in user's ID
    private val onReplaceClicked: ((bid: Bid) -> Unit)? = null, // Pass full Bid
    private val onOfferedItemsClicked: ((bid: Bid) -> Unit)? = null,
    private val onWithdrawClicked: ((bid: Bid) -> Unit)? = null // **** NEW CALLBACK ****
) : RecyclerView.Adapter<BidsPlacedAdapter.BidViewHolder>() {

    private val TAG = "BidsPlacedAdapter"
    private val itemCache = mutableMapOf<String, Item?>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateBids(newBids: List<Bid>) {
        this.bids = newBids
        itemCache.clear() // Clear cache when list updates significantly
        notifyDataSetChanged() // Or use DiffUtil
    }

    inner class BidViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val requestedItemBox: LinearLayout = itemView.findViewById(R.id.requested_item_box)
        private val requestedImage: ImageView = itemView.findViewById(R.id.requested_item_image)
        private val requestedName: TextView = itemView.findViewById(R.id.requested_item_name)
        val offeredItemsClickableArea: LinearLayout = itemView.findViewById(R.id.offered_item_box_clickable)
        private val offeredImage: ImageView = itemView.findViewById(R.id.offered_item_image)
        private val offeredName: TextView = itemView.findViewById(R.id.offered_item_name)
        private val moreOfferedItemsText: TextView = itemView.findViewById(R.id.tv_more_offered_items)
        private val bidStatusText: TextView = itemView.findViewById(R.id.tv_bid_status)
        val replaceButton: Button = itemView.findViewById(R.id.btn_replace)
        val withdrawButton: Button = itemView.findViewById(R.id.btn_withdraw_offer) // **** NEW VIEW ****

        fun bindPlaceholder() {
            requestedName.text = "Loading..."
            offeredName.text = "Loading..."
            moreOfferedItemsText.visibility = View.GONE
            requestedImage.setImageResource(R.drawable.ic_camera)
            offeredImage.setImageResource(R.drawable.ic_camera)
            bidStatusText.text = "Status: ..."
            bidStatusText.background = ColorDrawable(ContextCompat.getColor(itemView.context, R.color.grey))
            replaceButton.visibility = View.GONE
            withdrawButton.visibility = View.GONE // Hide initially
            offeredItemsClickableArea.setOnClickListener(null)
            requestedItemBox.setOnClickListener(null)
        }

        fun bind(
            bid: Bid,
            requestedItem: Item?,
            firstOfferedItem: Item?,
            context: Context
        ) {
            // --- Bind Requested Item --- (as before)
            requestedItem?.details?.let { details ->
                requestedName.text = details.productName
                if (details.imageUrls.isNotEmpty()) {
                    Picasso.get().load(details.imageUrls[0]).placeholder(R.drawable.ic_camera).error(R.drawable.baseline_error_24).fit().centerCrop().into(requestedImage)
                } else { requestedImage.setImageResource(R.drawable.ic_camera) }
            } ?: run { requestedName.text = "Item N/A"; requestedImage.setImageResource(R.drawable.baseline_error_24) }

            // --- Bind First Offered Item --- (as before)
            firstOfferedItem?.details?.let { details ->
                offeredName.text = details.productName
                if (details.imageUrls.isNotEmpty()) {
                    Picasso.get().load(details.imageUrls[0]).placeholder(R.drawable.ic_camera).error(R.drawable.baseline_error_24).fit().centerCrop().into(offeredImage)
                } else { offeredImage.setImageResource(R.drawable.ic_camera) }
            } ?: run { offeredName.text = "Item N/A"; offeredImage.setImageResource(R.drawable.baseline_error_24) }


            val remainingOfferedCount = bid.offeredItemIds.size - 1
            if (remainingOfferedCount > 0) {
                moreOfferedItemsText.text = "+ $remainingOfferedCount more item${if (remainingOfferedCount > 1) "s" else ""}"
                moreOfferedItemsText.visibility = View.VISIBLE
            } else {
                moreOfferedItemsText.visibility = View.GONE
            }

            // --- Bind Status --- (as before)
            bidStatusText.text = "Status: ${bid.status.capitalize()}"
            val statusColorRes = when (bid.status.lowercase()) {
                "pending", "start", "on_hold_item_negotiation" -> R.color.light_brown // Added on_hold
                "completed" -> R.color.dark_green
                "rejectedbyreceiver", "withdrawbybidder", "canceledbyadmin", "canceledbysystem_item_unavailable", "rejected_negotiation_failed" -> R.color.red // Added more
                else -> R.color.grey
            }
            bidStatusText.background = ContextCompat.getDrawable(context, statusColorRes)


            // --- Button Visibility & Click Logic ---
            val isBidderViewing = (bid.bidderId == currentLoggedInUserId)

            // Replace Button
            val canReplace = isBidderViewing && (bid.status == "pending" || bid.status == "start")
            replaceButton.visibility = if (canReplace) View.VISIBLE else View.GONE
            if (canReplace) {
                replaceButton.setOnClickListener { onReplaceClicked?.invoke(bid) } // Pass full Bid
            } else {
                replaceButton.setOnClickListener(null)
            }

            // Withdraw Button
            val canWithdraw = isBidderViewing && bid.status == "pending"
            withdrawButton.visibility = if (canWithdraw) View.VISIBLE else View.GONE
            if (canWithdraw) {
                withdrawButton.setOnClickListener { onWithdrawClicked?.invoke(bid) }
            } else {
                withdrawButton.setOnClickListener(null)
            }

            // Offered Items Area Click
            if (bid.offeredItemIds.isNotEmpty()) {
                offeredItemsClickableArea.isClickable = true
                offeredItemsClickableArea.setOnClickListener { onOfferedItemsClicked?.invoke(bid) }
            } else {
                offeredItemsClickableArea.isClickable = false
                offeredItemsClickableArea.setOnClickListener(null)
            }

            // Requested Item Area Click (Optional)
            requestedItemBox.isClickable = requestedItem != null
            requestedItemBox.setOnClickListener {
                requestedItem?.let {
                    // Potentially navigate to item details of requested item
                    // Requires another callback or specific handling
                    Log.d(TAG, "Requested item box clicked: ${it.id}")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bid, parent, false)
        return BidViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val bid = bids[position]
        holder.bindPlaceholder()

        fetchItemDetails(bid.itemId) { requestedItem ->
            val firstOfferedId = bid.offeredItemIds.firstOrNull() ?: ""
            fetchItemDetails(firstOfferedId) { firstOfferedItem ->
                if (holder.adapterPosition == position) {
                    holder.bind(bid, requestedItem, firstOfferedItem, holder.itemView.context)
                }
            }
        }
    }

    override fun getItemCount(): Int = bids.size

    private fun fetchItemDetails(itemId: String, callback: (Item?) -> Unit) {
        if (itemId.isBlank()) { callback(null); return }
        if (itemCache.containsKey(itemId)) { callback(itemCache[itemId]); return }

        FirebaseDatabase.getInstance().getReference("Items").child(itemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val item = try { snapshot.getValue(Item::class.java)?.apply { id = snapshot.key ?: itemId } }
                    catch (e: Exception) { Log.e(TAG, "Error parsing item $itemId", e); null }
                    itemCache[itemId] = item
                    callback(item)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to fetch item $itemId: ${error.message}")
                    itemCache[itemId] = null
                    callback(null)
                }
            })
    }
}







//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.LinearLayout // Import LinearLayout
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import com.squareup.picasso.Picasso
//
//class BidsPlacedAdapter(
//    private val bids: List<Bid>,
//    private val onReplaceClicked: ((bidId: String) -> Unit)? = null,
//    // Corrected: Make it nullable and provide a default empty lambda if not passed
//    private val onOfferedItemsClicked: ((bid: Bid) -> Unit)? = null
//    // You can also write it as:
//    // private val onOfferedItemsClicked: (Bid) -> Unit = {} // Non-nullable with empty default
//    // However, making it nullable ( ((bid: Bid) -> Unit)? = null ) gives more flexibility
//    // to the calling Fragment if it doesn't always need to implement this click.
//) : RecyclerView.Adapter<BidsPlacedAdapter.BidViewHolder>() {
//
//    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//    private val TAG = "BidsPlacedAdapter"
//    private val itemCache = mutableMapOf<String, Item?>()
//
//    inner class BidViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val requestedItemBox: LinearLayout = itemView.findViewById(R.id.requested_item_box)
//        private val requestedImage: ImageView = itemView.findViewById(R.id.requested_item_image)
//        private val requestedName: TextView = itemView.findViewById(R.id.requested_item_name)
//
//        // Reference to the clickable area for offered items
//        val offeredItemsClickableArea: LinearLayout = itemView.findViewById(R.id.offered_item_box_clickable) // Use this ID from your XML
//        private val offeredImage: ImageView = itemView.findViewById(R.id.offered_item_image)
//        private val offeredName: TextView = itemView.findViewById(R.id.offered_item_name)
//        private val moreOfferedItemsText: TextView = itemView.findViewById(R.id.tv_more_offered_items)
//        private val bidStatusText: TextView = itemView.findViewById(R.id.tv_bid_status)
//        val replaceButton: Button = itemView.findViewById(R.id.btn_replace)
//
//        fun bindPlaceholder() {
//            requestedName.text = "Loading..."
//            offeredName.text = "Loading..."
//            moreOfferedItemsText.visibility = View.GONE
//            requestedImage.setImageResource(R.drawable.ic_camera) // Ensure ic_camera exists
//            offeredImage.setImageResource(R.drawable.ic_camera)
//            bidStatusText.text = "Status: ..."
//            bidStatusText.background = ColorDrawable(ContextCompat.getColor(itemView.context, R.color.grey)) // Use ContextCompat
//            replaceButton.visibility = View.GONE
//            offeredItemsClickableArea.setOnClickListener(null) // Clear previous listener
//            requestedItemBox.setOnClickListener(null) // Clear previous listener
//        }
//
//        fun bind(
//            bid: Bid,
//            requestedItem: Item?,
//            firstOfferedItem: Item?,
//            context: Context
//        ) {
//            // --- Bind Requested Item ---
//            requestedItem?.details?.let { details ->
//                requestedName.text = details.productName
//                if (details.imageUrls.isNotEmpty()) {
//                    Picasso.get().load(details.imageUrls[0])
//                        .placeholder(R.drawable.ic_camera)
//                        .error(R.drawable.baseline_error_24) // Ensure baseline_error_24 exists
//                        .fit().centerCrop()
//                        .into(requestedImage)
//                } else {
//                    requestedImage.setImageResource(R.drawable.ic_camera)
//                }
//            } ?: run {
//                requestedName.text = "Item N/A"
//                requestedImage.setImageResource(R.drawable.baseline_error_24)
//            }
//
//            // --- Bind First Offered Item ---
//            firstOfferedItem?.details?.let { details ->
//                offeredName.text = details.productName
//                if (details.imageUrls.isNotEmpty()) {
//                    Picasso.get().load(details.imageUrls[0])
//                        .placeholder(R.drawable.ic_camera)
//                        .error(R.drawable.baseline_error_24)
//                        .fit().centerCrop()
//                        .into(offeredImage)
//                } else {
//                    offeredImage.setImageResource(R.drawable.ic_camera)
//                }
//            } ?: run {
//                offeredName.text = "Item N/A"
//                offeredImage.setImageResource(R.drawable.baseline_error_24)
//            }
//
//            // --- Show "+ X more" text if applicable ---
//            val remainingOfferedCount = bid.offeredItemIds.size - 1
//            if (remainingOfferedCount > 0) {
//                moreOfferedItemsText.text = "+ $remainingOfferedCount more item${if (remainingOfferedCount > 1) "s" else ""}"
//                moreOfferedItemsText.visibility = View.VISIBLE
//            } else {
//                moreOfferedItemsText.visibility = View.GONE
//            }
//
//            // --- Bind Status ---
//            bidStatusText.text = "Status: ${bid.status.capitalize()}"
//            val statusColorRes = when (bid.status.lowercase()) { // Use lowercase for robust comparison
//                "pending", "start" -> R.color.light_brown
//                "completed" -> R.color.dark_green
//                "rejectedbyreceiver", "withdrawbybidder", "canceledbyadmin" -> R.color.red
//                else -> R.color.grey
//            }
//            bidStatusText.background = ContextCompat.getDrawable(context, statusColorRes)
//
//            // --- Replace Button Visibility & Click ---
//            val canReplace = (bid.bidderId == currentUserId && (bid.status == "pending" || bid.status == "start"))
//            replaceButton.visibility = if (canReplace) View.VISIBLE else View.GONE
//            if (canReplace) {
//                replaceButton.setOnClickListener {
//                    onReplaceClicked?.invoke(bid.bidId)
//                }
//            } else {
//                replaceButton.setOnClickListener(null)
//            }
//
//            // **** SET ONCLICKLISTENER FOR OFFERED ITEMS AREA ****
//            if (bid.offeredItemIds.isNotEmpty()) {
//                offeredItemsClickableArea.isClickable = true
//                offeredItemsClickableArea.isFocusable = true
//                offeredItemsClickableArea.setOnClickListener {
//                    Log.d(TAG, "Offered items area clicked for bid: ${bid.bidId}")
//                    onOfferedItemsClicked?.invoke(bid) // Call the lambda passed from the Fragment
//                }
//            } else {
//                // If no offered items, make it non-clickable
//                offeredItemsClickableArea.isClickable = false
//                offeredItemsClickableArea.isFocusable = false
//                offeredItemsClickableArea.setOnClickListener(null)
//            }
//
//            // (Optional) Click listener for requested item box if needed
//            requestedItemBox.isClickable = requestedItem != null
//            requestedItemBox.isFocusable = requestedItem != null
//            requestedItemBox.setOnClickListener {
//                requestedItem?.let {
//                    // Example: If you had another callback
//                    // onRequestedItemClicked?.invoke(it)
//                    Log.d(TAG, "Requested item box clicked: ${it.id}")
//                    // For now, this click might not do anything unless you define its action
//                }
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bid, parent, false)
//        return BidViewHolder(view)
//    }
//
//    @SuppressLint("SetTextI18n") // If you use string formatting in bind
//    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
//        val bid = bids[position]
//        holder.bindPlaceholder()
//
//        fetchItemDetails(bid.itemId) { requestedItem ->
//            val firstOfferedId = bid.offeredItemIds.firstOrNull() ?: ""
//            fetchItemDetails(firstOfferedId) { firstOfferedItem ->
//                if (holder.adapterPosition == position) { // Check if holder is still valid for this position
//                    holder.bind(bid, requestedItem, firstOfferedItem, holder.itemView.context)
//                }
//            }
//        }
//    }
//
//    override fun getItemCount(): Int = bids.size
//
//    private fun fetchItemDetails(itemId: String, callback: (Item?) -> Unit) {
//        if (itemId.isBlank()) {
//            callback(null)
//            return
//        }
//        if (itemCache.containsKey(itemId)) {
//            callback(itemCache[itemId])
//            return
//        }
//        val itemRef = FirebaseDatabase.getInstance().getReference("Items").child(itemId)
//        itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val item = try {
//                    snapshot.getValue(Item::class.java)?.apply { id = snapshot.key ?: itemId }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing item $itemId", e)
//                    null
//                }
//                itemCache[itemId] = item
//                callback(item)
//            }
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "Failed to fetch item $itemId: ${error.message}")
//                itemCache[itemId] = null
//                callback(null)
//            }
//        })
//    }
//}
//
//




