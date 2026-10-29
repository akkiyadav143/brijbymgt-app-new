package com.brijbymgt.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_details",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BankDetailsEntity(
    @PrimaryKey
    val studentId: Long,
    val hasBankDetails: Boolean = false,
    val acHolderName: String = "",
    val acNo: String = "",
    val ifscCode: String = "",
    val branchName: String = ""
)

