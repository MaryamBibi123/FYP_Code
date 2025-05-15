package com.example.signuplogina.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.User
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter : RecyclerView.Adapter<UserHolder>() {
    interface OnItemClickListener {
        fun onUserSelected(position: Int, users: User)
    }

    private var listOfUsers = listOf<User>()
    private var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.userlistitem, parent, false)
        return UserHolder(view)
        Log.d("NavigationDebug", "🟢 UserAdapter Loaded")

    }

    override fun getItemCount(): Int {
        return listOfUsers.size
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val users = listOfUsers[position]

        // Ensure username is not null
        val name = users.fullName?.split("\\s".toRegex())?.get(0) ?: "Unknown"
        holder.profileName.text = name

        // Set online/offline status
        if (users.status == "Online") {
            holder.statusImageView.setImageResource(R.drawable.onlinestatus)
        } else {
            holder.statusImageView.setImageResource(R.drawable.offlinestatus)
        }

        // Load user profile image using Glide
        Glide.with(holder.itemView.context)
            .load(users.imageUrl)
            .placeholder(R.drawable.ic_profile)  // Optional placeholder
            .error(R.drawable.baseline_error_24)  // Optional error image
            .into(holder.imageProfile)

        // Handle item click
        holder.itemView.setOnClickListener {
            listener?.onUserSelected(position, users)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setList(list: List<User>) {
        this.listOfUsers = list
        notifyDataSetChanged()
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
}

class UserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val profileName: TextView = itemView.findViewById(R.id.userName)
    val imageProfile: CircleImageView = itemView.findViewById(R.id.imageViewUser)
    val statusImageView: ImageView = itemView.findViewById(R.id.statusOnline)
}

