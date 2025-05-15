package com.example.signuplogina.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.modal.Story

class StoryAdapter(
    private val stories: List<Story>
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.storyImage)
        val offer: TextView = itemView.findViewById(R.id.storyOffer)
        val want: TextView = itemView.findViewById(R.id.storyWant)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        // Set offer and want text
        holder.offer.text = "Offering: ${story.offerText}"
        holder.want.text = "Wants: ${story.wantText}"

        // Handle null or missing story image URL
        if (story.storyImageUrl.isNullOrEmpty()) {
            holder.image.setImageResource(R.drawable.ic_camera)  // Placeholder image
        } else {
            Glide.with(holder.itemView.context)
                .load(story.storyImageUrl)
                .placeholder(R.drawable.ic_camera)  // Show a placeholder while loading
                .into(holder.image)
        }
    }

    override fun getItemCount(): Int = stories.size
}
