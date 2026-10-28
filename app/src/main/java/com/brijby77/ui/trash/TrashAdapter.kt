package com.brijby77.ui.trash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.databinding.ItemTrashCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrashAdapter(
    private val onRestoreClicked: (StudentWithDetails) -> Unit,
    private val onDeletePermanentlyClicked: (StudentWithDetails) -> Unit,
    private val onSelectionChanged: (Set<Long>) -> Unit
) : ListAdapter<StudentWithDetails, TrashAdapter.ViewHolder>(DiffCallback) {

    private val selectedIds = mutableSetOf<Long>()

    fun getSelectedIds(): Set<Long> = selectedIds.toSet()

    fun setSelectedIds(ids: Set<Long>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds)
    }

    fun toggleSelectAll(selectAll: Boolean, currentList: List<StudentWithDetails>) {
        selectedIds.clear()
        if (selectAll) {
            for (item in currentList) {
                selectedIds.add(item.student.id)
            }
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds)
    }

    inner class ViewHolder(val binding: ItemTrashCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrashCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val s = item.student

        holder.binding.tvTrashStudentName.text = s.studentName.ifBlank { "No Name" }.uppercase()
        val cls = s.studentClass
        val formattedClass = if (cls.startsWith("Class", true) || cls.startsWith("CL", true)) cls else "Class $cls"
        holder.binding.tvTrashClassAndSr.text = "$formattedClass  |  SR No: ${s.srNumber.ifBlank { "-" }}  |  UID: ${s.uniqueId.ifBlank { "-" }}"

        val dateStr = if (s.deletedAt != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(s.deletedAt))
        } else "-"
        holder.binding.tvTrashDeletedAt.text = "Deleted at: $dateStr"

        val isSelected = selectedIds.contains(s.id)
        holder.binding.cbStudentSelect.setOnCheckedChangeListener(null)
        holder.binding.cbStudentSelect.isChecked = isSelected
        holder.binding.cbStudentSelect.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedIds.add(s.id)
            } else {
                selectedIds.remove(s.id)
            }
            onSelectionChanged(selectedIds)
        }

        holder.binding.root.setOnClickListener {
            val newState = !holder.binding.cbStudentSelect.isChecked
            holder.binding.cbStudentSelect.isChecked = newState
        }

        holder.binding.btnRestore.setOnClickListener { onRestoreClicked(item) }
        holder.binding.btnPermanentDelete.setOnClickListener { onDeletePermanentlyClicked(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<StudentWithDetails>() {
        override fun areItemsTheSame(oldItem: StudentWithDetails, newItem: StudentWithDetails): Boolean {
            return oldItem.student.id == newItem.student.id
        }

        override fun areContentsTheSame(oldItem: StudentWithDetails, newItem: StudentWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}

