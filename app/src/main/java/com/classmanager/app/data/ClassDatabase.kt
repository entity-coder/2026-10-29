package com.classmanager.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Student::class, FeePlan::class, Payment::class,
        AttendanceRecord::class, Test::class, Mark::class],
    version = 1,
    exportSchema = false
)
abstract class ClassDatabase : RoomDatabase() {
    abstract fun dao(): ClassDao

    companion object {
        @Volatile
        private var instance: ClassDatabase? = null

        fun get(context: Context): ClassDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClassDatabase::class.java,
                    "classmanager.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
