package com.example.signuplogina.modal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ExchangeAgreement(
    val bidId:String="",
    val bidderId: String = "",
    val receiverId: String = "",
    val meetingLocation: String = "",
    val meetingTime: String = "",
    val terms: String = "",
    var status: String = "start",
    var timestamp: Long = 0L,
    var isBidderConfirmed: Boolean = false,//done agreement
    var isReceiverConfirmed: Boolean = false,
    var otpByBidder: String = "",   // Bidder gives this OTP to Receiver
    var otpByReceiver: String = "", // Receiver gives this OTP to Bidder
    var isBidderPressedReveal: Boolean = false,//otp reveal
    var isReceiverPressedReveal: Boolean = false,
    var bidderOtpVerified: Boolean = false,   // ✅ New// enter correct otp
    var receiverOtpVerified: Boolean = false, // ✅ New
    var ratingGivenByBidder: Boolean=false,
    var ratingGivenByReceiver: Boolean=false,
// for reporting
    var reportFiledByBidder: Boolean = false,
    var reportFiledByReceiver: Boolean = false,
    var bidderReportId: String? = null, // ID of the report filed by bidder (links to /reports/)
    var receiverReportId: String? = null // ID of the report

) : Parcelable
