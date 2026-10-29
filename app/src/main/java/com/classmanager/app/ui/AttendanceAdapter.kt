package com.classmanager.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.classmanager.app.R
import com.classmanager.app.data.Student
import com.classmanager.app.databinding.ItemAttendanceBinding

/** One row per student with Present / Absent buttons; the marked state is
 *  highlighted, and tapping either button saves immediately. */
class AttendanceAdapter(
    private val onMark: (Student, Boolean) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.Holder>() {
    private var students: List<Student> = emptyList()
    private var marked: Map<Long, Boolean> = emptyMap()

    fun submit(students: List<Student>, marked: Map<Long, Boolean>) {
        this.students = students
        this.marked = marked
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val student = students[position]
        holder.binding.name.text = student.name
        holder.binding.batch.text = student.batch
        val state = marked[student.id]
        holder.binding.presentButton.isSelected = state == true
        holder.binding.absentButton.isSelected = state == false
        holder.binding.status.text = when (state) {
            true -> holder.itemView.context.getString(R.string.present)
            false -> holder.itemView.context.getString(R.string.absent)
            null -> holder.itemView.context.getString(R.string.not_marked)
        }
        holder.binding.presentButton.setOnClickListener { onMark(student, true) }
        holder.binding.absentButton.setOnClickListener { onMark(student, false) }
    }

    override fun getItemCount(): Int = students.size
}
