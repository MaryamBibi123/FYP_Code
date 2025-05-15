package com.example.signuplogina // Ensure correct package

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.Item // Ensure Item is imported
import com.example.signuplogina.R // Ensure R is imported
// import com.squareup.picasso.Picasso // Using Glide as per your previous ExchangeItemsAdapter
import com.bumptech.glide.Glide


class MyListingAdapter(
    private var items: List<Item>,
    private val isOwnerViewing: Boolean,
    private val onItemClicked: (Item) -> Unit,
    private val onBidReceivedClicked: (item: Item, childRecyclerView: RecyclerView, expandableLayout: View, button: Button) -> Unit
) : RecyclerView.Adapter<MyListingAdapter.MyListingViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class MyListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameText: TextView = itemView.findViewById(R.id.item_name)
        private val productImage: ImageView = itemView.findViewById(R.id.item_image)
        val buttonBidReceived: Button = itemView.findViewById(R.id.button_bid_received)
        val expandableLayout: View = itemView.findViewById(R.id.expandable_layout)
        val childRecyclerView: RecyclerView = itemView.findViewById(R.id.child_rv)
        private val unavailableOverlay: TextView = itemView.findViewById(R.id.unavailableOverlayItem)

        @SuppressLint("SetTextI18n")
        fun bind(item: Item) {
            productNameText.text = item.details.productName
            val imageUrl = item.details.imageUrls.firstOrNull()

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context).load(imageUrl) // Switched to Glide for consistency with other adapter
                    .placeholder(R.drawable.ic_camera).error(R.drawable.baseline_error_24)
                    .centerCrop().into(productImage)
            } else {
                productImage.setImageResource(R.drawable.ic_camera)
            }

            expandableLayout.visibility = View.GONE
            buttonBidReceived.text = itemView.context.getString(R.string.view_bids_button)

            if (isOwnerViewing) {
                val context = itemView.context
                var overlayText: String? = null
                var overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_default_background)
                var allowBidButtonInteraction = true // Can owner still click "View Bids"?
                var allowMainItemClick = true      // Owner can usually always click their item

                Log.d("MyListingAdapter", "Owner View - Binding Item: ${item.id}, AdminStatus: ${item.status}, ExchangeState: ${item.exchangeState}, IsAvailable: ${item.available}")

                // Logic based on Admin Status first, then Exchange State
                when (item.status.lowercase()) {
                    "pending" -> {
                        overlayText = "PENDING APPROVAL"
                        overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_pending_background)
                        allowBidButtonInteraction = false
                        allowMainItemClick = true // Owner might want to view/edit pending item
                    }
                    "rejected" -> {
                        overlayText = "REJECTED BY ADMIN"
                        overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_rejected_background)
                        allowBidButtonInteraction = false
                        allowMainItemClick = true // Owner can view why it was rejected
                    }
                    "removed_by_owner" -> {
                        overlayText = "REMOVED BY YOU"
                        overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_removed_background)
                        allowBidButtonInteraction = false
                        allowMainItemClick = false // Or true if they can view removed items
                    }
                    "removed_by_admin" -> {
                        overlayText = "REMOVED BY ADMIN"
                        overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_removed_background)
                        allowBidButtonInteraction = false
                        allowMainItemClick = false
                    }
                    "approved" -> {
                        // Item is admin-approved. Now check its exchange lifecycle state.
                        when (item.exchangeState.lowercase()) {
                            "in_negotiation" -> {
                                overlayText = "IN EXCHANGE DISCUSSION" // Specific text
                                overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_negotiation_background)
                                // Owner can still view bids (to see which ones are on_hold, and which one is active).
                                // The "Start Chat" for other bids will be disabled by ChildBidAdapter logic.
                                allowBidButtonInteraction = true
                                allowMainItemClick = true
                            }
                            "exchanged" -> {
                                overlayText = "EXCHANGED"
                                overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_exchanged_background)
                                allowBidButtonInteraction = false
                                allowMainItemClick = true // View what was exchanged
                            }
                            "none" -> {
                                // Approved, and not in any active exchange process.
                                // Its 'available' flag now determines everything for this state.
                                if (item.available) {
                                    overlayText = null // Fully available, no overlay
                                    allowBidButtonInteraction = true
                                    allowMainItemClick = true
                                } else {
                                    // Approved, not in an exchange, but owner/system made it unavailable
                                    overlayText = "CURRENTLY UNAVAILABLE"
                                    overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_unavailable_background)
                                    allowBidButtonInteraction = false
                                    allowMainItemClick = true // Owner might want to make it available again
                                }
                            }
                            else -> { // Unknown exchangeState
                                overlayText = "UNKNOWN EXCHANGE STATUS"
                                allowBidButtonInteraction = false
                            }
                        }
                    }
                    else -> { // Unknown admin status
                        overlayText = "STATUS: ${item.status.uppercase()}"
                        allowBidButtonInteraction = false
                        allowMainItemClick = true // Allow viewing details even for unknown status by owner
                    }
                }

                if (overlayText != null) {
                    itemView.alpha = 0.75f
                    unavailableOverlay.visibility = View.VISIBLE
                    unavailableOverlay.text = overlayText
                    unavailableOverlay.setBackgroundColor(overlayBackgroundColor)
                } else {
                    itemView.alpha = 1.0f
                    unavailableOverlay.visibility = View.GONE
                }

                itemView.isClickable = allowMainItemClick
                itemView.isFocusable = allowMainItemClick
                if (allowMainItemClick) {
                    itemView.setOnClickListener { onItemClicked(item) }
                } else {
                    itemView.setOnClickListener(null)
                }

                buttonBidReceived.isEnabled = allowBidButtonInteraction
                buttonBidReceived.alpha = if (allowBidButtonInteraction) 1.0f else 0.5f
                if (!allowBidButtonInteraction) {
                    buttonBidReceived.text = overlayText ?: item.status.uppercase() // Show relevant status on button
                    buttonBidReceived.setOnClickListener(null)
                } else {
                    buttonBidReceived.text = context.getString(R.string.view_bids_button)
                    buttonBidReceived.setOnClickListener {
                        onBidReceivedClicked(item, childRecyclerView, expandableLayout, buttonBidReceived)
                    }
                }
                // Always hide expandable for non-interactive states initially
                if (!allowBidButtonInteraction && overlayText != null) {
                    expandableLayout.visibility = View.GONE
                }


            } else {
                // --- ANOTHER USER IS VIEWING THIS LISTING ---
                // Fragment has already filtered to show only "approved" & "available" & exchangeState="none" items.
                Log.d("MyListingAdapter", "Other User View - Item: ${item.id} (Status: ${item.status}, Available: ${item.available}, ExchangeState: ${item.exchangeState})")
                itemView.alpha = 1.0f
                itemView.isClickable = true
                itemView.isFocusable = true
                unavailableOverlay.visibility = View.GONE
                itemView.setOnClickListener { onItemClicked(item) }

                buttonBidReceived.visibility = View.GONE
                expandableLayout.visibility = View.GONE
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyListingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return MyListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyListingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}




//package com.example.signuplogina // Ensure correct package
//
//import android.annotation.SuppressLint
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.core.content.ContextCompat // For colors
//import androidx.recyclerview.widget.RecyclerView
//import com.example.signuplogina.Item // Ensure Item is imported
//import com.example.signuplogina.R // Ensure R is imported
//import com.squareup.picasso.Picasso
//
//class MyListingAdapter(
//    private var items: List<Item>,
//    private val onItemClicked: (Item) -> Unit,
//    private val onBidReceivedClicked: (item: Item, childRecyclerView: RecyclerView, expandableLayout: View, button: Button) -> Unit,
//) : RecyclerView.Adapter<MyListingAdapter.MyListingViewHolder>() {
//
//    @SuppressLint("NotifyDataSetChanged")
//    fun updateList(newItems: List<Item>) {
//        items = newItems
//        notifyDataSetChanged()
//    }
//
//    inner class MyListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val productNameText: TextView = itemView.findViewById(R.id.item_name)
//        private val productImage: ImageView = itemView.findViewById(R.id.item_image)
//        val buttonBidReceived: Button = itemView.findViewById(R.id.button_bid_received)
//        val expandableLayout: View = itemView.findViewById(R.id.expandable_layout)
//        val childRecyclerView: RecyclerView = itemView.findViewById(R.id.child_rv)
//        private val unavailableOverlay: TextView = itemView.findViewById(R.id.unavailableOverlayItem)
//
//        @SuppressLint("SetTextI18n") // For string formatting if needed
//        fun bind(item: Item) {
//            productNameText.text = item.details.productName
//            val imageUrl = item.details.imageUrls.firstOrNull()
//
//            if (!imageUrl.isNullOrEmpty()) {
//                Picasso.get().load(imageUrl)
//                    .placeholder(R.drawable.ic_camera).error(R.drawable.baseline_error_24)
//                    .fit().centerCrop().into(productImage)
//            } else {
//                productImage.setImageResource(R.drawable.ic_camera)
//            }
//
//            expandableLayout.visibility = View.GONE
//            buttonBidReceived.text = itemView.context.getString(R.string.view_bids_button) // Default text
//
//            // --- Determine Item State for UI ---
//            val context = itemView.context
//            var overlayText: String? = null
//            var overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_default_background) // Define this color
//            var allowInteraction = true
//
//            when (item.status.lowercase()) { // Use lowercase for robust comparison
//                "approved" -> {
//                    if (item.available) {
//                        // Approved and available - no overlay
//                        allowInteraction = true
//                    } else {
//                        // Approved but not available (e.g., in another negotiation, or manually set unavailable by owner)
//                        overlayText = "UNAVAILABLE" // Or "IN USE" / "IN NEGOTIATION" if you have more specific state
//                        overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_unavailable_background)
//                        allowInteraction = false
//                    }
//                }
//                "pending" -> {
//                    overlayText = "PENDING APPROVAL"
//                    overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_pending_background)
//                    allowInteraction = false // Cannot interact with pending items (e.g., view bids)
//                }
//                "rejected" -> {
//                    overlayText = "REJECTED"
//                    overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_rejected_background)
//                    allowInteraction = false
//                }
//                "removed_by_owner", "removed_by_admin" -> {
//                    overlayText = "REMOVED"
//                    overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_removed_background)
//                    allowInteraction = false
//                }
//                "in_negotiation" -> { // If you add this status
//                    overlayText = "IN NEGOTIATION"
//                    overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_negotiation_background)
//                    allowInteraction = false // Usually disable "View Bids" if already in one specific negotiation
//                }
//                "exchanged" -> {
//                    overlayText = "EXCHANGED"
//                    overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_exchanged_background)
//                    allowInteraction = false
//                }
//                else -> { // Other statuses or if item.available is false for an unknown reason
//                    if (!item.available) {
//                        overlayText = "UNAVAILABLE"
//                        overlayBackgroundColor = ContextCompat.getColor(context, R.color.overlay_unavailable_background)
//                        allowInteraction = false
//                    }
//                }
//            }
//
//            Log.d("MyListingAdapter", "Item: ${item.id}, Status: ${item.status}, Available: ${item.available}, Overlay: $overlayText, Interaction: $allowInteraction")
//
//            if (allowInteraction) {
//                itemView.alpha = 1.0f
//                itemView.isClickable = true
//                itemView.isFocusable = true
//                unavailableOverlay.visibility = View.GONE
//                itemView.setOnClickListener { onItemClicked(item) }
//
//                buttonBidReceived.isEnabled = true
//                buttonBidReceived.alpha = 1.0f
//                buttonBidReceived.setOnClickListener {
//                    onBidReceivedClicked(item, childRecyclerView, expandableLayout, buttonBidReceived)
//                }
//            } else {
//                itemView.alpha = 0.65f // Dim slightly more
//                itemView.isClickable = false
//                itemView.isFocusable = false
//                itemView.setOnClickListener(null)
//                unavailableOverlay.visibility = View.VISIBLE
//                unavailableOverlay.text = overlayText ?: "STATUS: ${item.status.uppercase()}" // Fallback
//                unavailableOverlay.setBackgroundColor(overlayBackgroundColor)
//
//                buttonBidReceived.isEnabled = false
//                buttonBidReceived.alpha = 0.5f
//                buttonBidReceived.setOnClickListener(null)
//                buttonBidReceived.text = overlayText ?: item.status.uppercase() // Show status on button too
//                expandableLayout.visibility = View.GONE // Ensure bids section is hidden
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyListingViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
//        return MyListingViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MyListingViewHolder, position: Int) {
//        holder.bind(items[position])
//    }
//
//    override fun getItemCount(): Int = items.size
//}
//
//
//
