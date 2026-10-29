package com.brijbymgt.ui.exam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijbymgt.R
import com.brijbymgt.data.entity.ExamEntity

class ExamAdapter(
    private val onExamClick: (ExamEntity) -> Unit,
    private val onDeleteClick: (ExamEntity) -> Unit
) : ListAdapter<ExamEntity, ExamAdapter.ExamViewHolder>(DiffCallback()) {

    class ExamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemExamName)
        val tvDetails: TextView = view.findViewById(R.id.tvItemExamDetails)
        val tvStatus: TextView = view.findViewById(R.id.tvItemExamStatus)
        val btnDelete: Button = view.findViewById(R.id.btnItemDeleteExam)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exam_card, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        val exam = getItem(position)
        holder.tvName.text = exam.examName
        holder.tvDetails.text = "Type: ${exam.examType} | Class: ${exam.className} | Session: ${exam.academicSession}"
        holder.tvStatus.text = if (exam.isFinalized) "Finalized" else exam.status

        holder.itemView.setOnClickListener { onExamClick(exam) }
        holder.btnDelete.setOnClickListener { onDeleteClick(exam) }
    }

    class DiffCallback : DiffUtil.ItemCallback<ExamEntity>() {
        override fun areItemsTheSame(oldItem: ExamEntity, newItem: ExamEntity) = oldItem.examId == newItem.examId
        override fun areContentsTheSame(oldItem: ExamEntity, newItem: ExamEntity) = oldItem == newItem
    }
}

