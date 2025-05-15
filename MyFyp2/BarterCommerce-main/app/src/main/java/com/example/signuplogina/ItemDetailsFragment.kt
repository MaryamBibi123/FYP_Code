//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.viewpager2.widget.ViewPager2
//import com.bumptech.glide.Glide
//import com.example.signuplogina.databinding.FragmentItemDetailsBinding
//import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
//
//class ItemDetailsFragment : Fragment() {
//    private lateinit var binding: FragmentItemDetailsBinding
//    private val TAG = "ItemDetailsFragment"
//    private lateinit var imageAdapter: ImageAdapter
//    private lateinit var dotsIndicator: DotsIndicator
//
//
//    // Declare item variable
//    private var item: Item? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentItemDetailsBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    @SuppressLint("SetTextI18n")
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        item = arguments?.getParcelable("ITEM_DETAILS")
//
//        if (item == null) {
//            Log.e(TAG, "Item details not found in arguments!")
//            Toast.makeText(requireContext(), "Item details are missing", Toast.LENGTH_SHORT).show()
//            return
//        }
//        val isFromMyListings = arguments?.getBoolean("IS_FROM_MY_LISTINGS") ?: false
//        Log.d("HomeOrListings","isFromMyListings: $isFromMyListings")
//        Log.d(TAG, "Received item: $item")
//
//        binding.productName.text = item?.details?.productName
//        binding.productDescriptionText.text = "Description: ${item?.details?.description}"
//        binding.productCategoryText.text = "Category: ${item?.details?.category}"
//        binding.productConditionText.text = "Condition: ${item?.details?.condition}"
//        dotsIndicator = view.findViewById(R.id.dots_indicator)
//        val toggleLayout = binding.toggleDescriptionLayout
//        val arrowIcon = binding.arrowIcon
//        val descriptionText = binding.productDescriptionText
//
//        toggleLayout.setOnClickListener {
//            if (descriptionText.visibility == View.GONE) {
//                descriptionText.visibility = View.VISIBLE
//                arrowIcon.setImageResource(R.drawable.ic_arrow_right)
//            } else {
//                descriptionText.visibility = View.GONE
//                arrowIcon.setImageResource(R.drawable.arrow_back)
//            }
//        }
//
//
//        val imageUrls = item?.details?.imageUrls ?: emptyList()
//        imageAdapter = ImageAdapter(imageUrls, false)
//        binding.productImageCarousel.apply {
//            adapter = imageAdapter
//            orientation = ViewPager2.ORIENTATION_HORIZONTAL
//
//            // Connect DotsIndicator to ViewPager2 AFTER setting the adapter
//            dotsIndicator.setViewPager2(this) // 'this' refers to the ViewPager2
//        }
//        if (isFromMyListings) {
//            binding.bidNowButton.visibility = View.GONE
////            binding.bidsReceivedButton.visibility = View.VISIBLE
////            binding.bidsReceivedButton.text = "Bids Received"
////            // Set up action for the Bids Received button if needed
////            binding.bidsReceivedButton.setOnClickListener {
////                // Implement your functionality here
////                Toast.makeText(requireContext(), "Bids Received clicked", Toast.LENGTH_SHORT).show()
////            }
//        } else {
//            binding.bidNowButton.visibility = View.VISIBLE
////            binding.bidsReceivedButton.visibility = View.GONE
//            binding.bidNowButton.text = "Bid Now"
//            binding.bidNowButton.setOnClickListener {
//                // Implement bidding functionality here
//                Toast.makeText(requireContext(), "Bid Now clicked", Toast.LENGTH_SHORT).show()
//            }
//        }
////        TabLayoutMediator(binding.tabLayout, binding.productImageCarousel) { _, _ ->
////            // No need to do anything inside
////        }.attach()
//        // Back button functionality
//        binding.arrowBack.setOnClickListener {
//            findNavController().navigateUp()
//        }
//
//        // Fetch number of bids if product ID is available
////        item?.id?.let { fetchNumberOfBids(it) }
//
//        // Back button click listener
//        val imageView: ImageView = view.findViewById(R.id.arrowBack)
//        imageView.setOnClickListener {
//            findNavController().navigateUp()
//        }
//
////         Bid Now button click listener
//        binding.bidNowButton.setOnClickListener {
//            showBidConfirmationDialog(item!!)
//        }
//
////        binding.bidsReceivedButton.setOnClickListener{
////            showBidReceivedDetails(item!!)
////        }
//
//    }
//
////    private fun showBidReceivedDetails(item: Item) {
////
////
////
////    }
//
//
//    private fun showBidConfirmationDialog(SelectedProduct: Item) { // Pass full item instead of just product name & description
//        if (item == null) {
//            Log.e(TAG, "Item is null, cannot show bid dialog")
//            Toast.makeText(requireContext(), "Item details are missing", Toast.LENGTH_SHORT).show()
//            return
//        }
//        Log.d(TAG, "Showing bid confirmation dialog for product: ${SelectedProduct.details?.productName}")
//
//        val dialogBuilder = AlertDialog.Builder(requireContext())
//        dialogBuilder.setTitle("Select Your Exchange Item")
//        dialogBuilder.setMessage("Do you want to upload a new item or select an existing one?")
//
//        dialogBuilder.setPositiveButton("Upload New Item") { _, _ ->
//            Log.d(TAG, "User chose to upload a new item")
//            findNavController().navigate(R.id.action_itemDetailsFragment_to_addItemFragment)
//        }
//
//        dialogBuilder.setNeutralButton("Choose Existing") { _, _ ->
//            Log.d(TAG, "User chose to select an existing item")
//
//            // Pass `selectedProduct` to `SelectProductFragment`
//            val bundle = Bundle().apply {
//                putParcelable("selectedProduct", SelectedProduct)
//            }
//            findNavController().navigate(R.id.action_itemDetailsFragment_to_selectProductFragment, bundle)
//        }
//        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
//            Log.d(TAG, "User canceled the bid selection")
//            dialog.dismiss()
//        }
//
//        dialogBuilder.create().show()
//    }
//}
//
//
//


package com.example.signuplogina

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.signuplogina.databinding.FragmentItemDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator


// This class merges the working structure of your first code with the
// validation checks from the second code.
class ItemDetailsFragment : Fragment() {
    // Use nullable binding for safe fragment lifecycle handling
    private var _binding: FragmentItemDetailsBinding? = null
    private val binding get() = _binding!! // Accessor

    private val TAG = "ItemDetailsFragment"
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var dotsIndicator: DotsIndicator
    private var item: Item? = null

    // --- Added from Code 2 ---
    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var progressBarBidCheck: ProgressBar
    // --- End Added from Code 2 ---


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // --- Modified to use nullable binding ---
        _binding = FragmentItemDetailsBinding.inflate(inflater, container, false)

        // --- Added from Code 2 ---
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        database = FirebaseDatabase.getInstance()
        // --- End Added from Code 2 ---

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Added from Code 2 (ProgressBar Initialization) ---
        // Ensure you have '<ProgressBar android:id="@+id/progressBarBidCheck" ... />' in your XML
        try {
            progressBarBidCheck = binding.progressBar // Use binding
        } catch (e: Exception) {
            // Handle case where ProgressBar might be missing in layout to avoid crash
            Log.e(TAG, "ProgressBar with ID 'progressBarBidCheck' not found in layout!", e)
            // Optionally disable bidding entirely or show a different error
            binding.bidNowButton.isEnabled = false // Example: disable bidding
            Toast.makeText(
                requireContext(), "UI Error: Cannot initialize bidding.", Toast.LENGTH_LONG
            ).show()
        }
        // --- End Added from Code 2 ---


        // --- Argument Retrieval (From Code 1) ---
        item = arguments?.getParcelable("ITEM_DETAILS") // Use your consistent key

        if (item == null) {
            Log.e(TAG, "Item details not found in arguments!")
            Toast.makeText(requireContext(), "Item details are missing", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack() // Go back if item is null
            return
        }
        val isFromMyListings = arguments?.getBoolean("IS_FROM_MY_LISTINGS") ?: false
        Log.d("HomeOrListings", "isFromMyListings: $isFromMyListings")
        Log.d(TAG, "Received item: ${item?.details?.productName}")


        // --- Populate UI (From Code 1) ---
        binding.productName.text = item?.details?.productName
        binding.productDescriptionText.text = "Description: ${item?.details?.description}"
        binding.productCategoryText.text = "${item?.details?.category}"
        binding.productConditionText.text = "${item?.details?.condition}"
        dotsIndicator = binding.dotsIndicator // Use binding

        // --- Toggle Description (From Code 1 - Adjusted Icons) ---
        val toggleLayout = binding.toggleDescriptionLayout
        val arrowIcon = binding.arrowIcon
        val descriptionText = binding.productDescriptionText
        // Set initial state based on visibility if needed
        arrowIcon.setImageResource(if (descriptionText.visibility == View.VISIBLE) R.drawable.arrow_back else R.drawable.ic_arrow_right)
        toggleLayout.setOnClickListener {
            if (descriptionText.visibility == View.GONE) {
                descriptionText.visibility = View.VISIBLE
                arrowIcon.setImageResource(R.drawable.arrow_back) // Up when expanded
            } else {
                descriptionText.visibility = View.GONE
                arrowIcon.setImageResource(R.drawable.ic_arrow_right) // Down when collapsed
            }
        }

        // --- Image Carousel (From Code 1) ---
        val imageUrls = item?.details?.imageUrls ?: emptyList()
        imageAdapter = ImageAdapter(imageUrls, false) // Assuming ImageAdapter is defined
        binding.productImageCarousel.apply {
            adapter = imageAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            dotsIndicator.setViewPager2(this)
        }

        // --- Button Visibility (From Code 1) ---
        if (isFromMyListings) {
            binding.bidNowButton.visibility = View.GONE
        } else {
            binding.bidNowButton.visibility = View.VISIBLE
            binding.bidNowButton.text = "Bid Now"
        }

        // --- Back button functionality (From Code 1) ---
        binding.arrowBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // --- Bid Now button click listener (MERGED: Code 1 Structure + Code 2 Checks) ---
        binding.bidNowButton.setOnClickListener {
            // Check 1: Login status
            if (currentUser == null) {
                Toast.makeText(
                    requireContext(), "Please log in to place a bid.", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Check 2: Item data validity (Crucial: Check fields needed *before* asserting non-null)
            // Use 'itemId' consistently if that's your field name in the Item class and DB query
            val currentItemItemId = item?.id // Get ID safely
            val itemOwnerId = item?.userId // Get owner ID safely
            if (item == null || itemOwnerId == null || currentItemItemId == null) {
                Toast.makeText(
                    requireContext(),
                    "Cannot process bid. Item data incomplete.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Pre-bid check failed: item, userId, or itemId is null. Item: $item")
                return@setOnClickListener
            }

            // Check 3: Bidding on own item
            if (itemOwnerId == currentUser!!.uid) { // Safe to use !! for currentUser here after check 1
                Toast.makeText(
                    requireContext(), "You cannot bid on your own item.", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Check 4: Already bid (Asynchronous)
            if (!::progressBarBidCheck.isInitialized) { // Check if progress bar exists
                Log.e(TAG, "ProgressBarBidCheck not initialized, cannot proceed.")
                Toast.makeText(requireContext(), "UI Error during bid check.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            binding.bidNowButton.isEnabled = false
            progressBarBidCheck.visibility = View.VISIBLE

            // Using the more efficient Query method
            checkIfAlreadyBidViaQuery(currentItemItemId) { hasBid -> // Pass non-null itemId
                // Ensure view is still valid before updating UI
                if (_binding != null) {
                    binding.bidNowButton.isEnabled = true
                    progressBarBidCheck.visibility = View.GONE

                    if (hasBid) {
                        Log.d(
                            TAG,
                            "User ${currentUser!!.uid} has already bid on item $currentItemItemId."
                        )
                        Toast.makeText(
                            requireContext(),
                            "You have already placed a bid on this item.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.d(
                            TAG,
                            "Bid checks passed for item $currentItemItemId. Showing confirmation dialog."
                        )
                        // Safe to use !! for item here because of earlier checks
                        showBidConfirmationDialog(item!!) // Proceed only if checks pass
                    }
                } else {
                    Log.w(TAG, "View destroyed before bid check callback executed.")
                }
            }
        } // End of setOnClickListener

    } // --- End onViewCreated ---


    // --- Added from Code 2 (Asynchronous Check Function) ---
    private fun checkIfAlreadyBidViaQuery(currentItemId: String, callback: (Boolean) -> Unit) {
        if (currentUser == null) {
            callback(false)
            return
        }
        val userId = currentUser!!.uid
        val bidsRef = database.getReference("Bids")

        bidsRef.orderByChild("bidderId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var found = false
                    if (snapshot.exists()) {
                        for (bidSnapshot in snapshot.children) {
                            // Use 'itemId' consistently
                            val bidItemId = bidSnapshot.child("itemId").getValue(String::class.java)
                            if (bidItemId == currentItemId) {
                                found = true
                                break
                            }
                        }
                    }
                    Log.d(
                        TAG,
                        "Query check result for item $currentItemId and user $userId: found=$found"
                    )
                    // Check if fragment is still attached before calling back
                    if (isAdded) {
                        callback(found)
                    } else {
                        Log.w(TAG, "Fragment detached before bid query completed.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error querying bids", error.toException())
                    // Check if fragment is still attached before showing Toast/calling back
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Error checking previous bids", Toast.LENGTH_SHORT)
                            .show()
                        callback(false)
                    }
                }
            })
    }
    // --- End Added from Code 2 ---


    // --- Bid Confirmation Dialog Function (From Code 1 - with safety/clarity improvements) ---
    private fun showBidConfirmationDialog(selectedProduct: Item) {
        // Check if fragment is attached and context is available
        if (!isAdded || context == null) {
            Log.w(TAG, "Fragment not attached or context null, cannot show dialog.")
            return
        }
        // Item null check is implicitly done via checks before calling this

        Log.d(
            TAG,
            "Showing bid confirmation dialog for product: ${selectedProduct.details?.productName}"
        )

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Select Your Exchange Item")
        dialogBuilder.setMessage("Do you want to upload a new item or select an existing one?")

        dialogBuilder.setPositiveButton("Upload New Item") { _, _ ->
            Log.d(TAG, "User chose to upload a new item")
            try {
                findNavController().navigate(R.id.action_itemDetailsFragment_to_addItemFragment)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Navigation action to AddItemFragment not found", e)
                if (isAdded && context != null) Toast.makeText(
                    context, "Cannot navigate to add item.", Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialogBuilder.setNeutralButton("Choose Existing") { _, _ ->
            Log.d(TAG, "User chose to select an existing item")
            val bundle = Bundle().apply {
                putParcelable("selectedProduct", selectedProduct) // Pass the item being bid ON
            }
            try {
                findNavController().navigate(
                    R.id.action_itemDetailsFragment_to_selectProductFragment, bundle
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Navigation action to SelectProductFragment not found", e)
                if (isAdded && context != null) Toast.makeText(
                    context, "Cannot navigate to select item.", Toast.LENGTH_SHORT
                ).show()
            }
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            Log.d(TAG, "User canceled the bid selection")
            dialog.dismiss()
        }

        dialogBuilder.create().show()
    }
    // --- End Bid Confirmation Dialog Function ---

    // --- Added from Code 2 (Lifecycle Handling) ---
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference
    }
    // --- End Added from Code 2 ---

} // --- End Fragment Class ---