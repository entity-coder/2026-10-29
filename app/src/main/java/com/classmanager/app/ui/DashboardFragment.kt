package com.classmanager.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.duesOwed
import com.classmanager.app.data.fmtINR
import com.classmanager.app.data.fmtPct
import com.classmanager.app.data.monthsBetweenInclusive
import com.classmanager.app.data.todayIso
import com.classmanager.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** At-a-glance numbers: students, fees collected, total dues, today's
 *  attendance, plus the five largest defaulters for follow-up. */
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dao = ClassDatabase.get(requireContext()).dao()
        val adapter = DefaulterAdapter()
        binding.defaulterList.layoutManager = LinearLayoutManager(requireContext())
        binding.defaulterList.adapter = adapter
        val today = todayIso()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    dao.students(), dao.feePlans(), dao.allPayments(),
                    dao.attendanceForDate(today)
                ) { students, plans, payments, todayAttendance ->
                    val active = students.filter { it.active }
                    val planByBatch = plans.associateBy { it.batch }
                    val paidByStudent = payments.groupBy { it.studentId }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                    val dues = active.mapNotNull { student ->
                        val plan = planByBatch[student.batch] ?: return@mapNotNull null
                        val months = monthsBetweenInclusive(student.admitted, today)
                        val due = duesOwed(plan.monthlyFee, months,
                            paidByStudent[student.id] ?: 0.0) ?: 0.0
                        student to due
                    }
                    val present = todayAttendance.count { it.present }
                    val marked = todayAttendance.size
                    DashboardData(
                        studentCount = active.size,
                        collected = payments.sumOf { it.amount },
                        totalDues = dues.sumOf { it.second },
                        noPlanCount = active.count { planByBatch[it.batch] == null },
                        todayPct = if (marked == 0) null
                            else present * 100.0 / marked,
                        todayLine = if (marked == 0) "No attendance marked today yet"
                            else "$present of $marked present today",
                        defaulters = dues.filter { it.second > 0.0 }
                            .sortedByDescending { it.second }.take(5)
                    )
                }.collect { data ->
                    binding.studentCount.text = data.studentCount.toString()
                    binding.feesCollected.text = fmtINR(data.collected)
                    binding.totalDues.text = fmtINR(data.totalDues)
                    binding.todayAttendance.text = fmtPct(data.todayPct)
                    binding.todayAttendanceLine.text = data.todayLine
                    binding.noPlanNote.text =
                        if (data.noPlanCount == 0) "Every batch has a fee plan."
                        else "${data.noPlanCount} student(s) are in a batch with no fee plan — their dues show as unknown."
                    adapter.submit(data.defaulters)
                    binding.noDefaulters.visibility =
                        if (data.defaulters.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class DashboardData(
        val studentCount: Int,
        val collected: Double,
        val totalDues: Double,
        val noPlanCount: Int,
        val todayPct: Double?,
        val todayLine: String,
        val defaulters: List<Pair<com.classmanager.app.data.Student, Double>>
    )
}
