package com.example.signuplogina.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.signuplogina.R

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.Item
//import R
import com.example.signuplogina.databinding.ItemSelectableBinding

class SelectableItemsAdapter(
    private val onItemSelected: (Item) -> Unit
) : RecyclerView.Adapter<SelectableItemsAdapter.ItemViewHolder>() {

    private var itemsList = listOf<Item>()
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION  // <- Add this line
//    private val selectedItems = mutableListOf<Item>() // Track selected items


    inner class ItemViewHolder(private val binding: ItemSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item,isSelected:Boolean) {
            binding.productName.text = item.details.productName

            // Optionally you can load image if you want
            val imageUrl = item.details.imageUrls.firstOrNull()
            if (imageUrl != null) {
                // Glide or Picasso can be used
                // Example with Glide:
                 Glide.with(binding.productImage.context).load(imageUrl).into(binding.productImage)
            }

            if (isSelected) {
                binding.ivCheck.visibility = View.VISIBLE
                binding.cardItem.strokeWidth = 4 // optional - show border
                binding.cardItem.strokeColor = ContextCompat.getColor(binding.root.context, R.color.light_blue)
            } else {
                binding.ivCheck.visibility = View.GONE
                binding.cardItem.strokeWidth = 0
            }

            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedItemPosition
                    selectedItemPosition = adapterPosition
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedItemPosition)

                    onItemSelected(item)
                }
//                onItemSelected(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemsList[position], position == selectedItemPosition)
    }

    override fun getItemCount(): Int = itemsList.size

    fun submitList(newList: List<Item>) {
        itemsList = newList
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<Item> {
        return if (selectedItemPosition != RecyclerView.NO_POSITION) {
            listOf(itemsList[selectedItemPosition])
        } else {
            emptyList()
        }
    }
}
