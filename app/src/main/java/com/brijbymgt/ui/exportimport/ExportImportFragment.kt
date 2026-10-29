package com.brijbymgt.ui.exportimport

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.databinding.FragmentExportImportBinding
import com.brijbymgt.util.exportimport.BackupRestoreHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportImportFragment : Fragment() {

    private var _binding: FragmentExportImportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExportImportViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao(),
            db.documentDao()
        )
        ExportImportViewModelFactory(repo)
    }

    private var pendingExportType: String? = null

    private var pendingBackupScope = BackupRestoreHelper.BackupScope.ALL_STUDENTS
    private var pendingBackupClass: String? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        uri?.let { destUri ->
            lifecycleScope.launch {
                val format = when (pendingExportType) {
                    "QUICK_CSV" -> "CSV"
                    "QUICK_PDF" -> "PDF"
                    else -> "Excel"
                }
                val selClass = binding.spinnerExportClass.selectedItem?.toString() ?: "All Classes"
                val success = viewModel.exportQuickData(requireContext(), destUri, selClass, format)
                if (success) {
                    Toast.makeText(requireContext(), "Exported Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Export Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { sourceUri ->
            lifecycleScope.launch {
                val success = viewModel.importSimpleData(requireContext(), sourceUri)
                if (success) {
                    Toast.makeText(requireContext(), "Import processed successfully!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val createBackupZipLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        uri?.let { destUri ->
            lifecycleScope.launch {
                val success = viewModel.exportBackupScoped(requireContext(), destUri, pendingBackupScope, pendingBackupClass, null)
                if (success) {
                    Toast.makeText(requireContext(), "Student Backup ZIP created successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Backup Export Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val openRestoreZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { sourceUri ->
            lifecycleScope.launch {
                val preview = viewModel.previewRestore(requireContext(), sourceUri)
                if (!preview.isValid) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("❌ Invalid Backup")
                        .setMessage(preview.errorMessage ?: "The selected file is not a valid student backup ZIP.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                showRestorePreviewDialog(sourceUri, preview)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupButtons()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusMessage.collect { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinners() {
        val classes = listOf("All Classes", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 0", "CL Pass")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classes)
        binding.spinnerExportClass.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnExportCsv.setOnClickListener {
            pendingExportType = "QUICK_CSV"
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val selClass = binding.spinnerExportClass.selectedItem?.toString()?.replace(" ", "_") ?: "All"
            createDocumentLauncher.launch("Student_Export_${selClass}_$timeStr.csv")
        }

        binding.btnExportExcel.setOnClickListener {
            pendingExportType = "QUICK_EXCEL"
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val selClass = binding.spinnerExportClass.selectedItem?.toString()?.replace(" ", "_") ?: "All"
            createDocumentLauncher.launch("Student_Export_${selClass}_$timeStr.xlsx")
        }

        binding.btnExportPdf.setOnClickListener {
            pendingExportType = "QUICK_PDF"
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val selClass = binding.spinnerExportClass.selectedItem?.toString()?.replace(" ", "_") ?: "All"
            createDocumentLauncher.launch("Student_Export_${selClass}_$timeStr.pdf")
        }

        binding.btnSelectImportFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf(
                "text/comma-separated-values",
                "text/csv",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "*/*"
            ))
        }

        binding.btnBackupZip.setOnClickListener {
            showBackupScopeDialog()
        }

        binding.btnRestoreZip.setOnClickListener {
            openRestoreZipLauncher.launch(arrayOf(
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream",
                "*/*"
            ))
        }

        binding.cardTrashRecovery.setOnClickListener {
            findNavController().navigate(R.id.nav_trash)
        }
    }

    private fun showBackupScopeDialog() {
        val scopes = arrayOf("All Students", "Selected Class", "Custom Selected Students")
        AlertDialog.Builder(requireContext())
            .setTitle("📦 Select Backup Scope")
            .setItems(scopes) { _, which ->
                when (which) {
                    0 -> {
                        pendingBackupScope = BackupRestoreHelper.BackupScope.ALL_STUDENTS
                        pendingBackupClass = null
                        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        createBackupZipLauncher.launch("StudentBackup_$timeStr.zip")
                    }
                    1 -> {
                        showClassSelectionForBackup()
                    }
                    2 -> {
                        Toast.makeText(requireContext(), "Use Selected Export tab for custom student selection", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showClassSelectionForBackup() {
        val classes = arrayOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 0", "CL Pass")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Class for Backup")
            .setItems(classes) { _, which ->
                pendingBackupScope = BackupRestoreHelper.BackupScope.SELECTED_CLASS
                pendingBackupClass = classes[which]
                val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val classNameSlug = classes[which].replace(" ", "_")
                createBackupZipLauncher.launch("StudentBackup_${classNameSlug}_$timeStr.zip")
            }
            .show()
    }

    private fun showRestorePreviewDialog(sourceUri: Uri, preview: BackupRestoreHelper.RestorePreview) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(preview.createdAt))
        val message = buildString {
            append("• Backup Version: ${preview.manifestVersion}\n")
            append("• Created At: $timeStr\n")
            append("• Students in Backup: ${preview.studentCount}\n")
            append("• Photos: ${preview.photoCount}\n")
            append("• Attachments (11 slots): ${preview.attachmentCount}\n")
            append("• Detected Conflicts: ${preview.conflictCount}\n\n")
            if (preview.conflictCount > 0) {
                append("Conflicting Students:\n")
                for (c in preview.conflicts.take(5)) {
                    append(" - ${c.studentName} matches existing '${c.existingStudentName}' via ${c.matchedField}\n")
                }
                if (preview.conflicts.size > 5) {
                    append(" ... and ${preview.conflicts.size - 5} more.\n")
                }
                append("\nChoose Conflict Resolution Strategy:")
            } else {
                append("No conflicts detected. All students will be restored.")
            }
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("📥 Restore Preview & Conflicts")
            .setMessage(message)

        var selectedResolution = BackupRestoreHelper.ConflictResolution.UPDATE_EXISTING

        if (preview.conflictCount > 0) {
            val strategies = arrayOf("Update Existing (Recommended)", "Restore as New", "Skip Conflicting")
            val strategyValues = arrayOf(
                BackupRestoreHelper.ConflictResolution.UPDATE_EXISTING,
                BackupRestoreHelper.ConflictResolution.RESTORE_AS_NEW,
                BackupRestoreHelper.ConflictResolution.SKIP
            )
            builder.setSingleChoiceItems(strategies, 0) { _, which ->
                selectedResolution = strategyValues[which]
            }
        }

        builder.setPositiveButton("Proceed with Restore") { _, _ ->
            lifecycleScope.launch {
                val success = viewModel.executeRestore(requireContext(), sourceUri, selectedResolution)
                if (success) {
                    Toast.makeText(requireContext(), "Student Restore Completed Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Restore Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

