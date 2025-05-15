package com.example.signuplogina.modal

data class PollModel(
    val id: String="",
    val userId: String = "",
    val question: String = "",
    val option1: String = "",
    val option2: String = "",
    val option3: String = "",
    val votesOption1: Int = 0,
    val votesOption2: Int = 0,
    val votesOption3: Int = 0,
    val totalVotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
