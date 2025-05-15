package com.example.signuplogina

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminItemsActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemCardAdapter
    private var itemsList = mutableListOf<ItemCard>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_items)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val status = intent.getStringExtra("STATUS") ?: "pending"
        fetchItems(status)
    }

    private fun fetchItems(status: String) {
        db.collection("items").whereEqualTo("status", status)
            .get()
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
