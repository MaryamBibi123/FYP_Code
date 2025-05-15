// ChatFragment.kt
package com.example.signuplogina.fragments // Or your correct package

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast // Added
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat // Added
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.Utils
import com.example.signuplogina.adapter.MessageAdapter
import com.example.signuplogina.databinding.FragmentChatBinding
import com.example.signuplogina.databinding.FragmentChatfromHomeBinding
import com.example.signuplogina.modal.Messages
import com.example.signuplogina.mvvm.ChatAppViewModel
import com.example.signuplogina.mvvm.ChatBannerState // Import the BannerState

import de.hdodenhof.circleimageview.CircleImageView

class ChatFragment : Fragment() {

    lateinit var args: ChatFragmentArgs
    private var _binding: FragmentChatBinding?=null
    private val binding get() = _binding!!
    lateinit var viewModel: ChatAppViewModel
    lateinit var adapter: MessageAdapter
    lateinit var toolbar: Toolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Changed to non-nullable View
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        binding.lifecycleOwner = viewLifecycleOwner // Set lifecycle owner for DataBinding

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("NavigationDebug", "🟢 Chatfragment Loaded")

        toolbar = view.findViewById(R.id.toolBarChat) // Assuming toolBarChat is in fragment_chat.xml
        val circleImageView = toolbar.findViewById<CircleImageView>(R.id.chatImageViewUser)
        val textViewName = toolbar.findViewById<TextView>(R.id.chatUserName)
        val textViewStatus = view.findViewById<TextView>(R.id.chatUserStatus) // This ID seems to be outside toolbar in your XML
        val chatBackBtn = toolbar.findViewById<ImageView>(R.id.chatBackBtn)

        viewModel = ViewModelProvider(this).get(ChatAppViewModel::class.java)
        args = ChatFragmentArgs.fromBundle(requireArguments())

        binding.viewModel = viewModel // Assuming you use this in XML for viewModel.message

        Glide.with(view.context)
            .load(args.users.imageUrl ?: "") // Handle potential null
            .placeholder(R.drawable.person)
            .error(R.drawable.person)
            .dontAnimate()
            .into(circleImageView)

        textViewName.text = args.users.fullName
        textViewStatus.text = args.users.status // This might be a simple "Online" or similar, not the block status

        chatBackBtn.setOnClickListener {
            view.findNavController().navigate(R.id.action_chatFragment_to_homeChatFragment)
        }

        // --- NEW: Load participant statuses for banner and input disabling ---
        val currentUserId = Utils.getUidLoggedIn()
        val opponentUserId = args.users.userid // From navigation arguments

        if (currentUserId.isNotBlank() && !opponentUserId.isNullOrBlank()) {
            viewModel.loadChatParticipantsStatus(currentUserId, opponentUserId)
        } else {
            Log.e("ChatFragment", "Cannot load participant status: IDs missing.")
            binding.chatStatusBannerCard.visibility = View.VISIBLE // Assumes IDs are in fragment_chat.xml
            binding.tvChatStatusBannerMessage.text = getString(R.string.error_could_not_load_chat)
            binding.chatStatusBannerCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.overlay_rejected_background)
            )
            binding.editTextMessage.isEnabled = false
            binding.sendBtn.isEnabled = false
        }

        observeChatBannerState() // NEW: Call observer setup
        // --- END NEW ---


        binding.sendBtn.setOnClickListener {
            // --- NEW: Check banner state before sending ---
            if (viewModel.chatBannerState.value?.disablesChatInput == true) {
                Toast.makeText(context, getString(R.string.chat_currently_disabled), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // --- END NEW ---

            val messageToSend = binding.editTextMessage.text.toString().trim()
            if (messageToSend.isNotEmpty()) {
                val receiverId = args.users.userid.orEmpty()
                if (receiverId.isBlank()) {
                    Log.e("SendMessage", "❌ receiverId is null or blank in ChatFragment!")
//                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Assuming viewModel.message is two-way bound to editTextMessage
                // and viewModel.sendMessage uses viewModel.message.value
                viewModel.sendMessage(
                    Utils.getUidLoggedIn(),
                    receiverId,
                    args.users.fullName.orEmpty(),
                    args.users.imageUrl.orEmpty()
                )
                // If viewModel.sendMessage doesn't clear viewModel.message LiveData, clear EditText manually
                // binding.editTextMessage.text.clear()
            } else {
                Toast.makeText(context, "Messages Can't be Empty", Toast.LENGTH_SHORT).show()
            }
        }

        Log.e("userNull", "userNull ${args.users.userid}") // This log might be redundant if opponentUserId is already checked
        val opponentIdForMessages = args.users.userid
        if (!opponentIdForMessages.isNullOrBlank()){
            viewModel.getMessages(opponentIdForMessages).observe(viewLifecycleOwner, Observer { messages ->
                messages?.let { initRecyclerView(it.toMutableList()) }
            })
        } else {
            Log.e("ChatFragment", "Opponent ID is null or blank, cannot observe messages.")
        }
    }

    // --- NEW: Observer for Chat Banner State ---
    private fun observeChatBannerState() {
        viewModel.chatBannerState.observe(viewLifecycleOwner) { bannerState ->
            if (!isAdded || _binding == null) return@observe

            if (bannerState.isVisible) {
                binding.chatStatusBannerCard.visibility = View.VISIBLE // Assumes ID is in fragment_chat.xml
                binding.tvChatStatusBannerMessage.text = bannerState.message // Assumes ID is in fragment_chat.xml
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
    // --- END NEW ---

    private fun initRecyclerView(list: MutableList<Messages>) {
        adapter = MessageAdapter()
        val layoutManager = LinearLayoutManager(context)
        binding.messagesRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        adapter.setList(list)
        // adapter.notifyDataSetChanged() // setList should ideally handle this with DiffUtil or by calling it internally
        binding.messagesRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearChatParticipantListeners() // NEW: Clear listeners
        _binding = null
    }
}







//package com.example.signuplogina.fragments
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.widget.Toolbar
//import androidx.databinding.DataBindingUtil
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.bumptech.glide.Glide
//import com.example.signuplogina.R
//import com.example.signuplogina.Utils
//import com.example.signuplogina.adapter.MessageAdapter
//import com.example.signuplogina.databinding.FragmentChatBinding
//import com.example.signuplogina.modal.Messages
//import com.example.signuplogina.mvvm.ChatAppViewModel
//
//import de.hdodenhof.circleimageview.CircleImageView
//
//class ChatFragment : Fragment() {
//
//    lateinit var args: ChatFragmentArgs
//    lateinit var binding: FragmentChatBinding
//    lateinit var viewModel: ChatAppViewModel
//    lateinit var adapter: MessageAdapter
//    lateinit var toolbar: Toolbar
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_chat, container, false)
//        return binding.root
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d("NavigationDebug", "🟢 Chatfragment Loaded")
//
//        toolbar = view.findViewById(R.id.toolBarChat)
//        val circleImageView = toolbar.findViewById<CircleImageView>(R.id.chatImageViewUser)
//        val textViewName = toolbar.findViewById<TextView>(R.id.chatUserName)
//        val textViewStatus = view.findViewById<TextView>(R.id.chatUserStatus)
//        val chatBackBtn = toolbar.findViewById<ImageView>(R.id.chatBackBtn)
//
//        viewModel = ViewModelProvider(this).get(ChatAppViewModel::class.java)
//        args = ChatFragmentArgs.fromBundle(requireArguments())
//
//        binding.viewModel = viewModel
//        binding.lifecycleOwner = viewLifecycleOwner
//
//        // Load user profile picture with error handling
//        Glide.with(view.context)
//            .load(args.users.imageUrl)
//            .placeholder(R.drawable.person)
//            .error(R.drawable.person)
//            .dontAnimate()
//            .into(circleImageView)
//
//        textViewName.text = args.users.fullName
//        textViewStatus.text = args.users.status
//
//        chatBackBtn.setOnClickListener {
//            view.findNavController().navigate(R.id.action_chatFragment_to_homeChatFragment)
//        }
//
//        binding.sendBtn.setOnClickListener {
//            val message = binding.editTextMessage.text.toString().trim()
//            if (message.isNotEmpty()) {
////                viewModel.sendMessage(Utils.getUidLoggedIn(), args.users.userid!!, args.users.username!!, args.users.imageUrl!!)
//                val receiverId = args.users.userid.orEmpty()
//                if (receiverId.isBlank()) {
//                    Log.e("SendMessage", "❌ receiverId is null or blank in ChatFragment!")
//                    return@setOnClickListener
//                }
//
//                viewModel.sendMessage(
//                    Utils.getUidLoggedIn(),
//                    receiverId,
//                    args.users.fullName.orEmpty(),
//                    args.users.imageUrl.orEmpty()
//                )
//
//            }
//        }
//
//        Log.e("userNull", "userNull ${args.users.userid}")
//        viewModel.getMessages(args.users.userid!!).observe(viewLifecycleOwner, Observer {
//            initRecyclerView(it.toMutableList())
//        })
//    }
//
//    private fun initRecyclerView(list: MutableList<Messages>) {
//        adapter = MessageAdapter()
//        val layoutManager = LinearLayoutManager(context)
//        binding.messagesRecyclerView.layoutManager = layoutManager
//        layoutManager.stackFromEnd = true
//
//        adapter.setList(list)
//        adapter.notifyDataSetChanged()
//        binding.messagesRecyclerView.adapter = adapter
//    }
//}
