package com.classmanager.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Today's date as yyyy-MM-dd in the device's default timezone. */
fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun fmtINR(amount: Double): String =
    "₹%,.2f".format(Locale.ENGLISH, amount)

fun fmtPct(value: Double?): String =
    if (value == null) "—" else "%.1f%%".format(Locale.ENGLISH, value)
