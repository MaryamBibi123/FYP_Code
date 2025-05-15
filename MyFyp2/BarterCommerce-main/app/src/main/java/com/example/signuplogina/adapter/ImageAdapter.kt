package com.example.signuplogina.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.databinding.ItemImageViewBinding // Create this simple layout

class ImageAdapter(private val images: List<String>, private val isEditable: Boolean) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(private val binding: ItemImageViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            Glide.with(binding.ivItemImage.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile) // Add appropriate placeholder
                .error(R.drawable.baseline_error_24) // Add appropriate error image
                .into(binding.ivItemImage)
            // Add click listener or other logic if needed for editable version
        }
    }
}

// Create res/layout/item_image.xml
/*
<?xml version="1.0" encoding="utf-8"?>
<ImageView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/imageView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scaleType="centerCrop"
    android:contentDescription="@string/product_image_desc"/>
*/