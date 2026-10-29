package com.classmanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A class test. Marks live in [Mark] so one test holds many students. */
@Entity(tableName = "tests")
data class Test(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: String, // yyyy-MM-dd
    val maxMarks: Double
)
