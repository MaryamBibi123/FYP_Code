package com.example.signuplogina.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.R
import com.example.signuplogina.User
import com.example.signuplogina.adapter.FilteredUsersAdapter
import com.example.signuplogina.databinding.FragmentFilteredUsersBinding
import com.example.signuplogina.modal.FeedbackTags
import com.example.signuplogina.mvvm.FilteredUsersViewModel
import com.google.android.material.chip.Chip

class FilteredUsersFragment : Fragment() {

    private var _binding: FragmentFilteredUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FilteredUsersViewModel by viewModels()
    private lateinit var adapter: FilteredUsersAdapter // ❌ Was wrong type before

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilteredUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Initialize Adapter properly
        adapter = FilteredUsersAdapter(emptyList(), onUserClicked = { user ->navigateToUserProfile(user)})
        binding.recyclerUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUsers.adapter = adapter

        // ✅ Observe filtered users and update adapter
        viewModel.filteredUsers.observe(viewLifecycleOwner) {
            adapter.updateUsers(it)
        }

        // ✅ Observe available tags and create filter chips
        // Use FeedbackTags.allTags directly, not a filtered set from users
        FeedbackTags.allTags.forEach { feedbackTag ->
            val chip = Chip(requireContext()).apply {
                text = feedbackTag.label
                tag = feedbackTag // ✅ Now no naming conflict
                isCheckable = true
                setOnCheckedChangeListener { _, _ ->
                    applyChipFilters()
                }
            }
            binding.chipGroupFilter.addView(chip)
        }


//        viewModel.availableTags.observe(viewLifecycleOwner) { tags ->
//            binding.chipGroupFilter.removeAllViews()
//            tags.forEach { tag ->
//                val chip = Chip(requireContext()).apply {
//                    text = tag.label
//                    isCheckable = true
//                    setOnCheckedChangeListener { _, _ ->
//                        applyChipFilters()
//                    }
//                }
//                binding.chipGroupFilter.addView(chip)
//            }
//        }

        viewModel.loadUsers()
    }

    private fun navigateToUserProfile(user: User) {
        val bundle = Bundle().apply {
            putParcelable("user", user)
        }

        findNavController().navigate(R.id.action_filteredUsersFragment_to_userProfileFragment,bundle)
    }


    private fun applyChipFilters() {
        val selectedTags = mutableSetOf<FeedbackTags>()
        for (i in 0 until binding.chipGroupFilter.childCount) {
            val chip = binding.chipGroupFilter.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                val tag = chip.tag as? FeedbackTags
                if (tag != null) {
                    selectedTags.add(tag)
                }
            }
        }
        viewModel.applyFilters(selectedTags)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
