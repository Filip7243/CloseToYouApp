package com.example.closetoyou

import android.os.Parcel
import android.os.Parcelable
import java.time.Duration
import java.time.Instant

/*
    {
        "phoneNumber": "+48575603061",
        "latitude": 29.02,
        "longitude": 11.2,
        "hasPermission": true
    }
*/

data class Localization(
    val name: String?,
    val phoneNumber: String?,
    val latitude: Double,
    val longitude: Double,
    val hasPermission: Boolean
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readBoolean()
    ) {
    }

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(phoneNumber)
        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeBoolean(hasPermission)
    }

    companion object CREATOR : Parcelable.Creator<Localization> {
        override fun createFromParcel(parcel: Parcel): Localization {
            return Localization(parcel)
        }

        override fun newArray(size: Int): Array<Localization?> {
            return arrayOfNulls(size)
        }
    }

    fun getLastSeenText(): String {
        val now = Instant.now()
        val duration = Duration.between(Instant.now(), now)
        val minutesAgo = duration.toMinutes()

        return when {
            minutesAgo < 60 -> "$minutesAgo minut temu"
            else -> "${minutesAgo / 60} godzin temu"
        }
    }
}