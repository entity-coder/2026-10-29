package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.data.FeePlan
import com.classmanager.app.data.fmtINR
import com.classmanager.app.databinding.ItemFeePlanBinding

class FeePlanAdapter(
    private val onEdit: (FeePlan) -> Unit,
    private val onDelete: (FeePlan) -> Unit
) : RecyclerView.Adapter<FeePlanAdapter.Holder>() {
    private var items: List<FeePlan> = emptyList()

    fun submit(items: List<FeePlan>) {
        this.items = items
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemFeePlanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemFeePlanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val plan = items[position]
        holder.binding.batch.text = plan.batch
        holder.binding.fee.text = fmtINR(plan.monthlyFee) + " / month"
        holder.binding.editButton.setOnClickListener { onEdit(plan) }
        holder.binding.deleteButton.setOnClickListener { onDelete(plan) }
    }

    override fun getItemCount(): Int = items.size
}
