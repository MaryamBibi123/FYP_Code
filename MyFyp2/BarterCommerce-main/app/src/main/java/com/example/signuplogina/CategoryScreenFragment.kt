package com.example.signuplogina

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.signuplogina.databinding.FragmentCategoryScreenBinding
import com.google.firebase.database.*

class CategoryScreenFragment : Fragment() {

    private var _binding: FragmentCategoryScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemsAdapter: ItemsAdapter
    private lateinit var databaseRef: DatabaseReference
    private var valueEventListener: ValueEventListener? = null

    private var categoryName: String = "Unknown Category"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Safely get arguments using Bundle
        arguments?.let {
            categoryName = it.getString("CATEGORY_NAME", "Unknown Category")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.categoryTitle.text = categoryName

        binding.ivMenu.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.categoryItemsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        databaseRef = FirebaseDatabase.getInstance().getReference("Items")
        fetchItemsByCategory(categoryName)
    }

    private fun fetchItemsByCategory(category: String) {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val filteredItemsList = mutableListOf<Item>()

                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(Item::class.java)
                    if (item != null && item.details?.category == category) {
                        item.id = itemSnapshot.key ?: ""
                        filteredItemsList.add(item)
                    }
                }

                if (!::itemsAdapter.isInitialized) {
                    itemsAdapter = ItemsAdapter(
                        items = filteredItemsList,
                        onItemClicked = { item ->
//                            Log.d("CategoryScreen", "Item clicked: ${item.details?.productName}")
                            navigateToItemDetailsFragment(item)
                        },

                        onBidClicked = { item ->
                            Log.d("CategoryScreen", "Item liked: ${item.details?.productName}")
                        }
                    )
                    binding.categoryItemsRecyclerView.adapter = itemsAdapter
                } else {
                    itemsAdapter.updateItems(filteredItemsList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching items", error.toException())
            }
        }

        databaseRef.addValueEventListener(valueEventListener!!)
    }

    private fun navigateToItemDetailsFragment(item: Item) {
        // Prevent navigating to details if item is marked unavailable by the adapter logic
        // (Though adapter should disable click, this is an extra safeguard)
        if (!item.available || !item.status.equals("approved", ignoreCase = true)) {
            Toast.makeText(context, "This item is currently unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putParcelable("ITEM_DETAILS", item) // Pass the whole Item object
            putBoolean("IS_FROM_MY_LISTINGS", false)
        }
        findNavController().navigate(R.id.action_categoryScreenFragment_to_itemDetailsFragment, bundle)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { databaseRef.removeEventListener(it) }
        _binding = null
    }
}
