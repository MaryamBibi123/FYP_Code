package com.example.signuplogina.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.modal.PollModel
import com.example.signuplogina.databinding.FragmentShowGeneralPoolBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.R
import androidx.core.graphics.toColorInt

class PollAdapter(
    private var pollList: List<PollModel>,
    private val onPollClick: (PollModel) -> Unit,
    private val onOptionSelected: (PollModel, Int) -> Unit, // NEW CALLBACK
    private val currentUserId: String, // 👈 Add this


) : RecyclerView.Adapter<PollAdapter.PollViewHolder>() {

    inner class PollViewHolder(val binding: FragmentShowGeneralPoolBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(poll: PollModel) {
            // Set poll question
            binding.pollQuestionText.text = poll.question

            // Calculate percentages for each option
            val totalVotes = poll.totalVotes.coerceAtLeast(1) // Avoid division by zero
            val option1Percentage = (poll.votesOption1 * 100) / totalVotes
            val option2Percentage = (poll.votesOption2 * 100) / totalVotes
            val option3Percentage = (poll.votesOption3 * 100) / totalVotes

            // Update progress bars and percentages for each option
            binding.option1ProgressBar.progress = option1Percentage
            binding.option2ProgressBar.progress = option2Percentage
            binding.option3ProgressBar.progress = option3Percentage

            binding.option1Percentage.text = "$option1Percentage%"
            binding.option2Percentage.text = "$option2Percentage%"
            binding.option3Percentage.text = "$option3Percentage%"

            // Set option names
            binding.option1RadioButton.text = poll.option1
            binding.option2RadioButton.text = poll.option2
            binding.option3RadioButton.text = poll.option3

            // Set footer text with total votes and remaining time
            binding.pollFooterText.text =
                "${poll.totalVotes} votes • ${timeLeft(poll.timestamp)} left"


            // Handle radio button clicks
            binding.option1RadioButton.setOnClickListener {
                onOptionSelected(poll, 1)
            }
            binding.option2RadioButton.setOnClickListener {
                onOptionSelected(poll, 2)
            }
            binding.option3RadioButton.setOnClickListener {
                onOptionSelected(poll, 3)
            }

            // Handle item click
            binding.root.setOnClickListener {
                onPollClick(poll)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        val binding = FragmentShowGeneralPoolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        holder.bind(pollList[position])

        val poll=pollList[position]


//        if (poll.userId == currentUserId) {
//            // Highlight this item (e.g. change background color or border)
//            holder.itemView.setBackgroundColor("#FFEFD5".toColorInt()) // Light orange/yellow
//        } else {
//            // Normal background
//            holder.itemView.setBackgroundColor(Color.WHITE)
//        }
//
//        val isMostVoted = position == 0 // already sorted by vote count
//        holder.binding.badge.text="Top Voted"
//        holder.binding.badge.visibility = if (isMostVoted) View.VISIBLE else View.GONE
//

        val isUploader = poll.userId == currentUserId

        if (isUploader) {
            holder.binding.badge.visibility = View.VISIBLE
        } else {
            holder.binding.badge.visibility = View.GONE
        }


    }


    override fun getItemCount(): Int = pollList.size

    fun updateList(newList: List<PollModel>) {
        pollList = newList
        notifyDataSetChanged()
    }

    private fun timeLeft(timestamp: Long): String {
        // 24 hours from creation time
        val expireTime = timestamp + (24 * 60 * 60 * 1000)
        val millisLeft = expireTime - System.currentTimeMillis()
        val hours = millisLeft / (1000 * 60 * 60)
        return "$hours hours"
    }
}
