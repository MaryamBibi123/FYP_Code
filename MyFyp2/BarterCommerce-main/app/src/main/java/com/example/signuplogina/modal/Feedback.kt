    package com.example.signuplogina.modal

    data class Feedback(
        val reviewerId: String = "",
        val tags: List<FeedbackTags> = emptyList(),
        val rating: Int = 0,
        val timestamp: Long = 0L,


        val isReport: Boolean = false,              // True if this feedback submission includes a report
        val reportDescription: String? = null,     // User's textual description for the report
        val reportedUserId: String? = null
    ){
        // Required for Firebase deserialization
        constructor() : this("", emptyList(), 0, 0L)
    }
