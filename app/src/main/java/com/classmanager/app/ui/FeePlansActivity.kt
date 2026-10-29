package com.classmanager.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.classmanager.app.R
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.data.FeePlan
import com.classmanager.app.databinding.ActivityFeePlansBinding
import com.classmanager.app.databinding.DialogFeePlanBinding
import kotlinx.coroutines.launch

/**
 * Monthly fee per batch. Editing a plan changes future dues only: dues are
 * computed at read time from (months x current plan - paid), and payments
 * are never rewritten, so past receipts stay honest.
 */
class FeePlansActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeePlansBinding
    private val adapter = FeePlanAdapter(
        onEdit = { plan -> showPlanDialog(plan) },
        onDelete = { plan ->
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_plan)
                .setMessage(R.string.delete_plan_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        ClassDatabase.get(this@FeePlansActivity)
                            .dao().deleteFeePlan(plan)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeePlansBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.planList.layoutManager = LinearLayoutManager(this)
        binding.planList.adapter = adapter
        val dao = ClassDatabase.get(this).dao()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.feePlans().collect { plans ->
                    adapter.submit(plans)
                    binding.emptyView.visibility =
                        if (plans.isEmpty()) android.view.View.VISIBLE
                        else android.view.View.GONE
                }
            }
        }
        binding.addPlan.setOnClickListener { showPlanDialog(null) }
    }

    private fun showPlanDialog(existing: FeePlan?) {
        val dialogBinding = DialogFeePlanBinding.inflate(layoutInflater)
        if (existing != null) {
            dialogBinding.batch.setText(existing.batch)
            dialogBinding.batch.isEnabled = false
            dialogBinding.monthlyFee.setText(existing.monthlyFee.toString())
        }
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.add_plan else R.string.edit_plan)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val batch = dialogBinding.batch.text.toString().trim()
                val fee = dialogBinding.monthlyFee.text.toString()
                    .toDoubleOrNull() ?: 0.0
                if (batch.isEmpty() || fee <= 0.0) {
                    Toast.makeText(this, R.string.plan_invalid,
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    ClassDatabase.get(this@FeePlansActivity).dao()
                        .upsertFeePlan(FeePlan(batch = batch, monthlyFee = fee))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
