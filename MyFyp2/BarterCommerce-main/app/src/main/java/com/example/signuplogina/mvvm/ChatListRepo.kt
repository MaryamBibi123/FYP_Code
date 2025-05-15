package com.example.signuplogina.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.signuplogina.Utils
import com.example.signuplogina.modal.RecentChats

import com.google.firebase.database.*

class ChatListRepo {

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    // Reusable internal function
    private fun getChatListFromNode(nodeName: String): LiveData<List<RecentChats>> {
        val mainChatList = MutableLiveData<List<RecentChats>>()
        val userId = Utils.getUidLoggedIn()

        val databaseRef = firebaseDatabase.getReference(nodeName).child(userId)

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<RecentChats>()

                snapshot.children.forEach { data ->
                    val chatItem = data.getValue(RecentChats::class.java)
                    chatItem?.let { chatList.add(it) }
                }

                chatList.sortByDescending { it.time }
                mainChatList.value = chatList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "getChatListFromNode error: ${error.message}")
            }
        })

        return mainChatList
    }

    // 🔹 For normal chat
    fun getAllChatList(): LiveData<List<RecentChats>> {
        return getChatListFromNode("Conversations/chats")
    }

    // 🔹 For exchange room chat
    fun getAllExchangeChatsForCurrentUser(

    ): LiveData<List<RecentChats>> {
        val currentUserId = Utils.getUidLoggedIn()

        // Declare MutableLiveData, which can be updated internally
        val mainChatList = MutableLiveData<List<RecentChats>>()

        val dbRef = FirebaseDatabase.getInstance().reference
            .child("Conversations")
            .child("ExchangeChats")
            .child(currentUserId)

        // Add a listener to fetch data from Firebase
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<RecentChats>()

                // Process the snapshot and create a list of RecentChats
                for (otherUserSnapshot in snapshot.children) {
                    for (bidSnapshot in otherUserSnapshot.children) {
                        val chat = bidSnapshot.getValue(RecentChats::class.java)
                        chat?.let {
                            chatList.add(it)
                            Log.e("the main list","main ${chatList}")

                        }
                    }
                }

                // Update the MutableLiveData with the new list of chats
                mainChatList.value = chatList
//                Log.e("the main list","main ${chatList}")
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle failure (e.g., log the error or return an empty list)
                mainChatList.value = emptyList()
            }
        })

        // Return the MutableLiveData as LiveData to ensure immutability outside the repository
        return mainChatList
    }

}
