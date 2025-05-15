package com.example.signuplogina.modal

data class Messages(
    val sender : String? = "",
    val receiver: String? = "",
    val message: String? = "",
    val time: Long? = 0L,
    val bidId: String? = null

) {


    val id : String get() = "$sender-$receiver-$message-$time"
}