package com.brijbymgt.util.exportimport

import com.brijbymgt.data.entity.BankDetailsEntity
import com.brijbymgt.data.entity.HealthDetailsEntity
import com.brijbymgt.data.entity.StudentEntity
import com.brijbymgt.data.model.StudentWithDetails

object ImportManager {

    data class ImportRowItem(
        val rowIndex: Int,
        val studentWithDetails: StudentWithDetails,
        val status: RowStatus,
        val errorMessage: String = "",
        val existingStudentId: Long = 0L
    )

    enum class RowStatus {
        VALID,
        DUPLICATE,
        ERROR
    }

    enum class DuplicateAction {
        SKIP,
        UPDATE,
        IMPORT_AS_NEW
    }

    data class ColumnMapping(
        val fileColumnIndex: Int,
        val fileColumnHeader: String,
        var mappedField: String // App field name
    )

    data class ImportSummary(
        val totalRows: Int,
        val importedCount: Int,
        val updatedCount: Int,
        val skippedCount: Int,
        val errorCount: Int,
        val errorDetails: List<String>
    )

    fun autoMapHeaders(headers: List<String>): List<ColumnMapping> {
        val supportedFields = FieldExtractor.ALL_SUPPORTED_FIELDS
        return headers.mapIndexed { idx, rawHeader ->
            val cleanHeader = rawHeader.trim()
            val lowerHeader = cleanHeader.lowercase().replace("_", " ").replace("'", "").replace(".", " ")

            val matchedField = supportedFields.firstOrNull { field ->
                field.equals(cleanHeader, ignoreCase = true)
            } ?: when {
                lowerHeader.contains("mother name") || lowerHeader.contains("mother") || lowerHeader == "m n" || lowerHeader == "m name" || lowerHeader.contains("mata") -> "Mother Name"
                lowerHeader.contains("mother adhar") || lowerHeader.contains("mother aadhaar") || lowerHeader == "m a" || lowerHeader == "m adhar" -> "Mother Aadhaar"
                lowerHeader.contains("father name") || lowerHeader.contains("father") || lowerHeader == "fa n" || lowerHeader == "f n" || lowerHeader == "f name" || lowerHeader.contains("pita") -> "Father Name"
                lowerHeader.contains("father adhar") || lowerHeader.contains("father aadhaar") || lowerHeader == "fa a" || lowerHeader == "f a" || lowerHeader == "f adhar" -> "Father Aadhaar"
                lowerHeader.contains("student name") || lowerHeader.contains("student") || lowerHeader == "name" -> "Student Name"
                lowerHeader.contains("student adhar") || lowerHeader.contains("student aadhaar") || lowerHeader == "adr" -> "Student Aadhaar"
                lowerHeader.contains("sr number") || lowerHeader.contains("sr_number") || lowerHeader == "sr num" -> "SR Number"
                lowerHeader.contains("sr no") || lowerHeader == "sr" || lowerHeader == "sno" -> "Sr No"
                lowerHeader.contains("unique id") || lowerHeader.contains("uid") -> "Unique ID"
                lowerHeader.contains("pen") -> "PEN No"
                lowerHeader.contains("aapar") || lowerHeader.contains("apr") -> "AAPAR ID"
                lowerHeader.contains("class") || lowerHeader.contains("std") -> "Class"
                lowerHeader.contains("dob") || lowerHeader.contains("birth") -> "Date of Birth"
                lowerHeader.contains("adm") || lowerHeader.contains("admission") -> "Admission Date"
                lowerHeader.contains("c/o") || lowerHeader.contains("care") -> "C/o"
                lowerHeader.contains("gender") || lowerHeader.contains("sex") -> "Gender"
                lowerHeader.contains("caste") || lowerHeader.contains("category") -> "Caste"
                lowerHeader.contains("mobile") || lowerHeader.contains("mob") || lowerHeader.contains("phone") -> "Mobile Number"
                lowerHeader.contains("address") || lowerHeader.contains("adrs") -> "Address"
                lowerHeader.contains("ration card") || lowerHeader.contains("rashan card") || lowerHeader == "ration" || lowerHeader == "rashan" -> "Ration Card"
                else -> supportedFields.firstOrNull { field ->
                    field.lowercase().contains(lowerHeader) || lowerHeader.contains(field.lowercase())
                } ?: ""
            }
            ColumnMapping(idx, cleanHeader, matchedField)
        }
    }

    fun processImportRows(
        rawRows: List<List<String>>,
        mappings: List<ColumnMapping>,
        existingSrNumbers: Map<String, Long>,
        existingUniqueIds: Map<String, Long>
    ): List<ImportRowItem> {
        if (rawRows.size <= 1) return emptyList()

        val results = mutableListOf<ImportRowItem>()
        val dataRows = rawRows.subList(1, rawRows.size)

        for (idx in dataRows.indices) {
            val rowValues = dataRows[idx]
            val rowNum = idx + 2 // 1-based row number in spreadsheet

            val fieldMap = mutableMapOf<String, String>()
            for (m in mappings) {
                if (m.mappedField.isNotBlank() && m.fileColumnIndex < rowValues.size) {
                    fieldMap[m.mappedField.lowercase().trim()] = rowValues[m.fileColumnIndex].trim()
                }
            }

            val studentName = getMapVal(fieldMap, listOf("student name", "name", "student_name", "student"))
            if (studentName.isBlank()) {
                val dummyStudent = StudentEntity(studentName = "Row $rowNum")
                results.add(ImportRowItem(rowNum, StudentWithDetails(dummyStudent, null, emptyList(), null), RowStatus.ERROR, "Student Name is required"))
                continue
            }

            val srNumber = getMapVal(fieldMap, listOf("sr number", "sr_number", "sr num"))
            val uniqueId = getMapVal(fieldMap, listOf("unique id", "unique_id", "uid"))
            val srNo = getMapVal(fieldMap, listOf("sr no", "sr_no", "sno"))
            val penNo = getMapVal(fieldMap, listOf("pen no", "pen number", "pen_no", "pen"))
            val aaparId = getMapVal(fieldMap, listOf("aapar id", "aapar_id", "apr id", "apr"))
            val studentClass = getMapVal(fieldMap, listOf("class", "student class", "std"), "Class 1")
            val dob = getMapVal(fieldMap, listOf("date of birth", "dob", "birth date"))
            val admissionDate = getMapVal(fieldMap, listOf("admission date", "adm date"))
            val studentAdhar = getMapVal(fieldMap, listOf("student aadhaar", "student adhar", "student_adhar", "student_aadhaar", "adhar", "aadhaar", "adr"))
            val co = getMapVal(fieldMap, listOf("c/o", "care of", "co"))
            val gender = getMapVal(fieldMap, listOf("gender", "sex"), "Male")
            val caste = getMapVal(fieldMap, listOf("caste", "category"), "GEN")
            val fatherName = getMapVal(fieldMap, listOf("father name", "father's name", "father_name", "fathername", "fa_n", "f_n", "f_name", "father", "pita ka naam", "pita"))
            val fatherAdhar = getMapVal(fieldMap, listOf("father aadhaar", "father adhar", "father's aadhaar", "father's adhar", "father_adhar", "father_aadhaar", "fa_a", "f_a", "f_adhar", "father aadhar"))
            val motherName = getMapVal(fieldMap, listOf("mother name", "mother's name", "mother_name", "mothername", "m_n", "m_name", "mother", "mata ka naam", "mata"))
            val motherAdhar = getMapVal(fieldMap, listOf("mother aadhaar", "mother adhar", "mother's aadhaar", "mother's adhar", "mother_adhar", "mother_aadhaar", "m_a", "m_adhar", "m_aadhaar", "mother aadhar"))
            val mobileNo = getMapVal(fieldMap, listOf("mobile number", "mobile no", "mobile_no", "mobile", "phone", "mob"))
            val address = getMapVal(fieldMap, listOf("address", "adrs"))
            val other = getMapVal(fieldMap, listOf("other"))
            val rationCard = getMapVal(fieldMap, listOf("ration card", "rashan card", "ration", "rashan"))

            val student = StudentEntity(
                srNo = srNo,
                srNumber = srNumber,
                uniqueId = uniqueId,
                penNo = penNo,
                aaparId = aaparId,
                dbt = if (getMapVal(fieldMap, listOf("dbt")).equals("Yes", true)) "Yes" else "No",
                studentClass = formatClass(studentClass),
                studentName = studentName,
                dob = dob,
                admissionDate = admissionDate,
                studentAdhar = studentAdhar,
                co = co,
                gender = if (gender.equals("Female", true)) "Female" else "Male",
                caste = formatCaste(caste),
                fatherName = fatherName,
                fatherAdhar = fatherAdhar,
                motherName = motherName,
                motherAdhar = motherAdhar,
                mobileNo = mobileNo,
                address = address,
                other = other,
                rationCard = rationCard
            )

            // Health
            val height = getMapVal(fieldMap, listOf("height (cm)", "height"))
            val weight = getMapVal(fieldMap, listOf("weight (kg)", "weight"))
            val bg = getMapVal(fieldMap, listOf("blood group"))
            val hb = getMapVal(fieldMap, listOf("hb"))
            val eye = getMapVal(fieldMap, listOf("eye sight"))
            val deworm = getMapVal(fieldMap, listOf("deworming"))

            val hasHealth = height.isNotBlank() || weight.isNotBlank() || bg.isNotBlank()
            val healthDetails = if (hasHealth) {
                HealthDetailsEntity(
                    studentId = 0,
                    hasHealthDetails = true,
                    height = height,
                    weight = weight,
                    bloodGroup = bg,
                    hb = hb,
                    eyeSight = eye,
                    deworming = deworm
                )
            } else null

            // Bank
            val acHolder = getMapVal(fieldMap, listOf("a/c holder name", "account holder name"))
            val acNo = getMapVal(fieldMap, listOf("account number", "a/c no", "ac no"))
            val ifsc = getMapVal(fieldMap, listOf("ifsc code"))
            val branch = getMapVal(fieldMap, listOf("branch name"))

            val hasBank = acNo.isNotBlank() || acHolder.isNotBlank()
            val bankDetails = if (hasBank) {
                BankDetailsEntity(
                    studentId = 0,
                    hasBankDetails = true,
                    acHolderName = acHolder,
                    acNo = acNo,
                    ifscCode = ifsc,
                    branchName = branch
                )
            } else null

            val item = StudentWithDetails(student, healthDetails, emptyList(), bankDetails)

            // Duplicate Detection
            var existingId = 0L
            var isDuplicate = false

            if (srNumber.isNotBlank() && existingSrNumbers.containsKey(srNumber.lowercase().trim())) {
                isDuplicate = true
                existingId = existingSrNumbers[srNumber.lowercase().trim()] ?: 0L
            } else if (uniqueId.isNotBlank() && existingUniqueIds.containsKey(uniqueId.lowercase().trim())) {
                isDuplicate = true
                existingId = existingUniqueIds[uniqueId.lowercase().trim()] ?: 0L
            }

            if (isDuplicate) {
                results.add(ImportRowItem(rowNum, item, RowStatus.DUPLICATE, "Duplicate SR Number/Unique ID found", existingId))
            } else {
                results.add(ImportRowItem(rowNum, item, RowStatus.VALID))
            }
        }

        return results
    }

    private fun getMapVal(map: Map<String, String>, keys: List<String>, defaultValue: String = ""): String {
        for (k in keys) {
            val v = map[k.lowercase().trim()]
            if (!v.isNullOrBlank()) return v
        }
        return defaultValue
    }

    private fun formatClass(input: String): String {
        val trimmed = input.trim()
        if (trimmed.equals("Class 0", true) || trimmed == "0") return "Class 0"
        if (trimmed.equals("CL Pass", true) || trimmed.equals("Pass", true)) return "CL Pass"
        for (i in 1..8) {
            if (trimmed.equals("Class $i", true) || trimmed.equals("CL $i", true) || trimmed == "$i") {
                return "Class $i"
            }
        }
        return input.ifBlank { "Class 1" }
    }

    private fun formatCaste(input: String): String {
        return when (input.uppercase().trim()) {
            "GEN", "GENERAL" -> "GEN"
            "OBC" -> "OBC"
            "SC" -> "SC"
            "ST" -> "ST"
            "MINORITY", "MIN" -> "MINORITY"
            else -> "GEN"
        }
    }
}

