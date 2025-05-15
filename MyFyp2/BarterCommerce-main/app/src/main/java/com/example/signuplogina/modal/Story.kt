package com.example.signuplogina.modal

data class Story(
//    val storyId: String = "",                // Optional: Useful for updates or deletions
    val itemId: String = "",                 // Reference to the listed item
    val userId: String = "",
    val userName: String = "",
    val userProfileUrl: String? = null,
    val offerText: String = "",              // e.g. "Offering: 🎸"
    val wantText: String = "",               // e.g. "Wants: 🎧"
    val storyImageUrl: String? = null,       // Thumbnail or preview of the item
    val timestamp: Long = System.currentTimeMillis()
)
