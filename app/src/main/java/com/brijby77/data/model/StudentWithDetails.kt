package com.brijby77.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.brijby77.data.entity.BankDetailsEntity
import com.brijby77.data.entity.DocumentEntity
import com.brijby77.data.entity.HealthDetailsEntity
import com.brijby77.data.entity.HeightWeightHistoryEntity
import com.brijby77.data.entity.StudentEntity

data class StudentWithDetails(
    @Embedded val student: StudentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val healthDetails: HealthDetailsEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val heightWeightHistory: List<HeightWeightHistoryEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val bankDetails: BankDetailsEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val documents: List<DocumentEntity> = emptyList()
)

