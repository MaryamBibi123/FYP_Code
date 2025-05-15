package com.example.signuplogina

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ItemCardAdapter(private val items: List<ItemCard>) :
    RecyclerView.Adapter<ItemCardAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.itemName.text = item.productName
        holder.itemStatus.text = "Status: ${item.status}"
        holder.itemCategory.text = "Category: ${item.category}"
        holder.itemCondition.text = "Condition: ${item.condition}"
        holder.itemAvailability.text = "Availability: ${item.availability}"
        holder.itemBids.text = "Bids: ${item.bidsCount}"

        // Load image using Glide
        Glide.with(holder.itemView.context).load(item.imageUrl).into(holder.itemImage)

        holder.btnApprove.setOnClickListener { updateItemStatus(item.id, "approved") }
        holder.btnReject.setOnClickListener { updateItemStatus(item.id, "rejected") }
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemImage: ImageView = view.findViewById(R.id.item_image)
        val itemName: TextView = view.findViewById(R.id.item_name)
        val itemStatus: TextView = view.findViewById(R.id.item_status)
        val itemCategory: TextView = view.findViewById(R.id.item_category)
        val itemCondition: TextView = view.findViewById(R.id.item_condition)
        val itemAvailability: TextView = view.findViewById(R.id.item_availability)
        val itemBids: TextView = view.findViewById(R.id.item_bids)
        val btnApprove: Button = view.findViewById(R.id.btn_approve)
        val btnReject: Button = view.findViewById(R.id.btn_reject)
    }

    private fun updateItemStatus(itemId: String, status: String) {
        FirebaseFirestore.getInstance().collection("items").document(itemId)
            .update("status", status)
    }
}
