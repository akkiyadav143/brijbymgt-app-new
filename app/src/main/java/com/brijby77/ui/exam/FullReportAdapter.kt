package com.brijby77.ui.exam

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijby77.R
import com.brijby77.data.repository.ConsolidatedStudentReport

class FullReportAdapter(
    private val onStudentClick: (ConsolidatedStudentReport) -> Unit = {}
) : ListAdapter<ConsolidatedStudentReport, FullReportAdapter.FullReportViewHolder>(DiffCallback()) {

    class FullReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvReportExamHeader)
        val tvStudentName: TextView = view.findViewById(R.id.tvReportStudentName)
        val tvStudentDetails: TextView = view.findViewById(R.id.tvReportStudentDetails)
        val layoutSubjectTable: LinearLayout = view.findViewById(R.id.layoutSubjectTable)
        val tvTotalMarks: TextView = view.findViewById(R.id.tvReportTotalMarks)
        val tvGrade: TextView = view.findViewById(R.id.tvReportGrade)
        val tvPassStatus: TextView = view.findViewById(R.id.tvReportPassStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_full_report_card, parent, false)
        return FullReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: FullReportViewHolder, position: Int) {
        val report = getItem(position)
        val student = report.student

        holder.itemView.setOnClickListener {
            onStudentClick(report)
        }

        holder.tvHeader.text = "CONSOLIDATED ANNUAL EXAMINATION MARKSHET\nAcademic Session: ${report.academicSession}"
        holder.tvStudentName.text = "Student Name: ${student.studentName}"
        holder.tvStudentDetails.text = "SR Number: ${student.srNumber.ifBlank { "-" }} | Class: ${student.className} | Father: ${student.fatherName.ifBlank { "-" }} | Gender: ${student.gender}"

        while (holder.layoutSubjectTable.childCount > 1) {
            holder.layoutSubjectTable.removeViewAt(1)
        }

        val context = holder.itemView.context

        for (subj in report.subjectMarksList) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(4, 8, 4, 8)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val tvSub = TextView(context).apply {
                text = subj.subjectName
                textSize = 11f
                setTextColor(Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f)
            }
            val tvU1 = TextView(context).apply {
                text = subj.unit1
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (subj.unit1 == "Ab" || subj.unit1 == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }
            val tvHY = TextView(context).apply {
                text = subj.halfYearly
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (subj.halfYearly == "Ab" || subj.halfYearly == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
            }
            val tvU2 = TextView(context).apply {
                text = subj.unit2
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (subj.unit2 == "Ab" || subj.unit2 == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }
            val tvYr = TextView(context).apply {
                text = subj.yearly
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (subj.yearly == "Ab" || subj.yearly == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }
            val tvSubTot = TextView(context).apply {
                text = "${subj.subjectTotalObtained.toInt()}/${subj.subjectTotalMax}"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            }

            rowLayout.addView(tvSub)
            rowLayout.addView(tvU1)
            rowLayout.addView(tvHY)
            rowLayout.addView(tvU2)
            rowLayout.addView(tvYr)
            rowLayout.addView(tvSubTot)

            holder.layoutSubjectTable.addView(rowLayout)
        }

        // GRAND TOTAL Row
        val grandTotalRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setPadding(4, 6, 4, 6)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val tvGrandTotalLabel = TextView(context).apply {
            text = "GRAND TOTAL"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f)
        }
        val tvU1GrandTot = TextView(context).apply {
            text = "${report.unit1TotalObtained.toInt()}/${report.unit1TotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        val tvHYGrandTot = TextView(context).apply {
            text = "${report.halfYearlyTotalObtained.toInt()}/${report.halfYearlyTotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
        }
        val tvU2GrandTot = TextView(context).apply {
            text = "${report.unit2TotalObtained.toInt()}/${report.unit2TotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        val tvYrGrandTot = TextView(context).apply {
            text = "${report.yearlyTotalObtained.toInt()}/${report.yearlyTotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        val tvOverallGrandTot = TextView(context).apply {
            text = "${report.overallObtained.toInt()}/${report.overallMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
        }

        grandTotalRow.addView(tvGrandTotalLabel)
        grandTotalRow.addView(tvU1GrandTot)
        grandTotalRow.addView(tvHYGrandTot)
        grandTotalRow.addView(tvU2GrandTot)
        grandTotalRow.addView(tvYrGrandTot)
        grandTotalRow.addView(tvOverallGrandTot)

        holder.layoutSubjectTable.addView(grandTotalRow)

        holder.tvTotalMarks.text = "Overall Total: ${report.overallObtained.toInt()} / ${report.overallMax} (${String.format("%.2f", report.percentage)}%)"
        holder.tvGrade.text = "Grade: ${report.grade} | Rank: #${report.rank}"
        holder.tvPassStatus.text = report.passStatus
        holder.tvPassStatus.setBackgroundColor(if (report.passStatus == "PASS") Color.parseColor("#16A34A") else Color.parseColor("#DC2626"))
    }

    class DiffCallback : DiffUtil.ItemCallback<ConsolidatedStudentReport>() {
        override fun areItemsTheSame(oldItem: ConsolidatedStudentReport, newItem: ConsolidatedStudentReport) = oldItem.student.studentId == newItem.student.studentId
        override fun areContentsTheSame(oldItem: ConsolidatedStudentReport, newItem: ConsolidatedStudentReport) = oldItem == newItem
    }
}

