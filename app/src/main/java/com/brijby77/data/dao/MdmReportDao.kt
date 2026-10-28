package com.brijby77.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.brijby77.data.entity.MdmReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MdmReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMdmReport(report: MdmReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMdmReports(reports: List<MdmReportEntity>): List<Long>

    @Query("SELECT * FROM mdm_reports")
    fun getAllMdmReports(): Flow<List<MdmReportEntity>>

    @Query("SELECT * FROM mdm_reports")
    fun getAllMdmReportsDirect(): List<MdmReportEntity>
}

