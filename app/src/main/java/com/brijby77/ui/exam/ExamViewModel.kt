package com.brijby77.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijby77.data.entity.ExamEntity
import com.brijby77.data.entity.ExamStudentEntity
import com.brijby77.data.entity.MarksEntity
import com.brijby77.data.entity.SubjectEntity
import com.brijby77.data.repository.ExamRepository
import com.brijby77.data.repository.ConsolidatedStudentReport
import com.brijby77.data.repository.StudentExamResultSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExamViewModel(private val repository: ExamRepository) : ViewModel() {

    val allExams: Flow<List<ExamEntity>> = repository.allExams

    private val _selectedExamId = MutableStateFlow<Long>(-1L)
    val selectedExamId: StateFlow<Long> = _selectedExamId.asStateFlow()

    fun setSelectedExamId(examId: Long) {
        _selectedExamId.value = examId
    }

    suspend fun getExamById(examId: Long): ExamEntity? {
        return repository.getExamById(examId)
    }

    fun getSubjects(examId: Long): Flow<List<SubjectEntity>> {
        return repository.getSubjectsForExam(examId)
    }

    fun getActiveSubjects(examId: Long): Flow<List<SubjectEntity>> {
        return repository.getActiveSubjectsForExam(examId)
    }

    suspend fun addSubject(
        examId: Long,
        subjectName: String,
        maxMarks: Int,
        passingMarks: Int,
        subjectOrder: Int,
        includeInResult: Boolean
    ): Long {
        return repository.addSubject(examId, subjectName, maxMarks, passingMarks, subjectOrder, includeInResult)
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        repository.updateSubject(subject)
    }

    suspend fun deleteOrDeactivateSubject(examId: Long, subjectId: Long): String {
        return repository.deleteOrDeactivateSubject(examId, subjectId)
    }

    suspend fun updateSubjectOrder(subjectId: Long, subjectOrder: Int) {
        repository.updateSubjectOrder(subjectId, subjectOrder)
    }

    suspend fun updateExamWideMaxMarks(examId: Long, maxMarks: Int) {
        repository.updateExamWideMaxMarks(examId, maxMarks)
    }

    fun getExamStudents(examId: Long): Flow<List<ExamStudentEntity>> {
        return repository.getExamStudents(examId)
    }

    fun getMarks(examId: Long): Flow<List<MarksEntity>> {
        return repository.getMarksForExam(examId)
    }

    suspend fun createExam(
        examName: String,
        academicSession: String,
        examType: String,
        className: String,
        startDate: String,
        endDate: String,
        customSubjects: List<Pair<String, Int>>,
        examWideMaxMarks: Int = 100
    ): Long {
        return repository.createExamWithSubjectsAndStudents(
            examName, academicSession, examType, className, startDate, endDate, customSubjects, examWideMaxMarks
        )
    }

    suspend fun saveMarksList(marksList: List<MarksEntity>) {
        repository.saveMarksList(marksList)
    }

    suspend fun saveSingleMark(mark: MarksEntity) {
        repository.saveSingleMark(mark)
    }

    suspend fun finalizeExam(examId: Long, isFinalized: Boolean) {
        repository.finalizeExam(examId, isFinalized)
    }

    suspend fun deleteExam(examId: Long) {
        repository.deleteExam(examId)
    }

    suspend fun calculateResults(examId: Long): List<StudentExamResultSummary> {
        return repository.calculateStudentResultSummaries(examId)
    }

    suspend fun ensureStandardExamsForClass(className: String): List<ExamEntity> {
        return repository.ensureStandardExamsForClass(className)
    }

    suspend fun getConsolidatedFullReport(className: String): List<ConsolidatedStudentReport> {
        return repository.getConsolidatedFullReportForClass(className)
    }
}

class ExamViewModelFactory(private val repository: ExamRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

