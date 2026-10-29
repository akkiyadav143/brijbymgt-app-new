package com.brijbymgt.ui.exportimport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.util.exportimport.BackupRestoreHelper
import com.brijbymgt.util.exportimport.CsvExporter
import com.brijbymgt.util.exportimport.CsvImporter
import com.brijbymgt.util.exportimport.CustomExportManager
import com.brijbymgt.util.exportimport.ExcelExporter
import com.brijbymgt.util.exportimport.ExcelImporter
import com.brijbymgt.util.exportimport.FieldExtractor
import com.brijbymgt.util.exportimport.ImportManager
import com.brijbymgt.util.exportimport.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportImportViewModel(private val repository: StudentRepository) : ViewModel() {

    val allStudents: StateFlow<List<StudentWithDetails>> = repository.allActiveStudents.let { flow ->
        val state = MutableStateFlow<List<StudentWithDetails>>(emptyList())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state
    }

    private val _customCriteria = MutableStateFlow(CustomExportManager.FilterCriteria())
    val customCriteria: StateFlow<CustomExportManager.FilterCriteria> = _customCriteria

    private val _selectedFields = MutableStateFlow(FieldExtractor.ALL_SUPPORTED_FIELDS)
    val selectedFields: StateFlow<List<String>> = _selectedFields

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage: SharedFlow<String> = _statusMessage

    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null) // (current, total)
    val progress: StateFlow<Pair<Int, Int>?> = _progress

    fun updateCustomCriteria(criteria: CustomExportManager.FilterCriteria) {
        _customCriteria.value = criteria
    }

    fun updateSelectedFields(fields: List<String>) {
        _selectedFields.value = fields
    }

    fun getFilteredStudentsForExport(): List<StudentWithDetails> {
        return CustomExportManager.filterStudents(allStudents.value, _customCriteria.value)
    }

    suspend fun exportQuickData(
        context: Context,
        uri: Uri,
        selectedClass: String,
        format: String
    ): Boolean {
        val allActive = allStudents.value
        val studentsToExport = when {
            selectedClass.equals("All Classes", true) || selectedClass.equals("All", true) -> allActive
            selectedClass.equals("Class 0", true) -> allActive.filter { it.student.studentClass.equals("Class 0", true) || it.student.studentClass == "0" }
            selectedClass.equals("CL Pass", true) -> allActive.filter { it.student.studentClass.equals("CL Pass", true) || it.student.studentClass.equals("Pass", true) }
            else -> allActive.filter {
                val c = it.student.studentClass
                val rawC = c.replace("Class ", "").replace("CL ", "").trim()
                val rawTarget = selectedClass.replace("Class ", "").replace("CL ", "").trim()
                c.equals(selectedClass, true) || rawC.equals(rawTarget, true)
            }
        }

        if (studentsToExport.isEmpty()) {
            _statusMessage.emit("No students found for class: $selectedClass")
            return false
        }

        val allFields = FieldExtractor.ALL_SUPPORTED_FIELDS
        _progress.value = 0 to studentsToExport.size

        val success = when (format.uppercase().trim()) {
            "CSV" -> CsvExporter.exportToCsv(context, uri, studentsToExport, allFields)
            "EXCEL", "XLSX" -> ExcelExporter.exportToExcel(context, uri, studentsToExport, allFields)
            "PDF" -> PdfExporter.exportToPdf(
                context, uri, studentsToExport, allFields,
                "Class: $selectedClass (All Fields)"
            )
            else -> false
        }

        _progress.value = null
        if (success) {
            _statusMessage.emit("Quick Export completed successfully!")
        } else {
            _statusMessage.emit("Failed to export file")
        }
        return success
    }

    suspend fun exportCustomData(
        context: Context,
        uri: Uri,
        format: String
    ): Boolean {
        val studentsToExport = getFilteredStudentsForExport()
        val fieldsToExport = _selectedFields.value

        if (studentsToExport.isEmpty() || fieldsToExport.isEmpty()) {
            _statusMessage.emit("No data or fields selected to export")
            return false
        }

        _progress.value = 0 to studentsToExport.size

        val success = when (format.uppercase().trim()) {
            "CSV" -> CsvExporter.exportToCsv(context, uri, studentsToExport, fieldsToExport)
            "EXCEL", "XLSX" -> ExcelExporter.exportToExcel(context, uri, studentsToExport, fieldsToExport)
            "PDF" -> PdfExporter.exportToPdf(
                context, uri, studentsToExport, fieldsToExport,
                "Class: ${_customCriteria.value.selectedClass}, Gender: ${_customCriteria.value.selectedGender}, Caste: ${_customCriteria.value.selectedCaste}"
            )
            else -> false
        }

        _progress.value = null
        if (success) {
            _statusMessage.emit("Custom Export completed successfully!")
        } else {
            _statusMessage.emit("Failed to export file")
        }
        return success
    }

    suspend fun exportBackup(context: Context, uri: Uri): Boolean {
        return exportBackupScoped(context, uri, BackupRestoreHelper.BackupScope.ALL_STUDENTS, null, null)
    }

    suspend fun exportBackupScoped(
        context: Context,
        uri: Uri,
        scope: BackupRestoreHelper.BackupScope,
        selectedClass: String?,
        customSelectedIds: List<Long>?
    ): Boolean {
        val allActive = allStudents.value
        val success = BackupRestoreHelper.exportBackupZip(context, uri, allActive, scope, selectedClass, customSelectedIds) { current, total, msg ->
            _progress.value = current to total
        }
        _progress.value = null
        if (success) {
            _statusMessage.emit("Student Backup ZIP created successfully!")
        } else {
            _statusMessage.emit("Failed to create student backup ZIP")
        }
        return success
    }

    suspend fun previewRestore(context: Context, uri: Uri): BackupRestoreHelper.RestorePreview {
        return BackupRestoreHelper.previewRestoreZip(context, uri, repository)
    }

    suspend fun executeRestore(
        context: Context,
        uri: Uri,
        conflictResolution: BackupRestoreHelper.ConflictResolution
    ): Boolean {
        val success = BackupRestoreHelper.importBackupZip(context, uri, repository, conflictResolution) { current, total, msg ->
            _progress.value = current to total
        }
        _progress.value = null
        if (success) {
            _statusMessage.emit("Student Backup Recovery completed successfully!")
        } else {
            _statusMessage.emit("Failed to restore backup")
        }
        return success
    }

    suspend fun importBackup(context: Context, uri: Uri): Boolean {
        return executeRestore(context, uri, BackupRestoreHelper.ConflictResolution.UPDATE_EXISTING)
    }

    suspend fun importSimpleData(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val fileName = uri.lastPathSegment?.lowercase() ?: ""

                val rows = if (fileName.endsWith(".xlsx") || mimeType.contains("sheet")) {
                    ExcelImporter.parseXlsxFile(context, uri)
                } else {
                    CsvImporter.parseCsvFile(context, uri)
                }

                if (rows.isEmpty() || rows.size <= 1) {
                    _statusMessage.emit("Import file is empty or missing data rows")
                    return@withContext false
                }

                val headers = rows[0]
                val mappings = ImportManager.autoMapHeaders(headers)

                val existingStudents = allStudents.value
                val existingSrMap = existingStudents.associate { it.student.srNumber.lowercase().trim() to it.student.id }
                val existingUidMap = existingStudents.associate { it.student.uniqueId.lowercase().trim() to it.student.id }

                val importItems = ImportManager.processImportRows(rows, mappings, existingSrMap, existingUidMap)

                var importedCount = 0
                var updatedCount = 0
                var errorCount = 0

                for (item in importItems) {
                    when (item.status) {
                        ImportManager.RowStatus.VALID -> {
                            val student = item.studentWithDetails.student.copy(id = 0)
                            repository.saveStudent(
                                student,
                                item.studentWithDetails.healthDetails,
                                item.studentWithDetails.heightWeightHistory,
                                item.studentWithDetails.bankDetails
                            )
                            importedCount++
                        }
                        ImportManager.RowStatus.DUPLICATE -> {
                            val student = item.studentWithDetails.student.copy(id = item.existingStudentId)
                            repository.saveStudent(
                                student,
                                item.studentWithDetails.healthDetails,
                                item.studentWithDetails.heightWeightHistory,
                                item.studentWithDetails.bankDetails
                            )
                            updatedCount++
                        }
                        ImportManager.RowStatus.ERROR -> {
                            errorCount++
                        }
                    }
                }

                _statusMessage.emit("Import completed: $importedCount imported, $updatedCount updated, $errorCount errors")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.emit("Import failed: ${e.localizedMessage}")
                false
            }
        }
    }
}

class ExportImportViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportImportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

