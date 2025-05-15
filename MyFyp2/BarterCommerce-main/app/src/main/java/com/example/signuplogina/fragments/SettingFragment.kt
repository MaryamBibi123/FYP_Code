package com.example.signuplogina.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.signuplogina.R
import com.bumptech.glide.Glide
import com.example.signuplogina.databinding.FragmentSettingBinding
import com.example.signuplogina.mvvm.ChatAppViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*

class SettingFragment : Fragment() {

    private lateinit var viewModel: ChatAppViewModel
    private lateinit var binding: FragmentSettingBinding

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var storageRef: FirebaseStorage
    private var uri: Uri? = null
    private lateinit var bitmap: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("NavigationDebug", "🟢 SettingsFragment Loaded")

        viewModel = ViewModelProvider(this)[ChatAppViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        storageRef = FirebaseStorage.getInstance()

        initActivityResultLaunchers()
        loadUserProfile()

        binding.settingBackBtn.setOnClickListener {
            view.findNavController().navigate(R.id.action_settingFragment_to_homeChatFragment)
        }

        binding.settingUpdateButton.setOnClickListener {
            updateUserProfile()
        }

        binding.settingUpdateImage.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun initActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { uploadImageToFirebaseStorage(it) }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val imageUri = result.data?.data
            imageUri?.let {
                val imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                uploadImageToFirebaseStorage(imageBitmap)
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = android.app.AlertDialog.Builder( requireContext())
        builder.setTitle("Choose your profile picture")
        builder.setItems(options) { dialog, item ->
            when (options[item]) {
                "Take Photo" -> cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                "Choose from Gallery" -> galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
    Log.d("UsersLog","log using Users")
        dbRef.get().addOnSuccessListener {
            val username = it.child("username").value?.toString()
            val profileImage = it.child("imageUrl").value?.toString()

            binding.settingName.setText(username)
            Glide.with(requireContext())
                .load(profileImage)
                .placeholder(R.drawable.person)
                .into(binding.settingUpdateImage)
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to load user profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val username = binding.settingName.text.toString()
        val profileImageUrl = uri?.toString()

        val updates = hashMapOf<String, Any>(
            "username" to username
        )

        profileImageUrl?.let {
            updates["imageUrl"] = it
        }

        FirebaseDatabase.getInstance().getReference("Users")
            .child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebaseStorage(imageBitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        bitmap = imageBitmap
        binding.settingUpdateImage.setImageBitmap(imageBitmap)

        val imageRef = storageRef.reference.child("Photos/${UUID.randomUUID()}.jpg")
        imageRef.putBytes(data).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                uri = downloadUri
                Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image!", Toast.LENGTH_SHORT).show()
        }
    }
}