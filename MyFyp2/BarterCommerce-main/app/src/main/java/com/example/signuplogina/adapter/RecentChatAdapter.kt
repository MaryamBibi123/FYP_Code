package com.example.signuplogina.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.signuplogina.R
import com.example.signuplogina.modal.RecentChats
import com.google.firebase.database.*  // Firebase Realtime Database import
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentChatAdapter : RecyclerView.Adapter<MyChatListHolder>() {
    interface onChatClicked {
        fun getOnChatCLickedItem(position: Int, chatList: RecentChats)
    }

    private var isFromExchangeRoom: Boolean = false  // Step 1: Add the flag
    private var listOfChats = mutableListOf<RecentChats>() // Changed to mutable list
    private var listener: onChatClicked? = null


    fun setModeFromExchangeRoom(flag: Boolean) {
        isFromExchangeRoom = flag
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyChatListHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recentchatlist, parent, false)
        return MyChatListHolder(view)
        Log.d("NavigationDebug", "🟢 RecentChatAdapter Loaded")

    }

    override fun getItemCount(): Int {
        return listOfChats.size
    }

    override fun onBindViewHolder(holder: MyChatListHolder, position: Int) {
        val chatlist = listOfChats[position]
//        val bidId = chatlist.bidId

//        holder.userName.text = chatlist.name
        if (isFromExchangeRoom) {
            holder.userName.text = chatlist.roomName ?: "Room"
        } else {
            holder.userName.text = chatlist.name ?: "User"
        }
        val lastMessage = chatlist.message?.split(" ")?.take(4)?.joinToString(" ") ?: ""
        holder.lastMessage.text = "${chatlist.person}: $lastMessage"

        Glide.with(holder.itemView.context).load(chatlist.friendsimage).into(holder.imageView)
        val timeString = chatlist.time?.let {
            val date = Date(it)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault()) // or "hh:mm a" for AM/PM
            format.format(date)
        }
        holder.timeView.text = timeString ?: ""

        holder.itemView.setOnClickListener {
            listener?.getOnChatCLickedItem(position, chatlist)
        }
    }

    fun setOnChatClickListener(listener: onChatClicked) {
        this.listener = listener
    }

    fun setList(list: MutableList<RecentChats>) {
        this.listOfChats = list


    }

    fun generateOtpsAndStore(bidId: String) {
        val otpForReceiver = (100000..999999).random().toString()
        val otpForBidder = (100000..999999).random().toString()
        val currentTimeMillis = System.currentTimeMillis()

        val otpData = mapOf(
            "otpForReceiver" to otpForReceiver,
            "otpForBidder" to otpForBidder,
            "bidderOtpVerified" to false,
            "receiverOtpVerified" to false,
            "status" to "PendingOTP",
            "otpGeneratedAt" to currentTimeMillis
        )

        FirebaseDatabase.getInstance().getReference("ExchangeAgreements")
            .child(bidId)
            .updateChildren(otpData)
    }


    // Fetch data from Firebase Realtime Database
    fun fetchRecentChats(userId: String) {
        Log.d("NavigationDebug", "🟢 RecentChatAdapter fetch rrecentchats")
        val databaseRef = FirebaseDatabase.getInstance().getReference("Messages").child(userId)

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latestMessagesMap = mutableMapOf<String, RecentChats>()
                listOfChats.clear()

                for (child in snapshot.children) {
                    val chat = child.getValue(RecentChats::class.java)
                    if (chat != null && chat.friendid != null) {
                        val existingChat = latestMessagesMap[chat.friendid]
                        val currentTime = (chat.time ?: child.key).toString().toLongOrNull() ?: 0L
                        val existingTime = existingChat?.time?.toString()?.toLongOrNull() ?: 0L

                        if (existingChat == null || existingTime < currentTime) {
                            latestMessagesMap[chat.friendid!!] = chat.copy(time = currentTime)
                        }


                    }
                }

                listOfChats.addAll(latestMessagesMap.values.sortedByDescending { it.time })
                notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Log or handle error
            }
        })
    }

}

class MyChatListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageView: CircleImageView = itemView.findViewById(R.id.recentChatImageView)
    val userName: TextView = itemView.findViewById(R.id.recentChatTextName)
    val lastMessage: TextView = itemView.findViewById(R.id.recentChatTextLastMessage)
    val timeView: TextView = itemView.findViewById(R.id.recentChatTextTime)
}

