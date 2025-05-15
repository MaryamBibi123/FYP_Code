package com.example.signuplogina.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.Item // Ensure Item is imported
import com.example.signuplogina.R   // Ensure R is imported

class ExchangeItemsAdapter(
    private var items: MutableList<Item> = mutableListOf(),
    private val currentUserId: String,
    private val currentBidIdForContext: String, // ID of the bid this chat room is for
    private val onCardClick: (Item) -> Unit
) : RecyclerView.Adapter<ExchangeItemsAdapter.ExchangeItemViewHolder>() {

    private val TAG = "ExchangeItemsAdapter"

    inner class ExchangeItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemCondition: TextView = itemView.findViewById(R.id.itemCondition)
        val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
        val itemCategory: TextView = itemView.findViewById(R.id.itemCategory)
        val expandableLayout: LinearLayout = itemView.findViewById(R.id.expandableLayout)
        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        val unavailableOverlay: TextView = itemView.findViewById(R.id.unavailableOverlayExchange)
        val ownershipLabel: TextView = itemView.findViewById(R.id.tvItemOwnershipLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exchange_card, parent, false)
        return ExchangeItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ExchangeItemViewHolder, position: Int) {
        val item = items[position]
        Log.d(TAG, "Binding item ${item.id} (AdminStatus: ${item.status}, ExchangeState: ${item.exchangeState}, Available: ${item.available}, LockedBy: ${item.lockedByBidId}) for bid $currentBidIdForContext")


        if (item.userId == currentUserId) {
            holder.ownershipLabel.text = "Your Item"
            holder.ownershipLabel.setBackgroundResource(R.drawable.label_background_yours)
            (holder.itemView as? com.google.android.material.card.MaterialCardView)?.setStrokeColor(
                ContextCompat.getColor(holder.itemView.context, R.color.dark_green)
            )
        } else {
            holder.ownershipLabel.text = "Their Item" // Simplified for now
            holder.ownershipLabel.setBackgroundResource(R.drawable.label_background_theirs)
            (holder.itemView as? com.google.android.material.card.MaterialCardView)?.setStrokeColor(
                ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)
            )
        }
        holder.ownershipLabel.visibility = View.VISIBLE

        // --- Bind other data (as before) ---
        holder.itemName.text = item.details.productName
        holder.itemCondition.text = "Condition: ${item.details.condition}"
        holder.itemDescription.text = "Description: ${item.details.description}"
        holder.itemCategory.text = "Category: ${item.details.category}"

        if (item.details.imageUrls.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.details.imageUrls[0])
                .placeholder(R.drawable.ic_camera).error(R.drawable.baseline_error_24)
                .centerCrop().into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(R.drawable.ic_camera)
        }

        var isExpandedLocally = false
        holder.expandableLayout.visibility = View.GONE
        holder.expandIcon.setImageResource(R.drawable.arrow_down)
        holder.expandIcon.rotation = 0f

        // --- Determine UI based on item's state IN THE CONTEXT OF THIS CHAT ---
        var displayAsInteractive = false // Default to not interactive
        var overlayText: String? = "UNAVAILABLE (Context Error)" // Default error text
        val context = holder.itemView.context
        var overlayString: String? = null
        var overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_default_background)


        when (item.status.lowercase()) {
            "owner_temporarily_blocked" -> {
                overlayString = "ITEM ON HOLD (Owner Temp. Restricted)"
                overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_warning_background)
                displayAsInteractive = false
            }
            "owner_permanently_blocked" -> {
                overlayString = "ITEM UNAVAILABLE (Owner Perm. Blocked)"
                overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_error_background)
                displayAsInteractive = false
            }
            "approved" -> {
                when (item.exchangeState.lowercase()) {
                    "in_negotiation" -> {
                        if (item.lockedByBidId == currentBidIdForContext) {
                            overlayString = null
                            displayAsInteractive = true
                        } else {
                            overlayString = "IN ANOTHER EXCHANGE"
                            overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_negotiation_background)
                            displayAsInteractive = false
                        }
                    }
                    "exchanged" -> {
                        overlayString = "ALREADY EXCHANGED"
                        overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_exchanged_background)
                        displayAsInteractive = false
                    }
                    "on_hold_owner_blocked" -> {
                        overlayString = "EXCHANGE STALLED (Owner Temp. Restricted)"
                        overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_warning_background)
                        displayAsInteractive = false
                    }
                    "archived_owner_blocked" -> {
                        overlayString = "EXCHANGE ARCHIVED (Owner Perm. Blocked)"
                        overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_error_background)
                        displayAsInteractive = false
                    }
                    "none" -> {
                        if (!item.available) {
                            overlayString = "ITEM UNAVAILABLE"
                            displayAsInteractive = false
                        } else {
                            overlayString = null
                        }
                    }
                    else -> {
                        overlayString = "ITEM STATUS: ${item.exchangeState.uppercase()}"
                        displayAsInteractive = false
                    }
                }
            }
            else -> {
                overlayString = "ITEM ${item.status.uppercase()}"
                displayAsInteractive = false
                // TODO: Set appropriate overlayBgColorInt for other item.status if needed
            }
        }

        holder.itemView.alpha = if (displayAsInteractive) 1.0f else 0.6f
//        when (item.status.lowercase()) {
//            "owner_temporarily_blocked" -> {
//                overlayText = "ITEM ON HOLD (Owner Temp. Restricted)"
//                overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_warning_background)
//                displayAsInteractive = false
//            }
//            "owner_permanently_blocked" -> {
//                overlayText = "ITEM UNAVAILABLE (Owner Perm. Blocked)"
//                overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_error_background)
//                displayAsInteractive = false
//            }
//            "approved" -> { // Item itself is approved by admin, now check its exchange state for *this* bid
//                when (item.exchangeState.lowercase()) {
//                    "in_negotiation" -> {
//                        if (item.lockedByBidId == currentBidIdForContext) {
//                            // This item is part of THIS active negotiation AND owner is not blocked. Good to go.
//                            displayAsInteractive = true
//                            overlayText = null
//                        } else {
//                            overlayText = "IN ANOTHER EXCHANGE"
//                            overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_negotiation_background)
//                            displayAsInteractive = false // Cannot interact if locked by another bid
//                        }
//                    }
//                    "exchanged" -> {
//                        overlayText = "ALREADY EXCHANGED"
//                        overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_exchanged_background)
//                        displayAsInteractive = false
//                    }
//                    "on_hold_owner_blocked" -> { // Item is in this exchange, but owner is temp blocked
//                        overlayText = "EXCHANGE STALLED (Owner Temp. Restricted)"
//                        overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_warning_background)
//                        displayAsInteractive = false
//                    }
//                    "archived_owner_blocked" -> { // Item is in this exchange, but owner is perm blocked
//                        overlayText = "EXCHANGE ARCHIVED (Owner Perm. Blocked)"
//                        overlayBgColorInt = ContextCompat.getColor(context, R.color.overlay_error_background)
//                        displayAsInteractive = false
//                    }
//                    "none" -> { // Approved, not in any specific exchange state *yet* for this item
//                        if (!item.available) { // Should ideally not happen if filtered upstream
//                            overlayText = "ITEM UNAVAILABLE"
//                            displayAsInteractive = false
//                        } else {
//                            displayAsInteractive = true // Available for this exchange
//                            overlayText = null
//                        }
//                    }
//                    else -> { // Unknown exchangeState
//                        overlayText = "ITEM STATUS: ${item.exchangeState.uppercase()}"
//                        displayAsInteractive = false
//                    }
//                }
//            }
//            // Handle other item.status like "pending", "rejected", "removed_by_owner", etc.
//            else -> {
//                overlayText = "ITEM ${item.status.uppercase()}"
//                // Determine overlayBgColorInt based on item.status
//                displayAsInteractive = false
//            }
//        }

        // Apply UI based on determination
        if (displayAsInteractive) {
            holder.itemView.alpha = 1.0f
            holder.itemView.setOnClickListener { onCardClick(item) }
            holder.expandIcon.setOnClickListener {
                // Toggle expansion (ensure isExpandedLocally is managed correctly)
                isExpandedLocally = !isExpandedLocally // This needs to be stored per item or reset
                holder.expandableLayout.visibility = if (isExpandedLocally) View.VISIBLE else View.GONE
                holder.expandIcon.animate().rotation(if (isExpandedLocally) 180f else 0f).setDuration(200).start()
            }
            holder.unavailableOverlay.visibility = View.GONE
        } else {
            holder.itemView.alpha = 0.5f // Dimmed
            holder.itemView.setOnClickListener(null)
            holder.expandIcon.setOnClickListener(null)
            holder.expandableLayout.visibility = View.GONE // Always collapsed if not interactive
            holder.unavailableOverlay.visibility = View.VISIBLE
            holder.unavailableOverlay.text = overlayString ?: "UNAVAILABLE"
            holder.unavailableOverlay.setBackgroundColor(overlayBgColorInt)
        }

    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}





//
//
//
//
//
//package com.example.signuplogina.adapter
//
//import android.annotation.SuppressLint
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.TextView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.signuplogina.Item // Ensure Item is imported
//import com.example.signuplogina.R   // Ensure R is imported
//
//class ExchangeItemsAdapter(
//    private var items: MutableList<Item> = mutableListOf(),
//    private val currentUserId: String,
//    private val currentBidIdForContext: String, // ID of the bid this chat room is for
//    private val onCardClick: (Item) -> Unit
//) : RecyclerView.Adapter<ExchangeItemsAdapter.ExchangeItemViewHolder>() {
//
//    private val TAG = "ExchangeItemsAdapter"
//
//    inner class ExchangeItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
//        val itemName: TextView = itemView.findViewById(R.id.itemName)
//        val itemCondition: TextView = itemView.findViewById(R.id.itemCondition)
//        val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
//        val itemCategory: TextView = itemView.findViewById(R.id.itemCategory)
//        val expandableLayout: LinearLayout = itemView.findViewById(R.id.expandableLayout)
//        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
//        val unavailableOverlay: TextView = itemView.findViewById(R.id.unavailableOverlayExchange)
//        val ownershipLabel: TextView = itemView.findViewById(R.id.tvItemOwnershipLabel)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeItemViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_exchange_card, parent, false)
//        return ExchangeItemViewHolder(view)
//    }
//
//    @SuppressLint("SetTextI18n")
//    override fun onBindViewHolder(holder: ExchangeItemViewHolder, position: Int) {
//        val item = items[position]
//        Log.d(TAG, "Binding item ${item.id} (AdminStatus: ${item.status}, ExchangeState: ${item.exchangeState}, Available: ${item.available}, LockedBy: ${item.lockedByBidId}) for bid $currentBidIdForContext")
//
//        // --- Set Ownership Label ---
//        // (Your existing ownership label logic - seems okay)
//        if (item.userId == currentUserId) {
//            holder.ownershipLabel.text = "Your Item"
//            holder.ownershipLabel.setBackgroundResource(R.drawable.label_background_yours)
//            (holder.itemView as? com.google.android.material.card.MaterialCardView)?.setStrokeColor(
//                ContextCompat.getColor(holder.itemView.context, R.color.dark_green)
//            )
//        } else {
//            holder.ownershipLabel.text = "Their Item" // Simplified for now
//            holder.ownershipLabel.setBackgroundResource(R.drawable.label_background_theirs)
//            (holder.itemView as? com.google.android.material.card.MaterialCardView)?.setStrokeColor(
//                ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)
//            )
//        }
//        holder.ownershipLabel.visibility = View.VISIBLE
//
//        // --- Bind other data (as before) ---
//        holder.itemName.text = item.details.productName
//        holder.itemCondition.text = "Condition: ${item.details.condition}"
//        holder.itemDescription.text = "Description: ${item.details.description}"
//        holder.itemCategory.text = "Category: ${item.details.category}"
//
//        if (item.details.imageUrls.isNotEmpty()) {
//            Glide.with(holder.itemView.context).load(item.details.imageUrls[0])
//                .placeholder(R.drawable.ic_camera).error(R.drawable.baseline_error_24)
//                .centerCrop().into(holder.itemImage)
//        } else {
//            holder.itemImage.setImageResource(R.drawable.ic_camera)
//        }
//
//        var isExpandedLocally = false
//        holder.expandableLayout.visibility = View.GONE
//        holder.expandIcon.setImageResource(R.drawable.arrow_down)
//        holder.expandIcon.rotation = 0f
//
//        // --- Determine UI based on item's state IN THE CONTEXT OF THIS CHAT ---
//        var displayAsInteractive = false // Default to not interactive
//        var overlayText: String? = "UNAVAILABLE (Context Error)" // Default error text
//        val context = holder.itemView.context
//        var overlayBgColor = ContextCompat.getColor(context, R.color.overlay_default_background)
//
//
//        // Primary check: Admin Status
//        if (item.status.equals("approved", ignoreCase = true)) {
//            // Item is admin-approved. Now check its exchange state.
//            when (item.exchangeState.lowercase()) {
//                "in_negotiation" -> {
//                    if (item.lockedByBidId == currentBidIdForContext) {
//                        // This item IS part of THIS current, active negotiation.
//                        // It should be fully visible and interactive within this chat.
//                        displayAsInteractive = true
//                        overlayText = null // No overlay needed
//                        Log.i(TAG, "Item ${item.id} [${item.details.productName}] is 'in_negotiation' for THIS bid ($currentBidIdForContext). Displaying normally.")
//                    } else {
//                        // Item is approved but locked by a DIFFERENT bid.
//                        displayAsInteractive = false
//                        overlayText = "IN ANOTHER EXCHANGE"
//                        overlayBgColor = ContextCompat.getColor(context, R.color.overlay_negotiation_background) // Use a distinct color
//                        Log.w(TAG, "Item ${item.id} [${item.details.productName}] is 'in_negotiation' but locked by different bid (${item.lockedByBidId}) than current ($currentBidIdForContext).")
//                    }
//                }
//                "exchanged" -> {
//                    // Item has already been successfully exchanged (possibly in another deal).
//                    displayAsInteractive = false
//                    overlayText = "ALREADY EXCHANGED"
//                    overlayBgColor = ContextCompat.getColor(context, R.color.overlay_exchanged_background)
//                }
//                "none" -> {
//                    // Item is admin-approved and NOT in any exchange negotiation according to its exchangeState.
//                    // This state is normal for items not yet involved in a 'started' bid.
//                    // Within an active chat, if an item (requested or offered) is in this state,
//                    // it means it *should* be available for this negotiation unless its global 'available' flag is false.
//                    if (item.available) { // Check the global availability flag
//                        displayAsInteractive = true
//                        overlayText = null
//                        Log.i(TAG, "Item ${item.id} [${item.details.productName}] is 'approved', 'none' exchangeState, and 'available'. Displaying normally.")
//                    } else {
//                        // Approved, exchangeState 'none', but globally 'unavailable'.
//                        // This is an edge case for an item *within an active chat*.
//                        // It might imply it was part of the bid, but something made it unavailable globally *after* the chat started
//                        // but *before* its exchangeState was set to 'in_negotiation' or it was correctly unlocked.
//                        // Or, it's an offered item that the bidder made unavailable themselves.
//                        displayAsInteractive = false
//                        overlayText = "ITEM UNAVAILABLE"
//                        overlayBgColor = ContextCompat.getColor(context, R.color.overlay_unavailable_background)
//                        Log.w(TAG, "Item ${item.id} [${item.details.productName}] is 'approved', 'none' exchangeState, but globally 'unavailable'.")
//                    }
//                }
//                else -> { // Unknown exchangeState
//                    displayAsInteractive = false
//                    overlayText = "STATUS: ${item.exchangeState.uppercase()}"
//                    Log.w(TAG, "Item ${item.id} [${item.details.productName}] has unknown exchangeState: ${item.exchangeState}")
//                }
//            }
//        } else {
//            // Admin status is NOT "approved" (e.g., "pending", "rejected", "removed_...")
//            displayAsInteractive = false
//            overlayText = "ITEM ${item.status.uppercase()}"
//            overlayBgColor = when(item.status.lowercase()){
//                "pending" -> ContextCompat.getColor(context, R.color.overlay_pending_background)
//                "rejected" -> ContextCompat.getColor(context, R.color.overlay_rejected_background)
//                else -> ContextCompat.getColor(context, R.color.overlay_removed_background)
//            }
//            Log.w(TAG, "Item ${item.id} [${item.details.productName}] has admin status '${item.status}'. Not interactive in chat.")
//        }
//
//        // Apply UI based on determination
//        if (displayAsInteractive) {
//            holder.itemView.alpha = 1.0f
//            holder.itemView.isClickable = true
//            holder.itemView.isFocusable = true
//            holder.itemView.setOnClickListener { onCardClick(item) }
//            holder.expandIcon.setOnClickListener {
//                isExpandedLocally = !isExpandedLocally
//                holder.expandableLayout.visibility = if (isExpandedLocally) View.VISIBLE else View.GONE
//                holder.expandIcon.animate().rotation(if (isExpandedLocally) 180f else 0f).setDuration(200).start()
//            }
//            holder.unavailableOverlay.visibility = View.GONE
//        } else {
//            holder.itemView.alpha = 0.6f // Make it more dimmed
//            holder.itemView.isClickable = false
//            holder.itemView.isFocusable = false
//            holder.itemView.setOnClickListener(null)
//            holder.expandIcon.setOnClickListener(null)
//            holder.expandableLayout.visibility = View.GONE
//            holder.unavailableOverlay.visibility = View.VISIBLE
//            holder.unavailableOverlay.text = overlayText ?: "UNAVAILABLE"
//            holder.unavailableOverlay.setBackgroundColor(overlayBgColor)
//        }
//    }
//
//    override fun getItemCount() = items.size
//
//    @SuppressLint("NotifyDataSetChanged")
//    fun updateItems(newItems: List<Item>) {
//        items.clear()
//        items.addAll(newItems)
//        notifyDataSetChanged()
//    }
//}
