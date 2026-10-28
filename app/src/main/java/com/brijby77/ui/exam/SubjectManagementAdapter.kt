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
import com.brijby77.data.entity.SubjectEntity

class SubjectManagementAdapter(
    private val onEdit: (SubjectEntity) -> Unit,
    private val onDeleteOrDeactivate: (SubjectEntity) -> Unit,
    private val onMoveUp: (SubjectEntity) -> Unit,
    private val onMoveDown: (SubjectEntity) -> Unit
) : ListAdapter<SubjectEntity, SubjectManagementAdapter.SubjectViewHolder>(DiffCallback()) {

    class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvStatus: TextView = view.findViewById(R.id.tvSubjectStatus)
        val tvDetails: TextView = view.findViewById(R.id.tvSubjectDetails)
        val btnMoveUp: Button = view.findViewById(R.id.btnSubMoveUp)
        val btnMoveDown: Button = view.findViewById(R.id.btnSubMoveDown)
        val btnEdit: Button = view.findViewById(R.id.btnSubEdit)
        val btnDelete: Button = view.findViewById(R.id.btnSubDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject_management, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = getItem(position)
        holder.tvName.text = subject.subjectName
        holder.tvStatus.text = if (subject.isActive) "Active" else "Deactivated"
        holder.tvStatus.setTextColor(if (subject.isActive) android.graphics.Color.parseColor("#16A34A") else android.graphics.Color.parseColor("#DC2626"))

        val includeStr = if (subject.includeInResult) "Yes" else "No"
        holder.tvDetails.text = "Max: ${subject.maxMarks} | Pass: ${subject.passingMarks} | Order: ${subject.subjectOrder} | Result: $includeStr"

        holder.btnMoveUp.setOnClickListener { onMoveUp(subject) }
        holder.btnMoveDown.setOnClickListener { onMoveDown(subject) }
        holder.btnEdit.setOnClickListener { onEdit(subject) }
        holder.btnDelete.setOnClickListener { onDeleteOrDeactivate(subject) }
    }

    class DiffCallback : DiffUtil.ItemCallback<SubjectEntity>() {
        override fun areItemsTheSame(oldItem: SubjectEntity, newItem: SubjectEntity) = oldItem.subjectId == newItem.subjectId
        override fun areContentsTheSame(oldItem: SubjectEntity, newItem: SubjectEntity) = oldItem == newItem
    }
}

