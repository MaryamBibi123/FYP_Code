package com.example.signuplogina.fragments

import AskTradeBattleBottomSheet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.adapter.PollPagerAdapter
import com.example.signuplogina.adapter.StoryAdapter
import com.example.signuplogina.databinding.FragmentBarterBuzzBinding
import com.example.signuplogina.modal.Story
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BarterBuzzFragment : Fragment() {

    private lateinit var binding: FragmentBarterBuzzBinding
    private lateinit var storyAdapter: StoryAdapter
    private val storyList = mutableListOf<Story>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBarterBuzzBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        loadStories()

        val adapter = PollPagerAdapter(this)
        binding.pollsViewPager.adapter = adapter

        val tabTitles = listOf("General", "Trade Battle")
        TabLayoutMediator(binding.pollsTabLayout, binding.pollsViewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Optional: FAB can switch to General tab (index 0)
//        binding.addPollButton.setOnClickListener {
//            binding.pollsViewPager.currentItem = 0
//        }

        binding.addPollButton.setOnClickListener {
            when (binding.pollsViewPager.currentItem) {
                0 -> AskGeneralPollBottomSheet().show(parentFragmentManager, "AskGeneralPoll")
                1 -> AskTradeBattleBottomSheet().show(parentFragmentManager, "AskTradeBattlePoll")
//                2 -> AskTradeBattleFragment().show(parentFragmentManager, "AskTradeBattlePoll")
            }
        }

    }

    private fun setupRecyclerView() {
        storyAdapter = StoryAdapter(storyList)
        binding.storyRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = storyAdapter
        }
    }





    private fun loadStories() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Stories")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                storyList.clear()

                for (userSnap in snapshot.children) {
                    val userId = userSnap.key

                    // Each user has only one story stored at "Stories/userId/story"
                    val storySnap = userSnap.child("story")
                    val story = storySnap.getValue(Story::class.java)

                    if (userId == currentUserId) {
                        if (story != null) {
                            // Add current user's story at the start of the list
                            storyList.add(0, story)
                        } else {
                            // No story, show default card for current user
                            val defaultStory = Story(
//                                storyId = "story",
                                itemId = "",
                                userId = currentUserId ?: "",
                                userName = "You",
                                userProfileUrl = null,
                                offerText = "No story yet",
                                wantText = "Tap + to add",
                                storyImageUrl = null,
                                timestamp = System.currentTimeMillis()
                            )
                            storyList.add(0, defaultStory)
                        }
                    } else {
                        // Add other users' stories
                        if (story != null) {
                            storyList.add(story)
                        }
                    }
                }

                storyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading stories", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
