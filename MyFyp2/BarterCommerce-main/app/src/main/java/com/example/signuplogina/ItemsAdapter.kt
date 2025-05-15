package com.example.signuplogina

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton // Keep if used elsewhere, not in layouts provided
import android.util.Log
import android.annotation.SuppressLint
import com.example.signuplogina.R // Ensure R is imported
import com.example.signuplogina.Item // Ensure Item is imported
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ItemsAdapter(
    private var items: MutableList<Item>,
    private val onItemClicked: (Item) -> Unit,
    private val onBidClicked: (Item) -> Unit, // Assuming this is for the "Like" action now
    private val useLatestLayout: Boolean = false
) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged") // Consider DiffUtil later
    fun updateItems(newItems: List<Item>) {
        Log.d("AdapterDebug", "Updating items with ${newItems.size} items")
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (useLatestLayout) R.layout.latest_item else R.layout.item_product
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // Pass useLatestLayout flag to bind if needed, though logic mostly applies to grid
        holder.bind(item, onItemClicked, onBidClicked, useLatestLayout)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Views common to both layouts (or potentially nullable if not)
        private val productName: TextView = itemView.findViewById(R.id.product_name)
        private val productImage: ImageView = itemView.findViewById(R.id.product_image)
        private val conditionBadge: TextView = itemView.findViewById(R.id.product_condition)
        //descrition
        private val description: TextView? = itemView.findViewById(R.id.descriptionTitle)
        // View specific to item_product layout (nullable find)
        private val userRating: TextView? = itemView.findViewById(R.id.owner_rating_text)

        private val unavailableOverlay: TextView? = itemView.findViewById(R.id.unavailableOverlay)


//        fun bind(item: Item, onItemClicked: (Item) -> Unit, onLikeClicked: (Item) -> Unit, isLatestLayout: Boolean) {
//            val context = itemView.context
//            productName.text = item.details.productName
//
//            Glide.with(context)
//                .load(item.details.imageUrls.firstOrNull() ?: "")
//                .placeholder(R.drawable.ic_photos)
//                .error(R.drawable.baseline_error_24)
//                .centerCrop()
//                .into(productImage)
//
//            val condition = item.details.condition
//            conditionBadge.text = condition
//            when (condition.lowercase()) {
//                "good" -> conditionBadge.setBackgroundColor(Color.parseColor("#A3D9A5"))
//                "used" -> conditionBadge.setBackgroundColor(Color.parseColor("#A5C8E1"))
//                "fair" -> conditionBadge.setBackgroundColor(Color.parseColor("#F7C59F"))
//                "new" -> conditionBadge.setBackgroundColor(Color.parseColor("#4CAF50"))
//                else -> conditionBadge.setBackgroundColor(Color.GRAY)
//            }
//            conditionBadge.visibility = View.VISIBLE
//
//            if (isLatestLayout) {
//                description?.text = item.details.description
//                userRating?.text = "Loading rating..."
//
//                val itemId = item.id
//                if (!itemId.isNullOrBlank()) {
//                    val itemRef = FirebaseDatabase.getInstance().getReference("Items").child(itemId)
//                    itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            val userId = snapshot.child("userId").getValue(String::class.java)
//                            if (!userId.isNullOrBlank()) {
//                                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
//                                userRef.get().addOnSuccessListener { userSnapshot ->
//                                    val user = userSnapshot.getValue(User::class.java)
//                                    val ratingValue = user?.ratings?.averageRating ?: 0.0
//                                    userRating?.text = "OwnerRating:  $ratingValue"
//                                }.addOnFailureListener {
//                                    userRating?.text = "Rating: 0"
//                                }
//                            } else {
//                                userRating?.text = "OwnerRating:  0"
//                            }
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {
//                            userRating?.text = "OwnerRating:  0"
//                        }
//                    })
//                } else {
//                    userRating?.text = "OwnerRating:  0"
//                }
//            }
//
//            val isItemAvailable = item.available && item.status.equals("approved", ignoreCase = true)
//            val userId = item.userId ?: ""
//
//            if (userId.isNotBlank()) {
//                val blockedRef = FirebaseDatabase.getInstance().getReference("BlockedUsers").child(userId)
//                blockedRef.get().addOnSuccessListener { blockedSnapshot ->
//                    val isBlocked = blockedSnapshot.exists()
//
//                    if (isItemAvailable && !isBlocked) {
//                        itemView.alpha = 1.0f
//                        itemView.isClickable = true
//                        itemView.isFocusable = true
//                        itemView.setOnClickListener { onItemClicked(item) }
//                        unavailableOverlay?.visibility = View.GONE
//                    } else {
//                        itemView.alpha = 0.5f
//                        itemView.isClickable = false
//                        itemView.isFocusable = false
//                        itemView.setOnClickListener(null)
//                        unavailableOverlay?.visibility = View.VISIBLE
//                        unavailableOverlay?.text = if (isBlocked) "Blocked User" else "Unavailable"
//                    }
//                }.addOnFailureListener {
//                    // Fallback if blocked status can't be determined
//                    if (isItemAvailable) {
//                        itemView.alpha = 1.0f
//                        itemView.isClickable = true
//                        itemView.setOnClickListener { onItemClicked(item) }
//                        unavailableOverlay?.visibility = View.GONE
//                    } else {
//                        itemView.alpha = 0.5f
//                        itemView.isClickable = false
//                        itemView.setOnClickListener(null)
//                        unavailableOverlay?.visibility = View.VISIBLE
//                        unavailableOverlay?.text = "Unavailable"
//                    }
//                }
//            }
//        }

        fun bind(item: Item, onItemClicked: (Item) -> Unit, onLikeClicked: (Item) -> Unit, isLatestLayout: Boolean) {
            val context = itemView.context
            productName.text = item.details.productName

            Glide.with(context)
                .load(item.details.imageUrls.firstOrNull() ?: "")
                .placeholder(R.drawable.ic_photos) // Ensure ic_photos exists
                .error(R.drawable.baseline_error_24)
                .centerCrop()
                .into(productImage)

            // Condition Badge logic (existing)
            val condition = item.details.condition
            conditionBadge.text = condition
            // ... (your existing when block for conditionBadge.setBackgroundColor) ...
            conditionBadge.visibility = View.VISIBLE

            if (isLatestLayout) {
                description?.text = item.details.description
                // User rating fetching logic (existing) - This part is fine as is.
                // ... (your existing user rating fetch logic) ...
            }

            // --- NEW STATUS AND INTERACTIVITY LOGIC ---
            var isEffectivelyAvailable = true
            var overlayDisplayMessage: String? = null
            itemView.alpha = 1.0f // Default to fully visible

            // 1. Check the item's own status first
            when (item.status.lowercase()) {
                "approved" -> {
                    // If item is approved, then we check its 'available' flag and owner's block status
                    if (!item.available) {
                        isEffectivelyAvailable = false
                        overlayDisplayMessage = "ITEM UNAVAILABLE"
                    }
                    // If item.available is true, we'll proceed to check owner's block status below
                }
                "owner_temporarily_blocked" -> {
                    isEffectivelyAvailable = false
                    overlayDisplayMessage = "OWNER TEMPORARILY RESTRICTED"
                    itemView.alpha = 0.6f // Dim for temporary issues
                }
                "owner_permanently_blocked" -> {
                    isEffectivelyAvailable = false
                    overlayDisplayMessage = "OWNER PERMANENTLY BLOCKED"
                    itemView.alpha = 0.4f // Dim more for permanent issues
                }
                "pending" -> {
                    isEffectivelyAvailable = false
                    overlayDisplayMessage = "PENDING APPROVAL"
                    itemView.alpha = 0.7f
                }
                "rejected" -> {
                    isEffectivelyAvailable = false
                    overlayDisplayMessage = "ITEM REJECTED"
                    itemView.alpha = 0.5f
                }
                "removed_by_admin", "removed_by_owner" -> {
                    isEffectivelyAvailable = false
                    overlayDisplayMessage = "ITEM REMOVED"
                    itemView.alpha = 0.5f
                }
                else -> { // Unknown status
                    isEffectivelyAvailable = false
                    overlayDisplayMessage = "STATUS: ${item.status.uppercase()}"
                    itemView.alpha = 0.5f
                }
            }

            // 2. If item is potentially available (based on its own status), check owner's global block status
            //    This `/BlockedUsers` check is a simpler, direct way.
            //    It overrides the item's individual status if the user is globally blocked.
            if (isEffectivelyAvailable && item.userId.isNotBlank()) {
                val blockedRef = FirebaseDatabase.getInstance().getReference("BlockedUsers").child(item.userId)
                blockedRef.get().addOnSuccessListener { blockedSnapshot ->
                    val isOwnerGloballyBlocked = blockedSnapshot.exists() && blockedSnapshot.getValue(Boolean::class.java) == true

                    if (isOwnerGloballyBlocked) {
                        isEffectivelyAvailable = false
                        overlayDisplayMessage = "OWNER BLOCKED" // More generic since /BlockedUsers doesn't store temp/perm
                        itemView.alpha = 0.4f
                    }
                    // Update UI after this async check
                    updateItemViewInteractivity(item, isEffectivelyAvailable, overlayDisplayMessage, onItemClicked)

                }.addOnFailureListener {
                    Log.w("ItemsAdapter", "Failed to check owner block status for item ${item.id}. Assuming not blocked for safety.")
                    // In case of failure, rely on item.status and item.available checked earlier.
                    // isEffectivelyAvailable and overlayDisplayMessage would retain values from the item.status checks.
                    updateItemViewInteractivity(item, isEffectivelyAvailable, overlayDisplayMessage, onItemClicked)
                }
            } else {
                // If item is already determined unavailable by its own status, or no userId, apply UI directly
                updateItemViewInteractivity(item, isEffectivelyAvailable, overlayDisplayMessage, onItemClicked)
            }
        }

        // Helper function to apply UI changes based on final availability
        private fun updateItemViewInteractivity(
            item: Item,
            isEffectivelyAvailable: Boolean,
            overlayMsg: String?,
            onItemClicked: (Item) -> Unit
        ) {
            if (isEffectivelyAvailable) {
                itemView.alpha = 1.0f
                itemView.isClickable = true
                itemView.isFocusable = true
                itemView.setOnClickListener { onItemClicked(item) }
                unavailableOverlay?.visibility = View.GONE
            } else {
                // If alpha was already set for specific statuses, this might override it.
                // Consider if you want specific dimming for temp vs perm vs generic unavailable.
                // For simplicity here, using a general 0.5f if not already dimmer.
                if (itemView.alpha > 0.6f) itemView.alpha = 0.5f

                itemView.isClickable = false
                itemView.isFocusable = false
                itemView.setOnClickListener(null)
                unavailableOverlay?.visibility = View.VISIBLE
                unavailableOverlay?.text = overlayMsg ?: "UNAVAILABLE" // Default message
            }
        }


    }
}








//package com.example.signuplogina
//
//import android.graphics.Color
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.google.android.material.button.MaterialButton
//import android.util.Log
//
//class ItemsAdapter(
//    private var items: MutableList<Item>,
//    private val onItemClicked: (Item) -> Unit,
//    private val onBidClicked: (Item) -> Unit,
//    private val useLatestLayout: Boolean = false // 👈 this determines which XML to load
//
//) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {
//
//    fun updateItems(newItems: List<Item>) {
//        Log.d("AdapterDebug", "Updating items with ${newItems.size} items")
//        items.clear()  // Clear existing items
//        items.addAll(newItems)  // Add new items
//        notifyDataSetChanged()  // Notify adapter
//    }
//
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//
////        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
////        return ViewHolder(view)
//        val layoutRes = if (useLatestLayout) R.layout.latest_item else R.layout.item_product
//        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = items[position]
//        holder.bind(item, onItemClicked, onBidClicked)
//    }
//
//    override fun getItemCount(): Int = items.size
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val productName: TextView = itemView.findViewById(R.id.product_name)
//        private val productImage: ImageView = itemView.findViewById(R.id.product_image)
//        private val conditionBadge :TextView=itemView.findViewById(R.id.product_condition)
// //       private val bidButton: MaterialButton = itemView.findViewById(R.id.bid_button) // Ensure this exists in XML
//
//        fun bind(item: Item, onItemClicked: (Item) -> Unit, onBidClicked: (Item) -> Unit) {
//            productName.text = item.details?.productName ?: "Unknown Product"
//            Glide.with(itemView.context)
//                .load(item.details?.imageUrls?.firstOrNull() ?: "")
//                .placeholder(R.drawable.ic_photos)
//                .error(R.drawable.baseline_error_24)
//                .into(productImage)
//           val condition=item.details?.condition
//            when (condition) {
//                "Good" -> conditionBadge.setBackgroundColor(Color.parseColor("#A3D9A5")) // light green
//                "Used" -> conditionBadge.setBackgroundColor(Color.parseColor("#A5C8E1")) // soft blue
//                "Fair" -> conditionBadge.setBackgroundColor(Color.parseColor("#F7C59F")) // soft orange
//            }
//            conditionBadge.text=item.details?.condition
//
//            itemView.setOnClickListener { onItemClicked(item) }
//
//     //       bidButton.setOnClickListener {
//     //           onBidClicked(item) // Notify callback to place a bid
//       //     }
//        }
//    }
//}
