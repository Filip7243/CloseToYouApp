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
    val hasPermission: Boolean,
    val updatedAt: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readBoolean(),
        parcel.readString()
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

    fun getUpdatedAtAsInstant(): Instant {
        return Instant.parse(updatedAt) // Konwersja String na Instant
    }

    fun getLastSeenText(): String {
        val now = Instant.now()
        val updatedAtInstant = getUpdatedAtAsInstant()
        val duration = Duration.between(updatedAtInstant, now)
        val minutesAgo = duration.toMinutes()
        val hoursAgo = duration.toHours()

        return when {
            minutesAgo < 60 -> "$minutesAgo minut temu"
            hoursAgo < 24 -> "$hoursAgo godzin temu"
            else -> "${hoursAgo / 24} dni temu"
        }
    }

}