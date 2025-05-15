package com.example.signuplogina.mvvm

import android.graphics.Color
import android.provider.Telephony.Sms.Conversations
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signuplogina.MyApplication
import com.example.signuplogina.SharedPrefs
import com.example.signuplogina.Utils
import com.example.signuplogina.R
import com.example.signuplogina.modal.Messages
import com.example.signuplogina.modal.RecentChats
import com.example.signuplogina.User
import com.example.signuplogina.notifications.entity.NotificationData
import com.example.signuplogina.notifications.entity.PushNotification
import com.example.signuplogina.notifications.entity.Token
import com.example.signuplogina.notifications.network.RetrofitInstance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*




data class ChatBannerState(
    val isVisible: Boolean = false,
    val message: String = "",
    val backgroundColorRes: Int = Color.TRANSPARENT,
    val disablesChatInput: Boolean = false
)
class ChatAppViewModel : ViewModel() {

    val message = MutableLiveData<String>()
    val name = MutableLiveData<String>()
    val imageUrl = MutableLiveData<String>()

    val usersRepo = UsersRepo()
    val messageRepo = MessageRepo()
    val chatlistRepo = ChatListRepo()

    private val database = FirebaseDatabase.getInstance().reference
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private val _chatBannerState = MediatorLiveData<ChatBannerState>()
    val chatBannerState: LiveData<ChatBannerState> get() = _chatBannerState

    // LiveData for the two users in the current chat
    private val _currentUserDetails = MutableLiveData<User?>()
    private val _opponentUserDetails = MutableLiveData<User?>()

    // Firebase Listeners
    private var currentUserDetailsListener: ValueEventListener? = null
    private var currentUserRef: DatabaseReference? = null
    private var opponentUserDetailsListener: ValueEventListener? = null
    private var opponentUserRef: DatabaseReference? = null


    init {
        // Get current user details once (or listen if admin can block them while app is open)
        // For simplicity, let's assume a one-time fetch or that MainActivity handles current user's block.
        // The primary concern here is the *opponent's* status.

            getCurrentUser()

        _chatBannerState.addSource(_currentUserDetails) { updateUserDisabledState() }
        _chatBannerState.addSource(_opponentUserDetails) { updateUserDisabledState() }
    }

    // Call this from ChatFragment/ChatfromHome onViewCreated
    fun loadChatParticipantsStatus(currentUserId: String, opponentUserId: String) {
        clearChatParticipantListeners() // Clear previous before setting new

        // 1. Current User's Status (optional if MainActivity handles this robustly)
        //    For a more responsive UI within the chat if current user gets blocked elsewhere.
        currentUserRef = database.child("Users").child(currentUserId)
        currentUserDetailsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _currentUserDetails.postValue(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatAppVM", "Failed to load current user details: ${error.message}")
                _currentUserDetails.postValue(null)
            }
        }
        currentUserRef?.addValueEventListener(currentUserDetailsListener!!)


        // 2. Opponent's User Status
        opponentUserRef = database.child("Users").child(opponentUserId)
        opponentUserDetailsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _opponentUserDetails.postValue(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatAppVM", "Failed to load opponent user details: ${error.message}")
                _opponentUserDetails.postValue(null)
            }
        }
        opponentUserRef?.addValueEventListener(opponentUserDetailsListener!!)
    }


    private fun updateUserDisabledState() {
        val currentUser = _currentUserDetails.value
        val opponentUser = _opponentUserDetails.value

        var bannerMsg: String? = null
        var bannerColor: Int = R.color.light_gray // Default for generic disabled
        var disableInput = false

        // Check current user's block status (from UserRatingStats)
        currentUser?.ratings?.let { stats ->
            when {
                stats.isPermanentlyBlocked -> {
                    bannerMsg = "YOUR ACCOUNT IS PERMANENTLY BLOCKED. Chat disabled."
                    bannerColor = R.color.overlay_rejected_background
                    disableInput = true
                }
                stats.isTemporarilyBlocked && (System.currentTimeMillis() < stats.blockExpiryTimestamp || stats.blockExpiryTimestamp == 0L) -> {
                    val expiry = if(stats.blockExpiryTimestamp != 0L) SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(stats.blockExpiryTimestamp)) else "Indefinite"
                    bannerMsg = "YOUR ACCOUNT IS TEMPORARILY RESTRICTED until $expiry. Chat disabled."
                    bannerColor = R.color.light_orange
                    disableInput = true
                }
            }
        }
        // Check legacy statusByAdmin for current user if ratings is null
        if (currentUser?.statusByAdmin == "blocked" && bannerMsg == null) {
            bannerMsg = "YOUR ACCOUNT IS RESTRICTED. Chat disabled."
            bannerColor = R.color.light_orange
            disableInput = true
        }


        // If current user is not blocked, check opponent's status
        if (!disableInput) {
            opponentUser?.ratings?.let { stats ->
                when {
                    stats.isPermanentlyBlocked -> {
                        bannerMsg = "CHAT DISABLED: ${opponentUser.fullName ?: "Opponent"} is permanently blocked."
                        bannerColor = R.color.overlay_rejected_background
                        disableInput = true
                    }
                    stats.isTemporarilyBlocked && (System.currentTimeMillis() < stats.blockExpiryTimestamp || stats.blockExpiryTimestamp == 0L) -> {
                        val expiry = if(stats.blockExpiryTimestamp != 0L) SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(stats.blockExpiryTimestamp)) else "Indefinite"
                        bannerMsg = "CHAT DISABLED: ${opponentUser.fullName ?: "Opponent"} is temporarily restricted until $expiry."
                        bannerColor = R.color.light_orange
                        disableInput = true
                    }
                }
            }
            // Check legacy statusByAdmin for opponent if ratings is null and current user not blocked
            if (opponentUser?.statusByAdmin == "blocked" && bannerMsg == null) {
                bannerMsg = "CHAT DISABLED: ${opponentUser.fullName ?: "Opponent"} is restricted."
                bannerColor = R.color.light_orange
                disableInput = true
            }
        }

        if (bannerMsg != null) {
            _chatBannerState.postValue(ChatBannerState(true, bannerMsg!!, bannerColor, disableInput))
        } else {
            _chatBannerState.postValue(ChatBannerState(false, "", R.color.transparent, false))
        }
    }

    fun clearChatParticipantListeners() {
        currentUserDetailsListener?.let { currentUserRef?.removeEventListener(it) }
        opponentUserDetailsListener?.let { opponentUserRef?.removeEventListener(it) }
        currentUserDetailsListener = null
        currentUserRef = null
        opponentUserDetailsListener = null
        opponentUserRef = null
        Log.d("ChatAppVM", "Chat participant listeners cleared.")
    }


    // ... (your existing sendMessage, getMessages, getUsers, etc.)

    override fun onCleared() {
        super.onCleared()
        clearChatParticipantListeners()
    }


    fun getUsers(): LiveData<List<User>> {

        ///.differnt Type
        return usersRepo.getAllUsersForChat()
    }

    fun sendMessage(sender: String, receiver: String, friendname: String, friendimage: String) =
        viewModelScope.launch(Dispatchers.IO) {

            val context = MyApplication.instance.applicationContext
            Log.e("send message", "send message inchatviewmodel")

            val chatTime = System.currentTimeMillis()
//            Log.e("Time is ")
            val msgMap = mapOf(
                "sender" to sender,
                "receiver" to receiver,
                "message" to message.value!!,
                "time" to chatTime,
                "bidId" to "null"// we can also specify "" here
            )

            val chatRoomId = listOf(sender, receiver).sorted().joinToString("")

            // Store the actual message
            database.child("Messages")
                .child("chats")
                .child(chatRoomId).child("messages")
                .child(chatTime.toString())
                .setValue(msgMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        message.postValue("")

                        val sharedPrefs = SharedPrefs()
                        val senderName = sharedPrefs.getValue("username") ?: "Sender"
                        val senderImage = imageUrl.value ?: "default_img_url"

                        // Correct structure: Conversation / senderId / receiverId
                        val senderConversation = mapOf(
                            "friendid" to receiver,
                            "time" to chatTime,
                            "sender" to sender,
                            "message" to msgMap["message"]!!,
                            "friendsimage" to friendimage,
                            "name" to friendname,
                            "person" to "you"
                        )

                        val receiverConversation = mapOf(
                            "friendid" to sender,
                            "time" to chatTime,
                            "sender" to sender,
                            "message" to msgMap["message"]!!,
                            "friendsimage" to senderImage,
                            "name" to senderName,
                            "person" to senderName
                        )

                        // ✅ Correct Firebase paths
                        database.child("Conversations").child("chats").child(sender).child(receiver)
                            .setValue(senderConversation)
                        Log.e("receiverid", "receiverid $receiver")
                        database.child("Conversations").child("chats").child(receiver).child(sender)
                            .setValue(receiverConversation)

                        // ✅ Push Notification
                        database.child("Tokens").child(receiver).get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.getValue(Token::class.java)?.let { tokenObj ->
                                    val message = msgMap["message"] as? String ?: "New message"
                                    val userName = senderName.split(" ").firstOrNull() ?: "User"

                                    val notification = PushNotification(
                                        NotificationData(userName, message),
                                        tokenObj.token ?: ""
                                    )

                                    sendNotification(notification)
                                }
                            }
                    }
                }
        }

    fun getMessages(friend: String): LiveData<List<Messages>> {
        return messageRepo.getChatMessages(friend)
    }

    fun getRecentUsersAllChats(): LiveData<List<RecentChats>> {
        Log.e("Auth for user","Auth Calling")

        return chatlistRepo.getAllChatList()
    }

    fun sendNotification(notification: PushNotification) = viewModelScope.launch {
        try {
            RetrofitInstance.api.postNotification(notification)
        } catch (e: Exception) {
            Log.e("ViewModelError", e.toString())
        }
    }

    fun getCurrentUser() = viewModelScope.launch(Dispatchers.IO) {
//        val context = MyApplication.instance.applicationContext
        val userId = Utils.getUidLoggedIn()
//Updation
        database.child("Users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(User::class.java)?.let { user ->
                    name.postValue(user.fullName!!)
                    imageUrl.postValue(user.imageUrl!!)

                    SharedPrefs().setValue("username", user.fullName!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", error.message)
            }
        })
    }

    fun updateProfile() = viewModelScope.launch(Dispatchers.IO) {
        val context = MyApplication.instance.applicationContext
        val userId = Utils.getUidLoggedIn()

        val updateMap = mapOf(
            "username" to name.value!!,
            "imageUrl" to imageUrl.value!!
        )

        database.child("Users").child(userId).updateChildren(updateMap).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
            }
        }

        val sharedPrefs = SharedPrefs()
        val friendId = sharedPrefs.getValue("friendid") ?: return@launch

        val updateChatMap = mapOf(
            "friendsimage" to imageUrl.value!!,
            "name" to name.value!!,
            "person" to name.value!!
        )

        database.child("Conversation$friendId").child(userId).updateChildren(updateChatMap)
        database.child("Conversation$userId").child(friendId).child("person").setValue("you")
    }
}