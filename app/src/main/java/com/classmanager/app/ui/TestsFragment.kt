package com.classmanager.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.R
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.Test
import com.classmanager.app.data.todayIso
import com.classmanager.app.databinding.DialogTestBinding
import com.classmanager.app.databinding.FragmentTestsBinding
import kotlinx.coroutines.launch

/** Test list: create a test, tap one to enter per-student marks. */
class TestsFragment : Fragment() {
    private var _binding: FragmentTestsBinding? = null
    private val binding get() = _binding!!
    private val adapter = TestAdapter(
        onClick = { test ->
            val intent = Intent(requireContext(), TestDetailActivity::class.java)
            intent.putExtra(TestDetailActivity.EXTRA_TEST_ID, test.id)
            startActivity(intent)
        },
        onDelete = { test ->
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_test)
                .setMessage(R.string.delete_test_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        ClassDatabase.get(requireContext()).dao().deleteTest(test)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.testList.layoutManager = LinearLayoutManager(requireContext())
        binding.testList.adapter = adapter
        val dao = ClassDatabase.get(requireContext()).dao()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.tests().collect { tests ->
                    adapter.submit(tests)
                    binding.emptyView.visibility =
                        if (tests.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        binding.addTest.setOnClickListener {
            val dialogBinding = DialogTestBinding.inflate(layoutInflater)
            dialogBinding.date.setText(todayIso())
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_test)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.save) { _, _ ->
                    val name = dialogBinding.name.text.toString().trim()
                    val max = dialogBinding.maxMarks.text.toString()
                        .toDoubleOrNull() ?: 0.0
                    val date = dialogBinding.date.text.toString().trim()
                    if (name.isEmpty() || max <= 0.0 ||
                        !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)
                    ) {
                        Toast.makeText(requireContext(), R.string.test_invalid,
                            Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    viewLifecycleOwner.lifecycleScope.launch {
                        dao.insertTest(Test(name = name, date = date, maxMarks = max))
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
