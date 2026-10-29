package com.classmanager.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One fee payment. Payments are append-only: they are never edited or
 * deleted, so the money trail stays auditable. [receiptNo] is assigned at
 * insert time as CM-0001, CM-0002, ... in insertion order.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = Student::class,
        parentColumns = ["id"],
        childColumns = ["studentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("studentId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val amount: Double,
    val method: String,
    val date: String, // yyyy-MM-dd
    val receiptNo: String
)
