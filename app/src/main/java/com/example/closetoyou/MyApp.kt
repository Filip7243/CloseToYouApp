package com.example.closetoyou

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.closetoyou.AppDatabase

class MyApp : Application() {

    companion object {
        private var database: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (database == null) {
                database = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "closetoyou-db"
                ).build()
            }
            return database!!
        }
    }

}