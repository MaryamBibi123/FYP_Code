package com.example.signuplogina.notifications

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.signuplogina.R
import com.example.signuplogina.SharedPrefs
import com.example.signuplogina.Utils
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

private const val CHANNEL_ID = "my_channel"

class NotificationReply : BroadcastReceiver() {

    // Initialize Firebase Realtime Database
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    override fun onReceive(context: Context?, intent: Intent?) {

        val notificationManager: NotificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        if (remoteInput != null) {

            // Get the reply text from the notification input
            val repliedText = remoteInput.getString("KEY_REPLY_TEXT")

            // Retrieve data from shared preferences
            val mysharedPrefs = SharedPrefs()
            val friendid = mysharedPrefs.getValue("friendid")
            val chatroomid = mysharedPrefs.getValue("chatroomid")
            val friendname = mysharedPrefs.getValue("friendname")
            val friendimage = mysharedPrefs.getValue("friendimage")

            // Create message data to save in the "Messages" section of the database
            val messageData = mapOf(
                "sender" to Utils.getUidLoggedIn(),
                "time" to Utils.getTime(),
                "receiver" to friendid!!,
                "message" to repliedText!!
            )

            // Push the new message to the "Messages" chat room
            val chatRef: DatabaseReference = database.reference.child("Messages").child(chatroomid!!)
                .child("chats").push()

            chatRef.setValue(messageData).addOnCompleteListener {
                if (it.isSuccessful) {
                    // Optionally handle success here if needed
                } else {
                    // Optionally handle failure here if needed
                }
            }

            // Create and update the sender's conversation data
            val senderConversationData = mapOf(
                "friendid" to friendid,
                "time" to Utils.getTime(),
                "sender" to Utils.getUidLoggedIn(),
                "message" to repliedText,
                "friendsimage" to friendimage!!,
                "name" to friendname!!,
                "person" to "you"
            )

            val senderConversationRef: DatabaseReference = database.reference.child("Conversation${Utils.getUidLoggedIn()}").child(friendid)
            senderConversationRef.setValue(senderConversationData).addOnCompleteListener {
                if (it.isSuccessful) {
                    // Optionally handle success here if needed
                } else {
                    // Optionally handle failure here if needed
                }
            }


            // Update the receiver's conversation data
            val receiverConversationData = mapOf(
                "message" to repliedText,
                "time" to Utils.getTime(),
                "person" to friendname!!
            )

            val receiverConversationRef: DatabaseReference = database.reference.child("Conversation${friendid}").child(Utils.getUidLoggedIn())
            receiverConversationRef.updateChildren(receiverConversationData).addOnCompleteListener {
                if (it.isSuccessful) {
                    // Optionally handle success here if needed
                } else {
                    // Optionally handle failure here if needed
                }
            }
            Log.e("NavigationDebug", "🟢 NotificationReply Loaded ")


            // Retrieve notification ID from shared preferences
            val sharedCustomPref = SharedPrefs()
            val replyid: Int = sharedCustomPref.getIntValue("values", 0)

            // Create and show the notification that the reply was sent
            val repliedNotification = NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.chatapp)
                .setContentText("Reply Sent")
                .build()

            // Notify the user with the reply ID
            notificationManager.notify(replyid, repliedNotification)
        }
    }
}
