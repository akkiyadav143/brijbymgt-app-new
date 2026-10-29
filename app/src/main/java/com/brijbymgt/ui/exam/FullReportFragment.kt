package com.brijbymgt.ui.exam

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.repository.ConsolidatedStudentReport
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentFullReportBinding
import com.brijbymgt.util.ConsolidatedPdfExporter
import com.brijbymgt.util.PdfPrintHelper
import kotlinx.coroutines.launch
import java.io.File

class FullReportFragment : Fragment() {

    private var _binding: FragmentFullReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExamViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = ExamRepository(
            db.examDao(),
            db.subjectDao(),
            db.examStudentDao(),
            db.marksDao(),
            db.studentDao()
        )
        ExamViewModelFactory(repo)
    }

    private lateinit var fullReportAdapter: FullReportAdapter
    private val availableClasses = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8")
    private var currentReports: List<ConsolidatedStudentReport> = emptyList()
    private var selectedReport: ConsolidatedStudentReport? = null

    private var pendingPdfFileToSave: File? = null

    private val saveSinglePdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { destinationUri ->
            pendingPdfFileToSave?.let { sourceFile ->
                try {
                    requireContext().contentResolver.openOutputStream(destinationUri)?.use { output ->
                        sourceFile.inputStream().use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(requireContext(), "Single Marksheet saved successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val saveAllPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { destinationUri ->
            pendingPdfFileToSave?.let { sourceFile ->
                try {
                    requireContext().contentResolver.openOutputStream(destinationUri)?.use { output ->
                        sourceFile.inputStream().use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(requireContext(), "All Marksheets saved successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSpinners()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        fullReportAdapter = FullReportAdapter(
            onStudentClick = { report ->
                selectedReport = report
                updateStudentSpinnerSelection(report)
                Toast.makeText(requireContext(), "Selected: ${report.student.studentName}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvFullReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fullReportAdapter
        }
    }

    private fun setupSpinners() {
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, availableClasses)
        binding.spinnerReportClass.adapter = classAdapter

        binding.spinnerReportClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in availableClasses.indices) {
                    val selectedClass = availableClasses[position]
                    loadConsolidatedReport(selectedClass)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateStudentSpinnerSelection(report: ConsolidatedStudentReport) {
        val index = currentReports.indexOf(report)
        if (index >= 0 && index < binding.spinnerReportStudent.count) {
            binding.spinnerReportStudent.setSelection(index)
        }
    }

    private fun loadConsolidatedReport(className: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val reports = viewModel.getConsolidatedFullReport(className)
            currentReports = reports
            if (reports.isEmpty()) {
                fullReportAdapter.submitList(emptyList())
                binding.spinnerReportStudent.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("No students available"))
                selectedReport = null
                Toast.makeText(requireContext(), "कोई छात्र या परीक्षा डेटा उपलब्ध नहीं है।", Toast.LENGTH_SHORT).show()
            } else {
                fullReportAdapter.submitList(reports)
                val studentLabels = reports.map { "SR: ${it.student.srNumber.ifBlank { "-" }} - ${it.student.studentName}" }
                val studentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, studentLabels)
                binding.spinnerReportStudent.adapter = studentAdapter
                binding.spinnerReportStudent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position in currentReports.indices) {
                            selectedReport = currentReports[position]
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                selectedReport = reports.firstOrNull()
            }
        }
    }

    private fun setupClickListeners() {
        // 1. Single Marksheet View
        binding.btnViewSingleMarksheet.setOnClickListener {
            val report = selectedReport
            if (report == null) {
                Toast.makeText(requireContext(), "Please select a student first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showSingleMarksheetPreviewDialog(report)
        }

        // 2. Print Single
        binding.btnPrintSingle.setOnClickListener {
            val report = selectedReport
            if (report == null) {
                Toast.makeText(requireContext(), "Please select a student first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val file = File(requireContext().cacheDir, "single_marksheet_${report.student.srNumber}.pdf")
                val success = requireContext().contentResolver.openOutputStream(android.net.Uri.fromFile(file))?.use {
                    ConsolidatedPdfExporter.generateConsolidatedPdf(requireContext(), report, it)
                } == true
                if (success) {
                    PdfPrintHelper.printPdfFile(requireContext(), file, "Marksheet_${report.student.srNumber}_${report.student.studentName}")
                } else {
                    Toast.makeText(requireContext(), "Failed to generate PDF for printing.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. Share Single PDF
        binding.btnShareSinglePdf.setOnClickListener {
            val report = selectedReport
            if (report == null) {
                Toast.makeText(requireContext(), "Please select a student first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val file = File(requireContext().cacheDir, "single_marksheet_${report.student.srNumber}.pdf")
                val success = requireContext().contentResolver.openOutputStream(android.net.Uri.fromFile(file))?.use {
                    ConsolidatedPdfExporter.generateConsolidatedPdf(requireContext(), report, it)
                } == true
                if (success) {
                    PdfPrintHelper.sharePdfFile(requireContext(), file)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate PDF for sharing.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 4. Save Single PDF (SAF)
        binding.btnSaveSinglePdf.setOnClickListener {
            val report = selectedReport
            if (report == null) {
                Toast.makeText(requireContext(), "Please select a student first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val file = File(requireContext().cacheDir, "single_marksheet_${report.student.srNumber}.pdf")
                val success = requireContext().contentResolver.openOutputStream(android.net.Uri.fromFile(file))?.use {
                    ConsolidatedPdfExporter.generateConsolidatedPdf(requireContext(), report, it)
                } == true
                if (success) {
                    pendingPdfFileToSave = file
                    val safeClassName = report.student.className.replace(" ", "_")
                    val safeSr = report.student.srNumber.ifBlank { "000" }
                    val safeName = report.student.studentName.replace(" ", "_")
                    val fileName = "${safeClassName}_SR${safeSr}_${safeName}_Consolidated_Marksheet.pdf"
                    saveSinglePdfLauncher.launch(fileName)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate PDF for saving.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. Print All Marksheets
        binding.btnPrintFullReport.setOnClickListener {
            if (currentReports.isEmpty()) {
                Toast.makeText(requireContext(), "No student reports available for this class.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val className = binding.spinnerReportClass.selectedItem?.toString()?.replace(" ", "_") ?: "Class"
            viewLifecycleOwner.lifecycleScope.launch {
                val file = File(requireContext().cacheDir, "${className}_all_marksheets.pdf")
                val success = requireContext().contentResolver.openOutputStream(android.net.Uri.fromFile(file))?.use {
                    ConsolidatedPdfExporter.generateAllConsolidatedPdf(requireContext(), currentReports, className, it)
                } == true
                if (success) {
                    PdfPrintHelper.printPdfFile(requireContext(), file, "${className}_Consolidated_Marksheets")
                } else {
                    Toast.makeText(requireContext(), "Failed to generate all marksheets PDF for printing.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6. Share All PDF
        binding.btnShareAllPdf.setOnClickListener {
            if (currentReports.isEmpty()) {
                Toast.makeText(requireContext(), "No student reports available for this class.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val className = binding.spinnerReportClass.selectedItem?.toString()?.replace(" ", "_") ?: "Class"
            viewLifecycleOwner.lifecycleScope.launch {
                val file = File(requireContext().cacheDir, "${className}_all_marksheets.pdf")
                val success = requireContext().contentResolver.openOutputStream(android.net.Uri.fromFile(file))?.use {
                    ConsolidatedPdfExporter.generateAllConsolidatedPdf(requireContext(), currentReports, className, it)
                } == true
                if (success) {
                    PdfPrintHelper.sharePdfFile(requireContext(), file)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate all marksheets PDF for sharing.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 7. Save All PDF (SAF)
        binding.btnSaveAllPdf.setOnClickListener {
            if (currentReports.isEmpty()) {
                Toast.makeText(requireContext(), "No student reports available for this class.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val className = binding.spinnerReportClass.selectedItem?.toString()?.replace(" ", "_") ?: "Class"
            viewLifecycleOwner.lifecycleScope.launch {
                val file = File(requireContext().cacheDir, "${className}_all_marksheets.pdf")
                val success = requireContext().contentResolver.openOutputStream(android.net.Uri.fromFile(file))?.use {
                    ConsolidatedPdfExporter.generateAllConsolidatedPdf(requireContext(), currentReports, className, it)
                } == true
                if (success) {
                    pendingPdfFileToSave = file
                    val fileName = "${className}_All_Students_Consolidated_Marksheets.pdf"
                    saveAllPdfLauncher.launch(fileName)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate all marksheets PDF for saving.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSingleMarksheetPreviewDialog(report: ConsolidatedStudentReport) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.item_full_report_card, null)
        val tvHeader = dialogView.findViewById<TextView>(R.id.tvReportExamHeader)
        val tvStudentName = dialogView.findViewById<TextView>(R.id.tvReportStudentName)
        val tvStudentDetails = dialogView.findViewById<TextView>(R.id.tvReportStudentDetails)
        val layoutSubjectTable = dialogView.findViewById<LinearLayout>(R.id.layoutSubjectTable)
        val tvTotalMarks = dialogView.findViewById<TextView>(R.id.tvReportTotalMarks)
        val tvGrade = dialogView.findViewById<TextView>(R.id.tvReportGrade)
        val tvPassStatus = dialogView.findViewById<TextView>(R.id.tvReportPassStatus)

        val student = report.student
        tvHeader.text = "CONSOLIDATED ANNUAL EXAMINATION MARKSHET\nAcademic Session: ${report.academicSession}"
        tvStudentName.text = "Student Name: ${student.studentName}"
        tvStudentDetails.text = "SR Number: ${student.srNumber.ifBlank { "-" }} | Class: ${student.className} | Father: ${student.fatherName.ifBlank { "-" }} | Gender: ${student.gender}"

        while (layoutSubjectTable.childCount > 1) {
            layoutSubjectTable.removeViewAt(1)
        }

        val context = requireContext()
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
                gravity = android.view.Gravity.CENTER
                setTextColor(if (subj.unit1 == "Ab" || subj.unit1 == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }
            val tvHY = TextView(context).apply {
                text = subj.halfYearly
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setTextColor(if (subj.halfYearly == "Ab" || subj.halfYearly == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
            }
            val tvU2 = TextView(context).apply {
                text = subj.unit2
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setTextColor(if (subj.unit2 == "Ab" || subj.unit2 == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }
            val tvYr = TextView(context).apply {
                text = subj.yearly
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setTextColor(if (subj.yearly == "Ab" || subj.yearly == "-") Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }
            val tvSubTot = TextView(context).apply {
                text = "${subj.subjectTotalObtained.toInt()}/${subj.subjectTotalMax}"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            }

            rowLayout.addView(tvSub)
            rowLayout.addView(tvU1)
            rowLayout.addView(tvHY)
            rowLayout.addView(tvU2)
            rowLayout.addView(tvYr)
            rowLayout.addView(tvSubTot)
            layoutSubjectTable.addView(rowLayout)
        }

        // Grand Total row
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
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        val tvHYGrandTot = TextView(context).apply {
            text = "${report.halfYearlyTotalObtained.toInt()}/${report.halfYearlyTotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
        }
        val tvU2GrandTot = TextView(context).apply {
            text = "${report.unit2TotalObtained.toInt()}/${report.unit2TotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        val tvYrGrandTot = TextView(context).apply {
            text = "${report.yearlyTotalObtained.toInt()}/${report.yearlyTotalMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        val tvOverallGrandTot = TextView(context).apply {
            text = "${report.overallObtained.toInt()}/${report.overallMax}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
        }

        grandTotalRow.addView(tvGrandTotalLabel)
        grandTotalRow.addView(tvU1GrandTot)
        grandTotalRow.addView(tvHYGrandTot)
        grandTotalRow.addView(tvU2GrandTot)
        grandTotalRow.addView(tvYrGrandTot)
        grandTotalRow.addView(tvOverallGrandTot)
        layoutSubjectTable.addView(grandTotalRow)

        tvTotalMarks.text = "Overall Total: ${report.overallObtained.toInt()} / ${report.overallMax} (${String.format("%.2f", report.percentage)}%)"
        tvGrade.text = "Grade: ${report.grade} | Rank: #${report.rank}"
        tvPassStatus.text = report.passStatus
        tvPassStatus.setBackgroundColor(if (report.passStatus == "PASS") Color.parseColor("#16A34A") else Color.parseColor("#DC2626"))

        AlertDialog.Builder(requireContext())
            .setTitle("Single Marksheet Preview - ${student.studentName}")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

