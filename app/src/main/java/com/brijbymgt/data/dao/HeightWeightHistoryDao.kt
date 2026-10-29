package com.brijbymgt.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.brijbymgt.data.entity.HeightWeightHistoryEntity

@Dao
interface HeightWeightHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHeightWeightHistory(history: List<HeightWeightHistoryEntity>): List<Long>

    @Query("DELETE FROM height_weight_history WHERE studentId = :studentId")
    fun deleteHistoryByStudentId(studentId: Long): Int
}

