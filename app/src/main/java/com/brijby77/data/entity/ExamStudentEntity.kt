package com.brijby77.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_students",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["examId"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("examId"), Index("studentId")]
)
data class ExamStudentEntity(
    @PrimaryKey(autoGenerate = true)
    val examStudentId: Long = 0L,
    val examId: Long,
    val studentId: Long,
    val srNumber: String,
    val studentName: String,
    val className: String,
    val gender: String,
    val fatherName: String
)

