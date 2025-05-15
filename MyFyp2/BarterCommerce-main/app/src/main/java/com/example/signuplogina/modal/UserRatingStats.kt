package com.example.signuplogina.modal

import android.os.Parcel
import android.os.Parcelable

    data class UserRatingStats(
        var totalBids: Int = 0,
        var totalExchanges: Int = 0,
        var totalStarsReceived: Int = 0,
        var averageRating: Float = 0f,
        var highestRating: Int = 0,
        var lowestRating: Int = 5,
        var lastExchangeTimestamp: Long = 0L,
        var isVerified: Boolean = false,
        var ratingBreakdown: Map<String, Int> = mapOf(
            "1_star" to 0,
            "2_star" to 0,
            "3_star" to 0,
            "4_star" to 0,
            "5_star" to 0
        ),
    //    var feedbackList: List<Feedback> = emptyList()
        var feedbackList: Map<String, Feedback> = mapOf(), // <-- CHANGED from List to Map
        var isTemporarilyBlocked: Boolean = false,
        var blockExpiryTimestamp: Long = 0L,
        var blockReason: String? = null,
        var isPermanentlyBlocked: Boolean = false,
        var blockHistory: List<BlockRecord> = emptyList(),
        var reportCount: Int = 0 // This will be incremented whe

    ) : Parcelable {

        val successRate: Float
            get() {
            if (totalExchanges == 0) return 0f

            val avgRatingWeight = (averageRating / 5f) * 60  // Rating worth 60 points max

            val tagScore = feedbackList.values.flatMap { it.tags }
                .groupingBy { it }
                .eachCount()

            val positiveTagWeight = (
                    (tagScore[FeedbackTags.BEST_SELLER] ?: 0) +
                            (tagScore[FeedbackTags.SMOOTH_EXCHANGE] ?: 0) +
                            (tagScore[FeedbackTags.FRIENDLY] ?: 0) +
                            (tagScore[FeedbackTags.TIME_PUNCTUAL] ?: 0) +
                            (tagScore[FeedbackTags.ACCURATE_ITEM_DESCRIPTION_POSITIVE] ?: 0)
                    ) * 4  // Each positive tag adds 4 points

            val negativeTagPenalty = (
                    (tagScore[FeedbackTags.NOT_RECOMMENDED] ?: 0) * 8 +
                            (tagScore[FeedbackTags.ITEM_NOT_AS_DESCRIBED] ?: 0) * 6
                    )  // Negative tags subtract points

            val totalTagWeight = (positiveTagWeight - negativeTagPenalty).coerceAtLeast(0)

            return (avgRatingWeight + totalTagWeight).coerceAtMost(100f)
        }

//    val successRate: Float
//        get() = if (totalBids > 0) totalExchanges.toFloat() / totalBids * 100 else 0f

    constructor(parcel: Parcel) : this(
        totalBids = parcel.readInt(),
        totalExchanges = parcel.readInt(),
        totalStarsReceived = parcel.readInt(),
        averageRating = parcel.readFloat(),
        highestRating = parcel.readInt(),
        lowestRating = parcel.readInt(),
        lastExchangeTimestamp = parcel.readLong(),
        isVerified = parcel.readByte() != 0.toByte(),
        ratingBreakdown = mutableMapOf<String, Int>().apply {
            val size = parcel.readInt()
            repeat(size) {
                val key = parcel.readString()
                val value = parcel.readInt()
                if (key != null) put(key, value)
            }
        },
//        feedbackList = mapOf<String, Feedback>().apply {
//            parcel.readMap(this, Feedback::class.java.classLoader)
//        }

        feedbackList = mutableMapOf<String, Feedback>().apply {
            @Suppress("UNCHECKED_CAST")
            val tempMap = parcel.readHashMap(Feedback::class.java.classLoader) as? Map<String, Feedback>
            if (tempMap != null) putAll(tempMap)
        }

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(totalBids)
        parcel.writeInt(totalExchanges)
        parcel.writeInt(totalStarsReceived)
        parcel.writeFloat(averageRating)
        parcel.writeInt(highestRating)
        parcel.writeInt(lowestRating)
        parcel.writeLong(lastExchangeTimestamp)
        parcel.writeByte(if (isVerified) 1 else 0)

        parcel.writeInt(ratingBreakdown.size)
        for ((key, value) in ratingBreakdown) {
            parcel.writeString(key)
            parcel.writeInt(value)
        }

        parcel.writeMap(feedbackList)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UserRatingStats> {
        override fun createFromParcel(parcel: Parcel): UserRatingStats = UserRatingStats(parcel)
        override fun newArray(size: Int): Array<UserRatingStats?> = arrayOfNulls(size)
    }
}

data class BlockRecord (

    val blockType: String = "temporary", // "temporary", "permanent"
    val reason: String? = null,
    val blockedByAdminId: String? = null, // Optional: track which admin blocked
    val blockTimestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = 0L // Only for temporary
){

}


//package com.example.signuplogina.modal
//
//data class UserRatingStats(
//    var totalBids: Int = 0,
//    var totalExchanges: Int = 0,
//    var totalStarsReceived: Int = 0,
//    var averageRating: Float = 0f,
//    var highestRating: Int = 0,
//    var lowestRating: Int = 5,
//    var lastExchangeTimestamp: Long = 0L,
//    var isVerified: Boolean = false,
//    var ratingBreakdown: Map<String, Int> = mapOf(
//        "1_star" to 0,
//        "2_star" to 0,
//        "3_star" to 0,
//        "4_star" to 0,
//        "5_star" to 0
//    ),
//    var feedbackList: List<Feedback> = emptyList()
//) {
//    val successRate: Float
//        get() = if (totalBids > 0) totalExchanges.toFloat() / totalBids * 100 else 0f
//}
