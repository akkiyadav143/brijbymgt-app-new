package com.brijbymgt.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mdm_reports")
data class MdmReportEntity(
    @PrimaryKey
    val studentClass: String, // e.g., "Class 1", "Class 2" ... "Class 8"
    val mdmMale: Int = 0,
    val mdmFemale: Int = 0
)

