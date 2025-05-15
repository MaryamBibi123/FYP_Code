package com.example.signuplogina.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.Item
import com.example.signuplogina.R
// Import all necessary ViewBinding classes
import com.example.signuplogina.databinding.ApprovedItemBinding // For admin approved items (shows "Reject")
import com.example.signuplogina.databinding.RemovedItemBinding  // For admin rejected items (shows "Re-Approve") AND simple display
import com.example.signuplogina.databinding.PendingItemByAdminBinding // For admin pending items
//import com.example.signuplogina.databinding.ItemUserListingBinding // Assuming this is for user's own listings

class ItemListAdapter(
    private val currentDisplayStatus: String, // e.g., "pending", "approved", "rejected" - passed by Fragment
    private val isAdminView: Boolean,
    private val onItemClicked: (Item) -> Unit,
    // Admin actions (nullable as they only apply in admin view for specific statuses)
    private val onAdminApproveClicked: ((Item) -> Unit)? = null,
    private val onAdminRejectClicked: ((Item) -> Unit)? = null,
    private val onAdminReApproveClicked: ((Item) -> Unit)? = null,
    // User action (for user's own listings)
    private val onUserRemoveItemClicked: ((Item) -> Unit)? = null
) : ListAdapter<Item, RecyclerView.ViewHolder>(ItemDiffCallback()) {

    private val TAG = "ItemListAdapter"

    // --- VIEW TYPE CONSTANTS AND HELPER IN ONE COMPANION OBJECT ---
    companion object {
        private const val VIEW_TYPE_ADMIN_PENDING = 1
        private const val VIEW_TYPE_ADMIN_APPROVED = 2
        private const val VIEW_TYPE_ADMIN_REJECTED = 3
        private const val VIEW_TYPE_USER_OWN_LISTING = 4 // User's own item with a remove button
        private const val VIEW_TYPE_SIMPLE_DISPLAY = 5   // Generic display, no actions

        fun loadImage(imageView: ImageView, imageUrl: String?) {
            val placeholderDrawable = R.drawable.ic_camera // Ensure these drawables exist
            val errorDrawable = R.drawable.baseline_error_24

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(imageView.context)
                    .load(imageUrl)
                    .placeholder(placeholderDrawable)
                    .error(errorDrawable)
                    .centerCrop()
                    .into(imageView)
            } else {
                imageView.setImageResource(placeholderDrawable)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        if (isAdminView) {
            return when (currentDisplayStatus) { // Use currentDisplayStatus passed from Fragment
                UserDetailsPagerAdapter.STATUS_PENDING -> VIEW_TYPE_ADMIN_PENDING
                UserDetailsPagerAdapter.STATUS_APPROVED -> VIEW_TYPE_ADMIN_APPROVED
                UserDetailsPagerAdapter.STATUS_REJECTED -> VIEW_TYPE_ADMIN_REJECTED
                else -> {
                    Log.w(TAG, "AdminView: Unknown currentDisplayStatus '$currentDisplayStatus', falling back to simple display for item ${item.id}")
                    VIEW_TYPE_SIMPLE_DISPLAY
                }
            }
        } else {
            // User View (not admin)
            // Example: If it's the user's own approved item and they can remove it
            if (item.status == UserDetailsPagerAdapter.STATUS_APPROVED && onUserRemoveItemClicked != null) {
                // Here, you might need to check if 'item.userId' matches the logged-in user's ID
                // This would require passing currentUserId to the adapter or having fragment make this decision
                return VIEW_TYPE_USER_OWN_LISTING
            }
            return VIEW_TYPE_SIMPLE_DISPLAY // Default for user browsing items
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        Log.d(TAG, "onCreateViewHolder for viewType: $viewType")
        return when (viewType) {
            VIEW_TYPE_ADMIN_PENDING -> {
                val binding = PendingItemByAdminBinding.inflate(inflater, parent, false)
                PendingViewHolder(binding, onItemClicked, onAdminApproveClicked, onAdminRejectClicked)
            }
            VIEW_TYPE_ADMIN_APPROVED -> {
                val binding = ApprovedItemBinding.inflate(inflater, parent, false) // Uses btn_remove_item as "Reject"
                AdminApprovedViewHolder(binding, onItemClicked, onAdminRejectClicked)
            }
            VIEW_TYPE_ADMIN_REJECTED -> {
                val binding = RemovedItemBinding.inflate(inflater, parent, false) // Assumes btn_admin_re_approve exists here
                AdminRejectedViewHolder(binding, onItemClicked, onAdminReApproveClicked)
            }
            VIEW_TYPE_USER_OWN_LISTING -> {
                val binding = ApprovedItemBinding.inflate(inflater, parent, false) // Reusing, btn_remove_item is for user's removal
                UserOwnListingViewHolder(binding, onItemClicked, onUserRemoveItemClicked)
            }
            VIEW_TYPE_SIMPLE_DISPLAY -> {
                val binding = RemovedItemBinding.inflate(inflater, parent, false) // Basic layout without actions
                SimpleDisplayViewHolder(binding, onItemClicked)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d(TAG, "onBindViewHolder for pos $position, item: ${item.id}, viewType: ${holder.itemViewType}")
        when (holder) {
            is PendingViewHolder -> holder.bind(item)
            is AdminApprovedViewHolder -> holder.bind(item)
            is AdminRejectedViewHolder -> holder.bind(item)
            is UserOwnListingViewHolder -> holder.bind(item)
            is SimpleDisplayViewHolder -> holder.bind(item)
        }
    }

    // --- ViewHolder for Pending Items (Admin) ---
    class PendingViewHolder(
        private val binding: PendingItemByAdminBinding,
        private val onItemClicked: (Item) -> Unit,
        private val onApprove: ((Item) -> Unit)?,
        private val onReject: ((Item) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.productName.text = item.details.productName
            loadImage(binding.productImage, item.details.imageUrls.firstOrNull())
            binding.root.setOnClickListener { onItemClicked(item) }
            binding.btnAdminApprove.setOnClickListener { onApprove?.invoke(item) }
            binding.btnAdminReject.setOnClickListener { onReject?.invoke(item) }
        }
    }

    // --- ViewHolder for Approved Items (Admin can Reject) ---
    class AdminApprovedViewHolder(
        private val binding: ApprovedItemBinding, // Assumes btn_remove_item is used for Reject
        private val onItemClicked: (Item) -> Unit,
        private val onAdminReject: ((Item) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.productName.text = item.details.productName
            loadImage(binding.productImage, item.details.imageUrls.firstOrNull())
            binding.root.setOnClickListener { onItemClicked(item) }
//            binding.btnRemoveItem.text = "Reject" // Make sure ID is btn_remove_item
//            binding.btnRemoveItem.visibility = View.VISIBLE
//            binding.btnRemoveItem.setOnClickListener { onAdminReject?.invoke(item) }
        }
    }

    // --- ViewHolder for Rejected Items (Admin can Re-Approve) ---
    class AdminRejectedViewHolder(
        private val binding: RemovedItemBinding, // This layout needs a "Re-Approve" button
        private val onItemClicked: (Item) -> Unit,
        private val onAdminReApprove: ((Item) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        // **IMPORTANT**: Add a Button with id 'btn_admin_re_approve' to your removed_item.xml layout
//        private val btnReApprove: Button? = binding.root.findViewById(R.id.btn_admin_re_approve)

        fun bind(item: Item) {
            binding.productName.text = item.details.productName
            loadImage(binding.productImage, item.details.imageUrls.firstOrNull())
            binding.root.setOnClickListener { onItemClicked(item) }

//            if (btnReApprove == null) {
//                Log.e("ItemListAdapter", "btn_admin_re_approve not found in RemovedItemBinding layout!")
//            }
//            btnReApprove?.visibility = View.VISIBLE
//            btnReApprove?.text = "Re-Approve"
//            btnReApprove?.setOnClickListener { onAdminReApprove?.invoke(item) }
        }
    }

    // --- ViewHolder for User's Own Listing (User can Remove) ---
    class UserOwnListingViewHolder(
        private val binding: ApprovedItemBinding, // Reusing approved_item layout for user's own remove
        private val onItemClicked: (Item) -> Unit,
        private val onUserRemove: ((Item) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.productName.text = item.details.productName
            loadImage(binding.productImage, item.details.imageUrls.firstOrNull())
            binding.root.setOnClickListener { onItemClicked(item) }
//            binding.btnRemoveItem.text = "Remove Listing" // Using btn_remove_item from approved_item.xml
//            binding.btnRemoveItem.visibility = View.VISIBLE
//            binding.btnRemoveItem.setOnClickListener { onUserRemove?.invoke(item) }
        }
    }

    // --- ViewHolder for Simple Display (No Actions) ---
    class SimpleDisplayViewHolder(
        private val binding: RemovedItemBinding, // Reusing layout without action buttons
        private val onItemClicked: (Item) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.productName.text = item.details.productName
            loadImage(binding.productImage, item.details.imageUrls.firstOrNull())
            binding.root.setOnClickListener { onItemClicked(item) }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem == newItem
    }
}





//package com.example.signuplogina.adapter
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.signuplogina.Item
//import com.example.signuplogina.R
//// Import ViewBinding classes for BOTH layouts
//import com.example.signuplogina.databinding.ApprovedItemBinding // Layout WITH remove button
//import com.example.signuplogina.databinding.RemovedItemBinding  // Layout WITHOUT remove button
//
//class ItemListAdapter(
//    private val isShowingRejected: Boolean, // Flag to determine layout type
//    private val onItemClicked: (Item) -> Unit,
//    private val onRemoveClicked: ((Item) -> Unit)? = null // Optional: Only needed for the layout with remove button
//) : ListAdapter<Item, RecyclerView.ViewHolder>(ItemDiffCallback()) { // Use generic RecyclerView.ViewHolder
//
//    // --- View Type Constants ---
//    companion object {
//        // Represents the view WITH the remove button (for Approved/Active items)
//        private const val VIEW_TYPE_WITH_REMOVE = 1
//
//        // Represents the view WITHOUT the remove button (for Rejected items)
//        private const val VIEW_TYPE_SIMPLE = 2
//    }
//
//    // --- Determine View Type ---
//    override fun getItemViewType(position: Int): Int {
//        // If showing rejected items, use the simple layout.
//        // Otherwise (showing non-rejected), use the layout with the remove button.
//        return if (isShowingRejected) VIEW_TYPE_SIMPLE else VIEW_TYPE_WITH_REMOVE
//    }
//
//    // --- Create ViewHolder based on View Type ---
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        val inflater = LayoutInflater.from(parent.context)
//        return when (viewType) {
//            // *** CORRECTED MAPPING ***
//            VIEW_TYPE_WITH_REMOVE -> { // For Approved/Active items
//                // Inflate the layout WITH the remove button
//                val binding = ApprovedItemBinding.inflate(inflater, parent, false)
//                // Use the ViewHolder that handles the remove button
//                ApprovedViewHolder(binding, onItemClicked, onRemoveClicked)
//            }
//            // *** CORRECTED MAPPING ***
//            VIEW_TYPE_SIMPLE -> { // For Rejected items
//                // Inflate the layout WITHOUT the remove button
//                val binding = RemovedItemBinding.inflate(inflater, parent, false)
//                // Use the ViewHolder for the simple layout
//                RejectedViewHolder(binding, onItemClicked)
//            }
//
//            else -> throw IllegalArgumentException("Invalid view type")
//        }
//    }
//
//    // --- Bind ViewHolder based on its actual type ---
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val item = getItem(position)
//        when (holder) {
//            is RejectedViewHolder -> holder.bind(item)
//            is ApprovedViewHolder -> holder.bind(item)
//        }
//    }
//
//    // --- ViewHolder for RemovedItemBinding (Simple View - for Rejected Items) ---
//    class RejectedViewHolder(
//        private val binding: RemovedItemBinding, // Correct Binding
//        private val onItemClicked: (Item) -> Unit
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(item: Item) {
//            binding.productName.text = item.details.productName
//
//            val imageUrl = item.details.imageUrls.firstOrNull()
//            // Suggestion: Use a generic placeholder, not ic_profile
//            val placeholderDrawable = R.drawable.ic_profile
//            val errorDrawable = R.drawable.baseline_error_24 // Or your error image
//
//            if (!imageUrl.isNullOrEmpty()) {
//                Glide.with(binding.productImage.context)
//                    .load(imageUrl)
//                    .placeholder(placeholderDrawable)
//                    .error(errorDrawable)
//                    .centerCrop()
//                    .into(binding.productImage)
//            } else {
//                binding.productImage.setImageResource(placeholderDrawable)
//            }
//
//            // Click listener for the whole item
//            binding.root.setOnClickListener {
//                onItemClicked(item)
//            }
//            // No remove button listener needed here
//        }
//    }
//
//    // --- ViewHolder for ApprovedItemBinding (View with Remove - for Active Items) ---
//    class ApprovedViewHolder(
//        private val binding: ApprovedItemBinding, // Correct Binding
//        private val onItemClicked: (Item) -> Unit,
//        private val onRemoveClicked: ((Item) -> Unit)?
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(item: Item) {
//            binding.productName.text = item.details.productName
//
//            val imageUrl = item.details.imageUrls.firstOrNull()
//            // Suggestion: Use a generic placeholder, not ic_profile
//            val placeholderDrawable = R.drawable.ic_profile
//            val errorDrawable = R.drawable.baseline_error_24 // Or your error image
//
//
//            if (!imageUrl.isNullOrEmpty()) {
//                Glide.with(binding.productImage.context)
//                    .load(imageUrl)
//                    .placeholder(placeholderDrawable)
//                    .error(errorDrawable)
//                    .centerCrop()
//                    .into(binding.productImage)
//            } else {
//                binding.productImage.setImageResource(placeholderDrawable)
//            }
//
//            // Click listener for the remove button
//            binding.btnRemoveItem.setOnClickListener {
//                onRemoveClicked?.invoke(item) // Safely invoke the remove callback if provided
//            }
//            // Click listener for the whole item
//            binding.root.setOnClickListener {
//                onItemClicked(item)
//            }
//        }
//    }
//
//    // --- DiffUtil remains the same ---
//    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
//        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
//            return oldItem.id == newItem.id
//        }
//
//        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
//            return oldItem == newItem
//        }
//    }
//}