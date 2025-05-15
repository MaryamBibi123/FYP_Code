package com.example.signuplogina

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityUploadProfilePicBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.IOException

class UploadProfilePicActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadProfilePicBinding
    private lateinit var authProfile: FirebaseAuth
    private lateinit var storageReference: StorageReference
    private lateinit var databaseReference: FirebaseDatabase
    private lateinit var firebaseUser: FirebaseUser

    private var uriImage: Uri? = null // Holds the selected/captured image URI
    private var cameraImageUri: Uri? = null // Temporary URI for camera image storage

    companion object {
        private const val TAG = "UploadProfilePicAct"
    }

    // --- Activity Result Launchers ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                uriImage = data.data // Store the selected gallery image URI
                Picasso.get().load(uriImage).into(binding.profileImage)
                binding.uploadPicButton.isEnabled = true // Enable upload after selection
            } else {
                Toast.makeText(this, "Failed to retrieve image from gallery", Toast.LENGTH_SHORT).show()
                binding.uploadPicButton.isEnabled = false
            }
        } else {
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
            binding.uploadPicButton.isEnabled = false
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Image successfully saved to cameraImageUri by the camera app
            uriImage = cameraImageUri // Use the URI where the camera saved the image
            if (uriImage != null) {
                Picasso.get().load(uriImage).into(binding.profileImage)
                binding.uploadPicButton.isEnabled = true
            } else {
                Toast.makeText(this, "Failed to get camera image URI", Toast.LENGTH_SHORT).show()
                binding.uploadPicButton.isEnabled = false
            }
        } else {
            Toast.makeText(this, "Taking picture cancelled", Toast.LENGTH_SHORT).show()
            binding.uploadPicButton.isEnabled = false
            // Clean up the temporary camera file URI if cancelled
            cameraImageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadProfilePicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // No need for supportActionBar if using default theme without action bar

        authProfile = FirebaseAuth.getInstance()
        firebaseUser = authProfile.currentUser ?: run {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java) // Redirect to login
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return // Exit onCreate early
        }

        // Use consistent storage path (choose profile_images or DisplayPics)
        storageReference = FirebaseStorage.getInstance().reference.child("DisplayPics")
        databaseReference = FirebaseDatabase.getInstance() // Root database reference

        loadUserProfilePicture() // Load existing picture (from DB or Auth)

        binding.uploadPicButton.isEnabled = false // Disable upload initially

        // --- Button Listeners (using binding) ---
        binding.imageViewBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Correct back press handling
        }

        binding.uploadPicChooseButton.setOnClickListener {
            showImageSourceDialog()
        }

        binding.uploadPicButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE // Show progress bar
            binding.uploadPicButton.isEnabled = false // Disable button during upload
            binding.uploadPicChooseButton.isEnabled = false
            uploadPicToFirebase() // Changed function name for clarity
        }
    }

    private fun loadUserProfilePicture() {
        // Option 1: Load from Realtime Database (more common if URL used elsewhere)
        val userDbRef = databaseReference.getReference("Users").child(firebaseUser.uid)
        userDbRef.child("imageUrl").get().addOnSuccessListener { dataSnapshot ->
            val imageUrl = dataSnapshot.getValue(String::class.java)
            // Use a proper placeholder drawable resource
            val placeholder = R.drawable.ic_profile
            if (!imageUrl.isNullOrEmpty() && imageUrl != "YOUR_DEFAULT_PLACEHOLDER_URL_STRING") { // Check against your actual placeholder
                Log.d(TAG, "Loading image from DB: $imageUrl")
                Picasso.get().load(imageUrl).placeholder(placeholder).error(placeholder).into(binding.profileImage)
            } else {
                Log.d(TAG, "No valid image URL in DB, checking Auth profile.")
                // Option 2: Fallback to loading from Firebase Auth profile if DB URL is missing/default
                loadFromAuthProfile(placeholder)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to load image URL from DB: ${it.message}, checking Auth profile.")
            val placeholder = R.drawable.ic_profile
            loadFromAuthProfile(placeholder) // Fallback on DB read failure
        }
    }

    // Helper to load from Auth profile (used as fallback)
    private fun loadFromAuthProfile(placeholderResId: Int){
        firebaseUser.photoUrl?.let { authUri ->
            Log.d(TAG, "Loading image from Auth Profile: $authUri")
            Picasso.get().load(authUri).placeholder(placeholderResId).error(placeholderResId).into(binding.profileImage)
        } ?: run {
            Log.d(TAG, "No image URL in Auth Profile either.")
            binding.profileImage.setImageResource(placeholderResId)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Choose Image From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> "No Permission For camera"
                    1 -> openGallery()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun openGallery() {
        // Use ACTION_PICK for a more standard gallery experience
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        // Create temporary URI to store camera image
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Profile Pic")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        // Use MediaStore to get a content URI in the public Pictures directory or app-specific storage
        cameraImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (cameraImageUri == null) {
            // Fallback or alternative storage method if insert fails
            Log.e(TAG, "Failed to create MediaStore entry for camera image.")
            Toast.makeText(this, "Could not prepare storage for camera", Toast.LENGTH_SHORT).show()
            // Consider using FileProvider method as in your original code if MediaStore fails consistently
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri) // Tell camera where to save the image

        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }


    private fun uploadPicToFirebase() {
        if (uriImage != null) {
            binding.progressBar.visibility = View.VISIBLE // Show progress bar here
            binding.uploadPicButton.isEnabled = false
            binding.uploadPicChooseButton.isEnabled = false

            val fileExtension = getFileExtension(uriImage!!) ?: "jpg" // Get actual extension or default

            // --- CORRECTED PATH ---
            // Create a reference like: DisplayPics -> userId -> profile.jpg
            // Use a consistent filename like "profile" or use the UID again if preferred.
            val fileReference: StorageReference = storageReference // This is reference to "DisplayPics"
                .child(firebaseUser.uid) // Navigate into the user's specific folder
                .child("profile.$fileExtension") // Name the file inside the user's folder

            Log.d(TAG, "Attempting to upload to path: ${fileReference.path}") // Log the target path

            // Upload file to Firebase Storage
            fileReference.putFile(uriImage!!)
                .addOnSuccessListener { taskSnapshot ->
                    // Get download URL
                    fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        val downloadUrlString = downloadUri.toString()
                        Log.d(TAG, "Storage Upload Successful. URL: $downloadUrlString")

                        // 1. Update Firebase Authentication Profile
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build()
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { authUpdateTask ->
                                if (authUpdateTask.isSuccessful) {
                                    Log.d(TAG, "Firebase Auth profile updated successfully.")
                                    // 2. Update Realtime Database URL (Important!)
                                    updateDatabaseImageUrl(downloadUrlString)
                                } else {
                                    // Log Auth update failure but still try to update DB
                                    Log.e(TAG, "Failed to update Firebase Auth profile", authUpdateTask.exception)
                                    Toast.makeText(this, "Warning: Failed to update Auth profile. Updating DB only.", Toast.LENGTH_LONG).show()
                                    updateDatabaseImageUrl(downloadUrlString) // Proceed with DB update
                                }
                            }

                    }.addOnFailureListener { urlError ->
                        handleUploadFailure("Failed to get download URL: ${urlError.message}", urlError)
                    }
                }
                .addOnFailureListener { uploadError ->
                    handleUploadFailure("Image Upload Failed: ${uploadError.message}", uploadError)
                }
                .addOnProgressListener { snapshot ->
                    // Optional: Update progress bar if determinate
                }
        } else {
            binding.progressBar.visibility = View.GONE // Hide progress bar if no image
            binding.uploadPicButton.isEnabled = true // Re-enable buttons
            binding.uploadPicChooseButton.isEnabled = true
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper Function to update Realtime Database (No change needed here)
    private fun updateDatabaseImageUrl(imageUrl: String) {
        // ... (Keep this function as it was)
        val userDbRef = databaseReference.getReference("Users").child(firebaseUser.uid)
        userDbRef.child("imageUrl").setValue(imageUrl)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.uploadPicButton.isEnabled = true
                binding.uploadPicChooseButton.isEnabled = true
                Log.d(TAG, "Realtime Database imageUrl updated successfully.")
                Toast.makeText(this, "Profile Picture Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                finish() // Close this activity
            }
            .addOnFailureListener { dbError ->
                handleUploadFailure("Failed to update Database URL: ${dbError.message}", dbError)
            }
    }

    // Centralized failure handling for upload process (No change needed here)
    private fun handleUploadFailure(message: String, exception: Exception?){
        // ... (Keep this function as it was)
        binding.progressBar.visibility = View.GONE
        binding.uploadPicButton.isEnabled = true
        binding.uploadPicChooseButton.isEnabled = true
        Log.e(TAG, message, exception)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // Helper function to get file extension (No change needed here)

    // Helper function to get file extension from URI
    private fun getFileExtension(uri: Uri): String? {
        return try {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
                ?: contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (name != null && name.contains(".")) {
                                name.substring(name.lastIndexOf(".") + 1)
                            } else null
                        } else null
                    } else null
                }
        } catch (e: Exception){
            Log.e(TAG, "Error getting file extension", e)
            null
        }
    }

    // No longer need createImageFile if using MediaStore directly for camera
    // No longer need onActivityResult
}




//
//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.widget.ImageView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.FileProvider
//import com.example.signuplogina.databinding.ActivityUploadProfilePicBinding
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.auth.UserProfileChangeRequest
//import com.google.firebase.storage.FirebaseStorage
//import com.google.firebase.storage.StorageReference
//import com.squareup.picasso.Picasso
//import java.io.File
//import java.io.IOException
//import java.text.SimpleDateFormat
//import java.util.*
//
//class UploadProfilePicActivity : AppCompatActivity() {
//    private lateinit var uriImage: Uri
//    private lateinit var cameraImageUri: Uri
//    private val PICK_IMAGE_REQUEST = 1
//    private val CAMERA_REQUEST_CODE = 2
//    private lateinit var binding: ActivityUploadProfilePicBinding
//    private lateinit var imageViewUploadPic: ImageView
//    private lateinit var authProfile: FirebaseAuth
//    private lateinit var storageReference: StorageReference
//    private lateinit var firebaseUser: FirebaseUser
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityUploadProfilePicBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        imageViewUploadPic = findViewById(R.id.profileImage)
//
//        supportActionBar?.title = "Upload Profile Picture"
//
//        authProfile = FirebaseAuth.getInstance()
//
//        firebaseUser = authProfile.currentUser ?: run {
//            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        val backButton: ImageView = findViewById(R.id.imageView_Back)
//        backButton.setOnClickListener {
//            onBackPressed() // Go back to the previous activity or fragment
//        }
//
//        storageReference = FirebaseStorage.getInstance().reference.child("DisplayPics")
//
//        firebaseUser.photoUrl?.let { uri ->
//            Picasso.get().load(uri).into(imageViewUploadPic)
//        }
//
//        binding.uploadPicChooseButton.setOnClickListener {
//            showImageSourceDialog()
//        }
//
//        binding.uploadPicButton.setOnClickListener {
//            uploadPic()
//        }
//    }
//
//    private fun showImageSourceDialog() {
//        val options = arrayOf("Camera", "Gallery")
//        AlertDialog.Builder(this)
//            .setTitle("Choose an option")
//            .setItems(options) { _, which ->
//                when (which) {
//                    0 -> openCamera()
//                    1 -> openGallery()
//                }
//            }
//            .show()
//    }
//
//    private fun openGallery() {
//        val intent = Intent().apply {
//            type = "image/*"
//            action = Intent.ACTION_GET_CONTENT
//        }
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)
//    }
//
//    @SuppressLint("QueryPermissionsNeeded")
//    private fun openCamera() {
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        val photoFile = createImageFile()
//
//        photoFile?.let {
//            cameraImageUri = FileProvider.getUriForFile(
//                this,
//                "com.example.signuplogina.fileprovider",
//                it
//            )
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
//            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            startActivityForResult(intent, CAMERA_REQUEST_CODE)
//        } ?: run {
//            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun createImageFile(): File? {
//        return try {
//            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
//        } catch (e: IOException) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//    private fun uploadPic() {
//        if (::uriImage.isInitialized) {
//            val fileReference = storageReference.child("${firebaseUser.uid}.jpg")
//            fileReference.putFile(uriImage).addOnSuccessListener { taskSnapshot ->
//                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
//                    val profileUpdates = UserProfileChangeRequest.Builder()
//                        .setPhotoUri(uri)
//                        .build()
//
//                    firebaseUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            Toast.makeText(this, "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }.addOnFailureListener {
//                    Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()
//                }
//            }.addOnFailureListener {
//                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == Activity.RESULT_OK) {
//            when (requestCode) {
//                PICK_IMAGE_REQUEST -> {
//                    uriImage = data?.data!!
//                    imageViewUploadPic.setImageURI(uriImage)
//                }
//                CAMERA_REQUEST_CODE -> {
//                    uriImage = cameraImageUri
//                    imageViewUploadPic.setImageURI(uriImage)
//                }
//            }
//        }
//    }
//}
