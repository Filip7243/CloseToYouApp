package com.example.closetoyou

import com.example.closetoyou.ContactPhotoDao
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ContactPhoto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactPhotoDao(): ContactPhotoDao

    // Instancja bazy danych...

}