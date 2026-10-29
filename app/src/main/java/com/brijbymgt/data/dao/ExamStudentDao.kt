package com.brijbymgt.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.brijbymgt.data.entity.ExamStudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamStudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExamStudents(students: List<ExamStudentEntity>): List<Long>

    @Query("SELECT * FROM exam_students WHERE examId = :examId ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getExamStudentsForExam(examId: Long): Flow<List<ExamStudentEntity>>

    @Query("SELECT * FROM exam_students WHERE examId = :examId ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getExamStudentsForExamSync(examId: Long): List<ExamStudentEntity>

    @Query("DELETE FROM exam_students WHERE examId = :examId")
    fun deleteStudentsForExam(examId: Long): Int
}

