package com.brijbymgt.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.brijbymgt.data.entity.BankDetailsEntity
import com.brijbymgt.data.entity.DocumentEntity
import com.brijbymgt.data.entity.HealthDetailsEntity
import com.brijbymgt.data.entity.HeightWeightHistoryEntity
import com.brijbymgt.data.entity.StudentEntity

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

