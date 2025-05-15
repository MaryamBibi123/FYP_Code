package com.example.signuplogina

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R

class ImageAdapter(
    private val imageList: List<Any>, private val isFromAddItem: Boolean
) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (isFromAddItem) {
            R.layout.add_item  // This layout will be used from Add Item
        } else {
            R.layout.item_image_view // This layout for Item Details
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_image_view, parent, false)
//        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = imageList[position]

        if (item is Uri) {
            // Set URI (for Add Item Fragment)
            holder.imageView.setImageURI(item)
        } else if (item is String) {
            // Load image from URL using Glide (for Item Details Fragment)
            Glide.with(holder.itemView.context)
                .load(item)
                .placeholder(R.drawable.rounded_corners) // Add a placeholder image
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = imageList.size
}

