package com.classmanager.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.R
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.databinding.ActivityTestDetailBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Per-student marks for one test. Each row has its own Save button, so a
 * half-filled sheet never loses work: marks save one student at a time.
 * Scores above the test's max are refused, not silently clamped.
 */
class TestDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TEST_ID = "test_id"
    }

    private lateinit var binding: ActivityTestDetailBinding
    private lateinit var adapter: MarksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val testId = intent.getLongExtra(EXTRA_TEST_ID, -1)
        if (testId == -1L) {
            finish()
            return
        }
        val dao = ClassDatabase.get(this).dao()
        adapter = MarksAdapter { student, obtained, maxMarks ->
            if (obtained < 0.0 || obtained > maxMarks) {
                Toast.makeText(this, getString(R.string.mark_out_of_range, maxMarks),
                    Toast.LENGTH_SHORT).show()
                return@MarksAdapter
            }
            lifecycleScope.launch {
                dao.upsertMark(
                    com.classmanager.app.data.Mark(
                        testId = testId,
                        studentId = student.id,
                        obtained = obtained
                    )
                )
                Toast.makeText(this@TestDetailActivity,
                    R.string.mark_saved, Toast.LENGTH_SHORT).show()
            }
        }
        binding.marksList.layoutManager = LinearLayoutManager(this)
        binding.marksList.adapter = adapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    dao.tests(), dao.students(), dao.marksForTest(testId)
                ) { tests, students, marks ->
                    Triple(
                        tests.firstOrNull { it.id == testId },
                        students.filter { it.active },
                        marks.associate { it.studentId to it.obtained }
                    )
                }.collect { (test, students, marks) ->
                    if (test == null) {
                        finish()
                        return@collect
                    }
                    title = test.name
                    binding.testMeta.text =
                        "${test.date}  |  Max marks: ${test.maxMarks}"
                    adapter.submit(test.maxMarks, students, marks)
                    val scored = marks.size
                    binding.summary.text = if (students.isEmpty()) {
                        getString(R.string.no_active_students)
                    } else {
                        getString(R.string.marks_summary, scored, students.size)
                    }
                }
            }
        }
    }
}
