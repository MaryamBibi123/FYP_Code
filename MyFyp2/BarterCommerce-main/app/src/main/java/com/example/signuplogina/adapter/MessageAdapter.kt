package com.example.signuplogina.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.R
import com.example.signuplogina.Utils
import com.example.signuplogina.modal.Messages
import com.google.firebase.database.*  // Import Realtime Database package
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<MessageHolder>() {

    private var listOfMessage = mutableListOf<Messages>()  // Change to mutable list

    private val LEFT = 0
    private val RIGHT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        Log.d("NavigationDebug", "🟢 MessageAdapter Loaded")

        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == RIGHT) {
            val view = inflater.inflate(R.layout.chatitemright, parent, false)
            MessageHolder(view)
        } else {
            val view = inflater.inflate(R.layout.chatitemleft, parent, false)
            MessageHolder(view)
        }
    }

    override fun getItemCount() = listOfMessage.size

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val message = listOfMessage[position]

        holder.messageText.visibility = View.VISIBLE
        holder.timeOfSent.visibility = View.VISIBLE

        holder.messageText.text = message.message
        val timeString = message.time?.let {
            val date = Date(it)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.format(date)
        }
        holder.timeOfSent.text = timeString ?: ""
    }

    override fun getItemViewType(position: Int) =
        if (listOfMessage[position].sender == Utils.getUidLoggedIn()) RIGHT else LEFT

    // Method to fetch messages from Realtime Database
    // it is not being called and hte referecne is wrong ,
    fun fetchMessages(chatId: String) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages")
        databaseRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Messages::class.java)
                if (message != null) {
                    listOfMessage.add(message)
                    notifyItemInserted(listOfMessage.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle child updates if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle message removal if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle child move if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database errors
            }
        })
    }
    fun setList(newList: MutableList<Messages>) {

        this.listOfMessage = newList

    }
    // Method to send a new message to Realtime Database
    fun sendMessage(chatId: String, message: Messages) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages")
        val messageId = databaseRef.push().key  // Generate a unique key for the new message
        if (messageId != null) {
            databaseRef.child(messageId).setValue(message)
        }
    }
}

class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val messageText: TextView = itemView.findViewById(R.id.show_message)
    val timeOfSent: TextView = itemView.findViewById(R.id.timeView)
}
