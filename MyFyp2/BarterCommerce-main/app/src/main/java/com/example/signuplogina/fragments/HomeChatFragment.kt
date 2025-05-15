@file:Suppress("DEPRECATION")

package com.example.signuplogina.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
//import androidx.navigation.fragment.findNavController

import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.signuplogina.LoginActivity
import com.example.signuplogina.R
import com.example.signuplogina.adapter.RecentChatAdapter
import com.example.signuplogina.adapter.UserAdapter
import com.example.signuplogina.databinding.FragmentChatHomeBinding
import com.example.signuplogina.modal.RecentChats
import com.example.signuplogina.User
import com.example.signuplogina.mvvm.ChatAppViewModel

import com.google.firebase.auth.FirebaseAuth

class HomeChatFragment : Fragment(), UserAdapter.OnItemClickListener, RecentChatAdapter.onChatClicked {

    private lateinit var binding: FragmentChatHomeBinding
    private lateinit var viewModel: ChatAppViewModel
    private lateinit var userAdapter: UserAdapter
    private lateinit var recentChatAdapter: RecentChatAdapter
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("NavigationDebug", "🟢 HomeFragment Loaded")



        // Initialize ViewModel, Firebase Auth, and adapters
        viewModel = ViewModelProvider(this)[ChatAppViewModel::class.java]
        firebaseAuth = FirebaseAuth.getInstance()

        // Set the lifecycle owner for data binding
        binding.lifecycleOwner = viewLifecycleOwner

        // Initialize adapters
        userAdapter = UserAdapter()
        recentChatAdapter = RecentChatAdapter()

        // Set up RecyclerViews with their adapters and LayoutManagers
        binding.rvUsers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecentChats.layoutManager = LinearLayoutManager(context)
        binding.rvUsers.adapter = userAdapter
        binding.rvRecentChats.adapter = recentChatAdapter
        binding.tvShowAllUsers.setOnClickListener {
            findNavController().navigate(R.id.action_homeChatFragment_to_filteredUsersFragment)
        }
        // Observe and display users
        viewModel.getUsers().observe(viewLifecycleOwner, Observer { users ->
            userAdapter.setList(users)
        })

        binding.ExchangeChats.setOnClickListener {
            val action= HomeChatFragmentDirections.actionHomeChatFragmentToHomeExchangeFragment()
            view.findNavController().navigate(action)
        }
        // Observe and display recent chats
        viewModel.getRecentUsersAllChats().observe(viewLifecycleOwner, Observer { recentChats ->
            recentChatAdapter.setList(recentChats.toMutableList())
        })
        //commented because the path in the fetchRecentChats is wrong
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        userId?.let {
//            recentChatAdapter.fetchRecentChats(it) // <- This calls your adapter function
//        }

        // Set listeners for the adapters
        userAdapter.setOnClickListener(this)
        recentChatAdapter.setOnChatClickListener(this)

        // Observe the profile image from the ViewModel
//        viewModel.imageUrl.observe(viewLifecycleOwner, Observer { imageUrl ->
//            Glide.with(requireContext()).load(imageUrl).into(binding.tlImage)
//        })

        // Toolbar: Log out button
//        binding.toolbarMain.findViewById<View>(R.id.logOut).setOnClickListener {
//            logOut()
//        }

        // Toolbar: Navigate to Settings
//        binding.tlImage.setOnClickListener {
//            view.findNavController().navigate(R.id.action_homeChatFragment_to_settingFragment)
//        }
    }

    // Log out method
    private fun logOut() {
        firebaseAuth.signOut()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()  // Optional: finish the current activity if you want to close the app
    }

    // Handle user selection
    override fun onUserSelected(position: Int, users: User) {
        val action = HomeChatFragmentDirections.actionHomeChatFragmentToChatFragment(users)
        view?.findNavController()?.navigate(action)
    }

    // Handle recent chat selection
    override fun getOnChatCLickedItem(position: Int, chatList: RecentChats) {
        val action = HomeChatFragmentDirections.actionHomeChatFragmentToChatfromHome(chatList)
        view?.findNavController()?.navigate(action)
    }
}
