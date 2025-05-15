package com.example.signuplogina

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.databinding.FragmentSelectProductBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ... (your other imports for FCM - OkHttp, JSONObject, etc.)

class SelectProductFragment : Fragment() {

    private var _binding: FragmentSelectProductBinding? = null
    private val binding get() = _binding!!

    private val allUserItems = mutableListOf<Item>()
    private lateinit var productAdapter: ProductAdapter
    private lateinit var database: DatabaseReference

    private var itemBeingBidOn: Item? = null // Item being bid on (received from previous screen)
    private var selectedOfferedItems = mutableListOf<Item>() // List of items selected by user to offer

    private var isReplacing: Boolean = false
    private var existingBidId: String? = null
    // private var existingOfferedItemIds: List<String>? = null // If editing, pass current IDs

    // FCM related (keep your existing logic)
    private var fcmAccessToken: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectProductBinding.inflate(inflater, container, false)

        itemBeingBidOn = arguments?.getParcelable("selectedProduct") // Item they want to get
        isReplacing = arguments?.getBoolean("isReplacing", false) == true
        existingBidId = arguments?.getString("bidId")
        // If replacing, you might also pass the current offeredItemIds to pre-select them
        // val initialOfferedIds = arguments?.getStringArray("initialOfferedItemIds")?.toList()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Your FCM token retrieval logic
        lifecycleScope.launch {
            fcmAccessToken = getAccessToken(requireContext())
            // ... your existing FCM token handling ...
        }

        database = FirebaseDatabase.getInstance().getReference("Items")

//        productAdapter = ProductAdapter(allUserItems, maxSelection = 3) { items ->
//            selectedOfferedItems.clear()
//            selectedOfferedItems.addAll(items)
//            updateNextButtonState()
//        }

        // Correct instantiation using the detailed callback
        productAdapter = ProductAdapter(
            productList = allUserItems, // Pass the list that will be populated
            maxSelection = 3,           // Your defined maximum
            onSelectionAttempt = { attemptedItem, currentAdapterSelectedItems, limitWasReached ->
                // The 'attemptedItem' is the one the user just clicked on.
                // 'currentAdapterSelectedItems' is the state of selection *after* the adapter processed the click.
                // 'limitWasReached' is true if the user tried to select an item when already at max capacity.

                if (limitWasReached) {
                    // Show AlertDialog because the limit was hit when trying to ADD an item

                    showMaxSelectionAlertDialog(productAdapter.maxSelection)
                    // The adapter itself will not have added the 'attemptedItem' if the limit was reached.
                    // So, currentAdapterSelectedItems will still be at max capacity.
                }
                selectedOfferedItems.clear()
                selectedOfferedItems.addAll(currentAdapterSelectedItems)
                updateNextButtonState()
            }
        )
        binding.imageViewBack.setOnClickListener {
            // findNavController().popBackStack() // More robust than supportFragmentManager
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.nextButton.setOnClickListener {
            if (selectedOfferedItems.isNotEmpty()) {
                if (isReplacing && existingBidId != null) {
                    showReplacementConfirmationDialog()
                } else {
                    showBidConfirmationDialog()
                }
            } else {
                Toast.makeText(requireContext(), "Please select at least one item to offer.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.productRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }

        fetchUserItemsFromFirebase()
    }

    private fun updateNextButtonState() {
        if (selectedOfferedItems.isNotEmpty()) {
            binding.nextButton.isEnabled = true
            binding.nextButton.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.dark_blue)
            binding.nextButton.text = "Next (${selectedOfferedItems.size} selected)"
        } else {
            binding.nextButton.isEnabled = false
            binding.nextButton.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.grey) // Assuming you have a grey color
            binding.nextButton.text = "Next"
        }
    }
    private fun showMaxSelectionAlertDialog(maxItems: Int) {
        // Ensure fragment is still added to an activity and context is available
        if (!isAdded || context == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Selection Limit Reached")
            .setMessage("You can select a maximum of $maxItems items to offer.")
            .setPositiveButton("OK", null) // null listener just dismisses the dialog
            .show()
    }

    private fun showReplacementConfirmationDialog() {
        val offeredItemNames = selectedOfferedItems.joinToString { it.details.productName }
        AlertDialog.Builder(requireContext())
            .setTitle("Replace Offer")
            .setMessage("Replace current offer with: $offeredItemNames?")
            .setPositiveButton("Yes") { _, _ -> updateOfferedItemsInBid() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateOfferedItemsInBid() {
        val bidId = existingBidId ?: return
        if (selectedOfferedItems.isEmpty()) {
            Toast.makeText(requireContext(), "No items selected for replacement.", Toast.LENGTH_SHORT).show()
            return
        }
        val newOfferedItemIds = selectedOfferedItems.map { it.id }

        val bidRef = FirebaseDatabase.getInstance().getReference("Bids").child(bidId)

        // Before updating offeredItemIds, you might need to update bidsPlaced on old items
        // This is complex and depends on how strictly you track. For simplicity, just updating the bid.
        bidRef.child("offeredItemIds").setValue(newOfferedItemIds)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Offered items updated.", Toast.LENGTH_SHORT).show()
                // TODO: Also update the Items -> itemID -> bidsPlaced for the NEW offered items
                // And potentially remove from OLD offered items. This is a multi-step update.
                // For now, navigating back.
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update bid: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showBidConfirmationDialog() {
        if (itemBeingBidOn == null || selectedOfferedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Missing product details or no items offered.", Toast.LENGTH_SHORT).show()
            return
        }

        val offeredItemNames = selectedOfferedItems.joinToString(separator = ", ") { it.details.productName }
        val message = "Are you sure you want to offer: \n${offeredItemNames} \n\nFor: ${itemBeingBidOn?.details?.productName}?"

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Bid")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> addBidToFirebase() }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

    private fun addBidToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        val bidderId = currentUser.uid

        val requestedItemId = itemBeingBidOn?.id ?: return
        val receiverId = itemBeingBidOn?.userId ?: return
        val offeredIds = selectedOfferedItems.map { it.id } // Get list of IDs

        if (offeredIds.isEmpty()) {
            Toast.makeText(requireContext(), "No items selected to offer.", Toast.LENGTH_SHORT).show()
            return
        }

        val bidsReference = FirebaseDatabase.getInstance().getReference("Bids")
        val newBidId = bidsReference.push().key ?: return

        // Create the Bid object using the new structure
        val newBid = Bid(
            bidId = newBidId,
            bidderId = bidderId,
            receiverId = receiverId,
            itemId = requestedItemId, // Item being requested
            offeredItemIds = offeredIds, // LIST of offered item IDs
            timestamp = System.currentTimeMillis(), // Use client time or ServerValue.TIMESTAMP
            status = "pending",
            bidderName = currentUser.displayName ?: "" // Optional, for convenience
        )

        val updates = mutableMapOf<String, Any?>()
        updates["Bids/$newBidId"] = newBid // Store the Bid object directly

        // Update user's placed bids
        updates["Users/$bidderId/bids/placed/$newBidId"] = true
        // Update receiver's received bids
        updates["Users/$receiverId/bids/received/$newBidId"] = true
        // Update requested item's bidsReceived
        updates["Items/$requestedItemId/bidsReceived/$newBidId"] = true // Corrected path based on Item model
        // Update EACH offered item's bidsPlaced
        offeredIds.forEach { offeredId ->
            updates["Items/$offeredId/bidsPlaced/$newBidId"] = true // Corrected path
        }

        FirebaseDatabase.getInstance().reference.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show()
                sendNotificationToProductOwner(itemBeingBidOn!!) // Assuming itemBeingBidOn is not null here
                // Navigate back twice if coming from ItemDetails -> SelectProduct
                findNavController().popBackStack(R.id.itemDetailsFragment, true) // Pop back to before item details
                findNavController().popBackStack() // Pop SelectProductFragment itself
                // Or navigate to a "Bids Placed" screen or home.
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to place bid: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun fetchUserItemsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            // ... (handle not logged in) ...
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        database.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allUserItems.clear()
                    if (snapshot.exists()) {
                        for (itemSnapshot in snapshot.children) {
                            val item = itemSnapshot.getValue(Item::class.java)
                            // Only add available items that are not the one being bid on
                            if (item != null && item.available && item.status == "approved" && item.id != itemBeingBidOn?.id) {
                                item.id = itemSnapshot.key ?: "" // Ensure ID is set from snapshot key
                                allUserItems.add(item)
                            }
                        }
                        productAdapter.notifyDataSetChanged()
                        if (allUserItems.isEmpty()){
                            Toast.makeText(requireContext(), "You have no other available items to offer.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "You have no items available.", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE
                }
                override fun onCancelled(error: DatabaseError) {
                    // ... (handle error) ...
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    // --- Your FCM Methods (getAccessToken, sendNotificationToProductOwner, sendPushNotification) ---
    // Keep these as they are, but ensure sendNotificationToProductOwner uses fcmAccessToken
    private suspend fun getAccessToken(context: Context): String? { // Renamed for clarity
        return withContext(Dispatchers.IO) {
            try {
                // Ensure "firebase-adminsdk.json" is in your app's assets folder
                val serviceAccountStream = context.assets.open("firebase-adminsdk.json") // Or your SDK file name
                val credentials = GoogleCredentials.fromStream(serviceAccountStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                credentials.refreshIfExpired()
                credentials.accessToken.tokenValue
            } catch (e: Exception) {
                Log.e("FCM_TOKEN_ERROR", "Error getting access token", e)
                null
            }
        }
    }
    private fun sendNotificationToProductOwner(productBeingBidOn: Item) {
        Log.d("FCM_Debug", "sendNotificationToProductOwner for product: ${productBeingBidOn.id}")
        val productOwnerId = productBeingBidOn.userId
        if (productOwnerId.isEmpty()) {
            Log.e("FCM_Debug", "Product owner ID is empty for item ${productBeingBidOn.id}")
            return
        }

        val productOwnerTokenRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(productOwnerId).child("fcmToken")

        productOwnerTokenRef.get().addOnSuccessListener { snapshot ->
            val productOwnerToken = snapshot.value as? String
            Log.d("FCM_Debug", "Product Owner FCM Token: $productOwnerToken")

            if (!productOwnerToken.isNullOrEmpty() && fcmAccessToken != null) {
                val offeredItemsSummary = selectedOfferedItems.take(2)
                    .joinToString(", ") { it.details.productName } +
                        if (selectedOfferedItems.size > 2) " & more" else ""

                val notificationData = JSONObject().apply {
                    put("title", "New Bid on Your Item!")
                    put("body", "You received an offer of ($offeredItemsSummary) for ${productBeingBidOn.details.productName}.")
                }
                val dataPayload = JSONObject().apply {
                    put("type", "NEW_BID")
                    put("bidId",  existingBidId ?: "unknown_bid_id") // Pass bidId
                    put("itemId", productBeingBidOn.id) // Item that received the bid
                }

                val fcmPayload = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", productOwnerToken)
                        put("notification", notificationData)
                        put("data", dataPayload)
                    })
                }
                sendPushNotification(fcmPayload, fcmAccessToken!!) // Pass the fetched OAuth token
            } else {
                if (productOwnerToken.isNullOrEmpty()) Log.e("FCM_Debug", "Product owner FCM token is null or empty.")
                if (fcmAccessToken == null) Log.e("FCM_Debug", "FCM Access Token (OAuth) is null.")
            }
        }.addOnFailureListener {
            Log.e("FCM_Debug", "Failed to get product owner FCM token", it)
        }
    }

    private fun sendPushNotification(payload: JSONObject, oauthToken: String) { // Added oauthToken parameter
        // ... your existing sendPushNotification logic using the passed oauthToken ...
        // Make sure to use the oauthToken in the Authorization header:
        // .addHeader("Authorization", "Bearer $oauthToken")
        // The rest of your OkHttp call should be fine.
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SelectProductFragment" // Consistent TAG
    }
}














//package com.example.signuplogina
//
//import android.app.AlertDialog
//import android.content.ContentValues.TAG
//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.signuplogina.databinding.FragmentSelectProductBinding
//import com.google.auth.oauth2.GoogleCredentials
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.Call
//import okhttp3.Callback
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.RequestBody.Companion.toRequestBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.Response
//import org.json.JSONObject
//import java.io.File
//import java.io.FileInputStream
//import java.io.IOException
//
//class SelectProductFragment : Fragment() {
//
//    private lateinit var binding: FragmentSelectProductBinding
//    private val productList = mutableListOf<Item>()
//    private lateinit var productAdapter: ProductAdapter
//    private lateinit var database: DatabaseReference
//    private var selectedProduct: Item? = null // Item being bid on
//    private var offeredItem: Item? = null // Item selected from list
//    private var token: String? = null
//
//    private var isReplacing: Boolean = false
//    private var existingBidId: String? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentSelectProductBinding.inflate(inflater, container, false)
//
//        // Receive the selected product (The Item to be bid on)
//        selectedProduct = arguments?.getParcelable("selectedProduct")
//        isReplacing = arguments?.getBoolean("isReplacing", false) == true
//        existingBidId = arguments?.getString("bidId")
//
//        return binding.root
//    }
//
//
//    private suspend fun getAccessToken(context: Context): String? {
//        return withContext(Dispatchers.IO) { // ✅ Run on background thread
//            try {
//                val credentials =
//                    GoogleCredentials.fromStream(context.assets.open("firebase-adminsdk.json"))
//                        .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
//
//                credentials.refreshIfExpired() // ✅ Prevents token expiration
//                credentials.accessToken.tokenValue // ✅ Return token
//
//            } catch (e: Exception) {
//                Log.e("FCM", "Failed to get access token", e)
//                null
//            }
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
////        getAccessToken(requireContext())
//        val context = requireContext()
//        Log.e(TAG, "onViewCreated: $context")
//        lifecycleScope.launch {
//            token = getAccessToken(requireContext())
//            if (token != null) {
//                Log.d("FCM", "Access Token Retrieved: $token")
//
//                // ✅ Save token in SharedPreferences
//                val prefs = requireContext().getSharedPreferences("FCM_Prefs", Context.MODE_PRIVATE)
//                prefs.edit().putString("fcm_token", token).apply()
//            } else {
//                Log.e("FCM", "Failed to get access token")
//            }
//        }
//
//        database = FirebaseDatabase.getInstance().getReference("Items")
//
//        // Initialize RecyclerView and adapter
//        productAdapter = ProductAdapter(productList) { item ->
//            item.details?.let { details ->
//                Toast.makeText(
//                    requireContext(), "Selected: ${details.productName}", Toast.LENGTH_SHORT
//                ).show()
//
//                offeredItem = item  // Set the selected item
//                binding.nextButton.isEnabled = true
//                binding.nextButton.backgroundTintList =
//                    ContextCompat.getColorStateList(requireContext(), R.color.dark_blue)
//            } ?: run {
//                Toast.makeText(requireContext(), "Selected item has no details", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//
//        binding.imageViewBack.setOnClickListener {
//            requireActivity().supportFragmentManager.popBackStack()
//        }
//
//        binding.nextButton.setOnClickListener {
//            if (offeredItem != null) {
//                if (isReplacing && existingBidId != null) {
//                    showReplacementConfirmationDialog()
//                } else {
//                    showBidConfirmationDialog()
//                }
//            } else {
//                Toast.makeText(requireContext(), "Please select a product.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//        binding.productRecyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = productAdapter
//        }
//
//        fetchUserItemsFromFirebase()
//    }
//
//    private fun showReplacementConfirmationDialog() {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Replace Offer")
//            .setMessage("Replace current offered item with ${offeredItem?.details?.productName}?")
//            .setPositiveButton("Yes") { _, _ -> updateOfferedItemInBid() }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun updateOfferedItemInBid() {
//        val bidId = existingBidId ?: return
//        val offeredItemId = offeredItem?.id ?: return
//
//        val bidRef = FirebaseDatabase.getInstance().getReference("Bids/$bidId")
//
//        bidRef.child("offeredItemId").setValue(offeredItemId)
//            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Offered item updated.", Toast.LENGTH_SHORT).show()
//                requireActivity().supportFragmentManager.popBackStack()
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to update bid.", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//
//
//    private fun showBidConfirmationDialog() {
//
//        if (selectedProduct == null || offeredItem == null) {
//            Toast.makeText(requireContext(), "Missing product details!  ${selectedProduct == null}  ${offeredItem == null} ", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        AlertDialog.Builder(requireContext()).setTitle("Confirm Bid")
//            .setMessage("Are you sure you want to bid ${offeredItem?.details?.productName} for ${selectedProduct?.details?.productName}?")
//            .setPositiveButton("Yes") { _, _ ->
//                addBidToFirebase()
//            }.setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }.create().show()
//    }
//
//
//
//
//    // Assuming this is inside a Fragment or Activity where you have `selectedProduct` and `offeredItem`
////    private fun addBidToFirebase() {
////        val firebaseAuth = FirebaseAuth.getInstance()
////        val currentFirebaseUser = firebaseAuth.currentUser
////        if (currentFirebaseUser == null) {
////            Toast.makeText(requireContext(), "You must be logged in to place a bid.", Toast.LENGTH_SHORT).show()
////            return
////        }
////        val userId = currentFirebaseUser.uid
////
////        // Make sure selectedProduct and offeredItem are not null and have IDs
////        val currentSelectedProduct = selectedProduct ?: run {
////            Toast.makeText(requireContext(), "Selected product is missing.", Toast.LENGTH_SHORT).show()
////            return
////        }
////        val currentOfferedItem = offeredItem ?: run {
////            Toast.makeText(requireContext(), "Offered item is missing.", Toast.LENGTH_SHORT).show()
////            return
////        }
////
////        val itemId = currentSelectedProduct.id
////        val receiverId = currentSelectedProduct.userId
////        val offeredItemId = currentOfferedItem.id
////
////        val bidsReference = FirebaseDatabase.getInstance().getReference("Bids")
////        val newBidId = bidsReference.push().key ?: run {
////            Toast.makeText(requireContext(), "Could not generate bid ID.", Toast.LENGTH_SHORT).show()
////            return
////        }
////
////        // Get bidder's name (prefer display name if available)
////        val bidderName = currentFirebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Someone"
////        val itemName = currentSelectedProduct.details.productName.takeIf { it.isNotBlank() } ?: "your item"
////
////        val bidData = mapOf(
////            "bidId" to newBidId,
////            "bidderId" to userId,
////            "bidderName" to bidderName,             // For notification message
////            "receiverId" to receiverId,
////            "itemId" to itemId,
////            "itemName" to itemName,                 // For notification message
////            "offeredItemId" to offeredItemId,
////            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
////            "status" to "pending" // Example status
////        )
////
////        val updates = mutableMapOf<String, Any>()
////        updates["Bids/$newBidId"] = bidData
////        updates["Users/$userId/bids/placed/$newBidId"] = true             // Bidder's placed bids
////        updates["Users/$receiverId/bids/received/$newBidId"] = true       // Receiver's received bids
////        updates["Items/$itemId/bids/bidsReceived/$newBidId"] = true       // Bids received on the item
////         updates["Items/$offeredItemId/bids/bidsPlaced/$newBidId"] = true // Optional: Track on offered item
////
////        FirebaseDatabase.getInstance().reference.updateChildren(updates)
////            .addOnSuccessListener {
////                Toast.makeText(requireContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show()
////                // Notification will be sent by the Cloud Function
////                requireActivity().supportFragmentManager.popBackStack() // Or other navigation
////            }
////            .addOnFailureListener { exception ->
////                Log.e("AddBid", "Failed to add bid", exception)
////                Toast.makeText(
////                    requireContext(), "Failed to place bid: ${exception.message}", Toast.LENGTH_LONG
////                ).show()
////            }
////    }
//
//
//
//
//
//    private fun addBidToFirebase() {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Toast.makeText(requireContext(), "You must be logged in.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val itemId = selectedProduct?.id ?: return  // The item being bid on
//        val receiverId = selectedProduct?.userId ?: return  // The owner of that item
//        val offeredItemId = offeredItem?.id ?: return  // The item being offered in exchange
//
//        val bidsReference = FirebaseDatabase.getInstance().getReference("Bids")
//        val newBidId = bidsReference.push().key ?: return  // Generate unique bid ID
//
//        val bidData = mapOf(
//            "bidId" to newBidId,
//            "bidderId" to userId,
//            "receiverId" to receiverId,
//            "itemId" to itemId,
//            "offeredItemId" to offeredItemId,
//            "timestamp" to ServerValue.TIMESTAMP,
//            "status" to "pending"
//        )
//
//        val updates = mutableMapOf<String, Any>()
//
//        // Store bid in Bids collection
//        updates["Bids/$newBidId"] = bidData
//
//        // Store reference under User's placed bids
//        updates["Users/$userId/bids/placed/$newBidId"] = true
//
//        // Store reference under Receiver's received bids
//        updates["Users/$receiverId/bids/received/$newBidId"] = true
//
//        // Store reference under the Item's bids
//        updates["Items/$itemId/bids/bidsReceived/$newBidId"] = true
//        updates["Items/$offeredItemId/bids/bidsPlaced/$newBidId"] = true
//
//
//        FirebaseDatabase.getInstance().reference.updateChildren(updates).addOnSuccessListener {
//            Toast.makeText(requireContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show()
//            // Send notification to product owner
//            sendNotificationToProductOwner(selectedProduct!!)
//            requireActivity().supportFragmentManager.popBackStack()
//        }.addOnFailureListener { exception ->
//            Toast.makeText(
//                requireContext(), "Failed to add bid: ${exception.message}", Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
//
//    private fun sendNotificationToProductOwner(selectedProduct: Item) {
//        Log.d(
//            "FCM_Debug",
//            "sendNotificationToProductOwner() called for product: ${selectedProduct.id}"
//        )
//
//        // Get the product owner's FCM token from Firebase
//        val productOwnerTokenRef =
//            FirebaseDatabase.getInstance().getReference("Users").child(selectedProduct.userId)
//                .child("fcmToken")
//
//        productOwnerTokenRef.get().addOnSuccessListener { snapshot ->
//            val productOwnerToken = snapshot.value as? String
//            Log.d("FCM_Debug", "Product Owner Token: $productOwnerToken")
//
//            if (!productOwnerToken.isNullOrEmpty()) {
//                selectedProduct.details?.let { details ->
//                    // Prepare the notification data with product details
//                    val notificationData = JSONObject().apply {
//                        put("title", "New Bid on Your Product")
//                        put("body", "Someone placed a bid on: ${details.productName}")
//                    }
//
//                    // Prepare the FCM payload with additional product details
//                    val jsonPayload = JSONObject().apply {
//                        put("message", JSONObject().apply {
//                            put("token", productOwnerToken)
//                            put("notification", notificationData)
//                            put("data", JSONObject().apply {
//                                put("productId", selectedProduct.id)
//                                put("productName", details.productName)
////                                put("OfferedProduct", offeredProduct.productName)
//                            })
//                        })
//                    }
//
//                    // ✅ Send the notification using the OAuth-authenticated API request
//                    sendPushNotification(jsonPayload)
//                } ?: run {
//                    Toast.makeText(
//                        requireContext(), "Cannot get product details.", Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }
//
//    private fun sendPushNotification(payload: JSONObject) {
//        try {
//            val url = "https://fcm.googleapis.com/v1/projects/mytodolist-e6bae/messages:send"
//
//            val client = OkHttpClient()
//            val requestBody =
//                payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
//
//            val request = Request.Builder().url(url)
//                .addHeader("Authorization", "Bearer $token") // ✅ Use token from argument
//                .addHeader("Content-Type", "application/json").post(requestBody).build()
//
//            client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    Log.e("FCM", "Failed to send notification", e)
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    Log.d("FCM", "Notification sent successfully: ${response.body?.string()}")
//                }
//            })
//        } catch (e: Exception) {
//            Log.e("FCM", "Error sending push notification", e)
//        }
//    }
//
//    private fun fetchUserItemsFromFirebase() {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Toast.makeText(requireContext(), "You must be logged in.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        binding.progressBar.visibility = View.VISIBLE
//
//        val database = FirebaseDatabase.getInstance().getReference("Items")
//
//        database.orderByChild("userId").equalTo(userId) // Fetch only the logged-in user's items
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    productList.clear()
//                    if (snapshot.exists()) {
//                        for (itemSnapshot in snapshot.children) { // Loop through only the user's items
//                            val product = itemSnapshot.getValue(Item::class.java)
//                            if (product != null) {
//                                productList.add(product)
//                            }
//                        }
//                        productAdapter.notifyDataSetChanged()
//                    } else {
//                        Toast.makeText(
//                            requireContext(), "You have no items available.", Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    binding.progressBar.visibility = View.GONE
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    binding.progressBar.visibility = View.GONE
//                    Toast.makeText(
//                        requireContext(), "Failed to fetch your products.", Toast.LENGTH_SHORT
//                    ).show()
//                }
//            })
//    }
//
//    companion object {
//        val TAG = "check for null offered and selected item"
//    }
//}