package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.data.Student
import com.classmanager.app.data.fmtINR
import com.classmanager.app.databinding.ItemDefaulterBinding

/** Small read-only rows for the dashboard's top-defaulters list. */
class DefaulterAdapter : RecyclerView.Adapter<DefaulterAdapter.Holder>() {
    private var items: List<Pair<Student, Double>> = emptyList()

    fun submit(items: List<Pair<Student, Double>>) {
        this.items = items
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemDefaulterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDefaulterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (student, due) = items[position]
        holder.binding.name.text = student.name
        holder.binding.batch.text = student.batch
        holder.binding.due.text = fmtINR(due)
    }

    override fun getItemCount(): Int = items.size
}
