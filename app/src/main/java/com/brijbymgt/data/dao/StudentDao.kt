package com.brijbymgt.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.brijbymgt.data.entity.StudentEntity
import com.brijbymgt.data.model.StudentWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStudent(student: StudentEntity): Long

    @Update
    fun updateStudent(student: StudentEntity): Int

    @Transaction
    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getAllActiveStudentsWithDetails(): Flow<List<StudentWithDetails>>

    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getAllActiveStudentsSync(): List<StudentEntity>

    @Transaction
    @Query("SELECT * FROM students WHERE isDeleted = 0 AND studentClass NOT IN ('Class 0', '0', 'CL Pass') ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getMainClassActiveStudentsWithDetails(): Flow<List<StudentWithDetails>>

    @Transaction
    @Query("SELECT * FROM students WHERE isDeleted = 0 AND (studentClass = :studentClass OR studentClass = 'Class ' || :studentClass OR REPLACE(studentClass, 'Class ', '') = :studentClass OR REPLACE(studentClass, 'Class ', 'CL ') = REPLACE(:studentClass, 'Class ', 'CL ')) ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getActiveStudentsByClass(studentClass: String): Flow<List<StudentWithDetails>>

    @Query("SELECT * FROM students WHERE isDeleted = 0 AND (studentClass = :studentClass OR studentClass = 'Class ' || :studentClass OR REPLACE(studentClass, 'Class ', '') = :studentClass OR REPLACE(studentClass, 'Class ', 'CL ') = REPLACE(:studentClass, 'Class ', 'CL ')) ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getActiveStudentsByClassSync(studentClass: String): List<StudentEntity>

    @Transaction
    @Query("SELECT * FROM students WHERE isDeleted = 0 AND (studentClass IN ('Class 0', '0') OR studentClass = 'CL Pass') ORDER BY CASE WHEN srNumber GLOB '*[0-9]*' THEN CAST(srNumber AS INTEGER) ELSE 999999 END ASC, srNumber ASC")
    fun getCl0AndPassStudents(): Flow<List<StudentWithDetails>>

    @Transaction
    @Query("SELECT * FROM students WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashStudents(): Flow<List<StudentWithDetails>>

    @Transaction
    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentWithDetailsById(id: Long): StudentWithDetails?

    @Query("UPDATE students SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    fun softDeleteStudent(id: Long, timestamp: Long): Int

    @Query("UPDATE students SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    fun restoreStudent(id: Long): Int

    @Delete
    fun deleteStudentPermanently(student: StudentEntity): Int

    @Query("DELETE FROM students WHERE id = :id")
    fun deleteStudentByIdPermanently(id: Long): Int

    @Query("DELETE FROM students WHERE isDeleted = 1")
    fun emptyTrash(): Int

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0 AND srNumber = :srNumber LIMIT 1")
    fun checkDuplicateSrNumber(srNumber: String): Int

    @Query("SELECT COUNT(*) FROM students WHERE isDeleted = 0 AND uniqueId = :uniqueId LIMIT 1")
    fun checkDuplicateUniqueId(uniqueId: String): Int
}

