package com.brijbymgt.ui.exam

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijbymgt.R
import com.brijbymgt.data.entity.ExamStudentEntity

data class StudentMarkItem(
    val examStudent: ExamStudentEntity,
    var obtainedMarks: Double,
    var isAbsent: Boolean,
    val maxMarks: Int,
    val passingMarks: Int,
    val srIndex: Int,
    var isSaved: Boolean = false
)

class MarksEntryAdapter(
    private val onMarkChanged: (StudentMarkItem) -> Unit
) : ListAdapter<StudentMarkItem, MarksEntryAdapter.MarkViewHolder>(DiffCallback()) {

    class MarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSrNo: TextView = view.findViewById(R.id.tvSrNo)
        val tvName: TextView = view.findViewById(R.id.tvMarkStudentName)
        val tvSr: TextView = view.findViewById(R.id.tvMarkSrNumber)
        val tvMarksInfo: TextView = view.findViewById(R.id.tvRowMarksInfo)
        val etMarks: EditText = view.findViewById(R.id.etObtainedMarks)
        val cbAbsent: CheckBox = view.findViewById(R.id.cbIsAbsent)
        val tvSaveStatus: TextView = view.findViewById(R.id.tvSaveStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_marks_entry_row, parent, false)
        return MarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarkViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvSrNo.text = "${item.srIndex}."
        holder.tvName.text = item.examStudent.studentName
        holder.tvSr.text = "SR: ${item.examStudent.srNumber.ifBlank { "-" }}"
        holder.tvMarksInfo.text = "Max: ${item.maxMarks} | Pass: ${item.passingMarks}"

        holder.etMarks.removeTextChangedListener(holder.itemView.tag as? TextWatcher)

        if (item.isAbsent) {
            holder.etMarks.setText("Ab")
            holder.etMarks.isEnabled = false
        } else {
            holder.etMarks.setText(if (item.obtainedMarks > 0.0) item.obtainedMarks.toString().removeSuffix(".0") else "")
            holder.etMarks.isEnabled = true
        }

        holder.cbAbsent.isChecked = item.isAbsent

        if (item.isSaved) {
            holder.tvSaveStatus.visibility = View.VISIBLE
            holder.tvSaveStatus.text = "✓"
        } else {
            holder.tvSaveStatus.visibility = View.GONE
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                if (str.equals("Ab", true)) {
                    item.isAbsent = true
                    item.obtainedMarks = 0.0
                } else {
                    val parsed = str.toDoubleOrNull() ?: 0.0
                    if (parsed > item.maxMarks) {
                        holder.etMarks.error = "Max: ${item.maxMarks}"
                    } else if (parsed < 0.0) {
                        holder.etMarks.error = "Min: 0"
                    } else {
                        item.obtainedMarks = parsed
                        item.isSaved = false
                        holder.tvSaveStatus.visibility = View.GONE
                        onMarkChanged(item)
                    }
                }
            }
        }
        holder.etMarks.addTextChangedListener(watcher)
        holder.itemView.tag = watcher

        holder.cbAbsent.setOnCheckedChangeListener { _, isChecked ->
            item.isAbsent = isChecked
            item.isSaved = false
            holder.tvSaveStatus.visibility = View.GONE
            holder.etMarks.isEnabled = !isChecked
            if (isChecked) {
                item.obtainedMarks = 0.0
                holder.etMarks.setText("Ab")
            } else {
                holder.etMarks.setText("")
            }
            onMarkChanged(item)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StudentMarkItem>() {
        override fun areItemsTheSame(oldItem: StudentMarkItem, newItem: StudentMarkItem) = oldItem.examStudent.studentId == newItem.examStudent.studentId
        override fun areContentsTheSame(oldItem: StudentMarkItem, newItem: StudentMarkItem) = oldItem == newItem
    }
}

