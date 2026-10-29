package com.classmanager.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    // ---- students ----
    @Query("SELECT * FROM students ORDER BY name COLLATE NOCASE ASC")
    fun students(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun studentById(id: Long): Student?

    @Query("SELECT DISTINCT batch FROM students WHERE active = 1 ORDER BY batch COLLATE NOCASE ASC")
    fun batches(): Flow<List<String>>

    @Insert
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT COUNT(*) FROM payments WHERE studentId = :id")
    suspend fun paymentCountFor(id: Long): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :id")
    suspend fun attendanceCountFor(id: Long): Int

    @Query("SELECT COUNT(*) FROM marks WHERE studentId = :id")
    suspend fun markCountFor(id: Long): Int

    // ---- fee plans ----
    @Query("SELECT * FROM fee_plans ORDER BY batch COLLATE NOCASE ASC")
    fun feePlans(): Flow<List<FeePlan>>

    @Query("SELECT * FROM fee_plans WHERE batch = :batch")
    suspend fun feePlanFor(batch: String): FeePlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeePlan(plan: FeePlan)

    @Delete
    suspend fun deleteFeePlan(plan: FeePlan)

    // ---- payments ----
    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY id DESC")
    fun paymentsFor(studentId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun allPayments(): Flow<List<Payment>>

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun paymentTotalCount(): Int

    @Insert
    suspend fun insertPayment(payment: Payment)

    // ---- attendance ----
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun attendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun attendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(record: AttendanceRecord)

    // ---- tests & marks ----
    @Query("SELECT * FROM tests ORDER BY date DESC, id DESC")
    fun tests(): Flow<List<Test>>

    @Query("SELECT * FROM tests WHERE id = :id")
    suspend fun testById(id: Long): Test?

    @Insert
    suspend fun insertTest(test: Test): Long

    @Delete
    suspend fun deleteTest(test: Test)

    @Query("SELECT * FROM marks WHERE testId = :testId")
    fun marksForTest(testId: Long): Flow<List<Mark>>

    @Query("SELECT * FROM marks WHERE studentId = :studentId")
    fun marksForStudent(studentId: Long): Flow<List<Mark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMark(mark: Mark)
}
