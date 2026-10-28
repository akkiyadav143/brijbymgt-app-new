package com.brijby77.util

import android.content.Context
import android.net.Uri
import com.brijby77.data.entity.BankDetailsEntity
import com.brijby77.data.entity.HealthDetailsEntity
import com.brijby77.data.entity.StudentEntity
import com.brijby77.data.model.StudentWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object CsvExcelHelper {

    data class ImportPreviewData(
        val totalRows: Int,
        val validRecords: List<StudentWithDetails>,
        val duplicateRecords: List<StudentWithDetails>,
        val invalidCount: Int
    )

    suspend fun exportStudentsToCsv(
        context: Context,
        uri: Uri,
        students: List<StudentWithDetails>,
        selectedFields: List<String>? = null // null means all fields
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
                val writer = OutputStreamWriter(outputStream, "UTF-8")

                val headers = if (selectedFields != null && selectedFields.isNotEmpty()) {
                    selectedFields
                } else {
                    listOf(
                        "Sr No", "SR Number", "Unique ID", "PEN No", "AAPAR ID", "DBT", "Class",
                        "Student Name", "DOB", "Admission Date", "Student Adhar", "C/o", "Gender",
                        "Caste", "Father Name", "Father Adhar", "Mother Name", "Mother Adhar",
                        "Mobile No", "Address", "Other", "Ration Card",
                        "Height", "Weight", "Blood Group", "Hb", "Eye Sight", "Deworming",
                        "A/c Holder Name", "A/c No", "IFSC Code", "Branch Name"
                    )
                }

                // Write header line
                writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

                for (item in students) {
                    val s = item.student
                    val h = item.healthDetails
                    val b = item.bankDetails

                    val rowValues = if (selectedFields != null && selectedFields.isNotEmpty()) {
                        selectedFields.map { field ->
                            getFieldValueByName(field, s, h, b)
                        }
                    } else {
                        listOf(
                            s.srNo, s.srNumber, s.uniqueId, s.penNo, s.aaparId, s.dbt, s.studentClass,
                            s.studentName, s.dob, s.admissionDate, s.studentAdhar, s.co, s.gender,
                            s.caste, s.fatherName, s.fatherAdhar, s.motherName, s.motherAdhar,
                            s.mobileNo, s.address, s.other, s.rationCard,
                            h?.height ?: "-", h?.weight ?: "-", h?.bloodGroup ?: "-", h?.hb ?: "-", h?.eyeSight ?: "-", h?.deworming ?: "-",
                            b?.acHolderName ?: "-", b?.acNo ?: "-", b?.ifscCode ?: "-", b?.branchName ?: "-"
                        )
                    }

                    writer.write(rowValues.joinToString(",") { escapeCsv(it) } + "\n")
                }

                writer.flush()
                writer.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun parseImportFile(
        context: Context,
        uri: Uri,
        existingSrNumbers: Set<String>
    ): ImportPreviewData {
        return withContext(Dispatchers.IO) {
            val validRecords = mutableListOf<StudentWithDetails>()
            val duplicateRecords = mutableListOf<StudentWithDetails>()
            var invalidCount = 0
            var totalRows = 0

            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ImportPreviewData(0, emptyList(), emptyList(), 0)
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

                val headerLine = reader.readLine() ?: return@withContext ImportPreviewData(0, emptyList(), emptyList(), 0)
                val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.isBlank()) {
                        line = reader.readLine()
                        continue
                    }
                    totalRows++
                    val values = parseCsvLine(line)

                    val studentMap = mutableMapOf<String, String>()
                    for (i in headers.indices) {
                        if (i < values.size) {
                            studentMap[headers[i]] = values[i].trim()
                        }
                    }

                    val name = getMapValue(studentMap, listOf("student name", "name", "student_name"))
                    val studentClass = getMapValue(studentMap, listOf("class", "student class", "student_class"), "Class 1")
                    val gender = getMapValue(studentMap, listOf("gender", "sex"), "Male")
                    val dbt = getMapValue(studentMap, listOf("dbt"), "No")

                    if (name.isBlank()) {
                        invalidCount++
                    } else {
                        val srNumber = getMapValue(studentMap, listOf("sr number", "sr_number", "sr no", "sr_no"))
                        val uniqueId = getMapValue(studentMap, listOf("unique id", "unique_id"))
                        val penNo = getMapValue(studentMap, listOf("pen no", "pen_no", "pen"))
                        val aaparId = getMapValue(studentMap, listOf("aapar id", "aapar_id", "aapar"))
                        val dob = getMapValue(studentMap, listOf("dob", "date of birth"))
                        val admissionDate = getMapValue(studentMap, listOf("admission date", "admission_date"))
                        val studentAdhar = getMapValue(studentMap, listOf("student adhar", "student_adhar", "adhar"))
                        val co = getMapValue(studentMap, listOf("c/o", "co", "care of"))
                        val caste = getMapValue(studentMap, listOf("caste", "category"), "GEN")
                        val fatherName = getMapValue(studentMap, listOf("father name", "father_name"))
                        val fatherAdhar = getMapValue(studentMap, listOf("father adhar", "father_adhar"))
                        val motherName = getMapValue(studentMap, listOf("mother name", "mother_name"))
                        val motherAdhar = getMapValue(studentMap, listOf("mother adhar", "mother_adhar"))
                        val mobileNo = getMapValue(studentMap, listOf("mobile no", "mobile_no", "mobile"))
                        val address = getMapValue(studentMap, listOf("address"))
                        val other = getMapValue(studentMap, listOf("other"))
                        val rationCard = getMapValue(studentMap, listOf("ration card", "rashan card", "ration", "rashan"))

                        val student = StudentEntity(
                            srNo = getMapValue(studentMap, listOf("sr no", "sr_no")),
                            srNumber = srNumber,
                            uniqueId = uniqueId,
                            penNo = penNo,
                            aaparId = aaparId,
                            dbt = if (dbt.equals("Yes", true)) "Yes" else "No",
                            studentClass = formatClassString(studentClass),
                            studentName = name,
                            dob = dob,
                            admissionDate = admissionDate,
                            studentAdhar = studentAdhar,
                            co = co,
                            gender = if (gender.equals("Female", true)) "Female" else "Male",
                            caste = formatCasteString(caste),
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
                        val height = getMapValue(studentMap, listOf("height"))
                        val weight = getMapValue(studentMap, listOf("weight"))
                        val bloodGroup = getMapValue(studentMap, listOf("blood group", "blood_group"))
                        val hb = getMapValue(studentMap, listOf("hb"))
                        val eyeSight = getMapValue(studentMap, listOf("eye sight", "eye_sight"))
                        val deworming = getMapValue(studentMap, listOf("deworming"))

                        val hasHealth = height.isNotBlank() || weight.isNotBlank() || bloodGroup.isNotBlank()
                        val healthDetails = if (hasHealth) {
                            HealthDetailsEntity(
                                studentId = 0,
                                hasHealthDetails = true,
                                height = height,
                                weight = weight,
                                bloodGroup = bloodGroup,
                                hb = hb,
                                eyeSight = eyeSight,
                                deworming = deworming
                            )
                        } else null

                        // Bank
                        val acHolder = getMapValue(studentMap, listOf("a/c holder name", "ac holder name", "account holder"))
                        val acNo = getMapValue(studentMap, listOf("a/c no", "ac no", "account no", "account_no"))
                        val ifsc = getMapValue(studentMap, listOf("ifsc code", "ifsc"))
                        val branch = getMapValue(studentMap, listOf("branch name", "branch"))

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

                        val studentWithDetails = StudentWithDetails(student, healthDetails, emptyList(), bankDetails)

                        if (srNumber.isNotBlank() && existingSrNumbers.contains(srNumber.lowercase().trim())) {
                            duplicateRecords.add(studentWithDetails)
                        } else {
                            validRecords.add(studentWithDetails)
                        }
                    }

                    line = reader.readLine()
                }

                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            ImportPreviewData(
                totalRows = totalRows,
                validRecords = validRecords,
                duplicateRecords = duplicateRecords,
                invalidCount = invalidCount
            )
        }
    }

    private fun getFieldValueByName(
        field: String,
        s: StudentEntity,
        h: HealthDetailsEntity?,
        b: BankDetailsEntity?
    ): String {
        return when (field.lowercase().trim()) {
            "sr no" -> s.srNo
            "sr number" -> s.srNumber
            "unique id" -> s.uniqueId
            "pen no", "pen number" -> s.penNo
            "aapar id" -> s.aaparId
            "dbt" -> s.dbt
            "class" -> s.studentClass
            "student name" -> s.studentName
            "date of birth", "dob" -> s.dob
            "admission date" -> s.admissionDate
            "student adhar" -> s.studentAdhar
            "c/o" -> s.co
            "gender" -> s.gender
            "caste" -> s.caste
            "father name" -> s.fatherName
            "father adhar" -> s.fatherAdhar
            "mother name" -> s.motherName
            "mother adhar" -> s.motherAdhar
            "mobile no", "mobile number" -> s.mobileNo
            "address" -> s.address
            "other" -> s.other
            "ration card", "rashan card" -> s.rationCard
            "height" -> h?.height ?: "-"
            "weight" -> h?.weight ?: "-"
            "blood group" -> h?.bloodGroup ?: "-"
            "hb" -> h?.hb ?: "-"
            "eye sight" -> h?.eyeSight ?: "-"
            "deworming" -> h?.deworming ?: "-"
            "a/c holder name" -> b?.acHolderName ?: "-"
            "a/c no", "account number" -> b?.acNo ?: "-"
            "ifsc code" -> b?.ifscCode ?: "-"
            "branch name" -> b?.branchName ?: "-"
            else -> ""
        }
    }

    private fun getMapValue(map: Map<String, String>, keys: List<String>, defaultValue: String = ""): String {
        for (k in keys) {
            if (map.containsKey(k) && !map[k].isNullOrBlank()) {
                return map[k]!!
            }
        }
        return defaultValue
    }

    private fun formatClassString(input: String): String {
        val trimmed = input.trim()
        if (trimmed.equals("Class 0", true) || trimmed.equals("CL 0", true) || trimmed.equals("0", true)) return "Class 0"
        if (trimmed.equals("CL Pass", true) || trimmed.equals("Class Pass", true) || trimmed.equals("Pass", true)) return "CL Pass"
        for (i in 1..8) {
            if (trimmed.equals("Class $i", true) || trimmed.equals("CL $i", true) || trimmed == "$i") {
                return "Class $i"
            }
        }
        return input.ifBlank { "Class 1" }
    }

    private fun formatCasteString(input: String): String {
        val upper = input.uppercase().trim()
        return when (upper) {
            "GEN", "GENERAL" -> "GEN"
            "OBC" -> "OBC"
            "SC" -> "SC"
            "ST" -> "ST"
            "MINORITY", "MIN" -> "MINORITY"
            else -> "GEN"
        }
    }

    private fun escapeCsv(text: String): String {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (c in line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        result.add(sb.toString())
        return result
    }
}

