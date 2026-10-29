package com.classmanager.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One student's score on one test. Unique per (test, student). */
@Entity(
    tableName = "marks",
    foreignKeys = [
        ForeignKey(
            entity = Test::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["testId", "studentId"], unique = true)]
)
data class Mark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long,
    val studentId: Long,
    val obtained: Double
)
