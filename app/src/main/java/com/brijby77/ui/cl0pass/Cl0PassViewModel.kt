package com.brijby77.ui.cl0pass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Cl0PassViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val _searchField = MutableStateFlow("All")
    val searchField: StateFlow<String> = _searchField

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredList: StateFlow<List<StudentWithDetails>> = combine(
        repository.cl0AndPassStudents,
        _selectedFilter,
        _searchField,
        _searchQuery
    ) { allStudents, filter, field, query ->
        filterStudents(allStudents, filter, field, query)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchField(field: String) {
        _searchField.value = field
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch {
            repository.moveToTrash(id)
        }
    }

    private fun filterStudents(
        list: List<StudentWithDetails>,
        filter: String,
        field: String,
        query: String
    ): List<StudentWithDetails> {
        val q = query.trim().lowercase()

        return list.filter { item ->
            val s = item.student

            // 1. Class filter
            val matchesClass = when (filter) {
                "Class 0" -> s.studentClass.equals("Class 0", ignoreCase = true)
                "CL Pass" -> s.studentClass.equals("CL Pass", ignoreCase = true)
                else -> true
            }

            if (!matchesClass) return@filter false

            // 2. Search query
            if (q.isEmpty()) return@filter true

            when (field) {
                "SR Number" -> s.srNumber.lowercase().contains(q)
                "Student Name" -> s.studentName.lowercase().contains(q)
                "Student Adhar" -> s.studentAdhar.lowercase().contains(q)
                "Mobile No" -> s.mobileNo.lowercase().contains(q)
                "Father Name" -> s.fatherName.lowercase().contains(q)
                "Mother Name" -> s.motherName.lowercase().contains(q)
                else -> { // All
                    s.studentName.lowercase().contains(q) ||
                    s.srNumber.lowercase().contains(q) ||
                    s.studentAdhar.lowercase().contains(q) ||
                    s.mobileNo.lowercase().contains(q) ||
                    s.fatherName.lowercase().contains(q) ||
                    s.motherName.lowercase().contains(q) ||
                    s.uniqueId.lowercase().contains(q)
                }
            }
        }
    }
}

class Cl0PassViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(Cl0PassViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return Cl0PassViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

