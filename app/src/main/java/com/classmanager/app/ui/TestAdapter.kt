package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.data.Test
import com.classmanager.app.databinding.ItemTestBinding

class TestAdapter(
    private val onClick: (Test) -> Unit,
    private val onDelete: (Test) -> Unit
) : RecyclerView.Adapter<TestAdapter.Holder>() {
    private var items: List<Test> = emptyList()

    fun submit(items: List<Test>) {
        this.items = items
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemTestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemTestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val test = items[position]
        holder.binding.name.text = test.name
        holder.binding.meta.text = "${test.date}  |  Max ${test.maxMarks}"
        holder.binding.root.setOnClickListener { onClick(test) }
        holder.binding.deleteButton.setOnClickListener { onDelete(test) }
    }

    override fun getItemCount(): Int = items.size
}
