package com.classmanager.app.ui

import android.content.Intent
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
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.duesOwed
import com.classmanager.app.data.fmtINR
import com.classmanager.app.data.monthsBetweenInclusive
import com.classmanager.app.data.todayIso
import com.classmanager.app.databinding.FragmentFeesBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Fee dues per student: months from admission through this month x the
 * batch's monthly plan, minus payments, clamped at zero. Students in a
 * batch with no plan show "unknown" rather than a fake zero. Largest dues
 * first, so follow-up starts at the top. Fee plans are managed on a
 * separate screen (Fee plans button).
 */
class FeesFragment : Fragment() {
    private var _binding: FragmentFeesBinding? = null
    private val binding get() = _binding!!
    private val adapter = FeesAdapter { student ->
        val intent = Intent(requireContext(), StudentDetailActivity::class.java)
        intent.putExtra(StudentDetailActivity.EXTRA_STUDENT_ID, student.id)
        startActivity(intent)
    }
    private var selectedBatch: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.feesList.layoutManager = LinearLayoutManager(requireContext())
        binding.feesList.adapter = adapter
        binding.feePlansButton.setOnClickListener {
            startActivity(Intent(requireContext(), FeePlansActivity::class.java))
        }
        val dao = ClassDatabase.get(requireContext()).dao()
        val today = todayIso()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    dao.students(), dao.feePlans(), dao.allPayments()
                ) { students, plans, payments ->
                    Triple(students, plans, payments)
                }.collect { (students, plans, payments) ->
                    val active = students.filter { it.active }
                    val planByBatch = plans.associateBy { it.batch }
                    val paidByStudent = payments.groupBy { it.studentId }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                    val batches = active.map { it.batch }.distinct().sorted()
                    val spinnerItems =
                        listOf(getString(R.string.all_batches)) + batches
                    val spinnerAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item, spinnerItems)
                    spinnerAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item)
                    if (binding.batchSpinner.adapter == null) {
                        binding.batchSpinner.adapter = spinnerAdapter
                    }
                    binding.batchSpinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?, view: View?,
                                position: Int, id: Long
                            ) {
                                selectedBatch =
                                    if (position == 0) null else spinnerItems[position]
                                render(active, planByBatch, paidByStudent, today)
                            }

                            override fun onNothingSelected(
                                parent: AdapterView<*>?
                            ) {}
                        }
                    render(active, planByBatch, paidByStudent, today)
                }
            }
        }
    }

    private fun render(
        active: List<com.classmanager.app.data.Student>,
        planByBatch: Map<String, com.classmanager.app.data.FeePlan>,
        paidByStudent: Map<Long, Double>,
        today: String
    ) {
        val filtered = if (selectedBatch == null) active
            else active.filter { it.batch == selectedBatch }
        val rows = filtered.map { student ->
            val plan = planByBatch[student.batch]
            val paid = paidByStudent[student.id] ?: 0.0
            val months = monthsBetweenInclusive(student.admitted, today)
            FeesAdapter.Row(
                student = student,
                monthlyFee = plan?.monthlyFee,
                monthsCharged = months,
                paid = paid,
                due = duesOwed(plan?.monthlyFee, months, paid)
            )
        }.sortedWith(
            compareByDescending<FeesAdapter.Row> { it.due ?: -1.0 }
                .thenBy { it.student.name })
        adapter.submit(rows)
        val totalDue = rows.mapNotNull { it.due }.sum()
        binding.totalDues.text = getString(R.string.total_dues_value, fmtINR(totalDue))
        binding.emptyView.visibility =
            if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
