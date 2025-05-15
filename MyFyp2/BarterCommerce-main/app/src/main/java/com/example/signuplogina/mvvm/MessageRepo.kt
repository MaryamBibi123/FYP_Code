package com.example.signuplogina.mvvm

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.signuplogina.Utils
import com.example.signuplogina.modal.Messages
import com.google.firebase.database.*

class MessageRepo {

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    val currentUserId = Utils.getUidLoggedIn()

    // Shared internal method
    private fun getMessagesFromNode(
        friendId: String,
        nodeName: String,
        bidId: String? = null
    ): LiveData<List<Messages>> {
        val messages = MutableLiveData<List<Messages>>()
        val uniqueId: String
        if (nodeName == "Messages/chats") {
            uniqueId = listOf(currentUserId, friendId).sorted().joinToString("")

        } else {
            uniqueId = "$bidId-${listOf(currentUserId, friendId).sorted().joinToString("")}"
        }
        val databaseRef = firebaseDatabase.getReference(nodeName)

        databaseRef.child(uniqueId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messageList = mutableListOf<Messages>()

                    snapshot.children.forEach { data ->
                        val message = data.getValue(Messages::class.java)

                        if (
                            (message?.sender == currentUserId && message.receiver == friendId) ||
                            (message?.sender == friendId && message.receiver == currentUserId)
                        ) {
                            messageList.add(message)
                        }
                    }

                    messages.value = messageList.sortedBy { it.time }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "getMessages error from $nodeName: ${error.message}")
                }
            })

        return messages
    }

    // For regular chats
    fun getChatMessages(friendId: String): LiveData<List<Messages>> {
        return getMessagesFromNode(friendId, "Messages/chats", null)
    }

    // For exchange room chats
    fun getExchangeMessages(friendId: String, bidId: String): LiveData<List<Messages>> {
        return getMessagesFromNode(friendId, "Messages/ExchangeChats", bidId)
    }

    fun updateMeetingDetails(
        location: String,
        date: String,
        time: String,
        roomId: String,
        bidId: String
    ): Boolean {
        var meetingStored = false
        val meetingRef = FirebaseDatabase.getInstance()
            .getReference("Messages")
            .child("ExchangeChats")
            .child(roomId)
            .child("meeting")
        val agreementReference =
            FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidId)


        val meetingData = mapOf(
            "location" to location,
            "date" to date,
            "time" to time,
            "setBy" to currentUserId,
            "confirmed" to false
        )
        val agreementData = mapOf(
            "meetingLocation" to location,
            "meetingTime" to "$date at $time"
        )
//set valusse or update?
        meetingRef.updateChildren(meetingData)
            .addOnSuccessListener {
                meetingStored = true
            }
            .addOnFailureListener {
                meetingStored = false
            }

        agreementReference.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // ✅ Agreement node exists, so we can safely update its meeting info
                agreementReference.updateChildren(agreementData)
                    .addOnSuccessListener {
                        meetingStored = true
                    }
                    .addOnFailureListener {
                        meetingStored = false
                    }
            } else {
                // ❌ Don't update if agreement doesn't exist, avoid overwriting with partial data
                Log.e("Agreement", "ExchangeAgreement does not exist. Skipping update.")
            }
        }

        return meetingStored
            }
    fun fetchMeetingDetails(
        roomId: String,
        onResult: (location: String?, date: String?, time: String?) -> Unit
    ) {
        Log.e("RoomId", "fetchMeetingDetails: $roomId")
        val meetingRef = FirebaseDatabase.getInstance()
            .getReference("Messages")
            .child("ExchangeChats")
            .child(roomId)
            .child("meeting")

        meetingRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val location = snapshot.child("location").getValue(String::class.java)
                val date = snapshot.child("date").getValue(String::class.java)
                val time = snapshot.child("time").getValue(String::class.java)
                onResult(location, date, time)
            } else {
                onResult(null, null, null)
            }
        }.addOnFailureListener { e ->
//            Toast.makeText(requireContext(), "Failed to fetch meeting: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(null, null, null)
        }
    }


}
