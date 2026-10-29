package com.brijbymgt.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_details",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HealthDetailsEntity(
    @PrimaryKey
    val studentId: Long,
    val hasHealthDetails: Boolean = false,
    val height: String = "",
    val weight: String = "",
    val bloodGroup: String = "",
    val hb: String = "",
    val eyeSight: String = "",
    val deworming: String = ""
)

