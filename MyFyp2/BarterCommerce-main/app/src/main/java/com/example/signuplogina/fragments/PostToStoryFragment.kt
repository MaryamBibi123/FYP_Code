package com.example.signuplogina.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.example.signuplogina.databinding.FragmentPostToStoryBinding
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.signuplogina.Item
import com.example.signuplogina.R
import com.example.signuplogina.modal.Story
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.FirebaseDatabase

class PostToStoryFragment : Fragment() {

    private lateinit var binding: FragmentPostToStoryBinding
    private lateinit var itemImageUrl: String
    private lateinit var itemOffered: String
    private lateinit var itemWanted: String
    private lateinit var item: Item



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostToStoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Receive data from arguments or ViewModel
        arguments?.let {
//            itemImageUrl = it.getString("imageUrl") ?: ""
//            itemOffered = it.getString("offered") ?: ""
//            itemWanted = it.getString("wanted") ?: ""
            item=it.getParcelable("item")!!
        }
        itemImageUrl = item.details.imageUrls[0]
        itemOffered = item.details.productName


        // Load image
        Glide.with(this)
            .load(itemImageUrl)
            .placeholder(R.drawable.ic_camera)
            .into(binding.itemImage)

        // Set texts
        binding.offeredText.text = "You are offering: $itemOffered"
       itemWanted= binding.wantedET.text.toString()
        binding.wantedText.text = "You want: $itemWanted"

        // Handle post button click
        binding.postToStoryBtn.setOnClickListener {
            postToFirebaseStory()
        }
    }

    private fun postToFirebaseStory() {
        val user = FirebaseAuth.getInstance().currentUser ?: return


        val ref = FirebaseDatabase.getInstance()
            .getReference("Stories")
            .child(user.uid)
//            .push()
            .child("story") // 👈 Fixed key, replaces the old one

        val story =
            Story(
//                storyId = ref.key ?: "" , // Get the generated push key
                itemId = item.id,
                userId = user.uid,
                userName = user.displayName ?: "Anonymous",
                userProfileUrl = user.photoUrl?.toString(),
                offerText = itemOffered,
                wantText = itemWanted,
                storyImageUrl = itemImageUrl,
                timestamp = System.currentTimeMillis()
            )


        ref.setValue(story).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Posted to story!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_postStoryFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Failed to post story", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
