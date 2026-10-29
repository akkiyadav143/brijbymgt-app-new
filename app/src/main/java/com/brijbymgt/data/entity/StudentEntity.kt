package com.brijbymgt.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val srNo: String = "",
    val srNumber: String = "",
    val uniqueId: String = "",
    val penNo: String = "",
    val aaparId: String = "",
    val dbt: String = "No", // "Yes" or "No"
    val studentClass: String = "Class 1", // "Class 0", "Class 1".."Class 8", "CL Pass"
    val studentName: String = "",
    val dob: String = "",
    val admissionDate: String = "",
    val studentAdhar: String = "",
    val co: String = "",
    val gender: String = "Male", // "Male" or "Female"
    val caste: String = "GEN", // "GEN", "OBC", "SC", "ST", "MINORITY"
    val fatherName: String = "",
    val fatherAdhar: String = "",
    val motherName: String = "",
    val motherAdhar: String = "",
    val mobileNo: String = "",
    val address: String = "",
    val other: String = "",
    val rationCard: String = "",
    val imagePath: String? = null,
    val fatherImagePath: String? = null,
    val motherImagePath: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

