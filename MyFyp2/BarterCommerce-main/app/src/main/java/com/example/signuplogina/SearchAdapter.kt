package com.example.signuplogina

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(private var searchResults: MutableList<String>) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultTextView: TextView = itemView.findViewById(R.id.searchEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result,parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.resultTextView.text = searchResults[position]

    }

    override fun getItemCount(): Int {
        return searchResults.size
    }

    fun updateData(newResults: List<String>){
        searchResults.clear()
        searchResults.addAll(newResults)
        notifyDataSetChanged()
    }

}
