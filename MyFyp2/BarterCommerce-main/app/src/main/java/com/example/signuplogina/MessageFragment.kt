//package com.example.signuplogina
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//
//class MessagesFragment : Fragment() {
//
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var messagesAdapter: MessagesAdapter
//    private val messagesList = mutableListOf<Message>()
//    private val db = FirebaseFirestore.getInstance()
//    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        return inflater.inflate(R.layout.fragment_message, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        recyclerView = view.findViewById(R.id.messagesRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(requireContext())
//        messagesAdapter = MessagesAdapter(messagesList, currentUserId) { message ->
//            val action = MessagesFragmentDirections.actionMessageFragmentToChatFragment(message.senderId, message.receiverId)
//            findNavController().navigate(action)
//        }
//
//        recyclerView.adapter = messagesAdapter
//
//        fetchMessages()
//    }
//
//    private fun fetchMessages() {
//        db.collection("chats")
//            .whereArrayContains("users", currentUserId) // Fetch chats where the user is involved
//            .addSnapshotListener { snapshot, exception ->
//                if (exception != null) {
//                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
//                    return@addSnapshotListener
//                }
//
//                if (snapshot != null) {
//                    messagesList.clear()
//                    for (document in snapshot.documents) {
//                        val lastMessage = document.get("lastMessage") as? String
//                        val senderId = document.getString("user1") ?: ""
//                        val receiverId = document.getString("user2") ?: ""
//                        val chatMessage = Message(document.id, lastMessage ?: "", System.currentTimeMillis(), senderId, receiverId)
//                        messagesList.add(chatMessage)
//                    }
//                    messagesAdapter.notifyDataSetChanged()
//                }
//            }
//    }
//}
