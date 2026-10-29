package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.data.Student
import com.classmanager.app.databinding.ItemStudentBinding

class StudentAdapter(private val onClick: (Student) -> Unit) :
    RecyclerView.Adapter<StudentAdapter.Holder>() {
    private var items: List<Student> = emptyList()

    fun submit(items: List<Student>) {
        this.items = items
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val student = items[position]
        holder.binding.name.text = student.name
        holder.binding.batch.text = student.batch
        holder.binding.phone.text =
            if (student.phone.isEmpty()) "No phone on file" else student.phone
        holder.binding.root.setOnClickListener { onClick(student) }
    }

    override fun getItemCount(): Int = items.size
}
