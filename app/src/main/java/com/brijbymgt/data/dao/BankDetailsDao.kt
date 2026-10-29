package com.brijbymgt.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.brijbymgt.data.entity.BankDetailsEntity

@Dao
interface BankDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBankDetails(bankDetails: BankDetailsEntity): Long

    @Query("DELETE FROM bank_details WHERE studentId = :studentId")
    fun deleteBankDetailsByStudentId(studentId: Long): Int
}

