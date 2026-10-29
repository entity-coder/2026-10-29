package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.R
import com.classmanager.app.data.Student
import com.classmanager.app.data.fmtINR
import com.classmanager.app.databinding.ItemFeeBinding

/** One row per student: batch, plan, months charged, paid, and dues. */
class FeesAdapter(private val onClick: (Student) -> Unit) :
    RecyclerView.Adapter<FeesAdapter.Holder>() {
    data class Row(
        val student: Student,
        val monthlyFee: Double?,
        val monthsCharged: Int,
        val paid: Double,
        val due: Double?
    )

    private var items: List<Row> = emptyList()

    fun submit(items: List<Row>) {
        this.items = items
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemFeeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemFeeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = items[position]
        val context = holder.itemView.context
        holder.binding.name.text = row.student.name
        holder.binding.batch.text = row.student.batch
        holder.binding.plan.text = if (row.monthlyFee == null) {
            context.getString(R.string.no_plan)
        } else {
            context.getString(
                R.string.plan_line, fmtINR(row.monthlyFee), row.monthsCharged)
        }
        holder.binding.paid.text =
            context.getString(R.string.paid_line, fmtINR(row.paid))
        holder.binding.due.text = if (row.due == null) {
            context.getString(R.string.dues_unknown)
        } else {
            context.getString(R.string.due_line, fmtINR(row.due))
        }
        holder.binding.root.setOnClickListener { onClick(row.student) }
    }

    override fun getItemCount(): Int = items.size
}
