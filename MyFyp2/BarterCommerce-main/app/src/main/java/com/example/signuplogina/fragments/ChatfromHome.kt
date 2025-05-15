package com.example.signuplogina.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.Utils
import com.example.signuplogina.adapter.MessageAdapter
import com.example.signuplogina.databinding.FragmentChatfromHomeBinding
import com.example.signuplogina.modal.Messages
import com.example.signuplogina.mvvm.ChatAppViewModel

import de.hdodenhof.circleimageview.CircleImageView

class ChatfromHome : Fragment() {

    lateinit var args: ChatfromHomeArgs
    private var _binding: FragmentChatfromHomeBinding?=null
    private val binding get() = _binding!!
    lateinit var viewModel: ChatAppViewModel
    lateinit var adapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chatfrom_home, container, false)
        binding.lifecycleOwner = viewLifecycleOwner // Set lifecycle owner for DataBinding

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("NavigationDebug", "🟢 ChatfromHOme Loaded")

        // Initializing the arguments, view model, and binding lifecycle owner
        args = ChatfromHomeArgs.fromBundle(requireArguments())
        viewModel = ViewModelProvider(this).get(ChatAppViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Set up toolbar with Glide for image and text view for user name
        val circleImageView = view.findViewById<CircleImageView>(R.id.chatImageViewUser)
        val textViewName = view.findViewById<TextView>(R.id.chatUserName)

        Glide.with(view.context)
            .load(args.recentchats.friendsimage!!)
            .placeholder(R.drawable.person)
            .dontAnimate()
            .into(circleImageView)

        textViewName.text = args.recentchats.name

        // Back button click listener
        binding.chatBackBtn.setOnClickListener {
            view.findNavController().navigate(R.id.action_chatfromHome_to_homeChatFragment)
        }

        // --- NEW: Load participant statuses for banner and input disabling ---
        val currentUserId = Utils.getUidLoggedIn()
        val opponentUserId = args.recentchats.friendid

        if (currentUserId.isNotBlank() && !opponentUserId.isNullOrBlank()) {
            viewModel.loadChatParticipantsStatus(currentUserId, opponentUserId)
        } else {
            Log.e("ChatfromHome", "Cannot load participant status: IDs missing.")
            binding.chatStatusBannerCard.visibility = View.VISIBLE
            binding.tvChatStatusBannerMessage.text = getString(R.string.error_could_not_load_chat)
            binding.chatStatusBannerCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.overlay_rejected_background)
            )
            binding.editTextMessage.isEnabled = false
            binding.sendBtn.isEnabled = false
        }

        observeChatBannerState() // NEW: Call observer setup
        // --- END NEW ---

        // Send message button click listener
        binding.sendBtn.setOnClickListener {
            // --- NEW: Check banner state before sending ---
            if (viewModel.chatBannerState.value?.disablesChatInput == true) {
                Toast.makeText(context, getString(R.string.chat_currently_disabled), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // --- END NEW ---

            // Your existing send logic:
            val receiverId = args.recentchats.friendid.orEmpty()
            if (receiverId.isBlank()) {
                Log.e("SendMessage", "❌ receiverId is null or blank in ChatfromHome!")
                return@setOnClickListener
            }

           if (binding.editTextMessage.text.toString().trim().isNotEmpty()) { // Check if there's actually text to send
                viewModel.sendMessage(
                    Utils.getUidLoggedIn(),
                    receiverId,
                    args.recentchats.name.orEmpty(),
                    args.recentchats.friendsimage.orEmpty()
                )

            } else {
                // Optional: Toast if user clicks send with empty message
                // Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        val friendIdForMessages = args.recentchats.friendid
        if (!friendIdForMessages.isNullOrBlank()) {
            viewModel.getMessages(friendIdForMessages).observe(viewLifecycleOwner, Observer { messages ->
                messages?.let { initRecyclerView(it.toMutableList()) }
            })
        } else {
            Log.e("ChatfromHome", "Friend ID is null or blank, cannot observe messages.")
        }
    }

    private fun observeChatBannerState() {
        viewModel.chatBannerState.observe(viewLifecycleOwner) { bannerState ->
            if (!isAdded || _binding == null) return@observe

            if (bannerState.isVisible) {
                binding.chatStatusBannerCard.visibility = View.VISIBLE
                binding.tvChatStatusBannerMessage.text = bannerState.message
                binding.chatStatusBannerCard.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.transparent)
                )
            } else {
                binding.chatStatusBannerCard.visibility = View.GONE
            }

            if (bannerState.disablesChatInput) {
                binding.editTextMessage.isEnabled = false
                binding.sendBtn.isEnabled = false
                binding.editTextMessage.hint = getString(R.string.chat_disabled_hint)
            } else {
                binding.editTextMessage.isEnabled = true
                binding.sendBtn.isEnabled = true
                binding.editTextMessage.hint = getString(R.string.enter_message_hint)
            }
        }
    }

    private fun initRecyclerView(list: MutableList<Messages>) {
        adapter = MessageAdapter()
        val layoutManager = LinearLayoutManager(context)
        binding.messagesRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        adapter.setList(list)
        binding.messagesRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearChatParticipantListeners()
        _binding = null
    }
}











//package com.example.signuplogina.fragments
//
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.appcompat.widget.Toolbar
//import androidx.databinding.DataBindingUtil
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.bumptech.glide.Glide
//import com.example.signuplogina.R
//import com.example.signuplogina.Utils
//import com.example.signuplogina.adapter.MessageAdapter
//import com.example.signuplogina.databinding.FragmentChatfromHomeBinding
//import com.example.signuplogina.modal.Messages
//import com.example.signuplogina.mvvm.ChatAppViewModel
//
//import de.hdodenhof.circleimageview.CircleImageView
//
//class ChatfromHome : Fragment() {
//
//    lateinit var args: ChatfromHomeArgs
//    lateinit var binding: FragmentChatfromHomeBinding
//    lateinit var viewModel: ChatAppViewModel
//    lateinit var adapter: MessageAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chatfrom_home, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d("NavigationDebug", "🟢 ChatfromHOme Loaded")
//
//        // Initializing the arguments, view model, and binding lifecycle owner
//        args = ChatfromHomeArgs.fromBundle(requireArguments())
//        viewModel = ViewModelProvider(this).get(ChatAppViewModel::class.java)
//        binding.viewModel = viewModel
//        binding.lifecycleOwner = viewLifecycleOwner
//
//        // Set up toolbar with Glide for image and text view for user name
//        val circleImageView = view.findViewById<CircleImageView>(R.id.chatImageViewUser)
//        val textViewName = view.findViewById<TextView>(R.id.chatUserName)
//
//        Glide.with(view.context)
//            .load(args.recentchats.friendsimage!!)
//            .placeholder(R.drawable.person)
//            .dontAnimate()
//            .into(circleImageView)
//
//        textViewName.text = args.recentchats.name
//
//        // Back button click listener
//        binding.chatBackBtn.setOnClickListener {
//            view.findNavController().navigate(R.id.action_chatfromHome_to_homeChatFragment)
//        }
//
//        // Send message button click listener
//        binding.sendBtn.setOnClickListener {
////            viewModel.sendMessage(
////                Utils.getUidLoggedIn(),
////                args.recentchats.friendid!!,
////                args.recentchats.name!!,
////                args.recentchats.friendsimage!!
////            )
//            val receiverId = args.recentchats.friendid.orEmpty()
//            if (receiverId.isBlank()) {
//                Log.e("SendMessage", "❌ receiverId is null or blank in ChatfromHome!")
//                return@setOnClickListener
//            }
//
//            viewModel.sendMessage(
//                Utils.getUidLoggedIn(),
//                receiverId,
//                args.recentchats.name.orEmpty(),
//                args.recentchats.friendsimage.orEmpty()
//            )
//
//        }
//
//        // Observe the messages from the ViewModel
//        viewModel.getMessages(args.recentchats.friendid!!).observe(viewLifecycleOwner, Observer {
//            initRecyclerView(it.toMutableList())
//        })
//    }
//
//    // Method to initialize RecyclerView
//    private fun initRecyclerView(list: MutableList<Messages>) {
//        // Set up the adapter
//        adapter = MessageAdapter()
//
//        // Initialize LayoutManager
//        val layoutManager = LinearLayoutManager(context)
//        binding.messagesRecyclerView.layoutManager = layoutManager
//        layoutManager.stackFromEnd = true
//
//        // Set the list of messages to the adapter
//        adapter.setList(list)
//
//        // Bind the adapter to the RecyclerView
//        binding.messagesRecyclerView.adapter = adapter
//    }
//}
