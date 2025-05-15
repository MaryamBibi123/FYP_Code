



package com.example.signuplogina.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R // Import R for resources
import com.example.signuplogina.Utils
// Import the *correct* binding class for your improved layout
import com.example.signuplogina.databinding.ItemTradeBidBinding // <<< CHANGE THIS IMPORT
import com.example.signuplogina.mvvm.BidModel
import com.example.signuplogina.mvvm.PollViewModel

// Pass ViewModel via constructor
class TradeBidsAdapter(
) : ListAdapter<BidModel, TradeBidsAdapter.BidViewHolder>(BidDiffCallback()) {
        private lateinit var bidsViewModel: PollViewModel

    // It's generally better to pass these via constructor too,
    // but we'll keep the setter as per your original code.
    // Be cautious about ensuring this is called *before* submitList.
    private lateinit var battleId: String
    private lateinit var listerId: String

    fun setBattleInfo(battleId: String, listerId: String) {
        this.battleId = battleId
        this.listerId = listerId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        // Inflate the IMPROVED layout binding
        val binding = ItemTradeBidBinding.inflate( // <<< USE IMPROVED BINDING
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        bidsViewModel=PollViewModel()
        // Pass the single ViewModel instance down to the ViewHolder
        return BidViewHolder(binding, bidsViewModel)
    }

    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val bidItem = getItem(position)

        // Crucial Check: Ensure battleId and listerId are set before binding
        if (!::battleId.isInitialized || !::listerId.isInitialized) {
            Log.e("TradeBidsAdapter", "Error: battleId or listerId not initialized before binding view!")
            // Optionally, you could hide the view or show an error state
            return // Avoid binding with uninitialized data
        }

        // Pass battleId and listerId to the bind function
        holder.bind(bidItem, battleId, listerId)

        // --- Handle Button Enabling/Disabling ---
        val currentUser = Utils.getUidLoggedIn()
        val isListerViewing = (listerId == currentUser)

        // Disable voting buttons if the current user is the poll lister
        holder.binding.fairButton.isEnabled = !isListerViewing
        holder.binding.goodButton.isEnabled = !isListerViewing
        holder.binding.bestButton.isEnabled = !isListerViewing

        // Optional: Add visual cue if buttons are disabled (e.g., lower alpha)
        val alphaValue = if (isListerViewing) 0.5f else 1.0f
        holder.binding.fairButton.alpha = alphaValue
        holder.binding.goodButton.alpha = alphaValue
        holder.binding.bestButton.alpha = alphaValue
        // --- End Button Enabling/Disabling ---
    }


    // --- ViewHolder ---
    inner class BidViewHolder(
        val binding: ItemTradeBidBinding, // <<< USE IMPROVED BINDING
        private val viewModel: PollViewModel // Receive ViewModel instance
    ) : RecyclerView.ViewHolder(binding.root) {

        // Receive battleId and listerId here to pass them to handleRatingChange
        fun bind(bid: BidModel, battleId: String, listerId: String) {
            val context = itemView.context // Get context once for resources

            with(binding) {
                // Bind existing views
                offerText.text = bid.offer
                userIdText.text = context.getString(R.string.bidder_prefix, bid.userId ?: "Unknown User") // Handle potential null
                pointsText.text = context.getString(R.string.total_points_format, bid.totalPoints)

                // Bind NEW count TextViews using string resources for formatting
                fairCountText.text = context.getString(R.string.fair_count_format, bid.ratings.fair)
                goodCountText.text = context.getString(R.string.good_count_format, bid.ratings.good)
                bestCountText.text = context.getString(R.string.best_count_format, bid.ratings.best)

                // Load image
                Glide.with(bidImage.context)
                    .load(bid.imageUrl)
                    .placeholder(R.drawable.image_background) // Use your placeholder
                    .error(R.drawable.image_background)       // Use your error drawable
                    .into(bidImage)

                // --- Click Listeners ---
                // Call handleRatingChange, passing necessary IDs and the rating value
                fairButton.setOnClickListener {
                    handleRatingChange(battleId, listerId, bid, 1)
                    // DO NOT update UI directly here!
                }

                goodButton.setOnClickListener {
                    handleRatingChange(battleId, listerId, bid, 2)
                    // DO NOT update UI directly here!
                }

                bestButton.setOnClickListener {
                    handleRatingChange(battleId, listerId, bid, 3)
                    // DO NOT update UI directly here!
                }
            }
        }

        // This function now only triggers the ViewModel action
        private fun handleRatingChange(battleId: String, listerId: String, bid: BidModel, newRating: Int) {
            // Call the ViewModel function - ensure its signature matches
            // Note: Passing the whole 'bid' object can sometimes be tricky with async updates.
            // Passing only IDs (battleId, bid.offeredId, newRating) might be safer if
            // the ViewModel fetches fresh data before updating.
            viewModel.handleRatingChange(listerId, battleId, bid, bid.offeredId, newRating)

            // IMPORTANT: Removed direct UI update:
            // binding.pointsText.text = "Total Points: ${bid.totalPoints}"
            // The UI will update automatically when the ListAdapter receives the new list.
        }
    }


    // --- DiffCallback ---
    // Use the unique ID of the *bid* (offeredId) for areItemsTheSame
    class BidDiffCallback : DiffUtil.ItemCallback<BidModel>() {
        override fun areItemsTheSame(oldItem: BidModel, newItem: BidModel): Boolean {
            return oldItem.offeredId == newItem.offeredId // <<< USE UNIQUE BID ID
        }

        override fun areContentsTheSame(oldItem: BidModel, newItem: BidModel): Boolean {
            // The default data class equals checks all properties.
            // This is usually fine, but ensure your Ratings class also implements equals correctly
            // if it's not a data class or if you need custom comparison logic.
            return oldItem == newItem
        }
    }
}















//package com.example.signuplogina.adapter
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.signuplogina.Utils
//import com.example.signuplogina.databinding.ItemTradeBidBinding
//import com.example.signuplogina.mvvm.BidModel
//import com.example.signuplogina.mvvm.PollViewModel
//import com.example.signuplogina.mvvm.Vote
//
//
//class TradeBidsAdapter : ListAdapter<BidModel, TradeBidsAdapter.BidViewHolder>(BidDiffCallback()) {
//
//    private lateinit var bidsViewModel: PollViewModel
//    private lateinit var battleId: String
//    private lateinit var listerId: String
//
//    fun setBattleId(battleId: String,listerId: String) {
//        this.battleId = battleId
//        this.listerId=listerId
//    }
//
//    inner class BidViewHolder(val binding: ItemTradeBidBinding) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(bid: BidModel, battleId: String) {
//            with(binding) {
//                offerText.text = bid.offer
//                userIdText.text = "by ${bid.userId}"
//                pointsText.text = "Total Points: ${bid.totalPoints}"
//
//                Glide.with(bidImage.context)
//                    .load(bid.imageUrl)
//                    .into(binding.bidImage)
//
//                fairButton.setOnClickListener {
//                    handleRatingChange(battleId, bid, 1)
//                }
//
//                goodButton.setOnClickListener {
//                    handleRatingChange(battleId, bid, 2)
//                }
//
//                bestButton.setOnClickListener {
//                    handleRatingChange(battleId, bid, 3)
//                }
//            }
//        }
//
//        private fun handleRatingChange(battleId: String, bid: BidModel, newRating: Int) {
//            bidsViewModel.handleRatingChange(listerId,battleId, bid, bid.offeredId, newRating)
//            binding.pointsText.text = "Total Points: ${bid.totalPoints}"
//        }
//    }
//
//    // DiffCallback for ListAdapter to optimize list updates
//    class BidDiffCallback : DiffUtil.ItemCallback<BidModel>() {
//        override fun areItemsTheSame(oldItem: BidModel, newItem: BidModel): Boolean {
//            return oldItem.userId == newItem.userId  // Assuming userId is unique
//        }
//
//        override fun areContentsTheSame(oldItem: BidModel, newItem: BidModel): Boolean {
//            return oldItem == newItem  // Compare the whole object for equality
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
//        val binding = ItemTradeBidBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        bidsViewModel = PollViewModel() // You should ideally inject this through ViewModelProvider
//
//        return BidViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
//        val bidItem = getItem(position)
//        holder.bind(bidItem,this.battleId)
//
//
//        if (listerId == Utils.getUidLoggedIn()) {
//
//            // Disable voting buttons
//            holder.binding.fairButton.isEnabled = false
//            holder.binding.goodButton.isEnabled = false
//            holder.binding.bestButton.isEnabled = false
//        } else {
//
//            // Enable voting buttons
//            holder.binding.fairButton.isEnabled = true
//            holder.binding.goodButton.isEnabled = true
//            holder.binding.bestButton.isEnabled = true
//        }
//    }
//}
//
//
//
//
