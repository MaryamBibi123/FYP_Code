package com.example.signuplogina

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class RejectedItemsFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemCardAdapter
    private var itemsList = mutableListOf<ItemCard>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_items, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchItems("rejected")
        return view
    }

    private fun fetchItems(status: String) {
        db.collection("items").whereEqualTo("status", status).get()
            .addOnSuccessListener { documents ->
                itemsList.clear()
                for (document in documents) {
                    val item = document.toObject(ItemCard::class.java).apply {
                        id = document.id
                    }
                    itemsList.add(item)
                }
                adapter = ItemCardAdapter(itemsList)
                recyclerView.adapter = adapter
            }
    }
}
