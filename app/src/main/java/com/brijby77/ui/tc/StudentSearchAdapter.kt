package com.brijby77.ui.tc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.R
import com.brijby77.data.entity.StudentEntity

class StudentSearchAdapter(
    private var students: List<StudentEntity>,
    private val onStudentClick: (StudentEntity) -> Unit
) : RecyclerView.Adapter<StudentSearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvResultName)
        val tvSr: TextView = view.findViewById(R.id.tvResultSr)
        val tvClass: TextView = view.findViewById(R.id.tvResultClass)
        val tvFather: TextView = view.findViewById(R.id.tvResultFather)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = "☑ ${student.studentName.ifBlank { "-" }}"
        holder.tvSr.text = "SR No: ${student.srNumber.ifBlank { "-" }}"
        holder.tvClass.text = "Class: ${student.studentClass.ifBlank { "-" }}"
        holder.tvFather.text = "Father: ${student.fatherName.ifBlank { "-" }}"

        holder.itemView.setOnClickListener {
            onStudentClick(student)
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateList(newList: List<StudentEntity>) {
        students = newList
        notifyDataSetChanged()
    }
}

