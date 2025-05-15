package com.example.signuplogina.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.Item
import com.example.signuplogina.R

class OfferedItemsViewPagerAdapter(
    private var items: List<Item> = listOf()
    // No click listener here, the fragment will handle clicks on "View Full Details" button
) : RecyclerView.Adapter<OfferedItemsViewPagerAdapter.OfferedItemViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferedItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offered_pager_card, parent, false) // Use the new card layout
        return OfferedItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferedItemViewHolder, position: Int) {
        if (items.isNotEmpty() && position < items.size) {
            holder.bind(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    inner class OfferedItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImageView: ImageView = itemView.findViewById(R.id.ivOfferedItemPageImage)
        private val itemNameTextView: TextView = itemView.findViewById(R.id.tvOfferedItemPageName)

        fun bind(item: Item) {
            itemNameTextView.text = item.details.productName

            if (item.details.imageUrls.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.details.imageUrls[0]) // Show the first image
                    .placeholder(R.drawable.box)
                    .error(R.drawable.baseline_error_24)
                    .centerInside() // Or centerCrop, depending on desired look
                    .into(itemImageView)
            } else {
                itemImageView.setImageResource(R.drawable.box)
            }
            // The click to go to ItemDetailsFragment will be handled by a button
            // in OfferedItemsDetailsFragment itself, not on the ViewPager page.
        }
    }
}