package com.example.signuplogina.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.Utils
import com.example.signuplogina.databinding.ItemTradeBinding
import com.example.signuplogina.mvvm.ItemsRepo
import com.example.signuplogina.mvvm.TradeBattleModel
import android.util.Log
class TradeBattleAdapter : RecyclerView.Adapter<TradeBattleAdapter.TradeViewHolder>() {

    private var tradeList = listOf<TradeBattleModel>()
    private val itemsRepo = ItemsRepo()  // ✅ Create ONCE here
    private var battleId: String = ""


    fun setBattleId(battleId: String) {
        this.battleId = battleId
    }

    inner class TradeViewHolder(val binding: ItemTradeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val bidsAdapter = TradeBidsAdapter() // ✅ Create ONCE per ViewHolder



        init {
            binding.bidsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.bidsRecyclerView.adapter = bidsAdapter
        }

        fun bind(tradeItem: TradeBattleModel) {
            // Load Main Item Details
            itemsRepo.getItemById(tradeItem.itemId) { mainItem ->
                binding.itemTitle.text = mainItem?.details?.productName
                binding.itemDescription.text = mainItem?.details?.description

                Glide.with(binding.itemImage.context)
                    .load(mainItem?.details?.imageUrls?.firstOrNull())
                    .into(binding.itemImage)
            }

            bidsAdapter.setBattleInfo(tradeItem.battleId,tradeItem.listerUid)

            // Load Bids (Map → List)
            bidsAdapter.submitList(tradeItem.bids?.values?.toList())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeViewHolder {
        val binding = ItemTradeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TradeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TradeViewHolder, position: Int) {
        val tradeItem = tradeList[position]
        holder.bind(tradeItem)

        // Check if poll is complete
        if (isPollComplete(tradeItem)) {
            // Disable voting buttons or any other views
            holder.binding.listerItemSection.isEnabled = false
            holder.binding.bidsRecyclerView.isEnabled = false
            // You can also dim the item to visually indicate it's disabled
            holder.itemView.alpha = 0.5f
        } else {
            holder.binding.listerItemSection.isEnabled = true
            holder.binding.bidsRecyclerView.isEnabled = true
            holder.itemView.alpha = 1f
        }


        holder.binding.menuButton.setOnClickListener {
            val context = holder.itemView.context
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Poll Closed")

            builder.setMessage("Winner: John Doe\nPoll is closed.")
            builder.setPositiveButton("OK", null)
            builder.show()
        }

        if (tradeItem.listerUid == Utils.getUidLoggedIn()) {
            holder.binding.yourPollTag.visibility = View.VISIBLE

            // Disable voting buttons
        } else {
            holder.binding.yourPollTag.visibility = View.GONE

            // Enable voting buttons

        }
    }

    private fun isPollComplete(model: TradeBattleModel): Boolean {
        val status= System.currentTimeMillis()>=model.endsAt
        Log.d("The poll is completed","$status")

        return System.currentTimeMillis() >= model.endsAt
    }


    override fun getItemCount() = tradeList.size

    fun submitList(list: List<TradeBattleModel>) {
        tradeList = list

        notifyDataSetChanged()
    }
}
