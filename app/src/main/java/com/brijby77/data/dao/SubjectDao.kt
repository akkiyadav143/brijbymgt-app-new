package com.brijby77.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.brijby77.data.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubject(subject: SubjectEntity): Long

    @Update
    fun updateSubject(subject: SubjectEntity): Int

    @Query("DELETE FROM exam_subjects WHERE subjectId = :subjectId")
    fun deleteSubject(subjectId: Long): Int

    @Query("SELECT * FROM exam_subjects WHERE subjectId = :subjectId LIMIT 1")
    fun getSubjectById(subjectId: Long): SubjectEntity?

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId ORDER BY subjectOrder ASC, subjectId ASC")
    fun getSubjectsForExam(examId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId ORDER BY subjectOrder ASC, subjectId ASC")
    fun getSubjectsForExamSync(examId: Long): List<SubjectEntity>

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId AND isActive = 1 ORDER BY subjectOrder ASC, subjectId ASC")
    fun getActiveSubjectsForExam(examId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM exam_subjects WHERE examId = :examId AND isActive = 1 ORDER BY subjectOrder ASC, subjectId ASC")
    fun getActiveSubjectsForExamSync(examId: Long): List<SubjectEntity>

    @Query("UPDATE exam_subjects SET isActive = :isActive WHERE subjectId = :subjectId")
    fun updateSubjectStatus(subjectId: Long, isActive: Boolean): Int

    @Query("UPDATE exam_subjects SET subjectOrder = :subjectOrder WHERE subjectId = :subjectId")
    fun updateSubjectOrder(subjectId: Long, subjectOrder: Int): Int
}

