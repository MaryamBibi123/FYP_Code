package com.example.signuplogina.modal

import android.os.Parcel
import android.os.Parcelable

data class Users(
    val userid: String? = "",
    val status: String? = "",
    val imageUrl: String? = "",
    val username: String? = "",
    val useremail: String? = "",
    val ratings: UserRatingStats? = null // <-- Added this line
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(UserRatingStats::class.java.classLoader) // <-- Also added here
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userid)
        parcel.writeString(status)
        parcel.writeString(imageUrl)
        parcel.writeString(username)
        parcel.writeString(useremail)
        parcel.writeParcelable(ratings, flags) // <-- Added here too
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Users> {
        override fun createFromParcel(parcel: Parcel): Users = Users(parcel)
        override fun newArray(size: Int): Array<Users?> = arrayOfNulls(size)
    }
}
