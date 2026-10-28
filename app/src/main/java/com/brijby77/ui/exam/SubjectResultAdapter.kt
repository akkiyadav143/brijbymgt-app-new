package com.brijby77.ui.exam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.R
import com.brijby77.data.entity.ExamStudentEntity
import com.brijby77.data.entity.MarksEntity

data class SubjectStudentMark(
    val student: ExamStudentEntity,
    val mark: MarksEntity?,
    val maxMarks: Int,
    val passingMarks: Int
)

class SubjectResultAdapter : ListAdapter<SubjectStudentMark, SubjectResultAdapter.SubjectViewHolder>(DiffCallback()) {

    class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSubjectStudentName)
        val tvDetails: TextView = view.findViewById(R.id.tvSubjectStudentDetails)
        val tvScore: TextView = view.findViewById(R.id.tvSubjectStudentScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject_result_row, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvName.text = item.student.studentName
        holder.tvDetails.text = "SR: ${item.student.srNumber} | Father: ${item.student.fatherName}"
        val scoreStr = if (item.mark != null) {
            if (item.mark.isAbsent) "Absent" else "${item.mark.obtainedMarks.toInt()} / ${item.maxMarks}"
        } else {
            "Not Entered"
        }
        val isPass = item.mark != null && !item.mark.isAbsent && item.mark.obtainedMarks >= item.passingMarks
        holder.tvScore.text = "$scoreStr | ${if (isPass) "PASS" else "FAIL"}"
        holder.tvScore.setTextColor(if (isPass) android.graphics.Color.parseColor("#16A34A") else android.graphics.Color.parseColor("#DC2626"))
    }

    class DiffCallback : DiffUtil.ItemCallback<SubjectStudentMark>() {
        override fun areItemsTheSame(oldItem: SubjectStudentMark, newItem: SubjectStudentMark) = oldItem.student.studentId == newItem.student.studentId
        override fun areContentsTheSame(oldItem: SubjectStudentMark, newItem: SubjectStudentMark) = oldItem == newItem
    }
}

