package com.brijby77.ui.exportimport

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijby77.SchoolApp
import com.brijby77.data.repository.StudentRepository
import com.brijby77.databinding.DialogExportShareBinding
import com.brijby77.databinding.FragmentSelectedExportBinding
import com.brijby77.util.exportimport.CsvExporter
import com.brijby77.util.exportimport.ExcelExporter
import com.brijby77.util.exportimport.FieldExtractor
import com.brijby77.util.exportimport.PdfExporter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SelectedExportFragment : Fragment() {

    private var _binding: FragmentSelectedExportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExportImportViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao()
        )
        ExportImportViewModelFactory(repo)
    }

    private lateinit var studentAdapter: StudentExportAdapter
    private val fieldCheckBoxMap = mutableMapOf<String, CheckBox>()
    private var lastExportedMime: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        uri?.let { destUri ->
            lifecycleScope.launch {
                executeExportToUri(destUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectedExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStudentSelection()
        setupFieldsGrid()
        setupButtons()
        observeViewModel()
    }

    private fun setupStudentSelection() {
        val classes = arrayOf("All", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 0", "CL Pass")
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classes)
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerExpClassFilter.adapter = classAdapter

        studentAdapter = StudentExportAdapter { count ->
            binding.tvExpSelectedCount.text = "Selected: $count Students"
        }

        binding.rvExpStudentSelection.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpStudentSelection.adapter = studentAdapter

        binding.spinnerExpClassFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val cls = classes[position]
                val query = binding.etExpSearchStudent.text.toString()
                studentAdapter.filter(query, cls)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.etExpSearchStudent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                val cls = binding.spinnerExpClassFilter.selectedItem?.toString() ?: "All"
                studentAdapter.filter(query, cls)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnExpSelectAll.setOnClickListener {
            studentAdapter.selectAll(true)
        }

        binding.btnExpUnselectAll.setOnClickListener {
            studentAdapter.selectAll(false)
        }
    }

    private fun setupFieldsGrid() {
        binding.gridFieldsContainer.removeAllViews()
        fieldCheckBoxMap.clear()

        for (field in FieldExtractor.ALL_SUPPORTED_FIELDS) {
            val cb = CheckBox(requireContext()).apply {
                text = field
                isChecked = true
                textSize = 11f
                setOnCheckedChangeListener { _, _ -> updateFieldsCount() }
            }
            fieldCheckBoxMap[field] = cb
            binding.gridFieldsContainer.addView(cb)
        }
        updateFieldsCount()
    }

    private fun updateFieldsCount() {
        val selectedCount = fieldCheckBoxMap.values.count { it.isChecked }
        val totalCount = fieldCheckBoxMap.size
        binding.tvSelectedFieldsCount.text = "Selected Fields: $selectedCount / $totalCount"
    }

    private fun setupButtons() {
        binding.btnSelectAllFields.setOnClickListener {
            fieldCheckBoxMap.values.forEach { it.isChecked = true }
            updateFieldsCount()
        }

        binding.btnClearAllFields.setOnClickListener {
            fieldCheckBoxMap.values.forEach { it.isChecked = false }
            updateFieldsCount()
        }

        binding.btnCustomExportSubmit.setOnClickListener {
            validateAndShowPreview()
        }
    }

    private fun validateAndShowPreview() {
        val selectedStudentIds = studentAdapter.selectedStudentIds
        if (selectedStudentIds.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one student.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedFields = fieldCheckBoxMap.filter { it.value.isChecked }.keys.toList()
        if (selectedFields.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one field.", Toast.LENGTH_SHORT).show()
            return
        }

        val format = when (binding.rgCustomFormat.checkedRadioButtonId) {
            binding.rbCustomFormatCsv.id -> "CSV"
            binding.rbCustomFormatPdf.id -> "PDF"
            else -> "Excel"
        }

        val summaryMsg = "• Selected Students: ${selectedStudentIds.size}\n" +
                "• Selected Fields: ${selectedFields.size} / ${FieldExtractor.ALL_SUPPORTED_FIELDS.size}\n" +
                "• Export Format: $format"

        AlertDialog.Builder(requireContext())
            .setTitle("📊 Export Summary Preview")
            .setMessage(summaryMsg)
            .setPositiveButton("Export") { _, _ ->
                val timeStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                val ext = when (format) {
                    "CSV" -> "csv"
                    "PDF" -> "pdf"
                    else -> "xlsx"
                }
                lastExportedMime = when (format) {
                    "CSV" -> "text/csv"
                    "PDF" -> "application/pdf"
                    else -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }

                createDocumentLauncher.launch("Student_Export_$timeStr.$ext")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun executeExportToUri(destUri: Uri) {
        val allActive = viewModel.allStudents.value
        val selectedIds = studentAdapter.selectedStudentIds
        val studentsToExport = allActive.filter { selectedIds.contains(it.student.id) }
        val fieldsToExport = fieldCheckBoxMap.filter { it.value.isChecked }.keys.toList()

        val format = when (binding.rgCustomFormat.checkedRadioButtonId) {
            binding.rbCustomFormatCsv.id -> "CSV"
            binding.rbCustomFormatPdf.id -> "PDF"
            else -> "Excel"
        }

        val success = when (format.uppercase().trim()) {
            "CSV" -> CsvExporter.exportToCsv(requireContext(), destUri, studentsToExport, fieldsToExport)
            "EXCEL", "XLSX" -> ExcelExporter.exportToExcel(requireContext(), destUri, studentsToExport, fieldsToExport)
            "PDF" -> PdfExporter.exportToPdf(
                requireContext(), destUri, studentsToExport, fieldsToExport,
                "Selected Export (${studentsToExport.size} students)"
            )
            else -> false
        }

        if (success) {
            showExportSuccessDialog(destUri, lastExportedMime)
        } else {
            Toast.makeText(requireContext(), "Failed to export file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExportSuccessDialog(uri: Uri, mimeType: String) {
        val dialogBinding = DialogExportShareBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvExportSuccessMsg.text = "File exported successfully!\nUri: ${uri.lastPathSegment}"

        dialogBinding.btnExportOpenFile.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No viewer app found to open file", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnExportShareFile.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Exported Student File"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to share file", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnExportDone.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allStudents.collectLatest { list ->
                studentAdapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

