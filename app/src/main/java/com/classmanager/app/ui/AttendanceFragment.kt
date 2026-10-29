package com.classmanager.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.R
import com.classmanager.app.data.AttendanceRecord
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.Student
import com.classmanager.app.data.todayIso
import com.classmanager.app.databinding.FragmentAttendanceBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Batch-wise daily attendance. Pick a batch and a date, then tap Present or
 * Absent per student — each tap saves immediately, so there is no separate
 * "save" step to forget. Back-to-back marking for another date just works:
 * one record per (student, date), replaced on re-mark.
 */
class AttendanceFragment : Fragment() {
    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private var students: List<Student> = emptyList()
    private var marked: Map<Long, Boolean> = emptyMap()
    private var batches: List<String> = emptyList()
    private var selectedBatch: String? = null
    private var selectedDate: String = todayIso()
    private lateinit var adapter: AttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AttendanceAdapter { student, present ->
            viewLifecycleOwner.lifecycleScope.launch {
                val dao = ClassDatabase.get(requireContext()).dao()
                dao.upsertAttendance(
                    AttendanceRecord(
                        studentId = student.id,
                        date = selectedDate,
                        present = present
                    )
                )
            }
        }
        binding.attendanceList.layoutManager = LinearLayoutManager(requireContext())
        binding.attendanceList.adapter = adapter
        binding.date.setText(selectedDate)
        val dao = ClassDatabase.get(requireContext()).dao()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    dao.students(), dao.batches(), dao.attendanceForDate(selectedDate)
                ) { allStudents, batchList, records ->
                    Triple(allStudents, batchList, records)
                }.collect { (allStudents, batchList, records) ->
                    students = allStudents.filter { it.active }
                    batches = batchList
                    marked = records.associate { it.studentId to it.present }
                    refreshBatchSpinner()
                    render()
                }
            }
        }
        binding.loadDate.setOnClickListener {
            val date = binding.date.text.toString().trim()
            if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) {
                selectedDate = date
                // Re-collect for the new date by recreating the flow owner:
                // simplest correct approach is to reload this fragment.
                parentFragmentManager.beginTransaction()
                    .detach(this).attach(this).commit()
            }
        }
    }

    private fun refreshBatchSpinner() {
        val items = listOf(getString(R.string.all_batches)) + batches
        val spinnerAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, items)
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        // Avoid resetting the selection while the user is picking.
        if (binding.batchSpinner.adapter == null ||
            (binding.batchSpinner.adapter as ArrayAdapter<*>).count != items.size
        ) {
            binding.batchSpinner.adapter = spinnerAdapter
            val index = selectedBatch?.let { items.indexOf(it) }?.takeIf { it >= 0 } ?: 0
            binding.batchSpinner.setSelection(index)
        }
        binding.batchSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val batch = if (position == 0) null else items[position]
                    if (batch != selectedBatch) {
                        selectedBatch = batch
                        render()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun render() {
        val filtered = if (selectedBatch == null) students
            else students.filter { it.batch == selectedBatch }
        adapter.submit(filtered, marked)
        val present = filtered.count { marked[it.id] == true }
        val total = filtered.count { marked.containsKey(it.id) }
        binding.summary.text = if (total == 0) {
            getString(R.string.attendance_none_marked)
        } else {
            getString(R.string.attendance_summary, present, total)
        }
        binding.emptyView.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
