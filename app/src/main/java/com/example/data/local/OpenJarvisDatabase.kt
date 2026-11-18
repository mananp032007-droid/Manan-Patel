package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MessageEntity::class,
        MemoryEntity::class,
        TaskEntity::class,
        TraceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OpenJarvisDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao
    abstract fun traceDao(): TraceDao

    companion object {
        @Volatile
        private var INSTANCE: OpenJarvisDatabase? = null

        fun getInstance(context: Context): OpenJarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OpenJarvisDatabase::class.java,
                    "openjarvis_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
