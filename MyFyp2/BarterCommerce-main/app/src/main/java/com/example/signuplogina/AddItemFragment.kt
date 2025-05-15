package com.example.signuplogina

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.navigation.fragment.findNavController
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddItemFragment : Fragment() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseItems: DatabaseReference
    private lateinit var storageReference: StorageReference

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: ImageAdapter


    private lateinit var productNameEditText: EditText


    private lateinit var descriptionEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var conditionSpinner: Spinner
    private lateinit var availabilitySpinner: Spinner
    private lateinit var uploadButton: Button

    private lateinit var price: EditText
    private lateinit var rvProductImages: RecyclerView
    private lateinit var backButton: ImageView
    private lateinit var chooseImageButton: Button // Reference to the new button


    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_item, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        databaseItems = FirebaseDatabase.getInstance().getReference("Items")
        storageReference = FirebaseStorage.getInstance().reference
        productNameEditText = view.findViewById(R.id.editTextProductName)
        descriptionEditText = view.findViewById(R.id.editTextDescription)
        categorySpinner = view.findViewById(R.id.spinnerCategory)
        conditionSpinner = view.findViewById(R.id.spinnerCondition)
        availabilitySpinner = view.findViewById(R.id.spinnerAvailability)
        uploadButton = view.findViewById(R.id.btnUploadItem)
        rvProductImages = view.findViewById(R.id.rvProductImages)
        price = view.findViewById(R.id.editTextPrice)
        backButton = view.findViewById(R.id.ivMenu)
        chooseImageButton = view.findViewById(R.id.btnChooseImage) // Initialize the button


        val categories = arrayOf(
            "Please select a category",
            "Electronics",
            "Furniture",
            "Cosmetics",
            "Clothing",
            "Books",
            "Vehicles"
        )
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        categorySpinner.adapter = categoryAdapter

        val conditions = arrayOf("Please select condition", "Good", "Used", "Fair")
        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            conditions
        )
        conditionSpinner.adapter = conditionAdapter

        val availabilityNumbers = (1..10).map { it.toString() }.toTypedArray()
        val availabilityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            availabilityNumbers
        )
        availabilitySpinner.adapter = availabilityAdapter

        // Set up listeners
        chooseImageButton.setOnClickListener { openGallery() } // Open gallery when button clicked
//        productImageView.setOnClickListener { openGallery() }
        uploadButton.setOnClickListener { uploadItemToFirebase() }
        backButton.setOnClickListener { requireActivity().onBackPressed() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the BottomNavigationBar when this fragment is displayed
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.GONE
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvProductImages)
        imagesAdapter = ImageAdapter(selectedImageUris, true)
        recyclerView.adapter = imagesAdapter


        val fab = requireActivity().findViewById<View>(R.id.fab)
        fab?.visibility = View.GONE
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.clipData?.let { clipData ->
                if (selectedImageUris.size + clipData.itemCount > 3) {
                    Toast.makeText(
                        requireContext(),
                        "You can only select 3 images.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                for (i in 0 until clipData.itemCount) {
                    selectedImageUris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                if (selectedImageUris.size >= 3) {
                    Toast.makeText(
                        requireContext(),
                        "You can only select 3 images.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                selectedImageUris.add(uri)
            }

            // Disable the button if 3 images are selected
            if (selectedImageUris.size == 3) {
                chooseImageButton.isEnabled = false
            }

            // Refresh RecyclerView
            imagesAdapter.notifyDataSetChanged()
        }
    }

    private fun uploadItemToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val itemId = databaseItems.push().key ?: return

        // Ensure exactly 3 images are selected
        if (selectedImageUris.size != 3) {
            Toast.makeText(
                requireContext(),
                "You must select exactly 3 images.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uploadedUrls = mutableListOf<String>()

        for (imageUri in selectedImageUris) {
            val storageRef =
                storageReference.child("Items/$itemId/$userId/images/${System.currentTimeMillis()}.jpg")

            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        uploadedUrls.add(uri.toString())

                        // When all images are uploaded, save item data to Firebase
                        if (uploadedUrls.size == selectedImageUris.size) {
                            saveItemDataToDatabase(itemId, uploadedUrls)
                        }
                    }
                }

                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Image upload failed.", Toast.LENGTH_SHORT)
                        .show()
                }
        }

    }

    private fun saveItemDataToDatabase(itemId: String, imageUrls: List<String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val productName = productNameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val condition = conditionSpinner.selectedItem.toString()
        val availability = availabilitySpinner.selectedItem.toString().toIntOrNull() ?: 0
        val price = price.text.toString().toDoubleOrNull() ?: 0.0

        // Validations
        if (productName.isEmpty() || description.isEmpty() || price <= 0.0) {
            Toast.makeText(
                requireContext(),
                "Please fill all required fields correctly.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (category == "Please select a category") {
            Toast.makeText(requireContext(), "Please select a valid category.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (condition == "Please select condition") {
            Toast.makeText(requireContext(), "Please select a valid condition.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (availability <= 0) {
            Toast.makeText(
                requireContext(),
                "Please select valid availability.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val itemDetails = ItemDetails(
            productName = productName,
            description = description,
            category = category,
            price = price,
            condition = condition,

            availability = availability,
            imageUrls = imageUrls,
            timestamp = System.currentTimeMillis(),
        )

        val item = Item(
            id = itemId, // Include the generated ID
            userId = userId,
            details = itemDetails,
            status = "pending",
            available=false


        )

        databaseItems.child(itemId).setValue(item).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Item uploaded successfully", Toast.LENGTH_SHORT)
                    .show()
                // store the item id to true in the users/items
                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                    .child("items").child(itemId)
                userRef.setValue(true).addOnCompleteListener { userTask ->
                    if (userTask.isSuccessful) {
                        Log.d(TAG, "Item ID stored in Users/Items successfully")
// post to story after uploading ,
                        val bundle = bundleOf(
                            "item" to item
                        )


                        findNavController().navigate(
                            R.id.action_addItemFragment_to_postToStoryFragment,
                            bundle
                        )
                    } else {
                        Log.e(TAG, "Failed to store Item ID in Users/Items: ${userTask.exception}")
                    }

                }
            } else {
                Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
            }
        }

        clearInputFields()
    }

    private fun clearInputFields() {
        productNameEditText.text.clear()
        descriptionEditText.text.clear()
        categorySpinner.setSelection(0)
        conditionSpinner.setSelection(0)
        availabilitySpinner.setSelection(0)
        selectedImageUri = null

        //disable the add button
        uploadButton.isEnabled = false//

    }


    override fun onDestroyView() {
        super.onDestroyView()

        // Show the BottomNavigationBar when leaving this fragment
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.VISIBLE

        val fab = requireActivity().findViewById<View>(R.id.fab)
        fab?.visibility = View.VISIBLE
    }

    companion object {
        private const val GALLERY_REQUEST_CODE = 1001
    }
}
