package com.example.signuplogina

data class ItemCard(
    val imageUrl: String,
    val productName: String,
    val description: String,
    val category: String,
    val condition: String,
    val availability: String,
    var id: String, // Unique product ID
    var bidsCount: Int = 0, // Default to 0 bids
    val status: String = ""
)


