package com.example.signuplogina.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.example.signuplogina.R
import com.example.signuplogina.databinding.BidSelectableBinding
import com.example.signuplogina.mvvm.BidRepository
class SelectableBidsAdapter(
    private val onSelectionChanged: (List<Item>) -> Unit
) : RecyclerView.Adapter<SelectableBidsAdapter.BidViewHolder>() {

    private var bidsList = listOf<Bid>()
    private var offeredItemsList = listOf<Item>()
    private val selectedItems = mutableListOf<Item>() // Track selected items
    private val selectedBidIds = mutableListOf<String>() // Track selected items
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION  // <- Add this line


    inner class BidViewHolder(private val binding: BidSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bid: Bid, offeredItem: Item?, isSelected:Boolean) {

            offeredItem?.let { item ->
                binding.offeredItem.text = "Item: ${item.details.productName}"
                val imageUrl = item.details.imageUrls.firstOrNull()
                if (imageUrl != null) {
                    Glide.with(binding.offeredImage.context)
                        .load(imageUrl)
                        .into(binding.offeredImage)
                }
                if (isSelected) {
                    binding.ivCheck.visibility = View.VISIBLE
                    binding.cardBid.strokeWidth = 4 // optional - show border
                    binding.cardBid.strokeColor = ContextCompat.getColor(binding.root.context, R.color.light_blue)
                } else {
                    binding.ivCheck.visibility = View.GONE
                    binding.cardBid.strokeWidth = 0
                }
                // Highlight if selected
                binding.root.isSelected = selectedItems.contains(item)

                binding.root.setOnClickListener {
                    if (selectedItems.contains(item)) {
                        selectedItems.remove(item)
                        selectedBidIds.remove(bid.bidId) // Also remove bidId

                        binding.root.isSelected = false
                    } else {
                        if (selectedItems.size < 2) { // Allow max 2
                            selectedItems.add(item)
                            selectedBidIds.add(bid.bidId) // Add bidId

                            binding.root.isSelected = true
                        }
                    }
                    onSelectionChanged(selectedItems) // Notify selection changed
                }
            } ?: run {
                binding.offeredItem.text = "Offered Item: (Loading...)"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        val binding = BidSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BidViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val bid = bidsList[position]
        val offeredItem = offeredItemsList.getOrNull(position)
        holder.bind(bid, offeredItem,position == selectedItemPosition)
    }

    override fun getItemCount(): Int = bidsList.size

    fun submitLists(newBids: List<Bid>, newOfferedItems: List<Item>) {
        bidsList = newBids
        offeredItemsList = newOfferedItems
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<Pair<Item, String>> {
        return selectedItems.zip(selectedBidIds) { item, bidId -> Pair(item, bidId) }
    }
//    fun getSelectedItems(): List<Item> = selectedItems
}



