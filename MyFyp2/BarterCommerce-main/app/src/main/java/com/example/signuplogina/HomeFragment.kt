package com.example.signuplogina

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.ItemsAdapter
import com.example.signuplogina.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollPosition = 0
    private lateinit var autoScrollRunnable: Runnable

    private val databaseRef = FirebaseDatabase.getInstance().getReference("Items")
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Store current user ID

    // Adapters
    private lateinit var latestItemsAdapter: ItemsAdapter // For horizontal scroll
    private lateinit var productGridAdapter: ItemsAdapter // For main grid

    private val TAG = "HomeFragment" // Added TAG for logging

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupLatestItemsRecyclerView()
        setupProductGridRecyclerView() // Setup grid adapter

        setupCategoryClickListeners()

        // Fetch data
        fetchLatestItems() // Fetches APPROVED items for top scroll
        fetchUserCategoriesAndDisplayItems() // Fetches ALL category items for grid
    }

    private fun setupLatestItemsRecyclerView() {
        Log.d(TAG, "Setting up Latest Items RecyclerView")
        // Use useLatestLayout = true for the horizontal adapter
        latestItemsAdapter = ItemsAdapter(
            mutableListOf(),
            onItemClicked = { item -> navigateToItemDetailsFragment(item) },
            onBidClicked = { item -> saveLikedItemToFirebase(item) }, // Assuming this is for liking
            useLatestLayout = true // <-- Use the latest_item layout
        )
        binding.latestItemsRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = latestItemsAdapter
        }
    }

    private fun setupProductGridRecyclerView() {
        Log.d(TAG, "Setting up Product Grid RecyclerView")
        // Use useLatestLayout = false for the grid adapter
        productGridAdapter = ItemsAdapter(
            mutableListOf(),
            onItemClicked = { item -> navigateToItemDetailsFragment(item) },
            onBidClicked = { item -> saveLikedItemToFirebase(item) }, // Liking
            useLatestLayout = false // <-- Use the item_product layout (with overlay logic)
        )
        binding.productGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.productGrid.adapter = productGridAdapter
    }


    private fun fetchLatestItems() {
        Log.d(TAG, "Fetching latest items...")
        val currentTime = System.currentTimeMillis()
        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L

        // Fetch recent items, filter client-side for status/availability
        databaseRef.orderByChild("details/timestamp") // Order by nested timestamp
            .limitToLast(100) // Fetch a larger pool to increase chances of finding 5 approved ones
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val recentItems = mutableListOf<Item>()
                    val olderItems = mutableListOf<Item>()
                    Log.d(TAG, "Latest items onDataChange received ${snapshot.childrenCount} potential items")

                    for (itemSnapshot in snapshot.children) {
                        try {
                            // --- Parse full Item object ---
                            val item = itemSnapshot.getValue(Item::class.java)
                            if (item != null) {
                                item.id = itemSnapshot.key ?: "" // Assign the ID

                                // --- Filter for Latest Items RecyclerView ---
                                // 1. Exclude current user's items
                                // 2. Must be APPROVED
                                // 3. Must be AVAILABLE
                                if (item.userId != currentUserId &&
                                    item.status.equals("approved", ignoreCase = true) &&
                                    item.available
                                ) {
                                    // Check timestamp for recent/older categorization (using details timestamp)
                                    if (currentTime - item.details.timestamp <= oneWeekMillis) {
                                        recentItems.add(item)
                                    } else {
                                        olderItems.add(item)
                                    }
                                }
                                // --- End Filtering ---
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing item ${itemSnapshot.key} in latest items fetch", e)
                        }
                    }

                    // Shuffle approved recent items
                    recentItems.shuffle()

                    // Take up to 5 approved items, prioritizing recent ones
                    val finalList = if (recentItems.size >= 5) {
                        recentItems.take(5)
                    } else {
                        // Fill remaining spots with approved older items
                        val needed = 5 - recentItems.size
                        olderItems.shuffle() // Shuffle older approved items too
                        recentItems + olderItems.take(needed)
                    }

                    Log.d(TAG, "Filtered ${finalList.size} approved, available latest items for display")
                    if (_binding != null) { // Check binding is not null
                        latestItemsAdapter.updateItems(finalList)
                        startAutoScroll(finalList.size)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching latest items", error.toException())
                    Toast.makeText(context, "Failed to load latest items", Toast.LENGTH_SHORT).show()
                }
            })
    }


    // onResume fetch might cause unnecessary reloads if data hasn't changed.
    // Consider removing if not explicitly needed or add pull-to-refresh.
    /*
    override fun onResume() {
        super.onResume()
        fetchLatestItems()
    }
    */

    private fun setupCategoryClickListeners() {
        binding.categoryElectronics.setOnClickListener { navigateToCategory("Electronics") }
        binding.categoryClothing.setOnClickListener { navigateToCategory("Clothing") }
        binding.categoryBooks.setOnClickListener { navigateToCategory("Books") }
        binding.categoryCosmetics.setOnClickListener { navigateToCategory("Cosmetics") }
        binding.categoryVehicles.setOnClickListener { navigateToCategory("Vehicles") }
        binding.categoryFurniture.setOnClickListener { navigateToCategory("Furniture") }
    }

    private fun navigateToCategory(category: String) {
        val bundle = Bundle().apply { putString("CATEGORY_NAME", category) }
        findNavController().navigate(R.id.action_homeFragment_to_categoryScreenFragment, bundle)
    }


    private fun fetchUserCategoriesAndDisplayItems() {
        Log.d(TAG, "Fetching user categories...")
        currentUserId?.let { uid ->
            val userCategoriesRef =
                FirebaseDatabase.getInstance().getReference("Users").child(uid).child("category")

            userCategoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val selectedCategories =
                        snapshot.children.mapNotNull { it.getValue(String::class.java) }
                    Log.d(TAG, "User categories fetched: $selectedCategories")

                    if (selectedCategories.isNotEmpty()) {
                        fetchItemsByCategories(selectedCategories)
                    } else {
                        Log.d(TAG, "No categories selected for user $uid. Grid will be empty.")
                        if (_binding != null) {
                            productGridAdapter.updateItems(emptyList()) // Clear grid
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching user categories", error.toException())
                }
            })
        } ?: Log.w(TAG, "Cannot fetch categories, user not logged in.")
    }

    private fun fetchItemsByCategories(selectedCategories: List<String>) {
        Log.d(TAG, "Fetching items for categories: $selectedCategories")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryItemsList = mutableListOf<Item>() // Renamed for clarity
                Log.d(TAG, "Category items onDataChange received ${snapshot.childrenCount} potential items")

                for (itemSnapshot in snapshot.children) {
                    try {
                        // --- Parse full Item object ---
                        val item = itemSnapshot.getValue(Item::class.java)
                        if (item != null) {
                            item.id = itemSnapshot.key ?: "" // Set ID

                            // --- Filter for Category Grid ---
                            // 1. Match category
                            // 2. Exclude current user's items
                            // *** NO filter for status/availability here ***
                            if (selectedCategories.contains(item.details.category) &&
                                item.userId != currentUserId)
                            {
                                categoryItemsList.add(item)
                            }
                            // --- End Filtering ---
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing item ${itemSnapshot.key} in category fetch", e)
                    }
                }
                Log.d(TAG, "Filtered ${categoryItemsList.size} items for categories $selectedCategories (includes unavailable)")
                if (_binding != null) {
                    // Update the grid adapter with ALL fetched category items
                    // The adapter will handle showing "Unavailable" overlay
                    productGridAdapter.updateItems(categoryItemsList)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching category items", error.toException())
                Toast.makeText(requireContext(), "Failed to load category items", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Example like function - adjust as needed
    private fun saveLikedItemToFirebase(item: Item) {
        Log.d(TAG, "Liking item: ${item.id}")
        val uid = currentUserId ?: run {
            Toast.makeText(context, "Please login to like items", Toast.LENGTH_SHORT).show()
            return
        }
        val likedItemsRef = FirebaseDatabase.getInstance().getReference("LikedItems").child(uid)
        // Check if item already liked? Using item ID as key might be better for checking existence
        likedItemsRef.child(item.id).setValue(item) // Example: Store whole item, or just relevant fields
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "${item.details.productName} added to liked items!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to like item: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToItemDetailsFragment(item: Item) {
        // Prevent navigating to details if item is marked unavailable by the adapter logic
        // (Though adapter should disable click, this is an extra safeguard)
        if (!item.available || !item.status.equals("approved", ignoreCase = true)) {
            Log.w(TAG, "Attempted to navigate to details of unavailable item: ${item.id}")
            Toast.makeText(context, "This item is currently unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Navigating to details for item: ${item.id}")
        val bundle = Bundle().apply {
            putParcelable("ITEM_DETAILS", item) // Pass the whole Item object
            putBoolean("IS_FROM_MY_LISTINGS", false)
        }
        findNavController().navigate(R.id.action_homeFragment_to_itemDetailsFragment, bundle)
    }


    private fun startAutoScroll(itemCount: Int) {
        if (::autoScrollRunnable.isInitialized) {
            handler.removeCallbacks(autoScrollRunnable)
        }
        if (itemCount > 0 && _binding != null && isAdded) { // Ensure fragment is valid before starting
            autoScrollPosition = 0 // Reset position when starting
            autoScrollRunnable = object : Runnable {
                override fun run() {
                    if (_binding != null && isAdded && view != null) {
                        try {
                            autoScrollPosition = (autoScrollPosition + 1) % itemCount
                            binding.latestItemsRecyclerView.smoothScrollToPosition(autoScrollPosition)
                            handler.postDelayed(this, 6000)
                        } catch (e: Exception) { Log.e(TAG, "Error during auto-scroll", e) }
                    } else { Log.w(TAG, "Auto-scroll stopped: Fragment/View invalid.") }
                }
            }
            handler.postDelayed(autoScrollRunnable, 6000)
            Log.d(TAG, "Auto-scroll started with $itemCount items.")
        } else { Log.d(TAG, "Auto-scroll not started.") }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        if (::autoScrollRunnable.isInitialized) {
            handler.removeCallbacks(autoScrollRunnable)
            Log.d(TAG, "Auto-scroll callbacks removed.")
        }
        binding.latestItemsRecyclerView.adapter = null
        binding.productGrid.adapter = null
        _binding = null
    }
}








//package com.example.signuplogina
//
//import android.content.Intent
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.signuplogina.databinding.FragmentHomeBinding
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//
//class HomeFragment : Fragment() {
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    private val handler = Handler(Looper.getMainLooper())
//    private var autoScrollPosition = 0
//
//    private lateinit var autoScrollRunnable: Runnable
//
//
//    private val databaseRef = FirebaseDatabase.getInstance().getReference("Items")
//    private val userId = FirebaseAuth.getInstance().currentUser?.uid
//    private lateinit var latestItemsAdapter: ItemsAdapter
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        return binding.root
//
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Setup RecyclerView for Main Products (Grid Layout)
//        binding.productGrid.layoutManager = GridLayoutManager(requireContext(), 2)
//        latestItemsAdapter = ItemsAdapter(
//            mutableListOf(),
//            onItemClicked = { item -> navigateToItemDetailsFragment(item) },
////            onBidClicked = { item -> saveLikedItemToFirebase(item) }
//            onBidClicked = {  }
//            ,true)
//        binding.latestItemsRecyclerView.apply {
//            layoutManager =
//                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//            adapter = latestItemsAdapter
//        }
//
//        setupCategoryClickListeners()
//        fetchLatestItems()
//
//        // Fetch and display products based on user categories
//        fetchUserCategoriesAndDisplayItems()
//    }
//
//    private fun fetchLatestItems() {
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//        val currentTime = System.currentTimeMillis()
//        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
//
//        databaseRef.orderByChild("timestamp")
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val recentItems = mutableListOf<Item>()
//                    val olderItems = mutableListOf<Item>()
//
//                    for (itemSnapshot in snapshot.children) {
//                        val id = itemSnapshot.key ?: ""
//                        val userId = itemSnapshot.child("userId").getValue(String::class.java) ?: ""
////                  change here
////                        val timestamp = itemSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
//
//                        // Exclude current user's items
//                        if (userId == currentUserId) continue
//
//                        val detailsSnapshot = itemSnapshot.child("details")
//
//                        val itemDetails = ItemDetails(
//                            productName = detailsSnapshot.child("productName").getValue(String::class.java) ?: "",
//                            description = detailsSnapshot.child("description").getValue(String::class.java) ?: "",
//                            category = detailsSnapshot.child("category").getValue(String::class.java) ?: "",
//                            condition = detailsSnapshot.child("condition").getValue(String::class.java) ?: "",
//                            price = detailsSnapshot.child("price").getValue(Double::class.java) ?: 0.0,
//                            availability = detailsSnapshot.child("availability").getValue(Int::class.java) ?: 0,
//                            imageUrls = detailsSnapshot.child("imageUrls").children.mapNotNull { it.getValue(String::class.java) },
//                             timestamp = itemSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
//
//                        )
//
//                        val item = Item(id, userId, itemDetails)
//
//                        if (currentTime - itemDetails.timestamp <= oneWeekMillis) {
//                            recentItems.add(item)
//                        } else {
//                            olderItems.add(item)
//                        }
//                    }
//
//                    // Shuffle to make it random
//                    recentItems.shuffle()
//
//                    val finalList = if (recentItems.size >= 5) {
//                        recentItems.take(5)
//                    } else {
//                        // Fill remaining spots with older items
//                        val needed = 5 - recentItems.size
//                        olderItems.shuffle()
//                        recentItems + olderItems.take(needed)
//                    }
//
//                    Log.d("AdapterDebug", "Showing ${finalList.size} items:")
//                    finalList.forEach {
//                        Log.d("AdapterDebug", "Item: ${it.details?.productName}, Time: ${it.details.timestamp}")
//                    }
//
//                    latestItemsAdapter.updateItems(finalList.toMutableList())
//                    startAutoScroll(finalList.size)
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("Firebase", "Error fetching items", error.toException())
//                }
//            })
//    }
//
////    private fun fetchLatestItems() {
////        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
////            override fun onDataChange(snapshot: DataSnapshot) {
////                val itemsList = mutableListOf<Item>()
////                Log.d("Firebase", "Total items fetched: ${snapshot.childrenCount}")
////
////                for (itemSnapshot in snapshot.children) {
////                    val id = itemSnapshot.key ?: ""
////                    val userId = itemSnapshot.child("userId").getValue(String::class.java) ?: ""
////                    val detailsSnapshot = itemSnapshot.child("details")
////
////                    val productName =
////                        detailsSnapshot.child("productName").getValue(String::class.java) ?: ""
////                    val description =
////                        detailsSnapshot.child("description").getValue(String::class.java) ?: ""
////                    val category =
////                        detailsSnapshot.child("category").getValue(String::class.java) ?: ""
////                    val condition =
////                        detailsSnapshot.child("condition").getValue(String::class.java) ?: ""
////                    val price = detailsSnapshot.child("price").getValue(Double::class.java) ?: 0.0
////                    val availability =
////                        detailsSnapshot.child("availability").getValue(Int::class.java) ?: 0
////                    val imageUrls =
////                        detailsSnapshot.child("imageUrls").children.mapNotNull { it.getValue(String::class.java) }
////
////                    val itemDetails = ItemDetails(
////                        productName = productName,
////                        description = description,
////                        category = category,
////                        condition = condition,
////                        price = price,
////                        availability = availability,
////                        imageUrls = imageUrls
////                    )
////
////                    val item = Item(
////                        id = id, userId = userId, details = itemDetails
////                    )
////
////                    itemsList.add(item)
////                }
////
////                val latestItems = itemsList.takeLast(5).toMutableList()
////
////                Log.d("AdapterDebug", "Latest Items Count: ${latestItems.size}")
////                for (item in latestItems) {
////                    Log.d("AdapterDebug", "Item Name: ${item.details?.productName}")
////                }
////
////                // ✅ Adapter is always initialized in onViewCreated(), just update
////                latestItemsAdapter.updateItems(latestItems)
////
////                startAutoScroll(latestItems.size)
////            }
////
////            override fun onCancelled(error: DatabaseError) {
////                Log.e("Firebase", "Error fetching latest items", error.toException())
////            }
////        })
////    }
//
//    override fun onResume() {
//        super.onResume()
//        fetchLatestItems()
//    }
//
//    private fun setupCategoryClickListeners() {
//        binding.categoryElectronics.setOnClickListener { navigateToCategory("Electronics") }
//        binding.categoryClothing.setOnClickListener { navigateToCategory("Clothing") }
//        binding.categoryBooks.setOnClickListener { navigateToCategory("Books") }
//        binding.categoryBeauty.setOnClickListener { navigateToCategory("Cosmetics") }
//        binding.categoryCars.setOnClickListener { navigateToCategory("vehicles") }
//        binding.categoryHomeAppliances.setOnClickListener { navigateToCategory("Home Appliances") }
//    }
//
//    private fun navigateToCategory(category: String) {
//        val bundle = Bundle().apply {
//            putString("CATEGORY_NAME", category)
//        }
//        findNavController().navigate(R.id.action_homeFragment_to_categoryScreenFragment, bundle)
//    }
//
//
//
//
//    private fun fetchUserCategoriesAndDisplayItems() {
//        userId?.let { uid ->
//            val userCategoriesRef =
//                FirebaseDatabase.getInstance().getReference("Users").child(uid).child("category")
//
//            userCategoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val selectedCategories =
//                        snapshot.children.mapNotNull { it.getValue(String::class.java) }
//
//                    if (selectedCategories.isNotEmpty()) {
//                        fetchItemsByCategories(selectedCategories) // Calls the updated function
//                    } else {
//                        Log.d("Firebase", "No categories selected for this user")
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("Firebase", "Error fetching user categories", error.toException())
//                }
//            })
//        }
//    }
//
//    private fun fetchItemsByCategories(selectedCategories: List<String>) {
//        val databaseRef = FirebaseDatabase.getInstance().getReference("Items")
//
//        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val filteredItemsList = mutableListOf<Item>()
//
//                for (itemSnapshot in snapshot.children) {
//                    val id = itemSnapshot.key ?: "" // 🔹 Add ID from Firebase key
//                    val userId = itemSnapshot.child("userId").getValue(String::class.java) ?: ""
//                    val detailsSnapshot = itemSnapshot.child("details")
//
//                    val productName =
//                        detailsSnapshot.child("productName").getValue(String::class.java) ?: ""
//                    val description =
//                        detailsSnapshot.child("description").getValue(String::class.java) ?: ""
//                    val category =
//                        detailsSnapshot.child("category").getValue(String::class.java) ?: ""
//                    val condition =
//                        detailsSnapshot.child("condition").getValue(String::class.java) ?: ""
//                    val price = detailsSnapshot.child("price").getValue(Double::class.java) ?: 0.0
//                    val availability =
//                        detailsSnapshot.child("availability").getValue(Int::class.java) ?: 0
//                    val imageUrls =
//                        detailsSnapshot.child("imageUrls").children.mapNotNull { it.getValue(String::class.java) }
//
//                    if (selectedCategories.contains(category)) {
//                        val itemDetails = ItemDetails(
//                            productName = productName,
//                            description = description,
//                            category = category,
//                            condition = condition,
//                            price = price,
//                            availability = availability,
//                            imageUrls = imageUrls
//                        )
//
//                        val item = Item(
//                            id = id, userId = userId, details = itemDetails
//                        )
//
//                        filteredItemsList.add(item)
//                    }
//
//                }
//
//                // Set up the adapter with filtered data
//                if (_binding != null) {
//
//                    val productAdapter = ItemsAdapter(
//                        items = filteredItemsList,
//                        onItemClicked = { item -> navigateToItemDetailsFragment(item) },
//                        onBidClicked = { item -> saveLikedItemToFirebase(item) },
//                        false)
//                    binding.productGrid.adapter = productAdapter
//                }
//            }
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("Firebase", "Error fetching items", error.toException())
//                Toast.makeText(requireContext(), "Failed to load items", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    private fun sendNotificationToOwner(itemId: String, bidAmount: Int) {
//        val ownerId = "PRODUCT_OWNER_ID" // Replace with the actual owner ID retrieval logic
//        val notificationsRef = FirebaseDatabase.getInstance().getReference("Notifications")
//        val notificationId = notificationsRef.push().key // Unique notification ID
//
//        if (notificationId == null) return
//
//        val notificationData = mapOf(
//            "message" to "Your product received a new bid of $$bidAmount",
//            "timestamp" to System.currentTimeMillis()
//        )
//
//        notificationsRef.child(ownerId).child(notificationId).setValue(notificationData)
//    }
//
//
//    private fun saveLikedItemToFirebase(item: Item) {
//        val likedItemsRef =
//            FirebaseDatabase.getInstance().getReference("LikedItems").child(userId ?: return)
//
//        // Push a new node and get its unique Firebase-generated key
//        val newLikedItemRef = likedItemsRef.push()
//
//        val likedItemData = mapOf(
//            "imageUrl" to (item.details?.imageUrls?.firstOrNull() ?: ""),
//            "productName" to (item.details?.productName ?: ""),
//            "description" to (item.details?.description ?: ""),
//            "category" to (item.details?.category ?: ""),
//            "condition" to (item.details?.condition ?: ""),
//            "availability" to item.details?.availability.toString()
//        )
//
//        newLikedItemRef.setValue(likedItemData).addOnSuccessListener {
//                Toast.makeText(
//                    requireContext(),
//                    "${item.details?.productName} added to liked items!",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }.addOnFailureListener {
//                Toast.makeText(
//                    requireContext(), "Failed to like item: ${it.message}", Toast.LENGTH_SHORT
//                ).show()
//            }
//    }
//
//    private fun navigateToItemDetailsFragment(item: Item) {
//        val bundle = Bundle().apply {
//            putParcelable("ITEM_DETAILS", item)
//            putBoolean("IS_FROM_MY_LISTINGS", false) // Indicates navigation from Home
//
//        }
//        // Navigate to ItemDetailsFragment using NavController
//        findNavController().navigate(R.id.action_homeFragment_to_itemDetailsFragment, bundle)
//    }
//
//
//    private fun startAutoScroll(itemCount: Int) {
//        if (itemCount > 0) {
//            autoScrollRunnable = object : Runnable {
//                override fun run() {
//                    if (_binding != null && isAdded) {
//                        autoScrollPosition = (autoScrollPosition + 1) % itemCount
//
//                        val layoutManager = binding.latestItemsRecyclerView.layoutManager as? LinearLayoutManager
//                        layoutManager?.let {
//                            // Scroll 1 item at a time horizontally
//                            binding.latestItemsRecyclerView.smoothScrollToPosition(autoScrollPosition)
//                        }
//
//                        handler.postDelayed(this, 6000)
//                    }
//                }
//            }
//            handler.postDelayed(autoScrollRunnable, 6000)
//        }
//    }
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//        handler.removeCallbacksAndMessages(null) // Clean up thehandler
//    }
//}
