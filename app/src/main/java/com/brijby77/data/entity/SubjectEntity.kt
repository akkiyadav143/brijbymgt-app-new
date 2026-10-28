package com.brijby77.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_subjects",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["examId"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("examId")]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val subjectId: Long = 0L,
    val examId: Long,
    val subjectName: String,
    val maxMarks: Int = 100,
    val passingMarks: Int = 33,
    val subjectOrder: Int = 0,
    val includeInResult: Boolean = true,
    val isActive: Boolean = true
)

