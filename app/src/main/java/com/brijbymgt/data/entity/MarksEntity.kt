package com.brijbymgt.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marks",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["examId"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("examId"), Index("studentId"), Index("subjectId")]
)
data class MarksEntity(
    @PrimaryKey(autoGenerate = true)
    val marksId: Long = 0L,
    val examId: Long,
    val studentId: Long,
    val subjectId: Long,
    val obtainedMarks: Double = 0.0,
    val isAbsent: Boolean = false
)

