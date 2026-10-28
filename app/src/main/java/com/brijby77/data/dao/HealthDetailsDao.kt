package com.brijby77.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.brijby77.data.entity.HealthDetailsEntity

@Dao
interface HealthDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHealthDetails(healthDetails: HealthDetailsEntity): Long

    @Query("DELETE FROM health_details WHERE studentId = :studentId")
    fun deleteHealthDetailsByStudentId(studentId: Long): Int
}

