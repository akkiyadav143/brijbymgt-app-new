package com.brijby77.ui.exam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.R
import com.brijby77.data.repository.StudentExamResultSummary

class ResultAdapter(
    private val onReportCardClick: (StudentExamResultSummary) -> Unit
) : ListAdapter<StudentExamResultSummary, ResultAdapter.ResultViewHolder>(DiffCallback()) {

    class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvResultRank)
        val tvName: TextView = view.findViewById(R.id.tvResultStudentName)
        val tvDetails: TextView = view.findViewById(R.id.tvResultDetails)
        val tvMarks: TextView = view.findViewById(R.id.tvResultMarks)
        val tvStatus: TextView = view.findViewById(R.id.tvResultStatus)
        val btnReportCard: Button = view.findViewById(R.id.btnViewReportCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result_row, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvRank.text = "#${item.rank}"
        holder.tvName.text = item.examStudent.studentName
        holder.tvDetails.text = "SR: ${item.examStudent.srNumber} | Father: ${item.examStudent.fatherName}"
        holder.tvMarks.text = "Marks: ${item.totalObtainedMarks.toInt()} / Sub Max: ${item.totalMaxMarks} (Exam Max: ${item.examWideMaxMarks}) | ${String.format("%.1f", item.percentage)}%"
        holder.tvStatus.text = "${item.passStatus} | Grade: ${item.grade}"
        holder.tvStatus.setTextColor(if (item.passStatus == "PASS") android.graphics.Color.parseColor("#16A34A") else android.graphics.Color.parseColor("#DC2626"))

        holder.btnReportCard.setOnClickListener { onReportCardClick(item) }
    }

    class DiffCallback : DiffUtil.ItemCallback<StudentExamResultSummary>() {
        override fun areItemsTheSame(oldItem: StudentExamResultSummary, newItem: StudentExamResultSummary) = oldItem.examStudent.studentId == newItem.examStudent.studentId
        override fun areContentsTheSame(oldItem: StudentExamResultSummary, newItem: StudentExamResultSummary) = oldItem == newItem
    }
}

