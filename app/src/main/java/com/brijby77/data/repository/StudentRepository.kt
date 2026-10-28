package com.brijby77.data.repository

import com.brijby77.data.dao.BankDetailsDao
import com.brijby77.data.dao.DocumentDao
import com.brijby77.data.dao.HealthDetailsDao
import com.brijby77.data.dao.HeightWeightHistoryDao
import com.brijby77.data.dao.MdmReportDao
import com.brijby77.data.dao.StudentDao
import com.brijby77.data.entity.BankDetailsEntity
import com.brijby77.data.entity.DocumentEntity
import com.brijby77.data.entity.HealthDetailsEntity
import com.brijby77.data.entity.HeightWeightHistoryEntity
import com.brijby77.data.entity.MdmReportEntity
import com.brijby77.data.entity.StudentEntity
import com.brijby77.data.model.CategoryStat
import com.brijby77.data.model.DashboardStats
import com.brijby77.data.model.StudentWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudentRepository(
    private val studentDao: StudentDao,
    private val healthDetailsDao: HealthDetailsDao,
    private val heightWeightHistoryDao: HeightWeightHistoryDao,
    private val bankDetailsDao: BankDetailsDao,
    private val mdmReportDao: MdmReportDao,
    private val documentDao: DocumentDao? = null
) {

    val allActiveStudents: Flow<List<StudentWithDetails>> = studentDao.getAllActiveStudentsWithDetails()
    val mainClassActiveStudents: Flow<List<StudentWithDetails>> = studentDao.getMainClassActiveStudentsWithDetails()
    val cl0AndPassStudents: Flow<List<StudentWithDetails>> = studentDao.getCl0AndPassStudents()
    val trashStudents: Flow<List<StudentWithDetails>> = studentDao.getTrashStudents()
    val mdmReports: Flow<List<MdmReportEntity>> = mdmReportDao.getAllMdmReports()

    fun getStudentsByClass(studentClass: String): Flow<List<StudentWithDetails>> {
        return studentDao.getActiveStudentsByClass(studentClass)
    }

    suspend fun getStudentWithDetailsById(id: Long): StudentWithDetails? {
        return withContext(Dispatchers.IO) {
            studentDao.getStudentWithDetailsById(id)
        }
    }

    suspend fun saveStudent(
        student: StudentEntity,
        healthDetails: HealthDetailsEntity?,
        heightWeightHistory: List<HeightWeightHistoryEntity>,
        bankDetails: BankDetailsEntity?,
        documents: List<DocumentEntity> = emptyList()
    ): Long {
        return withContext(Dispatchers.IO) {
            val studentId = if (student.id > 0) {
                studentDao.updateStudent(student)
                student.id
            } else {
                studentDao.insertStudent(student)
            }

            // Health details
            if (healthDetails != null && healthDetails.hasHealthDetails) {
                healthDetailsDao.insertHealthDetails(healthDetails.copy(studentId = studentId))
            } else {
                healthDetailsDao.deleteHealthDetailsByStudentId(studentId)
            }

            // Height/Weight history
            heightWeightHistoryDao.deleteHistoryByStudentId(studentId)
            if (healthDetails != null && healthDetails.hasHealthDetails && heightWeightHistory.isNotEmpty()) {
                val updatedHistory = heightWeightHistory.map { it.copy(studentId = studentId) }
                heightWeightHistoryDao.insertHeightWeightHistory(updatedHistory)
            }

            // Bank details
            if (bankDetails != null && bankDetails.hasBankDetails) {
                bankDetailsDao.insertBankDetails(bankDetails.copy(studentId = studentId))
            } else {
                bankDetailsDao.deleteBankDetailsByStudentId(studentId)
            }

            // Documents
            if (documentDao != null) {
                documentDao.deleteAllDocumentsForStudent(studentId)
                if (documents.isNotEmpty()) {
                    val updatedDocs = documents.map { it.copy(studentId = studentId) }
                    documentDao.insertDocuments(updatedDocs)
                }
            }

            studentId
        }
    }

    suspend fun moveToTrash(id: Long) {
        withContext(Dispatchers.IO) {
            studentDao.softDeleteStudent(id, System.currentTimeMillis())
        }
    }

    suspend fun restoreFromTrash(id: Long) {
        withContext(Dispatchers.IO) {
            studentDao.restoreStudent(id)
        }
    }

    suspend fun deletePermanently(id: Long) {
        withContext(Dispatchers.IO) {
            studentDao.deleteStudentByIdPermanently(id)
        }
    }

    suspend fun emptyTrash() {
        withContext(Dispatchers.IO) {
            studentDao.emptyTrash()
        }
    }

    suspend fun saveMdmReports(reports: List<MdmReportEntity>) {
        withContext(Dispatchers.IO) {
            mdmReportDao.insertOrUpdateMdmReports(reports)
        }
    }

    suspend fun checkDuplicateSrNumber(srNumber: String): Boolean {
        if (srNumber.isBlank()) return false
        return withContext(Dispatchers.IO) {
            studentDao.checkDuplicateSrNumber(srNumber.trim()) > 0
        }
    }

    suspend fun checkDuplicateUniqueId(uniqueId: String): Boolean {
        if (uniqueId.isBlank()) return false
        return withContext(Dispatchers.IO) {
            studentDao.checkDuplicateUniqueId(uniqueId.trim()) > 0
        }
    }

    suspend fun getAllActiveStudentsSync(): List<StudentEntity> {
        return withContext(Dispatchers.IO) {
            studentDao.getAllActiveStudentsSync()
        }
    }

    fun getDashboardStats(selectedClassFilter: String): Flow<DashboardStats> {
        return studentDao.getAllActiveStudentsWithDetails().map { allStudents ->
            val classCountsMap = mutableMapOf<String, Int>()
            var classes1To8Total = 0

            for (item in allStudents) {
                val classKey = normalizeClassKey(item.student.studentClass)
                classCountsMap[classKey] = (classCountsMap[classKey] ?: 0) + 1
                if (classKey in listOf("CL 1", "CL 2", "CL 3", "CL 4", "CL 5", "CL 6", "CL 7", "CL 8")) {
                    classes1To8Total++
                }
            }
            classCountsMap["TOTAL"] = classes1To8Total
            classCountsMap["ALL CL"] = classes1To8Total

            val filteredList = if (selectedClassFilter == "ALL CL") {
                allStudents.filter {
                    val key = normalizeClassKey(it.student.studentClass)
                    key in listOf("CL 1", "CL 2", "CL 3", "CL 4", "CL 5", "CL 6", "CL 7", "CL 8")
                }
            } else {
                val targetKey = normalizeClassKey(selectedClassFilter)
                allStudents.filter {
                    normalizeClassKey(it.student.studentClass) == targetKey
                }
            }

            calculateStatsFromList(filteredList, selectedClassFilter, classCountsMap)
        }
    }

    private fun normalizeClassKey(rawClass: String): String {
        val trimmed = rawClass.trim()
        if (trimmed.equals("CL Pass", true) || trimmed.equals("Class Pass", true) || trimmed.equals("Pass", true)) {
            return "CL Pass"
        }
        val num = trimmed.replace("Class ", "").replace("CL ", "").trim()
        return when (num) {
            "0" -> "CL 0"
            "1" -> "CL 1"
            "2" -> "CL 2"
            "3" -> "CL 3"
            "4" -> "CL 4"
            "5" -> "CL 5"
            "6" -> "CL 6"
            "7" -> "CL 7"
            "8" -> "CL 8"
            else -> "CL $num"
        }
    }

    private fun calculateStatsFromList(
        list: List<StudentWithDetails>,
        selectedClassFilter: String,
        classCountsMap: Map<String, Int>
    ): DashboardStats {
        var total = 0
        var male = 0
        var female = 0

        var genTotal = 0
        var genMale = 0
        var genFemale = 0

        var obcTotal = 0
        var obcMale = 0
        var obcFemale = 0

        var scTotal = 0
        var scMale = 0
        var scFemale = 0

        var stTotal = 0
        var stMale = 0
        var stFemale = 0

        var minorityTotal = 0
        var minorityMale = 0
        var minorityFemale = 0

        var aadharCount = 0
        var aaparCount = 0
        var dbtCount = 0

        for (item in list) {
            val student = item.student
            total++
            val isMale = student.gender.equals("Male", ignoreCase = true)
            if (isMale) male++ else female++

            if (student.studentAdhar.isNotBlank()) aadharCount++
            if (student.aaparId.isNotBlank()) aaparCount++
            if (student.dbt.equals("Yes", ignoreCase = true)) dbtCount++

            when (student.caste.uppercase().trim()) {
                "GEN" -> {
                    genTotal++
                    if (isMale) genMale++ else genFemale++
                }
                "OBC" -> {
                    obcTotal++
                    if (isMale) obcMale++ else obcFemale++
                }
                "SC" -> {
                    scTotal++
                    if (isMale) scMale++ else scFemale++
                }
                "ST" -> {
                    stTotal++
                    if (isMale) stMale++ else stFemale++
                }
                "MINORITY" -> {
                    minorityTotal++
                    if (isMale) minorityMale++ else minorityFemale++
                }
            }
        }

        return DashboardStats(
            totalStudents = total,
            totalMale = male,
            totalFemale = female,
            genStats = CategoryStat("GEN", genTotal, genMale, genFemale),
            obcStats = CategoryStat("OBC", obcTotal, obcMale, obcFemale),
            scStats = CategoryStat("SC", scTotal, scMale, scFemale),
            stStats = CategoryStat("ST", stTotal, stMale, stFemale),
            minorityStats = CategoryStat("MINORITY", minorityTotal, minorityMale, minorityFemale),
            aadharCount = aadharCount,
            aaparCount = aaparCount,
            dbtCount = dbtCount,
            selectedClassFilter = selectedClassFilter,
            classCounts = classCountsMap
        )
    }
}

