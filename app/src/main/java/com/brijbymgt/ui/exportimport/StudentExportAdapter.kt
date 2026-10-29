package com.brijbymgt.ui.exportimport

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.databinding.ItemStudentExportRowBinding
import java.util.Locale

class StudentExportAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<StudentExportAdapter.ViewHolder>() {

    private var allItems = listOf<StudentWithDetails>()
    private var filteredItems = listOf<StudentWithDetails>()
    val selectedStudentIds = mutableSetOf<Long>()

    fun submitList(list: List<StudentWithDetails>) {
        allItems = list
        filter("", "All")
    }

    fun filter(query: String, classFilter: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        filteredItems = allItems.filter { item ->
            val s = item.student
            val matchesClass = when (classFilter.lowercase(Locale.getDefault())) {
                "all" -> true
                "class 0", "0" -> s.studentClass.equals("Class 0", true) || s.studentClass == "0"
                "cl pass", "pass" -> s.studentClass.equals("CL Pass", true) || s.studentClass.equals("Pass", true)
                else -> {
                    val c = s.studentClass
                    val rawC = c.replace("Class ", "").replace("CL ", "").trim()
                    val rawTarget = classFilter.replace("Class ", "").replace("CL ", "").trim()
                    c.equals(classFilter, true) || rawC.equals(rawTarget, true)
                }
            }
            if (!matchesClass) return@filter false

            if (q.isEmpty()) return@filter true
            s.studentName.lowercase(Locale.getDefault()).contains(q) ||
            s.srNumber.lowercase(Locale.getDefault()).contains(q) ||
            s.srNo.lowercase(Locale.getDefault()).contains(q) ||
            s.studentAdhar.lowercase(Locale.getDefault()).contains(q)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedStudentIds.size)
    }

    fun selectAll(select: Boolean) {
        if (select) {
            filteredItems.forEach { selectedStudentIds.add(it.student.id) }
        } else {
            filteredItems.forEach { selectedStudentIds.remove(it.student.id) }
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedStudentIds.size)
    }

    inner class ViewHolder(val binding: ItemStudentExportRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemStudentExportRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        val s = item.student

        holder.binding.cbStudentSelect.setOnCheckedChangeListener(null)
        holder.binding.cbStudentSelect.isChecked = selectedStudentIds.contains(s.id)
        holder.binding.tvExpSr.text = s.srNo.ifBlank { "${position + 1}" }
        holder.binding.tvExpSrNo.text = s.srNumber.ifBlank { "-" }
        holder.binding.tvExpName.text = s.studentName.ifBlank { "Unnamed" }
        holder.binding.tvExpClass.text = s.studentClass

        holder.binding.cbStudentSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedStudentIds.add(s.id)
            } else {
                selectedStudentIds.remove(s.id)
            }
            onSelectionChanged(selectedStudentIds.size)
        }

        holder.itemView.setOnClickListener {
            val newState = !selectedStudentIds.contains(s.id)
            if (newState) {
                selectedStudentIds.add(s.id)
            } else {
                selectedStudentIds.remove(s.id)
            }
            holder.binding.cbStudentSelect.isChecked = newState
            onSelectionChanged(selectedStudentIds.size)
        }
    }

    override fun getItemCount() = filteredItems.size
}

