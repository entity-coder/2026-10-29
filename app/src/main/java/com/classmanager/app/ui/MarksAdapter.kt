package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.R
import com.classmanager.app.data.Student
import com.classmanager.app.databinding.ItemMarkBinding

/** One row per active student: name, current mark (if any), input + Save. */
class MarksAdapter(
    private val onSave: (Student, Double, Double) -> Unit
) : RecyclerView.Adapter<MarksAdapter.Holder>() {
    private var maxMarks: Double = 0.0
    private var students: List<Student> = emptyList()
    private var marks: Map<Long, Double> = emptyMap()

    fun submit(maxMarks: Double, students: List<Student>, marks: Map<Long, Double>) {
        this.maxMarks = maxMarks
        this.students = students
        this.marks = marks
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemMarkBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemMarkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val student = students[position]
        holder.binding.name.text = student.name
        holder.binding.batch.text = student.batch
        val existing = marks[student.id]
        holder.binding.currentMark.text = if (existing == null) {
            holder.itemView.context.getString(R.string.not_scored)
        } else {
            holder.itemView.context.getString(R.string.scored, existing, maxMarks)
        }
        holder.binding.markInput.setText(
            if (existing == null) "" else existing.toString())
        holder.binding.saveButton.setOnClickListener {
            val value = holder.binding.markInput.text.toString()
                .toDoubleOrNull()
            if (value == null) {
                holder.binding.markInput.error =
                    holder.itemView.context.getString(R.string.enter_number)
                return@setOnClickListener
            }
            onSave(student, value, maxMarks)
        }
    }

    override fun getItemCount(): Int = students.size
}
