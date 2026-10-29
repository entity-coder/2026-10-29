package com.classmanager.app.data

/** Pure fee/attendance/progress maths — no Android types, so the rules are
 *  easy to reason about and to test. Dates are yyyy-MM-dd strings. */

/** Whole calendar months from [startIso]'s month through [endIso]'s month,
 *  inclusive. Returns 0 when the end month is before the start month. */
fun monthsBetweenInclusive(startIso: String, endIso: String): Int {
    val start = startIso.split("-")
    val end = endIso.split("-")
    if (start.size < 2 || end.size < 2) return 0
    val startMonths = start[0].toIntOrNull()?.times(12)
        ?.plus(start[1].toIntOrNull() ?: return 0) ?: return 0
    val endMonths = end[0].toIntOrNull()?.times(12)
        ?.plus(end[1].toIntOrNull() ?: return 0) ?: return 0
    val diff = endMonths - startMonths + 1
    return if (diff < 0) 0 else diff
}

/**
 * Fee still owed. Returns null when the batch has no fee plan — dues are
 * unknown then, and the UI says so instead of inventing a zero.
 * Overpayments are clamped to zero: dues never go negative.
 */
fun duesOwed(monthlyFee: Double?, monthsCharged: Int, totalPaid: Double): Double? {
    if (monthlyFee == null) return null
    val owed = monthsCharged * monthlyFee - totalPaid
    return if (owed < 0.0) 0.0 else owed
}

/** Attendance percentage, or null when nothing is marked yet. */
fun attendancePct(present: Int, total: Int): Double? =
    if (total <= 0) null else present * 100.0 / total

/** Average score percentage across tests, or null with no marks. */
fun averagePct(marks: List<Pair<Double, Double>>): Double? {
    val maxTotal = marks.sumOf { it.second }
    if (marks.isEmpty() || maxTotal <= 0.0) return null
    return marks.sumOf { it.first } * 100.0 / maxTotal
}

/** Receipt numbers run CM-0001, CM-0002, ... in insertion order. */
fun receiptNo(countSoFar: Int): String = "CM-%04d".format(countSoFar + 1)
