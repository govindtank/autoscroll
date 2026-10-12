package com.droidtank.autoscroll.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppConfig::class, GestureRecording::class],
    version = 2,
    exportSchema = false
)
abstract class AutoScrollDatabase : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun gestureRecordingDao(): GestureRecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AutoScrollDatabase? = null

        fun getDatabase(context: Context): AutoScrollDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutoScrollDatabase::class.java,
                    "autoscroll_pro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
