package com.classmanager.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.R
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.Student
import com.classmanager.app.data.todayIso
import com.classmanager.app.databinding.DialogStudentBinding
import com.classmanager.app.databinding.FragmentStudentsBinding
import kotlinx.coroutines.launch

/** Student roster: search, add, and tap a student for their full record. */
class StudentsFragment : Fragment() {
    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!
    private val adapter = StudentAdapter { student ->
        val intent = Intent(requireContext(), StudentDetailActivity::class.java)
        intent.putExtra(StudentDetailActivity.EXTRA_STUDENT_ID, student.id)
        startActivity(intent)
    }
    private var allStudents: List<Student> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.studentList.layoutManager = LinearLayoutManager(requireContext())
        binding.studentList.adapter = adapter
        val dao = ClassDatabase.get(requireContext()).dao()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.students().collect { students ->
                    allStudents = students
                    applyFilter()
                }
            }
        }
        binding.search.doAfterTextChanged { applyFilter() }
        binding.addStudent.setOnClickListener { showStudentDialog(null) }
    }

    private fun applyFilter() {
        val query = binding.search.text.toString().trim().lowercase()
        val filtered = allStudents.filter { student ->
            (query.isEmpty() || student.name.lowercase().contains(query) ||
                student.phone.contains(query) ||
                student.batch.lowercase().contains(query)) &&
                // Hidden students stay out of the roster; they still exist
                // in history and can be shown again from the detail screen.
                student.active
        }
        adapter.submit(filtered)
        binding.emptyView.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showStudentDialog(existing: Student?) {
        val dialogBinding = DialogStudentBinding.inflate(layoutInflater)
        if (existing != null) {
            dialogBinding.name.setText(existing.name)
            dialogBinding.phone.setText(existing.phone)
            dialogBinding.guardianPhone.setText(existing.guardianPhone)
            dialogBinding.batch.setText(existing.batch)
            dialogBinding.admitted.setText(existing.admitted)
        } else {
            dialogBinding.admitted.setText(todayIso())
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) R.string.add_student else R.string.edit_student)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.name.text.toString().trim()
                val batch = dialogBinding.batch.text.toString().trim()
                val admitted = dialogBinding.admitted.text.toString().trim()
                if (name.isEmpty() || batch.isEmpty() ||
                    !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(admitted)
                ) {
                    Toast.makeText(requireContext(),
                        R.string.student_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val student = (existing ?: Student(
                    name = name, batch = batch, admitted = admitted
                )).copy(
                    name = name,
                    phone = dialogBinding.phone.text.toString().trim(),
                    guardianPhone = dialogBinding.guardianPhone.text.toString().trim(),
                    batch = batch,
                    admitted = admitted
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    val dao = ClassDatabase.get(requireContext()).dao()
                    if (existing == null) dao.insertStudent(student)
                    else dao.updateStudent(student)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
