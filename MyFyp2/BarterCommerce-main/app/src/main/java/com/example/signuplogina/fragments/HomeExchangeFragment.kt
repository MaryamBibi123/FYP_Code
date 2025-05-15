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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.signuplogina.LoginActivity
import com.example.signuplogina.R
import com.example.signuplogina.adapter.RecentChatAdapter
import com.example.signuplogina.adapter.UserAdapter
import com.example.signuplogina.modal.RecentChats
import com.example.signuplogina.User
import com.example.signuplogina.databinding.FragmentExchangeHomeBinding
import com.example.signuplogina.mvvm.ChatAppViewModel
import com.example.signuplogina.mvvm.ExchangeAppViewModel

import com.google.firebase.auth.FirebaseAuth

class HomeExchangeFragment : Fragment(), UserAdapter.OnItemClickListener, RecentChatAdapter.onChatClicked {

    private lateinit var binding: FragmentExchangeHomeBinding
    private lateinit var viewModel: ExchangeAppViewModel
    private lateinit var userAdapter: UserAdapter
    private lateinit var recentChatAdapter: RecentChatAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    lateinit var args: HomeExchangeFragmentArgs
    lateinit var recentChats: List<RecentChats>
    private var bannerIsForcingDisable = false

    var bidId: String? = null
    var roomName: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentExchangeHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("NavigationDebug", "🟢 HomeFragment Loaded")



        // Initialize ViewModel, Firebase Auth, and adapters
        viewModel = ViewModelProvider(this)[ExchangeAppViewModel::class.java]
        firebaseAuth = FirebaseAuth.getInstance()
        args= HomeExchangeFragmentArgs.fromBundle(requireArguments())
        // Set the lifecycle owner for data binding
        binding.lifecycleOwner = viewLifecycleOwner

        // Initialize adapters
        userAdapter = UserAdapter()
        recentChatAdapter = RecentChatAdapter()
        recentChatAdapter.setModeFromExchangeRoom(true) // Set to true for Exchange Room


        // Set up RecyclerViews with their adapters and LayoutManagers
        binding.rvUsers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecentChats.layoutManager = LinearLayoutManager(context)
        binding.rvUsers.adapter = userAdapter
        binding.rvRecentChats.adapter = recentChatAdapter
        bidId=args.bidId
        roomName=args.roomName
        if (!bidId.isNullOrEmpty() && !roomName.isNullOrEmpty()) {
            viewModel.createExchangeRoomIfNotExists(bidId!!, roomName!!) {
                observeChatAndUsers() // ✅ This is the callback that runs AFTER room creation
            }
        } else {
            observeChatAndUsers()
        }

    }
    private fun observeChatAndUsers() {
        viewModel.getUsers().observe(viewLifecycleOwner) { users ->
            userAdapter.setList(users)
        }

        viewModel.getRecentUsersAllChats().observe(viewLifecycleOwner) { recentChats ->
            recentChatAdapter.setList(recentChats.toMutableList())
            this.recentChats = recentChats
        }

//        viewModel.imageUrl.observe(viewLifecycleOwner) { imageUrl ->
//            Glide.with(requireContext()).load(imageUrl).into(binding.tlImage)
//        }

        userAdapter.setOnClickListener(this)
        recentChatAdapter.setOnChatClickListener(this)
    }

    // Log out method
    private fun logOut() {
        firebaseAuth.signOut()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()  // Optional: finish the current activity if you want to close the app
    }

    // in the mobile navigation make these things

    // Handle user selection
    override fun onUserSelected(position: Int, users: User) {
        //implement the dialogue to show room and open room.

//        val action = HomeExchangeFragmentDirections.actionHomeExchangeFragmentToChatExchangeFragment(users)
//        view?.findNavController()?.navigate(action)
    }

    // Handle recent chat selection
    override fun getOnChatCLickedItem(position: Int, chatList: RecentChats) {
        val action = HomeExchangeFragmentDirections.actionHomeExchangeFragmentToHomeChatExchangeFragment(chatList)
        view?.findNavController()?.navigate(action)
    }
}
