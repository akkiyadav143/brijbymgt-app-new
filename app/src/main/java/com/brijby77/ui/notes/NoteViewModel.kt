package com.brijby77.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijby77.data.database.AppDatabase
import com.brijby77.data.entity.NoteEntity
import com.brijby77.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val notesList: StateFlow<List<NoteEntity>> = _searchQuery
        .flatMapLatest { query ->
            repository.searchNotes(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insertNote(title: String, content: String, isPinned: Boolean, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title.trim(),
                content = content.trim(),
                isPinned = isPinned,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = repository.insertNote(note)
            onComplete(id)
        }
    }

    fun updateNote(id: Long, title: String, content: String, isPinned: Boolean, createdAt: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = id,
                title = title.trim(),
                content = content.trim(),
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis(),
                isPinned = isPinned
            )
            repository.updateNote(note)
            onComplete()
        }
    }

    suspend fun getNoteById(id: Long): NoteEntity? {
        return repository.getNoteById(id)
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.setPinned(note.id, !note.isPinned)
        }
    }
}

class NoteViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

