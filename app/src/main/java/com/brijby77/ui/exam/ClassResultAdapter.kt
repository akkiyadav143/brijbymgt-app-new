package com.brijby77.ui.exam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.R
import com.brijby77.data.repository.StudentExamResultSummary

class ClassResultAdapter : ListAdapter<StudentExamResultSummary, ClassResultAdapter.ClassResultViewHolder>(DiffCallback()) {

    class ClassResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvClassRank)
        val tvName: TextView = view.findViewById(R.id.tvClassStudentName)
        val tvDetails: TextView = view.findViewById(R.id.tvClassStudentDetails)
        val tvScore: TextView = view.findViewById(R.id.tvClassStudentScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class_result_row, parent, false)
        return ClassResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvRank.text = "#${item.rank}"
        holder.tvName.text = item.examStudent.studentName
        holder.tvDetails.text = "SR: ${item.examStudent.srNumber} | Class: ${item.examStudent.className}"
        holder.tvScore.text = "${item.totalObtainedMarks.toInt()} / ${item.totalMaxMarks} (${String.format("%.1f", item.percentage)}%) | ${item.passStatus}"
    }

    class DiffCallback : DiffUtil.ItemCallback<StudentExamResultSummary>() {
        override fun areItemsTheSame(oldItem: StudentExamResultSummary, newItem: StudentExamResultSummary) = oldItem.examStudent.studentId == newItem.examStudent.studentId
        override fun areContentsTheSame(oldItem: StudentExamResultSummary, newItem: StudentExamResultSummary) = oldItem == newItem
    }
}

