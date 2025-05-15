package com.example.signuplogina

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var itemsAdapter: ItemsAdapter
    private lateinit var database: DatabaseReference

    private val itemsList = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Initialize RecyclerView and EditText
        recyclerView = view.findViewById(R.id.trendingRecyclerView)
        searchInput = view.findViewById(R.id.searchEditText)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("Items")

        setupRecyclerView()
        loadAllItems()
        setupSearchListener()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2) // Set grid layout
        itemsAdapter = ItemsAdapter(itemsList,
            onItemClicked={item->navigateToItemDetailsFragment(item)}
        , ::onLikeClicked,false) // Initialize adapter
        recyclerView.adapter = itemsAdapter
    }

    private fun loadAllItems() {
        // Clear the current list to avoid duplication
        itemsList.clear()

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val item = userSnapshot.getValue(Item::class.java)
                    if (item != null) {
                        itemsList.add(item)
                    }
                }

                // Notify adapter about the new data
                itemsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching items: ${error.message}", error.toException())
            }
        })
    }

    private fun setupSearchListener() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadAllItems() // Show all items when the search box is empty
                } else {
                    searchItems(query) // Filter items based on search
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchItems(query: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                itemsList.clear() // Clear previous data to only show search results

                for (userSnapshot in snapshot.children) {
                    val item = userSnapshot.getValue(Item::class.java)

                    if (item != null) {
                        // Check if the query matches any field
                        if (item.details?.productName?.contains(query, ignoreCase = true) == true ||
                            item.details?.description?.contains(query, ignoreCase = true) == true ||
                            item.details?.category?.contains(query, ignoreCase = true) == true ||
                            item.details?.condition?.contains(query, ignoreCase = true) == true
                        ) {
                            itemsList.add(item)
                        }
                    }
                }

                // Notify the adapter to update the RecyclerView
                itemsAdapter.notifyDataSetChanged()

                // If no matching items are found, notify the user
                if (itemsList.isEmpty()) {
                    Toast.makeText(requireContext(), "No items found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Search error: ${error.message}", error.toException())
            }
        })
    }

    private fun onItemClicked(item: Item) {
        navigateToItemDetailsFragment(item)
    }

    private fun onLikeClicked(item: Item) {
//        saveLikedItemToFirebase(item)
    }

    private fun navigateToItemDetailsFragment(item: Item) {
        val bundle = Bundle().apply {
            putParcelable("ITEM_DETAILS", item)
            putBoolean("IS_FROM_MY_LISTINGS", false) // Indicates navigation from Home

        }
        // Navigate to ItemDetailsFragment using NavController
        findNavController().navigate(R.id.action_searchFragment_to_itemDetailsFragment, bundle)
    }

    private fun saveLikedItemToFirebase(item: Item) {
        val userId = Utils.getUidLoggedIn() // Replace with the actual user ID
        val likedItemsRef = FirebaseDatabase.getInstance()
            .getReference("LikedItems")
            .child(userId)

        likedItemsRef.child(item.userId).setValue(item) // Use unique item ID
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "${item.details?.productName} added to liked items!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to like item: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}