package com.example.signuplogina.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.User
import com.example.signuplogina.databinding.AdminUsersBinding
import com.example.signuplogina.modal.UserRatingStats
import java.util.concurrent.TimeUnit

class AdminUserAdapter(
    private val onSeeDetailsClicked: (User) -> Unit,
    private val onBlockUserClicked: (User) -> Unit
) : ListAdapter<User, AdminUserAdapter.UserViewHolder>(UserDiffCallback()) {

    inner class UserViewHolder(private val binding: AdminUsersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(user: User) {


            binding.apply {
                if(user.statusByAdmin=="blocked"){
                    BlockUserText.text = "Blocked"
                    BlockUserText.setTextColor(itemView.context.getColor(R.color.black))
                    blockUserButton.isEnabled = false
                }
                userFullName.text = user.fullName ?: "Unknown User"
                userExchanges.text = "Exchanges: ${user.ratings?.totalExchanges ?: 0}"
                userRating.text = "Rating: ${String.format("%.1f", user.ratings?.averageRating ?: 0f)}"
                user.ratings?.successRate?.let {

                    if(it>0.0){
                        userSuccessRate.text = "Success: ${user.ratings?.successRate}%"}
                    else{
                        userSuccessRate.text = "Success: 0%"
                    }
                }
                lastUpdated.text = getLastSeen(user.ratings?.lastExchangeTimestamp ?: 0L)

                // Load profile image
                Glide.with(userProfilePic.context)
                    .load(user.imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(userProfilePic)

                seeDetailsIcon.setOnClickListener { onSeeDetailsClicked(user) }
                blockUserButton.setOnClickListener { onBlockUserClicked(user) }
            }
        }

        private fun getLastSeen(timestamp: Long): String {
            if (timestamp == 0L) return "No recent activity"
            val now = System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(now - timestamp)
            return when (days) {
                0L -> "active today"
                1L -> "active yesterday"
                else -> "active $days days ago"
            }
        }



        private fun calculateSuccessRate(ratingStats: UserRatingStats?): Int {
            if (ratingStats == null || ratingStats.totalBids == 0) return 0
            return (ratingStats.totalExchanges * 100) / ratingStats.totalBids
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = AdminUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }


    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))

    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.userid == newItem.userid // Assuming UID uniquely identifies a user
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}
