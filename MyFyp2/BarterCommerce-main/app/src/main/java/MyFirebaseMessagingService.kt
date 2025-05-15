package com.example.signuplogina

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.signuplogina.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage




class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val prefs = getSharedPreferences("FCM_Prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("fcm_token", null)

        if (token != null) {
            Log.d("FCM", "Notification received! Token: $token")
        } else {
            Log.e("FCM", "Token is null! Make sure it's stored properly in the fragment.")
        }
    }
}





//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        val notification = remoteMessage.notification
//        if (notification != null) {
//            showNotification(notification.title ?: "New Message", notification.body ?: "You have a new notification")
//        } else {
//            // Handle data payload if no notification is present
//            remoteMessage.data.let { data ->
//                val title = data["title"] ?: "New Message"
//                val message = data["body"] ?: "You have a new notification"
//                showNotification(title, message)
//            }
//        }
//    }
////=======================================================
//    private fun showNotification(title: String, message: String) {
//        val channelId = "my_channel_id"
//
//        // Create Notification Channel (Required for Android 8.0+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createNotificationChannel(channelId)
//        }
//
//        val builder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_notification) // ✅ Replace with your own drawable
//            .setContentTitle(title)
//            .setContentText(message)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//        val manager = NotificationManagerCompat.from(this)
//
//        // ✅ Check for permission before showing notification (Android 13+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
//            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
//            Log.e("FCM", "Notification permission not granted!")
//            return
//        }
//
//        manager.notify(101, builder.build())
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun createNotificationChannel(channelId: String) {
//        val channel = NotificationChannel(
//            channelId,
//            "My Notifications",
//            NotificationManager.IMPORTANCE_HIGH
//        )
//        val manager = getSystemService(NotificationManager::class.java)
//        manager?.createNotificationChannel(channel)
//    }
//}



//==============================================

//xxxxxxxxxxxxxxxxxxxxxxxx

//=====================================================
//
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.media.RingtoneManager
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    companion object {
//        private const val TAG = "MyFirebaseMsgService"
//        // Ensure this CHANNEL_ID matches the one used in your Cloud Function's Android payload
//        const val CHANNEL_ID = "BID_NOTIFICATIONS_CHANNEL"
//    }
//
//    override fun onNewToken(token: String) {
//        Log.d(TAG, "Refreshed token: $token")
//        sendRegistrationToServer(token)
//    }
//
//    private fun sendRegistrationToServer(token: String?) {
//        token ?: run {
//            Log.w(TAG, "Cannot send null token to server.")
//            return
//        }
//
//        val firebaseUser = FirebaseAuth.getInstance().currentUser
//        firebaseUser?.uid?.let { userId ->
//            FirebaseDatabase.getInstance().getReference("Users")
//                .child(userId)
//                .child("fcmToken")
//                .setValue(token)
//                .addOnSuccessListener { Log.d(TAG, "FCM Token updated for user: $userId") }
//                .addOnFailureListener { e -> Log.e(TAG, "Failed to update FCM Token for $userId", e) }
//        } ?: Log.w(TAG, "Cannot send token to server: User not logged in or UID is null.")
//    }
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        Log.d(TAG, "From: ${remoteMessage.from}")
//
//        var notificationTitle: String? = null
//        var notificationBody: String? = null
//
//        // Check if message contains a notification payload (sent from FCM console or certain SDKs)
//        remoteMessage.notification?.let {
//            Log.d(TAG, "Message Notification Title: ${it.title}")
//            Log.d(TAG, "Message Notification Body: ${it.body}")
//            notificationTitle = it.title
//            notificationBody = it.body
//        }
//
//        // Check if message contains a data payload (this is what our Cloud Function primarily sends)
//        val dataPayload = remoteMessage.data
//        if (dataPayload.isNotEmpty()) {
//            Log.d(TAG, "Message data payload: $dataPayload")
//            // If notification part wasn't set in the FCM message, try to get from data
//            // (Our Cloud Function sets title/body in the 'notification' part of the payload)
//            if (notificationTitle == null) notificationTitle = dataPayload["title"]
//            if (notificationBody == null) notificationBody = dataPayload["body"]
//        }
//
//        // Show the notification if we have a title and body
//        if (!notificationTitle.isNullOrBlank() && !notificationBody.isNullOrBlank()) {
//            sendCustomNotification(notificationTitle!!, notificationBody!!, dataPayload)
//        } else {
//            Log.d(TAG, "Notification title or body is missing. Not showing notification.")
//        }
//    }
//
//    private fun sendCustomNotification(title: String, body: String, data: Map<String, String>) {
//        val intent = Intent(this, MainActivity::class.java) // <<--- CHANGE TO YOUR MAIN ACTIVITY
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//
//        // Pass data from the FCM message to the intent
//        data.forEach { (key, value) ->
//            intent.putExtra(key, value)
//        }
//        // Add a specific flag if you want MainActivity to know it was opened from this type of notification
//        if (data.containsKey("productId")) {
//            intent.putExtra("open_bid_details", true)
//        }
//
//
//        val requestCode = System.currentTimeMillis().toInt() // Unique request code
//        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
//        } else {
//            PendingIntent.FLAG_ONE_SHOT
//        }
//        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, pendingIntentFlag)
//
//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//
//        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_stat_ic_notification) // <<--- CREATE THIS DRAWABLE
//            .setContentTitle(title)
//            .setContentText(body)
//            .setAutoCancel(true)
//            .setSound(defaultSoundUri)
//            .setContentIntent(pendingIntent)
//            .setPriority(NotificationCompat.PRIORITY_HIGH) // For important notifications
//
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channelName = "Bid Notifications"
//            val channelDescription = "Notifications for new bids on your items"
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
//                description = channelDescription
//                // Optionally set other channel properties like lights, vibration, etc.
//                // enableLights(true)
//                // lightColor = Color.RED
//                // enableVibration(true)
//            }
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        // Use a unique ID for each notification if you want to show multiple,
//        // or a fixed ID if you want new ones to update/replace the old one.
//        val notificationId = System.currentTimeMillis().toInt()
//        notificationManager.notify(notificationId, notificationBuilder.build())
//    }
//}
//
//
//
//
//

//=========================
