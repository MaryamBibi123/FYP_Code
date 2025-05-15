package com.example.signuplogina

import android.os.Parcelable
import com.example.signuplogina.modal.UserRatingStats
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userid: String? = null,
    val fullName: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val mobile: String? = null,
    val useremail: String? = null,
    val imageUrl: String? = "https://www.pngarts.com/files/6/User-Avatar-in-Suit-PNG.png",
    val status: String? = null,
    val items: Map<String, Boolean>? = null,
    val ratings: UserRatingStats? = null,// <-- Added this line
    var statusByAdmin: String? = null,
    val bids: UserBids = UserBids(), // Contains bidsPlaced and bidsReceived maps
//
//    // --- Category Structure ---
//    // Stores data like { "0": "Books", "1": "Electronics" }
//    val category: Map<String, String> = emptyMap(),
) : Parcelable


//
@Parcelize
data class UserBids(
    val bidsPlaced: Map<String, Boolean> = emptyMap(),   // Will hold { bidId: true }
    val bidsReceived: Map<String, Boolean> = emptyMap() // Will hold { bidId: true }
) : Parcelable








//package com.example.signuplogina
//// helper class to read write user details
//// we will use this same helper class in userprofileactivity to get the snapshots of the data
//import android.os.Parcel
//import android.os.Parcelable
//import com.example.signuplogina.modal.UserRatingStats
//
//data class User(
//    val userid: String? = null,
//    val fullName: String? = null,
//    val dob: String? = null,
//    val gender: String? = null,
//    val mobile: String? = null,
//    val useremail: String? = null,
//    val imageUrl: String? = "https://www.pngarts.com/files/6/User-Avatar-in-Suit-PNG.png",
//    val status: String? = null,
//    val items:List<Item>?=null,// added after ,
//    val ratings: UserRatingStats? = null // <-- Added this line
//
//) : Parcelable
//{
//    constructor(parcel: Parcel) : this(
//        parcel.readString(), // userid
//        parcel.readString(), // fullName
//        parcel.readString(), // dob
//        parcel.readString(), // gender
//        parcel.readString(), // mobile
//        parcel.readString(), // useremail
//        parcel.readString(), // imageUrl
//        parcel.readString() ,// status
//        parcel.readList(mutableListOf<Item>().toMutableList(), Item::class.java.classLoader) as List<Item>, // Read the list of items with the correct class loader
//
//        parcel.readParcelable(UserRatingStats::class.java.classLoader) // <-- Also added here
//
//    )
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//        parcel.writeString(userid)
//        parcel.writeString(fullName)
//        parcel.writeString(dob)
//        parcel.writeString(gender)
//        parcel.writeString(mobile)
//        parcel.writeString(useremail)
//        parcel.writeString(imageUrl)
//        parcel.writeString(status)
////        parcel.writeList(items) // Write the list of items
//
//        parcel.writeParcelable(ratings, flags) // <-- Added here too
//
//    }
//
//    override fun describeContents(): Int = 0
//
//    companion object CREATOR : Parcelable.Creator<User> {
//        override fun createFromParcel(parcel: Parcel): User {
//            return User(parcel)
//        }
//
//        override fun newArray(size: Int): Array<User?> {
//            return arrayOfNulls(size)
//        }
//    }
//}
