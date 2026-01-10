package com.chatkeep.admin.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chatkeep.admin.core.database.entities.ItemEntity

@Database(entities = [ItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
