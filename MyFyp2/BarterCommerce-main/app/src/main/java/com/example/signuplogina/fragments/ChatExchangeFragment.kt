package com.example.signuplogina.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
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
import com.example.signuplogina.databinding.FragmentExchangeChatBinding
import com.example.signuplogina.modal.Messages
import com.example.signuplogina.mvvm.ChatAppViewModel
import com.example.signuplogina.mvvm.ExchangeAppViewModel

import de.hdodenhof.circleimageview.CircleImageView

class ChatExchangeFragment : Fragment() {

    lateinit var args: ChatExchangeFragmentArgs
    lateinit var binding: FragmentExchangeChatBinding
    lateinit var viewModel: ExchangeAppViewModel
    lateinit var adapter: MessageAdapter
    lateinit var toolbar: Toolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_exchange_chat, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("NavigationDebug", "🟢 Chatfragment Loaded")

        toolbar = view.findViewById(R.id.toolBarChat)
        val circleImageView = toolbar.findViewById<CircleImageView>(R.id.chatImageViewUser)
        val textViewName = toolbar.findViewById<TextView>(R.id.chatUserName)
        val textViewStatus = view.findViewById<TextView>(R.id.chatUserStatus)
        val chatBackBtn = toolbar.findViewById<ImageView>(R.id.chatBackBtn)

        viewModel = ViewModelProvider(this).get(ExchangeAppViewModel::class.java)
        args = ChatExchangeFragmentArgs.fromBundle(requireArguments())

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Load user profile picture with error handling
        Glide.with(view.context)
            .load(args.users.imageUrl)
            .placeholder(R.drawable.person)
            .error(R.drawable.person)
            .dontAnimate()
            .into(circleImageView)

        textViewName.text = args.users.fullName
        textViewStatus.text = args.users.status

        chatBackBtn.setOnClickListener {
            view.findNavController().navigate(R.id.action_chatExchangeFragment_to_homeExchangeFragment)
        }

        binding.sendBtn.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                val receiverId = args.users.userid.orEmpty()
//              val  bidId=args.bidId.orEmpty()
                if (receiverId.isBlank()) {
                    Log.e("SendMessage", "❌ receiverId is null or blank in ChatFragment!")
                    return@setOnClickListener
                }

                viewModel.sendMessage(
                    Utils.getUidLoggedIn(),
                    receiverId,
                    bidId="empty",// passing empty for testing ,
                    args.users.fullName.orEmpty(),
                    args.users.imageUrl.orEmpty()
                )

            }
        }
// the bidId is empty becasue user is one and there are multple room for 1 ,
        viewModel.getMessages(args.users.userid!!, bidId ="").observe(viewLifecycleOwner, Observer {
            initRecyclerView(it.toMutableList())
        })
    }

    private fun initRecyclerView(list: MutableList<Messages>) {
        adapter = MessageAdapter()
        val layoutManager = LinearLayoutManager(context)
        binding.messagesRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true

        adapter.setList(list)
        adapter.notifyDataSetChanged()
        binding.messagesRecyclerView.adapter = adapter
    }
}
