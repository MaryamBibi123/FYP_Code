package com.example.signuplogina.mvvm

data class TradeBattleModel(
    val battleId: String = "",
    val itemId: String = "",
    val listerUid: String = "",
    val status: String = "active",
    val createdAt: Long = 0L,
    val endsAt: Long = 0L,
    val winningBidId: String? = null,
    val chatRoomId: String? = null,
    val bids: Map<String, BidModel>? = null
)

data class BidModel(
    val offeredId: String = "",
    val userId: String = "",
    val bidId: String = "",
    val offer: String = "",
    val imageUrl: String = "",
    val ratings: Ratings = Ratings(),
    var totalPoints: Int = 0
)

data class Ratings(
    var fair: Int = 0,
    var good: Int = 0,
    var best: Int = 0
)

data class Vote(
    val bidId: String = "",
    val userId: String = "",
    val rating: Int = 0, // Rating value: 1 (Fair), 2 (Good), 3 (Best)
    val timestamp: Long = System.currentTimeMillis() // To track when the vote was cast
)