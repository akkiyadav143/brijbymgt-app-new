package com.brijby77.ui.addstudent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijby77.data.entity.BankDetailsEntity
import com.brijby77.data.entity.DocumentEntity
import com.brijby77.data.entity.HealthDetailsEntity
import com.brijby77.data.entity.HeightWeightHistoryEntity
import com.brijby77.data.entity.StudentEntity
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class AddStudentViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    private val _loadedStudent = MutableSharedFlow<StudentWithDetails?>()
    val loadedStudent: SharedFlow<StudentWithDetails?> = _loadedStudent

    private val _savedStudentWithDetails = MutableSharedFlow<StudentWithDetails?>()
    val savedStudentWithDetails: SharedFlow<StudentWithDetails?> = _savedStudentWithDetails

    fun loadStudentForEdit(id: Long) {
        viewModelScope.launch {
            val student = repository.getStudentWithDetailsById(id)
            _loadedStudent.emit(student)
        }
    }

    fun saveStudent(
        student: StudentEntity,
        healthDetails: HealthDetailsEntity?,
        heightWeightHistory: List<HeightWeightHistoryEntity>,
        bankDetails: BankDetailsEntity?,
        documents: List<DocumentEntity> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                repository.saveStudent(student, healthDetails, heightWeightHistory, bankDetails, documents)
                val allStudents = repository.getAllActiveStudentsSync()
                val newlyAdded = allStudents.maxByOrNull { it.id } ?: allStudents.lastOrNull()
                if (newlyAdded != null) {
                    val details = repository.getStudentWithDetailsById(newlyAdded.id)
                    _savedStudentWithDetails.emit(details)
                }
                _saveResult.emit(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _saveResult.emit(false)
            }
        }
    }
}

class AddStudentViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddStudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddStudentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
