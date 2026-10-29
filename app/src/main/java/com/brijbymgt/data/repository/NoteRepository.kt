package com.brijbymgt.data.repository

import com.brijbymgt.data.dao.NoteDao
import com.brijbymgt.data.entity.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query)
        }
    }

    suspend fun getNoteById(id: Long): NoteEntity? {
        return withContext(Dispatchers.IO) {
            noteDao.getNoteById(id)
        }
    }

    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?> {
        return noteDao.getNoteByIdFlow(id)
    }

    suspend fun insertNote(note: NoteEntity): Long {
        return withContext(Dispatchers.IO) {
            noteDao.insertNote(note)
        }
    }

    suspend fun updateNote(note: NoteEntity): Int {
        return withContext(Dispatchers.IO) {
            val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updatedNote)
        }
    }

    suspend fun deleteNote(note: NoteEntity) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    suspend fun deleteNoteById(id: Long) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteById(id)
        }
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            noteDao.updatePinStatus(id, isPinned, System.currentTimeMillis())
        }
    }
}

