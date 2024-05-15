package com.example.anysync.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Source::class], version = 1)
abstract class MainDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        var instance: MainDatabase? = null

        fun init(context: Context): MainDatabase? {
            this.instance = Room.databaseBuilder(
                context,
                MainDatabase::class.java,
                "database"
            ).build()
            return this.instance
        }
    }
}