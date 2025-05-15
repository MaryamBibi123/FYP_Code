package com.example.signuplogina

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
// import android.widget.Toast // Not used here for limit notification
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.databinding.ItemBarterBinding // Your view binding class

class ProductAdapter(
    private val productList: List<Item>,
    val maxSelection: Int = 3,
    // Callback: (itemAttempted, currentSelectedItems, wasLimitReachedWhenTryingToAdd)
    private val onSelectionAttempt: (item: Item, selectedItems: List<Item>, limitReached: Boolean) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val selectedItems = mutableSetOf<Item>()

    inner class ProductViewHolder(private val binding: ItemBarterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Make the CheckBox purely visual, controlled ONLY by our logic
            binding.productCheckBox.isClickable = false
            binding.productCheckBox.isFocusable = false
        }

        fun bind(item: Item, position: Int) {
            val isSelected = selectedItems.contains(item)

            // Set checkbox state FIRST based on our managed set
            binding.productCheckBox.isChecked = isSelected

            item.details?.let { details ->
                binding.productNameTextView.text = details.productName
                binding.productDescriptionTextView.text = details.description

                if (details.imageUrls.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(details.imageUrls[0])
                        .placeholder(R.drawable.ic_photos)
                        .into(binding.productImageView)
                } else {
                    binding.productImageView.setImageResource(R.drawable.ic_photos)
                }

                // Handle clicks ONLY on the root view
                binding.root.setOnClickListener {
                    toggleSelection(item, position)
                }
                // Remove any separate listener from productCheckBox if it existed

                // Ensure checkbox enabled state reflects item state (if needed)
                binding.productCheckBox.isEnabled = true // Re-enable if previously disabled

            } ?: run {
                binding.productNameTextView.text = "No Details Available"
                binding.productDescriptionTextView.text = ""
                binding.productImageView.setImageResource(R.drawable.ic_photos)
                binding.productCheckBox.isChecked = false
                binding.productCheckBox.isEnabled = false // Keep disabled if no details
            }
        }
    }

    private fun toggleSelection(item: Item, position: Int) {
        var limitReachedThisToggle = false
        val itemWasAlreadySelected = selectedItems.contains(item)

        if (itemWasAlreadySelected) {
            selectedItems.remove(item)
            // We need to update the UI for the deselected item
            notifyItemChanged(position)
        } else {
            // Trying to add a new item
            if (selectedItems.size < maxSelection) {
                selectedItems.add(item)
                // We need to update the UI for the newly selected item
                notifyItemChanged(position)
            } else {
                // Limit reached! Do NOT add item to selectedItems.
                limitReachedThisToggle = true
                // Crucially, DO NOT call notifyItemChanged(position) here,
                // as the visual state of this item should not change (it remains unchecked).
            }
        }

        // Always call the callback to inform the fragment of the attempt and the result
        onSelectionAttempt(item, selectedItems.toList(), limitReachedThisToggle)
    }

    // --- onCreateViewHolder, getItemCount, getSelectedItems, setSelectedItems remain the same ---
    // --- Make sure onBindViewHolder calls holder.bind(productList[position], position) ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemBarterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position], position) // Pass position
    }

    override fun getItemCount() = productList.size

    fun getSelectedItems(): List<Item> {
        return selectedItems.toList()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedItems(itemsToSelect: List<Item>) {
        selectedItems.clear()
        selectedItems.addAll(itemsToSelect.take(maxSelection))
        notifyDataSetChanged()
        val representativeItem = itemsToSelect.firstOrNull() ?: Item()
        onSelectionAttempt(representativeItem, selectedItems.toList(), false)
    }
}



//package com.example.signuplogina
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.signuplogina.databinding.ItemBarterBinding
//
//class ProductAdapter(
//    private val productList: List<Item>,
//    private val onItemClick: (Item) -> Unit
//) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
//
//    private var selectedPosition = -1 // To track the selected position
//    private var selectedProduct: Item? = null // To store the selected product
//
//    inner class ProductViewHolder(private val binding: ItemBarterBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(item: Item, isSelected: Boolean) {
//            // Safely access product details through the 'details' property:
//            item.details?.let { details ->
//
//                // Bind product details to the view
//                binding.productNameTextView.text = details.productName
//                binding.productDescriptionTextView.text = details.description
//
//                // Load product image using Glide (use the first image URL for now)
//                if (details.imageUrls.isNotEmpty()) {
//                    Glide.with(binding.root.context)
//                        .load(details.imageUrls[0]) // Access first image URL
//                        .placeholder(R.drawable.ic_photos) // Placeholder image
//                        .into(binding.productImageView)
//                } else {
//                    // Handle the case where there are no images
//                    binding.productImageView.setImageResource(R.drawable.ic_photos) // Default image
//                }
//
//                // Update radio button based on selection state
//                binding.productRadioButton.isChecked = isSelected
//                binding.productRadioButton.setOnClickListener {
//                    updateSelection(adapterPosition, item)
//                }
//
//                // Allow selection by clicking the item itself
//                binding.root.setOnClickListener {
//                    updateSelection(adapterPosition, item)
//                }
//
//            } ?: run {
//                // Handle the case where details is null (display default values or hide views)
//                binding.productNameTextView.text = "No Details Available"
//                binding.productDescriptionTextView.text = ""
//                binding.productImageView.setImageResource(R.drawable.ic_photos) // Default image
//                binding.productRadioButton.isChecked = false // Disable selection
//            }
//        }
//    }
//
//    private fun updateSelection(position: Int, item: Item) {
//        val previousSelectedPosition = selectedPosition
//        selectedPosition = position
//        selectedProduct = item
//        notifyItemChanged(previousSelectedPosition)
//        notifyItemChanged(selectedPosition)
//        onItemClick(item) // Notify the fragment of the selected item
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
//        val binding = ItemBarterBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return ProductViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
//        val isSelected = position == selectedPosition
//        holder.bind(productList[position], isSelected)
//    }
//
//    override fun getItemCount() = productList.size
//
//    fun getSelectedProduct(): Item? {
//        return selectedProduct
//    }
//}