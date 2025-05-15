package com.example.signuplogina

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*


class Utils {

    companion object {

        private val auth = FirebaseAuth.getInstance()
        private var userid: String = ""
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
        const val MESSAGE_RIGHT = 1
        const val MESSAGE_LEFT = 2
        const val CHANNEL_ID = "com.example.signuplogina.fragments"

        // Firebase Realtime Database instance
        private val database = FirebaseDatabase.getInstance()

        fun getUidLoggedIn(): String {
            Log.e("Auth for user","Auth for user${auth.currentUser}")
            if (auth.currentUser != null) {

                userid = auth.currentUser!!.uid
            }
            return userid
        }

        fun getTime(): String {
            val formatter = SimpleDateFormat("HH:mm:ss")
            val date: Date = Date(System.currentTimeMillis())
            return formatter.format(date)
            Log.e("NavigationDebug", "🟢 Utils Loaded ")

        }
        fun convertToPakistanTime(timestamp: Long): String {
            val calendar = Calendar.getInstance()
            val timeZone = TimeZone.getTimeZone("Asia/Karachi")
            calendar.timeZone = timeZone
            calendar.timeInMillis = timestamp

            val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = timeZone
            return dateFormat.format(calendar.time)
        }
    }








}
