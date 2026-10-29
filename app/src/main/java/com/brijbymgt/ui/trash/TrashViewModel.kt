package com.brijbymgt.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.util.DocumentStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(private val repository: StudentRepository, application: Application) : AndroidViewModel(application) {

    private val _selectedClassFilter = MutableStateFlow("All")
    val selectedClassFilter: StateFlow<String> = _selectedClassFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val trashList: StateFlow<List<StudentWithDetails>> = combine(
        repository.trashStudents,
        _selectedClassFilter,
        _searchQuery
    ) { allTrash, classFilter, query ->
        allTrash.filter { item ->
            val s = item.student
            // Class filter match
            val matchesClass = when (classFilter) {
                "All", "All Classes" -> true
                "Class 1-8" -> {
                    val cls = s.studentClass.replace("Class ", "").replace("CL ", "").trim()
                    cls in listOf("1", "2", "3", "4", "5", "6", "7", "8")
                }
                "CL0", "Class 0", "0" -> s.studentClass.equals("Class 0", true) || s.studentClass.equals("0", true) || s.studentClass.equals("CL 0", true)
                "CL Pass", "Pass" -> s.studentClass.equals("CL Pass", true) || s.studentClass.equals("Pass", true)
                else -> {
                    val targetNum = classFilter.replace("Class ", "").replace("CL ", "").trim()
                    val studentNum = s.studentClass.replace("Class ", "").replace("CL ", "").trim()
                    studentNum.equals(targetNum, true)
                }
            }

            // Search query match across: Student Name, SR Number, Unique ID, PEN Number, AAPAR ID, Aadhaar, Mobile, Father/Mother Name
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                val q = query.lowercase().trim()
                s.studentName.lowercase().contains(q) ||
                s.srNumber.lowercase().contains(q) ||
                s.uniqueId.lowercase().contains(q) ||
                s.penNo.lowercase().contains(q) ||
                s.aaparId.lowercase().contains(q) ||
                s.studentAdhar.lowercase().contains(q) ||
                s.mobileNo.lowercase().contains(q) ||
                s.fatherName.lowercase().contains(q) ||
                s.motherName.lowercase().contains(q)
            }

            matchesClass && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setClassFilter(filter: String) {
        _selectedClassFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun restoreStudent(id: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
        }
    }

    fun restoreStudents(ids: List<Long>) {
        viewModelScope.launch {
            for (id in ids) {
                repository.restoreFromTrash(id)
            }
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            DocumentStorageHelper.deleteAllStudentDocumentsFolder(getApplication(), id)
            repository.deletePermanently(id)
        }
    }

    fun deletePermanentlyBatch(ids: List<Long>) {
        viewModelScope.launch {
            for (id in ids) {
                DocumentStorageHelper.deleteAllStudentDocumentsFolder(getApplication(), id)
                repository.deletePermanently(id)
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            for (item in trashList.value) {
                DocumentStorageHelper.deleteAllStudentDocumentsFolder(getApplication(), item.student.id)
            }
            repository.emptyTrash()
        }
    }
}

class TrashViewModelFactory(private val repository: StudentRepository, private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrashViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

