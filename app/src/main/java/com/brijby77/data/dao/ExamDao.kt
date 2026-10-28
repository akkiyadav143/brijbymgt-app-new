package com.brijby77.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.brijby77.data.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExam(exam: ExamEntity): Long

    @Update
    fun updateExam(exam: ExamEntity): Int

    @Delete
    fun deleteExam(exam: ExamEntity): Int

    @Query("DELETE FROM exams WHERE examId = :examId")
    fun deleteExamById(examId: Long): Int

    @Query("SELECT * FROM exams ORDER BY examId DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY examId DESC")
    fun getAllExamsSync(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE examId = :examId LIMIT 1")
    fun getExamById(examId: Long): ExamEntity?

    @Query("UPDATE exams SET isFinalized = :isFinalized WHERE examId = :examId")
    fun finalizeExam(examId: Long, isFinalized: Boolean): Int

    @Query("UPDATE exams SET status = :status WHERE examId = :examId")
    fun updateExamStatus(examId: Long, status: String): Int

    @Query("UPDATE exams SET examWideMaxMarks = :maxMarks WHERE examId = :examId")
    fun updateExamWideMaxMarks(examId: Long, maxMarks: Int): Int
}

