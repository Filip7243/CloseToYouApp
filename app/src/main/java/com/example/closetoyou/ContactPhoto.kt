package com.example.closetoyou

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_photos")
data class ContactPhoto(
    @PrimaryKey val phoneNumber: String,
    var photoUri: String
)