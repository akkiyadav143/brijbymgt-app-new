package com.brijbymgt.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.brijbymgt.data.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    fun updateDocument(document: DocumentEntity): Int

    @Query("SELECT * FROM student_documents WHERE studentId = :studentId")
    fun getDocumentsForStudent(studentId: Long): List<DocumentEntity>

    @Query("SELECT * FROM student_documents WHERE studentId = :studentId")
    fun getDocumentsForStudentFlow(studentId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM student_documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): DocumentEntity?

    @Delete
    fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM student_documents WHERE id = :id")
    fun deleteDocumentById(id: Long)

    @Query("DELETE FROM student_documents WHERE studentId = :studentId")
    fun deleteAllDocumentsForStudent(studentId: Long)

    @Query("SELECT * FROM student_documents")
    fun getAllDocumentsSync(): List<DocumentEntity>
}

