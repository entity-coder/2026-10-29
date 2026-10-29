package com.classmanager.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.R
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.Payment
import com.classmanager.app.data.Student
import com.classmanager.app.data.attendancePct
import com.classmanager.app.data.averagePct
import com.classmanager.app.data.duesOwed
import com.classmanager.app.data.fmtINR
import com.classmanager.app.data.fmtPct
import com.classmanager.app.data.monthsBetweenInclusive
import com.classmanager.app.data.receiptNo
import com.classmanager.app.data.todayIso
import com.classmanager.app.databinding.ActivityStudentDetailBinding
import com.classmanager.app.databinding.DialogPaymentBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * One student's full record: attendance %, average test score, fee dues
 * (months from admission through this month x batch plan, minus paid),
 * and the append-only payment history with printable receipts.
 */
class StudentDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_STUDENT_ID = "student_id"
    }

    private lateinit var binding: ActivityStudentDetailBinding
    private var student: Student? = null
    private val paymentAdapter = PaymentAdapter { payment -> shareReceipt(payment) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val studentId = intent.getLongExtra(EXTRA_STUDENT_ID, -1)
        if (studentId == -1L) {
            finish()
            return
        }
        binding.paymentList.layoutManager = LinearLayoutManager(this)
        binding.paymentList.adapter = paymentAdapter
        val dao = ClassDatabase.get(this).dao()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    dao.students(),
                    dao.feePlans(),
                    dao.paymentsFor(studentId),
                    dao.attendanceForStudent(studentId),
                    dao.marksForStudent(studentId),
                    dao.tests()
                ) { students, plans, payments, attendance, marks, tests ->
                    val found = students.firstOrNull { it.id == studentId }
                    DetailData(found, plans.firstOrNull { it.batch == found?.batch },
                        payments, attendance, marks, tests)
                }.collect { data ->
                    val current = data.student ?: return@collect
                    student = current
                    title = current.name
                    binding.name.text = current.name
                    binding.batch.text = "Batch: ${current.batch}"
                    binding.contact.text = buildString {
                        if (current.phone.isNotEmpty()) append("Student: ${current.phone}")
                        if (current.guardianPhone.isNotEmpty()) {
                            if (isNotEmpty()) append("  |  ")
                            append("Guardian: ${current.guardianPhone}")
                        }
                        if (isEmpty()) append("No phone on file")
                    }
                    binding.admitted.text = "Admitted: ${current.admitted}"
                    val present = attendance.count { it.present }
                    binding.attendancePct.text = fmtPct(
                        attendancePct(present, attendance.size))
                    binding.attendanceLine.text =
                        "$present of ${attendance.size} days present"
                    val maxByTest = tests.associate { it.id to it.maxMarks }
                    binding.marksAvg.text = fmtPct(averagePct(
                        marks.mapNotNull { mark ->
                            val max = maxByTest[mark.testId] ?: return@mapNotNull null
                            mark.obtained to max
                        }))
                    binding.marksLine.text = "${marks.size} test score(s) recorded"
                    val paid = payments.sumOf { it.amount }
                    val plan = data.plan
                    binding.monthlyFee.text = if (plan == null) "No plan for this batch"
                        else fmtINR(plan.monthlyFee) + " / month"
                    val months = monthsBetweenInclusive(current.admitted, todayIso())
                    val due = duesOwed(plan?.monthlyFee, months, paid)
                    binding.dues.text = if (due == null) "Unknown (no fee plan)"
                        else fmtINR(due)
                    binding.paidTotal.text = fmtINR(paid)
                    binding.duesLine.text =
                        "$months month(s) charged x plan, minus paid"
                    paymentAdapter.submit(payments)
                    binding.noPayments.visibility =
                        if (payments.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        binding.recordPayment.setOnClickListener { showPaymentDialog() }
        binding.hideStudent.setOnClickListener { toggleActive() }
        binding.deleteStudent.setOnClickListener { confirmDelete() }
        binding.shareLatestReceipt.setOnClickListener {
            lifecycleScope.launch {
                // The adapter holds payments newest-first, so its first row
                // is the latest receipt — no extra query needed.
                val payment = paymentAdapter.latest() ?: run {
                    Toast.makeText(this@StudentDetailActivity,
                        R.string.no_payments_yet, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                shareReceipt(payment)
            }
        }
    }

    private fun showPaymentDialog() {
        val current = student ?: return
        val dialogBinding = DialogPaymentBinding.inflate(layoutInflater)
        dialogBinding.date.setText(todayIso())
        AlertDialog.Builder(this)
            .setTitle(R.string.record_payment)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.record) { _, _ ->
                val amount = dialogBinding.amount.text.toString()
                    .toDoubleOrNull() ?: 0.0
                val date = dialogBinding.date.text.toString().trim()
                if (amount <= 0.0 ||
                    !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)
                ) {
                    Toast.makeText(this, R.string.payment_invalid,
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val dao = ClassDatabase.get(this@StudentDetailActivity).dao()
                    val receipt = receiptNo(dao.paymentTotalCount())
                    dao.insertPayment(
                        Payment(
                            studentId = current.id,
                            amount = amount,
                            method = dialogBinding.method.text.toString().trim()
                                .ifEmpty { "Cash" },
                            date = date,
                            receiptNo = receipt
                        )
                    )
                    Toast.makeText(this@StudentDetailActivity,
                        getString(R.string.payment_recorded, receipt),
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toggleActive() {
        val current = student ?: return
        lifecycleScope.launch {
            val dao = ClassDatabase.get(this@StudentDetailActivity).dao()
            dao.updateStudent(current.copy(active = !current.active))
            Toast.makeText(this@StudentDetailActivity,
                if (current.active) R.string.student_hidden else R.string.student_shown,
                Toast.LENGTH_SHORT).show()
            if (current.active) finish()
        }
    }

    private fun confirmDelete() {
        val current = student ?: return
        lifecycleScope.launch {
            val dao = ClassDatabase.get(this@StudentDetailActivity).dao()
            val hasHistory = dao.paymentCountFor(current.id) > 0 ||
                dao.attendanceCountFor(current.id) > 0 ||
                dao.markCountFor(current.id) > 0
            if (hasHistory) {
                Toast.makeText(this@StudentDetailActivity,
                    R.string.cannot_delete_student, Toast.LENGTH_LONG).show()
                return@launch
            }
            AlertDialog.Builder(this@StudentDetailActivity)
                .setTitle(R.string.delete_student)
                .setMessage(R.string.delete_student_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        dao.deleteStudent(current)
                        finish()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /** Builds the receipt PDF and opens the share sheet for it. */
    private fun shareReceipt(payment: Payment) {
        val current = student ?: return
        lifecycleScope.launch {
            val file = ReceiptPdf.create(this@StudentDetailActivity, current, payment)
            if (file == null) {
                Toast.makeText(this@StudentDetailActivity,
                    R.string.receipt_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uri = FileProvider.getUriForFile(
                this@StudentDetailActivity,
                "${applicationContext.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_receipt)))
        }
    }

    private data class DetailData(
        val student: Student?,
        val plan: com.classmanager.app.data.FeePlan?,
        val payments: List<Payment>,
        val attendance: List<com.classmanager.app.data.AttendanceRecord>,
        val marks: List<com.classmanager.app.data.Mark>,
        val tests: List<com.classmanager.app.data.Test>
    )
}
