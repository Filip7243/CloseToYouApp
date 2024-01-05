package com.example.closetoyou


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContactPhotoDao {
    @Query("SELECT * FROM contact_photos WHERE phoneNumber = :phoneNumber")
    fun getPhotoByPhoneNumber(phoneNumber: String): ContactPhoto?

    @Insert
    fun insertPhoto(contactPhoto: ContactPhoto)

    @Query("SELECT * FROM contact_photos")
    fun getAllPhotos(): List<ContactPhoto>

    @Update
    fun updatePhoto(contactPhoto: ContactPhoto)
}
