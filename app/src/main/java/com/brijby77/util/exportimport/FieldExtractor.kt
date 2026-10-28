package com.brijby77.util.exportimport

import com.brijby77.data.model.StudentWithDetails

object FieldExtractor {

    val ALL_SUPPORTED_FIELDS = listOf(
        "Sr No",
        "SR Number",
        "Unique ID",
        "PEN No",
        "AAPAR ID",
        "DBT",
        "Class",
        "Student Name",
        "Date of Birth",
        "Admission Date",
        "Student Aadhaar",
        "C/o",
        "Gender",
        "Caste",
        "Father Name",
        "Father Aadhaar",
        "Mother Name",
        "Mother Aadhaar",
        "Mobile No",
        "Address",
        "Other",
        "Ration Card",
        "Height",
        "Weight",
        "Blood Group",
        "Hb",
        "Eye Sight",
        "Deworming",
        "A/c Holder Name",
        "A/c No",
        "IFSC Code",
        "Branch Name"
    )

    fun getFieldValue(fieldName: String, item: StudentWithDetails): String {
        val s = item.student
        val h = item.healthDetails
        val b = item.bankDetails

        return when (fieldName.lowercase().trim()) {
            "sr no" -> s.srNo
            "sr number" -> s.srNumber
            "unique id" -> s.uniqueId
            "pen no", "pen number" -> s.penNo
            "aapar id" -> s.aaparId
            "dbt" -> s.dbt
            "class" -> s.studentClass
            "student name", "name" -> s.studentName
            "date of birth", "dob" -> s.dob
            "admission date" -> s.admissionDate
            "student aadhaar", "student aadhar", "student adhar" -> s.studentAdhar
            "c/o", "care of" -> s.co
            "gender" -> s.gender
            "caste" -> s.caste
            "father name" -> s.fatherName
            "father aadhaar", "father aadhar", "father adhar" -> s.fatherAdhar
            "mother name" -> s.motherName
            "mother aadhaar", "mother aadhar", "mother adhar" -> s.motherAdhar
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
            "a/c holder name", "account holder name" -> b?.acHolderName ?: "-"
            "a/c no", "account number", "ac no" -> b?.acNo ?: "-"
            "ifsc code" -> b?.ifscCode ?: "-"
            "branch name" -> b?.branchName ?: "-"
            else -> ""
        }
    }
}

