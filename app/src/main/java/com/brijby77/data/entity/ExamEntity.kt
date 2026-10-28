package com.brijby77.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val examId: Long = 0L,
    val examName: String,
    val academicSession: String,
    val examType: String, // "Half Yearly", "Annual", "Unit Test", "Quarterly"
    val className: String, // "All Classes" or specific class like "Class 1"
    val startDate: String,
    val endDate: String,
    val status: String, // "Active", "Completed", "Pending"
    val isFinalized: Boolean = false,
    val examWideMaxMarks: Int = 100
)

