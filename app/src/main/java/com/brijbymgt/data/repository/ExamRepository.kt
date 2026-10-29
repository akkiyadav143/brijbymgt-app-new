package com.brijbymgt.data.repository

import com.brijbymgt.data.dao.ExamDao
import com.brijbymgt.data.dao.ExamStudentDao
import com.brijbymgt.data.dao.MarksDao
import com.brijbymgt.data.dao.StudentDao
import com.brijbymgt.data.dao.SubjectDao
import com.brijbymgt.data.entity.ExamEntity
import com.brijbymgt.data.entity.ExamStudentEntity
import com.brijbymgt.data.entity.MarksEntity
import com.brijbymgt.data.entity.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class StudentExamResultSummary(
    val examStudent: ExamStudentEntity,
    val marksList: List<MarksEntity>,
    val totalMaxMarks: Int,
    val examWideMaxMarks: Int,
    val totalObtainedMarks: Double,
    val percentage: Double,
    val grade: String,
    val passStatus: String, // "PASS" or "FAIL"
    val rank: Int = 0
)

val DEFAULT_SUBJECT_NAMES = listOf(
    "Hindi",
    "Math",
    "English",
    "Sanskrit",
    "Science",
    "Social Science",
    "History",
    "Home Science / Agriculture",
    "Art",
    "Sports & Health",
    "Moral Values"
)

class ExamRepository(
    private val examDao: ExamDao,
    private val subjectDao: SubjectDao,
    private val examStudentDao: ExamStudentDao,
    private val marksDao: MarksDao,
    private val studentDao: StudentDao
) {

    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()

    suspend fun getExamById(examId: Long): ExamEntity? {
        return withContext(Dispatchers.IO) {
            examDao.getExamById(examId)
        }
    }

    fun getSubjectsForExam(examId: Long): Flow<List<SubjectEntity>> {
        return subjectDao.getSubjectsForExam(examId)
    }

    suspend fun getSubjectsForExamSync(examId: Long): List<SubjectEntity> {
        return withContext(Dispatchers.IO) {
            subjectDao.getSubjectsForExamSync(examId)
        }
    }

    fun getActiveSubjectsForExam(examId: Long): Flow<List<SubjectEntity>> {
        return subjectDao.getActiveSubjectsForExam(examId)
    }

    suspend fun getActiveSubjectsForExamSync(examId: Long): List<SubjectEntity> {
        return withContext(Dispatchers.IO) {
            subjectDao.getActiveSubjectsForExamSync(examId)
        }
    }

    suspend fun addSubject(
        examId: Long,
        subjectName: String,
        maxMarks: Int,
        passingMarks: Int,
        subjectOrder: Int,
        includeInResult: Boolean
    ): Long {
        return withContext(Dispatchers.IO) {
            val subject = SubjectEntity(
                examId = examId,
                subjectName = subjectName,
                maxMarks = maxMarks,
                passingMarks = passingMarks,
                subjectOrder = subjectOrder,
                includeInResult = includeInResult,
                isActive = true
            )
            subjectDao.insertSubject(subject)
        }
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        withContext(Dispatchers.IO) {
            subjectDao.updateSubject(subject)
        }
    }

    suspend fun deleteOrDeactivateSubject(examId: Long, subjectId: Long): String {
        return withContext(Dispatchers.IO) {
            val enteredCount = marksDao.getEnteredMarksCountForSubject(examId, subjectId)
            if (enteredCount == 0) {
                subjectDao.deleteSubject(subjectId)
                "Subject deleted successfully."
            } else {
                subjectDao.updateSubjectStatus(subjectId, false)
                "Subject has recorded marks. Deactivated instead of deleted."
            }
        }
    }

    suspend fun updateSubjectOrder(subjectId: Long, subjectOrder: Int) {
        withContext(Dispatchers.IO) {
            subjectDao.updateSubjectOrder(subjectId, subjectOrder)
        }
    }

    suspend fun updateExamWideMaxMarks(examId: Long, maxMarks: Int) {
        withContext(Dispatchers.IO) {
            examDao.updateExamWideMaxMarks(examId, maxMarks)
        }
    }

    fun getExamStudents(examId: Long): Flow<List<ExamStudentEntity>> {
        return examStudentDao.getExamStudentsForExam(examId)
    }

    suspend fun getExamStudentsSync(examId: Long): List<ExamStudentEntity> {
        return withContext(Dispatchers.IO) {
            examStudentDao.getExamStudentsForExamSync(examId)
        }
    }

    fun getMarksForExam(examId: Long): Flow<List<MarksEntity>> {
        return marksDao.getMarksForExam(examId)
    }

    suspend fun getMarksForExamSync(examId: Long): List<MarksEntity> {
        return withContext(Dispatchers.IO) {
            marksDao.getMarksForExamSync(examId)
        }
    }

    suspend fun createExamWithSubjectsAndStudents(
        examName: String,
        academicSession: String,
        examType: String,
        className: String,
        startDate: String,
        endDate: String,
        customSubjects: List<Pair<String, Int>>, // List of (subjectName, maxMarks)
        examWideMaxMarks: Int = 100
    ): Long {
        return withContext(Dispatchers.IO) {
            val exam = ExamEntity(
                examName = examName,
                academicSession = academicSession,
                examType = examType,
                className = className,
                startDate = startDate,
                endDate = endDate,
                status = "Active",
                isFinalized = false,
                examWideMaxMarks = examWideMaxMarks
            )
            val examId = examDao.insertExam(exam)

            // Insert subjects
            val subjectsToInsert = if (customSubjects.isNotEmpty()) {
                customSubjects.mapIndexed { index, pair ->
                    SubjectEntity(
                        examId = examId,
                        subjectName = pair.first,
                        maxMarks = pair.second,
                        passingMarks = (pair.second * 0.33).toInt().coerceAtLeast(1),
                        subjectOrder = index + 1,
                        includeInResult = true,
                        isActive = true
                    )
                }
            } else {
                DEFAULT_SUBJECT_NAMES.mapIndexed { index, name ->
                    SubjectEntity(
                        examId = examId,
                        subjectName = name,
                        maxMarks = examWideMaxMarks,
                        passingMarks = (examWideMaxMarks * 0.33).toInt().coerceAtLeast(1),
                        subjectOrder = index + 1,
                        includeInResult = true,
                        isActive = true
                    )
                }
            }
            subjectDao.insertSubjects(subjectsToInsert)

            // Sync active students snapshot
            val rawStudents = if (className.equals("All Classes", true) || className.equals("ALL CL", true) || className.isBlank()) {
                studentDao.getAllActiveStudentsSync()
            } else {
                studentDao.getActiveStudentsByClassSync(className)
            }

            val examStudents = rawStudents.map { s ->
                ExamStudentEntity(
                    examId = examId,
                    studentId = s.id,
                    srNumber = s.srNumber,
                    studentName = s.studentName,
                    className = s.studentClass,
                    gender = s.gender,
                    fatherName = s.fatherName
                )
            }
            examStudentDao.insertExamStudents(examStudents)

            examId
        }
    }

    suspend fun saveMarksList(marksList: List<MarksEntity>) {
        withContext(Dispatchers.IO) {
            marksDao.insertMarksList(marksList)
        }
    }

    suspend fun saveSingleMark(mark: MarksEntity) {
        withContext(Dispatchers.IO) {
            marksDao.insertOrUpdateMarks(mark)
        }
    }

    suspend fun finalizeExam(examId: Long, isFinalized: Boolean) {
        withContext(Dispatchers.IO) {
            examDao.finalizeExam(examId, isFinalized)
            val newStatus = if (isFinalized) "Completed" else "Active"
            examDao.updateExamStatus(examId, newStatus)
        }
    }

    suspend fun deleteExam(examId: Long) {
        withContext(Dispatchers.IO) {
            examDao.deleteExamById(examId)
        }
    }

    suspend fun calculateStudentResultSummaries(examId: Long): List<StudentExamResultSummary> {
        return withContext(Dispatchers.IO) {
            val exam = examDao.getExamById(examId)
            val students = examStudentDao.getExamStudentsForExamSync(examId)
            val allSubjects = subjectDao.getSubjectsForExamSync(examId)
            // Result calculation sums up active subjects where includeInResult == true
            val activeIncludedSubjects = allSubjects.filter { it.isActive && it.includeInResult }
            val marks = marksDao.getMarksForExamSync(examId)

            val marksMap = marks.groupBy { it.studentId }
            val totalMaxMarks = activeIncludedSubjects.sumOf { it.maxMarks }
            val examWideMaxMarks = exam?.examWideMaxMarks ?: 100

            val summaries = students.map { student ->
                val studentMarks = marksMap[student.studentId] ?: emptyList()
                val studentMarksMap = studentMarks.associateBy { it.subjectId }

                var totalObtained = 0.0
                var hasFailed = false
                var isAbsentAny = false

                for (subject in activeIncludedSubjects) {
                    val m = studentMarksMap[subject.subjectId]
                    if (m != null) {
                        if (m.isAbsent) {
                            isAbsentAny = true
                            hasFailed = true
                        } else {
                            totalObtained += m.obtainedMarks
                            if (m.obtainedMarks < subject.passingMarks) {
                                hasFailed = true
                            }
                        }
                    } else {
                        hasFailed = true
                    }
                }

                val percentage = if (totalMaxMarks > 0) (totalObtained / totalMaxMarks) * 100.0 else 0.0
                val passStatus = if (!hasFailed && !isAbsentAny && totalMaxMarks > 0) "PASS" else "FAIL"

                val grade = when {
                    percentage >= 90.0 -> "A+"
                    percentage >= 80.0 -> "A"
                    percentage >= 65.0 -> "B+"
                    percentage >= 50.0 -> "B"
                    percentage >= 33.0 -> "C"
                    else -> "D / F"
                }

                StudentExamResultSummary(
                    examStudent = student,
                    marksList = studentMarks,
                    totalMaxMarks = totalMaxMarks,
                    examWideMaxMarks = examWideMaxMarks,
                    totalObtainedMarks = totalObtained,
                    percentage = percentage,
                    grade = grade,
                    passStatus = passStatus
                )
            }

            val sorted = summaries.sortedByDescending { it.totalObtainedMarks }
            sorted.mapIndexed { index, summary ->
                summary.copy(rank = index + 1)
            }
        }
    }

    suspend fun ensureStandardExamsForClass(className: String): List<ExamEntity> {
        return withContext(Dispatchers.IO) {
            val allExams = examDao.getAllExamsSync()
            val session = "2024-2025"
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            val defaultSubjects10 = DEFAULT_SUBJECT_NAMES.map { Triple(it, 10, 3) }
            val defaultSubjects30 = DEFAULT_SUBJECT_NAMES.map { Triple(it, 30, 10) }
            val defaultSubjects50 = DEFAULT_SUBJECT_NAMES.map { Triple(it, 50, 17) }

            val standardSpecs = listOf(
                Quad("Unit I", "Unit I Examination", 10, defaultSubjects10),
                Quad("Half Yearly", "Half Yearly Examination", 30, defaultSubjects30),
                Quad("Unit II", "Unit II Examination", 10, defaultSubjects10),
                Quad("Yearly", "Yearly Examination", 50, defaultSubjects50)
            )

            for (spec in standardSpecs) {
                val examType = spec.first
                val examName = "$examType - $className"
                val examWideMax = spec.third
                val subjectsList = spec.fourth

                val exists = allExams.any { it.examType.equals(examType, true) && (it.className.equals(className, true) || it.className.equals("All Classes", true)) }
                if (!exists) {
                    createExamWithCustomConfig(
                        examName = examName,
                        academicSession = session,
                        examType = examType,
                        className = className,
                        startDate = currentDate,
                        endDate = currentDate,
                        examWideMaxMarks = examWideMax,
                        subjects = subjectsList
                    )
                }
            }

            examDao.getAllExamsSync().filter { it.className.equals(className, true) || it.className.equals("All Classes", true) }
        }
    }

    private suspend fun createExamWithCustomConfig(
        examName: String,
        academicSession: String,
        examType: String,
        className: String,
        startDate: String,
        endDate: String,
        examWideMaxMarks: Int,
        subjects: List<Triple<String, Int, Int>>
    ): Long {
        return withContext(Dispatchers.IO) {
            val exam = ExamEntity(
                examName = examName,
                academicSession = academicSession,
                examType = examType,
                className = className,
                startDate = startDate,
                endDate = endDate,
                status = "Active",
                isFinalized = false,
                examWideMaxMarks = examWideMaxMarks
            )
            val examId = examDao.insertExam(exam)

            val subjectsToInsert = subjects.mapIndexed { index, triple ->
                SubjectEntity(
                    examId = examId,
                    subjectName = triple.first,
                    maxMarks = triple.second,
                    passingMarks = triple.third,
                    subjectOrder = index + 1,
                    includeInResult = true,
                    isActive = true
                )
            }
            subjectDao.insertSubjects(subjectsToInsert)

            val rawStudents = if (className.equals("All Classes", true)) {
                studentDao.getAllActiveStudentsSync()
            } else {
                studentDao.getActiveStudentsByClassSync(className)
            }

            val examStudents = rawStudents.map { s ->
                ExamStudentEntity(
                    examId = examId,
                    studentId = s.id,
                    srNumber = s.srNumber,
                    studentName = s.studentName,
                    className = s.studentClass,
                    gender = s.gender,
                    fatherName = s.fatherName
                )
            }
            examStudentDao.insertExamStudents(examStudents)

            examId
        }
    }

    suspend fun getConsolidatedFullReportForClass(className: String): List<ConsolidatedStudentReport> {
        return withContext(Dispatchers.IO) {
            val exams = ensureStandardExamsForClass(className)
            val examMap = exams.associateBy { it.examType.trim().lowercase() }
            val unit1Exam = examMap["unit i"]
            val halfYearlyExam = examMap["half yearly"]
            val unit2Exam = examMap["unit ii"]
            val yearlyExam = examMap["yearly"]

            val academicSession = exams.firstOrNull()?.academicSession ?: "2024-2025"

            val unit1Subjects = unit1Exam?.let { subjectDao.getSubjectsForExamSync(it.examId) } ?: emptyList()
            val unit1Marks = unit1Exam?.let { marksDao.getMarksForExamSync(it.examId) }?.groupBy { it.studentId } ?: emptyMap()

            val halfYearlySubjects = halfYearlyExam?.let { subjectDao.getSubjectsForExamSync(it.examId) } ?: emptyList()
            val halfYearlyMarks = halfYearlyExam?.let { marksDao.getMarksForExamSync(it.examId) }?.groupBy { it.studentId } ?: emptyMap()

            val unit2Subjects = unit2Exam?.let { subjectDao.getSubjectsForExamSync(it.examId) } ?: emptyList()
            val unit2Marks = unit2Exam?.let { marksDao.getMarksForExamSync(it.examId) }?.groupBy { it.studentId } ?: emptyMap()

            val yearlySubjects = yearlyExam?.let { subjectDao.getSubjectsForExamSync(it.examId) } ?: emptyList()
            val yearlyMarks = yearlyExam?.let { marksDao.getMarksForExamSync(it.examId) }?.groupBy { it.studentId } ?: emptyMap()

            val studentMap = mutableMapOf<Long, ExamStudentEntity>()
            for (exam in exams) {
                val ests = examStudentDao.getExamStudentsForExamSync(exam.examId)
                for (est in ests) {
                    studentMap[est.studentId] = est
                }
            }
            if (studentMap.isEmpty()) {
                val rawStudents = studentDao.getActiveStudentsByClassSync(className)
                for (s in rawStudents) {
                    studentMap[s.id] = ExamStudentEntity(
                        examId = -1L,
                        studentId = s.id,
                        srNumber = s.srNumber,
                        studentName = s.studentName,
                        className = s.studentClass,
                        gender = s.gender,
                        fatherName = s.fatherName
                    )
                }
            }

            val students = studentMap.values.sortedBy { s ->
                s.srNumber.trim().toLongOrNull() ?: Long.MAX_VALUE
            }

            val allSubjectsCombined = (unit1Subjects + halfYearlySubjects + unit2Subjects + yearlySubjects)
            val allSubjectNames = allSubjectsCombined.map { it.subjectName }.distinct()
            val sortedSubjectNames = allSubjectNames.sortedWith(compareBy { name ->
                val idx = DEFAULT_SUBJECT_NAMES.indexOfFirst { it.equals(name, true) }
                if (idx != -1) idx else {
                    val subObj = allSubjectsCombined.find { it.subjectName.equals(name, true) }
                    subObj?.subjectOrder ?: 100
                }
            })

            val reports = mutableListOf<ConsolidatedStudentReport>()

            for (student in students) {
                val u1Map = unit1Marks[student.studentId]?.associateBy { it.subjectId } ?: emptyMap()
                val hyMap = halfYearlyMarks[student.studentId]?.associateBy { it.subjectId } ?: emptyMap()
                val u2Map = unit2Marks[student.studentId]?.associateBy { it.subjectId } ?: emptyMap()
                val yrMap = yearlyMarks[student.studentId]?.associateBy { it.subjectId } ?: emptyMap()

                var unit1ObtSum = 0.0
                var unit1MaxSum = 0
                var halfYearlyObtSum = 0.0
                var halfYearlyMaxSum = 0
                var unit2ObtSum = 0.0
                var unit2MaxSum = 0
                var yearlyObtSum = 0.0
                var yearlyMaxSum = 0

                var hasFailed = false
                var isAbsentAny = false

                val subjectMarksList = mutableListOf<SubjectConsolidatedMarks>()

                for (subjName in sortedSubjectNames) {
                    val subU1 = unit1Subjects.find { it.subjectName.equals(subjName, true) }
                    var subU1Obt = 0.0
                    var subU1Max = 0
                    val u1Str = if (subU1 != null) {
                        val m = u1Map[subU1.subjectId]
                        if (m == null) "-"
                        else if (m.isAbsent) {
                            isAbsentAny = true
                            hasFailed = true
                            "Ab"
                        } else {
                            subU1Obt = m.obtainedMarks
                            subU1Max = subU1.maxMarks
                            if (subU1.includeInResult) {
                                unit1ObtSum += m.obtainedMarks
                                unit1MaxSum += subU1.maxMarks
                                if (m.obtainedMarks < subU1.passingMarks) hasFailed = true
                            }
                            "${m.obtainedMarks.toInt()}/${subU1.maxMarks}"
                        }
                    } else "-"

                    val subHY = halfYearlySubjects.find { it.subjectName.equals(subjName, true) }
                    var subHYObt = 0.0
                    var subHYMax = 0
                    val hyStr = if (subHY != null) {
                        val m = hyMap[subHY.subjectId]
                        if (m == null) "-"
                        else if (m.isAbsent) {
                            isAbsentAny = true
                            hasFailed = true
                            "Ab"
                        } else {
                            subHYObt = m.obtainedMarks
                            subHYMax = subHY.maxMarks
                            if (subHY.includeInResult) {
                                halfYearlyObtSum += m.obtainedMarks
                                halfYearlyMaxSum += subHY.maxMarks
                                if (m.obtainedMarks < subHY.passingMarks) hasFailed = true
                            }
                            "${m.obtainedMarks.toInt()}/${subHY.maxMarks}"
                        }
                    } else "-"

                    val subU2 = unit2Subjects.find { it.subjectName.equals(subjName, true) }
                    var subU2Obt = 0.0
                    var subU2Max = 0
                    val u2Str = if (subU2 != null) {
                        val m = u2Map[subU2.subjectId]
                        if (m == null) "-"
                        else if (m.isAbsent) {
                            isAbsentAny = true
                            hasFailed = true
                            "Ab"
                        } else {
                            subU2Obt = m.obtainedMarks
                            subU2Max = subU2.maxMarks
                            if (subU2.includeInResult) {
                                unit2ObtSum += m.obtainedMarks
                                unit2MaxSum += subU2.maxMarks
                                if (m.obtainedMarks < subU2.passingMarks) hasFailed = true
                            }
                            "${m.obtainedMarks.toInt()}/${subU2.maxMarks}"
                        }
                    } else "-"

                    val subYr = yearlySubjects.find { it.subjectName.equals(subjName, true) }
                    var subYrObt = 0.0
                    var subYrMax = 0
                    val yrStr = if (subYr != null) {
                        val m = yrMap[subYr.subjectId]
                        if (m == null) "-"
                        else if (m.isAbsent) {
                            isAbsentAny = true
                            hasFailed = true
                            "Ab"
                        } else {
                            subYrObt = m.obtainedMarks
                            subYrMax = subYr.maxMarks
                            if (subYr.includeInResult) {
                                yearlyObtSum += m.obtainedMarks
                                yearlyMaxSum += subYr.maxMarks
                                if (m.obtainedMarks < subYr.passingMarks) hasFailed = true
                            }
                            "${m.obtainedMarks.toInt()}/${subYr.maxMarks}"
                        }
                    } else "-"

                    val subjectTotalObtained = subU1Obt + subHYObt + subU2Obt + subYrObt
                    val subjectTotalMax = subU1Max + subHYMax + subU2Max + subYrMax

                    subjectMarksList.add(
                        SubjectConsolidatedMarks(
                            subjectName = subjName,
                            unit1 = u1Str,
                            halfYearly = hyStr,
                            unit2 = u2Str,
                            yearly = yrStr,
                            subjectTotalObtained = subjectTotalObtained,
                            subjectTotalMax = subjectTotalMax
                        )
                    )
                }

                val overallObtained = unit1ObtSum + halfYearlyObtSum + unit2ObtSum + yearlyObtSum
                val overallMax = unit1MaxSum + halfYearlyMaxSum + unit2MaxSum + yearlyMaxSum
                val percentage = if (overallMax > 0) (overallObtained / overallMax) * 100.0 else 0.0
                val passStatus = if (!hasFailed && !isAbsentAny && percentage >= 33.0 && overallMax > 0) "PASS" else "FAIL"

                val grade = when {
                    percentage >= 90.0 -> "A+"
                    percentage >= 80.0 -> "A"
                    percentage >= 65.0 -> "B+"
                    percentage >= 50.0 -> "B"
                    percentage >= 33.0 -> "C"
                    else -> "D / F"
                }

                reports.add(
                    ConsolidatedStudentReport(
                        student = student,
                        academicSession = academicSession,
                        subjectMarksList = subjectMarksList,
                        unit1TotalObtained = unit1ObtSum,
                        unit1TotalMax = unit1MaxSum,
                        halfYearlyTotalObtained = halfYearlyObtSum,
                        halfYearlyTotalMax = halfYearlyMaxSum,
                        unit2TotalObtained = unit2ObtSum,
                        unit2TotalMax = unit2MaxSum,
                        yearlyTotalObtained = yearlyObtSum,
                        yearlyTotalMax = yearlyMaxSum,
                        overallObtained = overallObtained,
                        overallMax = overallMax,
                        percentage = percentage,
                        grade = grade,
                        passStatus = passStatus,
                        rank = 1
                    )
                )
            }

            val sortedByMarks = reports.sortedByDescending { it.overallObtained }
            val ranked = sortedByMarks.mapIndexed { index, report ->
                report.copy(rank = index + 1)
            }

            ranked.sortedBy { it.student.srNumber.trim().toLongOrNull() ?: Long.MAX_VALUE }
        }
    }
}

data class SubjectConsolidatedMarks(
    val subjectName: String,
    val unit1: String,
    val halfYearly: String,
    val unit2: String,
    val yearly: String,
    val subjectTotalObtained: Double,
    val subjectTotalMax: Int
)

data class ConsolidatedStudentReport(
    val student: ExamStudentEntity,
    val academicSession: String,
    val subjectMarksList: List<SubjectConsolidatedMarks>,
    val unit1TotalObtained: Double,
    val unit1TotalMax: Int,
    val halfYearlyTotalObtained: Double,
    val halfYearlyTotalMax: Int,
    val unit2TotalObtained: Double,
    val unit2TotalMax: Int,
    val yearlyTotalObtained: Double,
    val yearlyTotalMax: Int,
    val overallObtained: Double,
    val overallMax: Int,
    val percentage: Double,
    val grade: String,
    val passStatus: String,
    val rank: Int
)

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

