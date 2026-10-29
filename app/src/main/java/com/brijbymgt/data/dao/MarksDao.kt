package com.brijbymgt.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.brijbymgt.data.entity.MarksEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMarks(marks: MarksEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMarksList(marksList: List<MarksEntity>): List<Long>

    @Query("SELECT * FROM marks WHERE examId = :examId")
    fun getMarksForExam(examId: Long): Flow<List<MarksEntity>>

    @Query("SELECT * FROM marks WHERE examId = :examId")
    fun getMarksForExamSync(examId: Long): List<MarksEntity>

    @Query("SELECT * FROM marks WHERE examId = :examId AND studentId = :studentId")
    fun getMarksForStudent(examId: Long, studentId: Long): List<MarksEntity>

    @Query("SELECT * FROM marks WHERE examId = :examId AND subjectId = :subjectId")
    fun getMarksForSubject(examId: Long, subjectId: Long): List<MarksEntity>

    @Query("SELECT COUNT(*) FROM marks WHERE examId = :examId AND subjectId = :subjectId AND (obtainedMarks > 0 OR isAbsent = 1)")
    fun getEnteredMarksCountForSubject(examId: Long, subjectId: Long): Int
}

