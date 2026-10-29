package com.classmanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Monthly fee for one batch. The batch name is the key: one plan per batch. */
@Entity(tableName = "fee_plans")
data class FeePlan(
    @PrimaryKey val batch: String,
    val monthlyFee: Double
)
