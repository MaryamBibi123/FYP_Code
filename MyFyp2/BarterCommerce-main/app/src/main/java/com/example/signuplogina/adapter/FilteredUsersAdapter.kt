package com.example.signuplogina.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.User
import com.example.signuplogina.databinding.ShowUserBinding
import com.example.signuplogina.modal.Users

class FilteredUsersAdapter(private var users: List<User>,
                           private val onUserClicked: (User) -> Unit
) :
    RecyclerView.Adapter<FilteredUsersAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: ShowUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ShowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)

    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        with(holder.binding) {
            textUserName.text = user.fullName
            ratingBar.rating = user.ratings?.averageRating ?: 0f
            Glide.with(imageUserProfile.context)
                .load(user.imageUrl)
                .placeholder(R.drawable.default_profile_image)
                .into(imageUserProfile)
        }
       holder.itemView.setOnClickListener {
           onUserClicked(user)

       }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers// mau have error ,
        notifyDataSetChanged()
    }
}
