package com.brijby77.ui.studentlist

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

class StudentListViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _selectedClassFilter = MutableStateFlow("All")
    val selectedClassFilter: StateFlow<String> = _selectedClassFilter

    private val _searchField = MutableStateFlow("All")
    val searchField: StateFlow<String> = _searchField

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow("Ascending")
    val sortOrder: StateFlow<String> = _sortOrder

    private val _sortByField = MutableStateFlow("None")
    val sortByField: StateFlow<String> = _sortByField

    private val _casteFilter = MutableStateFlow<String?>(null)
    private val _genderFilter = MutableStateFlow<String?>(null)
    private val _schemeFilter = MutableStateFlow<String?>(null)

    val filteredStudentList: StateFlow<List<StudentWithDetails>> = combine(
        combine(repository.mainClassActiveStudents, _selectedClassFilter, _searchField) { s, c, f -> Triple(s, c, f) },
        combine(_searchQuery, _sortOrder, _sortByField) { q, o, sf -> Triple(q, o, sf) },
        combine(_casteFilter, _genderFilter, _schemeFilter) { c, g, s -> Triple(c, g, s) }
    ) { group1, group2, group3 ->
        val allStudents = group1.first
        val classFilter = group1.second
        val field = group1.third

        val query = group2.first
        val order = group2.second
        val sortField = group2.third

        val caste = group3.first
        val gender = group3.second
        val scheme = group3.third

        val filtered = filterStudents(allStudents, classFilter, field, query, caste, gender, scheme)
        sortStudents(filtered, sortField, order)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setClassFilter(filter: String) {
        _selectedClassFilter.value = filter
    }

    fun setSearchField(field: String) {
        _searchField.value = field
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    fun setSortByField(field: String) {
        _sortByField.value = field
    }

    fun setCasteFilter(caste: String?) {
        _casteFilter.value = caste
    }

    fun setGenderFilter(gender: String?) {
        _genderFilter.value = gender
    }

    fun setSchemeFilter(scheme: String?) {
        _schemeFilter.value = scheme
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch {
            repository.moveToTrash(id)
        }
    }

    private fun normalizeClass(rawClass: String): String {
        val trimmed = rawClass.trim()
        if (trimmed.equals("Class 0", true) || trimmed.equals("CL 0", true) || trimmed == "0") return "Class 0"
        if (trimmed.equals("CL Pass", true) || trimmed.equals("Class Pass", true) || trimmed.equals("Pass", true)) return "CL Pass"
        val num = trimmed.replace("Class ", "").replace("CL ", "").trim()
        return "Class $num"
    }

    private fun sortStudents(
        list: List<StudentWithDetails>,
        sortField: String,
        order: String
    ): List<StudentWithDetails> {
        if (sortField.isBlank() || sortField.equals("None", true)) {
            return if (order.equals("Descending", true)) list.reversed() else list
        }

        val comparator = Comparator<StudentWithDetails> { a, b ->
            val sa = a.student
            val sb = b.student

            val result = when (sortField) {
                "Student Name" -> sa.studentName.compareTo(sb.studentName, ignoreCase = true)
                "Student Aadhaar" -> sa.studentAdhar.compareTo(sb.studentAdhar, ignoreCase = true)
                "Sr Number" -> (sa.srNumber.toLongOrNull() ?: 0L).compareTo(sb.srNumber.toLongOrNull() ?: 0L)
                "Sr No" -> (sa.srNo.toLongOrNull() ?: 0L).compareTo(sb.srNo.toLongOrNull() ?: 0L)
                "Unique ID" -> sa.uniqueId.compareTo(sb.uniqueId, ignoreCase = true)
                "PEN No" -> sa.penNo.compareTo(sb.penNo, ignoreCase = true)
                "AAPAR ID" -> sa.aaparId.compareTo(sb.aaparId, ignoreCase = true)
                "Class" -> normalizeClass(sa.studentClass).compareTo(normalizeClass(sb.studentClass), ignoreCase = true)
                "GEN", "OBC", "SC", "ST", "MINORITY" -> {
                    val aMatches = sa.caste.equals(sortField, ignoreCase = true)
                    val bMatches = sb.caste.equals(sortField, ignoreCase = true)
                    bMatches.compareTo(aMatches)
                }
                "Male", "Female" -> {
                    val aMatches = sa.gender.equals(sortField, ignoreCase = true)
                    val bMatches = sb.gender.equals(sortField, ignoreCase = true)
                    bMatches.compareTo(aMatches)
                }
                else -> sa.studentName.compareTo(sb.studentName, ignoreCase = true)
            }
            result
        }

        return if (order.equals("Descending", true)) {
            list.sortedWith(comparator.reversed())
        } else {
            list.sortedWith(comparator)
        }
    }

    private fun filterStudents(
        list: List<StudentWithDetails>,
        classFilter: String,
        searchField: String,
        query: String,
        caste: String?,
        gender: String?,
        scheme: String?
    ): List<StudentWithDetails> {
        val q = query.trim().lowercase()

        return list.filter { item ->
            val s = item.student

            // 1. Class filter (Normalized matching)
            val matchesClass = if (classFilter == "All") {
                true
            } else {
                val studentNormalized = normalizeClass(s.studentClass)
                val targetNormalized = normalizeClass(classFilter)
                studentNormalized.equals(targetNormalized, ignoreCase = true)
            }

            if (!matchesClass) return@filter false

            // 2. Caste filter
            if (!caste.isNullOrBlank() && !s.caste.equals(caste, ignoreCase = true)) {
                return@filter false
            }

            // 3. Gender filter
            if (!gender.isNullOrBlank() && !s.gender.equals(gender, ignoreCase = true)) {
                return@filter false
            }

            // 4. Scheme filter
            if (!scheme.isNullOrBlank()) {
                when (scheme.uppercase()) {
                    "AADHAR" -> if (s.studentAdhar.isBlank()) return@filter false
                    "APAAR" -> if (s.aaparId.isBlank()) return@filter false
                    "DBT" -> if (!s.dbt.equals("Yes", ignoreCase = true)) return@filter false
                }
            }

            // 5. Search query filter
            if (q.isEmpty()) return@filter true

            when (searchField) {
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
                    s.uniqueId.lowercase().contains(q) ||
                    s.penNo.lowercase().contains(q) ||
                    s.aaparId.lowercase().contains(q)
                }
            }
        }
    }
}

class StudentListViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

