package com.example.signuplogina.modal

import android.os.Parcel
import android.os.Parcelable

data class RecentChats(
    val bidId: String? = null,
    val friendid: String? = "",
    val friendsimage: String? = "",
    val time: Long? = 0,
    val name: String? = "",
    val sender: String? = "",
    val message: String? = "",
    val person: String? = "",
    val status: String? = "",
    val roomName: String? = "",

    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(bidId)
        parcel.writeString(friendid)
        parcel.writeString(friendsimage)
        parcel.writeLong(time ?: 0)
        parcel.writeString(name)
        parcel.writeString(sender)
        parcel.writeString(message)
        parcel.writeString(person)
        parcel.writeString(status)
        parcel.writeString(roomName)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecentChats> {
        override fun createFromParcel(parcel: Parcel): RecentChats {
            return RecentChats(parcel)
        }

        override fun newArray(size: Int): Array<RecentChats?> {
            return arrayOfNulls(size)
        }
    }


}