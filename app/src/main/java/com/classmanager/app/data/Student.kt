package com.classmanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One coaching-class student. [admitted] is the first fee month (yyyy-MM-dd):
 * fee dues count whole calendar months from that month through the current
 * month, inclusive — the same rule as the ClassManager web app. [active]
 * hides a student instead of deleting them, so history is never lost.
 */
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val guardianPhone: String = "",
    val batch: String,
    val admitted: String,
    val active: Boolean = true
)
