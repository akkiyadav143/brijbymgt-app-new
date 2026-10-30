package com.brijbymgt.data.repository

import android.content.Context
import android.util.Log
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.dao.BankDetailsDao
import com.brijbymgt.data.dao.DocumentDao
import com.brijbymgt.data.dao.HealthDetailsDao
import com.brijbymgt.data.dao.HeightWeightHistoryDao
import com.brijbymgt.data.dao.MdmReportDao
import com.brijbymgt.data.dao.StudentDao
import com.brijbymgt.data.entity.BankDetailsEntity
import com.brijbymgt.data.entity.DocumentEntity
import com.brijbymgt.data.entity.HealthDetailsEntity
import com.brijbymgt.data.entity.HeightWeightHistoryEntity
import com.brijbymgt.data.entity.MdmReportEntity
import com.brijbymgt.data.entity.StudentEntity
import com.brijbymgt.data.model.CategoryStat
import com.brijbymgt.data.model.DashboardStats
import com.brijbymgt.data.model.StudentWithDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentRepository(
    private val studentDao: StudentDao,
    private val healthDetailsDao: HealthDetailsDao,
    private val heightWeightHistoryDao: HeightWeightHistoryDao,
    private val bankDetailsDao: BankDetailsDao,
    private val mdmReportDao: MdmReportDao,
    private val documentDao: DocumentDao? = null,
    private val context: Context? = null
) {

    val allActiveStudents: Flow<List<StudentWithDetails>> = studentDao.getAllActiveStudentsWithDetails()
    val mainClassActiveStudents: Flow<List<StudentWithDetails>> = studentDao.getMainClassActiveStudentsWithDetails()
    val cl0AndPassStudents: Flow<List<StudentWithDetails>> = studentDao.getCl0AndPassStudents()
    val trashStudents: Flow<List<StudentWithDetails>> = studentDao.getTrashStudents()
    val mdmReports: Flow<List<MdmReportEntity>> = mdmReportDao.getAllMdmReports()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            syncFromFirestore()
        }
    }

    private fun getSchoolId(): String {
        val ctx = context ?: try { SchoolApp.instance } catch (_: Exception) { null }
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val email = prefs.getString("user_email", null)
            if (!email.isNullOrBlank()) {
                val cleanSchoolId = email.lowercase().trim().replace(".", "_")
                Log.d("StudentRepository", "getSchoolId: Using Email SchoolId = $cleanSchoolId")
                return cleanSchoolId
            }
        }

        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null && !authUser.email.isNullOrBlank()) {
            val cleanSchoolId = authUser.email!!.lowercase().trim().replace(".", "_")
            Log.d("StudentRepository", "getSchoolId: Using Auth Email SchoolId = $cleanSchoolId")
            return cleanSchoolId
        }

        if (authUser != null && !authUser.uid.isBlank()) {
            Log.d("StudentRepository", "getSchoolId: Using Firebase Auth UID = ${authUser.uid}")
            return authUser.uid
        }

        if (ctx != null) {
            val prefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val uid = prefs.getString("user_uid", null)
            if (!uid.isNullOrBlank()) {
                Log.d("StudentRepository", "getSchoolId: Using SharedPreferences UID = $uid")
                return uid
            }
        }

        Log.w("StudentRepository", "getSchoolId: Falling back to default_school")
        return "default_school"
    }

    suspend fun syncFromFirestore() {
        withContext(Dispatchers.IO) {
            try {
                val schoolId = getSchoolId()
                Log.d("StudentRepository", "syncFromFirestore: starting sync for schoolId=$schoolId")
                if (schoolId == "default_school") {
                    Log.w("StudentRepository", "syncFromFirestore: schoolId is default_school, skipping sync")
                    return@withContext
                }
                val firestore = FirebaseFirestore.getInstance()

                // 1. Pull Students from Firestore
                firestore.collection("schools")
                    .document(schoolId)
                    .collection("students")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        Log.d("StudentRepository", "syncFromFirestore: Students query success for $schoolId. Found ${snapshot.size()} documents.")
                        CoroutineScope(Dispatchers.IO).launch {
                            for (doc in snapshot.documents) {
                                try {
                                    val data = doc.data ?: continue
                                    val id = (data["id"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                    if (id == 0L) continue

                                    val isDeleted = data["isDeleted"] as? Boolean ?: false
                                    val deletedAt = (data["deletedAt"] as? Number)?.toLong()

                                    val student = StudentEntity(
                                        id = id,
                                        srNumber = data["srNumber"] as? String ?: "",
                                        uniqueId = data["uniqueId"] as? String ?: "",
                                        studentName = data["studentName"] as? String ?: "",
                                        fatherName = data["fatherName"] as? String ?: "",
                                        motherName = data["motherName"] as? String ?: "",
                                        studentClass = data["studentClass"] as? String ?: "Class 1",
                                        gender = data["gender"] as? String ?: "Male",
                                        caste = data["caste"] as? String ?: "GEN",
                                        dob = data["dob"] as? String ?: "",
                                        mobileNo = data["mobileNo"] as? String ?: "",
                                        studentAdhar = data["studentAdhar"] as? String ?: "",
                                        aaparId = data["aaparId"] as? String ?: "",
                                        dbt = data["dbt"] as? String ?: "No",
                                        address = data["address"] as? String ?: "",
                                        isDeleted = isDeleted,
                                        deletedAt = deletedAt
                                    )

                                    val existing = studentDao.getStudentWithDetailsById(id)
                                    if (existing == null) {
                                        studentDao.insertStudent(student)
                                    } else {
                                        studentDao.updateStudent(student)
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    val healthMap = data["healthDetails"] as? Map<String, Any>
                                    if (healthMap != null && (healthMap["hasHealthDetails"] as? Boolean == true)) {
                                        val healthEntity = HealthDetailsEntity(
                                            studentId = id,
                                            hasHealthDetails = true,
                                            bloodGroup = healthMap["bloodGroup"] as? String ?: "",
                                            height = healthMap["height"] as? String ?: "",
                                            weight = healthMap["weight"] as? String ?: ""
                                        )
                                        healthDetailsDao.insertHealthDetails(healthEntity)
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    val bankMap = data["bankDetails"] as? Map<String, Any>
                                    if (bankMap != null && (bankMap["hasBankDetails"] as? Boolean == true)) {
                                        val bankEntity = BankDetailsEntity(
                                            studentId = id,
                                            hasBankDetails = true,
                                            acNo = bankMap["acNo"] as? String ?: "",
                                            ifscCode = bankMap["ifscCode"] as? String ?: "",
                                            branchName = bankMap["branchName"] as? String ?: "",
                                            acHolderName = bankMap["acHolderName"] as? String ?: ""
                                        )
                                        bankDetailsDao.insertBankDetails(bankEntity)
                                    }

                                    Log.d("StudentRepository", "Pulled student $id (isDeleted=$isDeleted) from Firestore successfully")
                                } catch (e: Exception) {
                                    Log.e("StudentRepository", "Error parsing pulled student: ${e.message}", e)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentRepository", "Failed to pull students from Firestore for $schoolId: ${e.message}", e)
                    }

                // 2. Pull MDM Reports from Firestore
                firestore.collection("schools")
                    .document(schoolId)
                    .collection("mdm_reports")
                    .document("all_reports")
                    .get()
                    .addOnSuccessListener { doc ->
                        Log.d("StudentRepository", "syncFromFirestore: MDM reports query success for $schoolId. Exists=${doc.exists()}")
                        if (doc.exists()) {
                            @Suppress("UNCHECKED_CAST")
                            val reportsList = doc.get("reports") as? List<Map<String, Any>>
                            if (reportsList != null) {
                                val reports = reportsList.mapNotNull { rMap ->
                                    try {
                                        MdmReportEntity(
                                            studentClass = rMap["studentClass"] as? String ?: "",
                                            mdmMale = (rMap["mdmMale"] as? Number)?.toInt() ?: 0,
                                            mdmFemale = (rMap["mdmFemale"] as? Number)?.toInt() ?: 0
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (reports.isNotEmpty()) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        mdmReportDao.insertOrUpdateMdmReports(reports)
                                        Log.d("StudentRepository", "Pulled ${reports.size} MDM reports from Firestore")
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentRepository", "Failed to pull MDM reports from Firestore for $schoolId: ${e.message}", e)
                    }

            } catch (e: Exception) {
                Log.e("StudentRepository", "syncFromFirestore exception: ${e.message}", e)
            }
        }
    }

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
        val studentId = withContext(Dispatchers.IO) {
            val sId = if (student.id > 0) {
                studentDao.updateStudent(student)
                student.id
            } else {
                studentDao.insertStudent(student)
            }

            // Health details
            if (healthDetails != null && healthDetails.hasHealthDetails) {
                healthDetailsDao.insertHealthDetails(healthDetails.copy(studentId = sId))
            } else {
                healthDetailsDao.deleteHealthDetailsByStudentId(sId)
            }

            // Height/Weight history
            heightWeightHistoryDao.deleteHistoryByStudentId(sId)
            if (healthDetails != null && healthDetails.hasHealthDetails && heightWeightHistory.isNotEmpty()) {
                val updatedHistory = heightWeightHistory.map { it.copy(studentId = sId) }
                heightWeightHistoryDao.insertHeightWeightHistory(updatedHistory)
            }

            // Bank details
            if (bankDetails != null && bankDetails.hasBankDetails) {
                bankDetailsDao.insertBankDetails(bankDetails.copy(studentId = sId))
            } else {
                bankDetailsDao.deleteBankDetailsByStudentId(sId)
            }

            // Documents
            if (documentDao != null) {
                documentDao.deleteAllDocumentsForStudent(sId)
                if (documents.isNotEmpty()) {
                    val updatedDocs = documents.map { it.copy(studentId = sId) }
                    documentDao.insertDocuments(updatedDocs)
                }
            }

            sId
        }

        // Automatic background sync to Firestore (schools/{uid}/students/{studentId})
        withContext(Dispatchers.IO) {
            try {
                val schoolId = getSchoolId()
                val firestore = FirebaseFirestore.getInstance()
                val studentMap = hashMapOf<String, Any>(
                    "id" to studentId,
                    "srNumber" to student.srNumber,
                    "uniqueId" to student.uniqueId,
                    "studentName" to student.studentName,
                    "fatherName" to student.fatherName,
                    "motherName" to student.motherName,
                    "studentClass" to student.studentClass,
                    "gender" to student.gender,
                    "caste" to student.caste,
                    "dob" to student.dob,
                    "mobileNo" to student.mobileNo,
                    "studentAdhar" to student.studentAdhar,
                    "aaparId" to student.aaparId,
                    "dbt" to student.dbt,
                    "address" to student.address,
                    "isDeleted" to student.isDeleted,
                    "deletedAt" to (student.deletedAt ?: 0L),
                    "healthDetails" to (healthDetails?.let {
                        hashMapOf(
                            "hasHealthDetails" to it.hasHealthDetails,
                            "bloodGroup" to it.bloodGroup,
                            "height" to it.height,
                            "weight" to it.weight
                        )
                    } ?: mapOf()),
                    "bankDetails" to (bankDetails?.let {
                        hashMapOf(
                            "hasBankDetails" to it.hasBankDetails,
                            "acNo" to it.acNo,
                            "ifscCode" to it.ifscCode,
                            "branchName" to it.branchName,
                            "acHolderName" to it.acHolderName
                        )
                    } ?: mapOf()),
                    "heightWeightHistory" to heightWeightHistory,
                    "documents" to documents,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("schools")
                    .document(schoolId)
                    .collection("students")
                    .document(studentId.toString())
                    .set(studentMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("StudentRepository", "Firestore sync SUCCESS for student $studentId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentRepository", "Firestore sync FAILED for student $studentId: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("StudentRepository", "Firestore sync Exception: ${e.message}")
            }
        }

        return studentId
    }

    suspend fun moveToTrash(id: Long) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            studentDao.softDeleteStudent(id, timestamp)
            try {
                val schoolId = getSchoolId()
                if (schoolId != "default_school") {
                    FirebaseFirestore.getInstance()
                        .collection("schools")
                        .document(schoolId)
                        .collection("students")
                        .document(id.toString())
                        .update(mapOf("isDeleted" to true, "deletedAt" to timestamp))
                }
            } catch (e: Exception) {
                Log.e("StudentRepository", "moveToTrash Firestore sync error: ${e.message}")
            }
        }
    }

    suspend fun restoreFromTrash(id: Long) {
        withContext(Dispatchers.IO) {
            studentDao.restoreStudent(id)
            try {
                val schoolId = getSchoolId()
                if (schoolId != "default_school") {
                    FirebaseFirestore.getInstance()
                        .collection("schools")
                        .document(schoolId)
                        .collection("students")
                        .document(id.toString())
                        .update(mapOf("isDeleted" to false, "deletedAt" to null))
                }
            } catch (e: Exception) {
                Log.e("StudentRepository", "restoreFromTrash Firestore sync error: ${e.message}")
            }
        }
    }

    suspend fun deletePermanently(id: Long) {
        withContext(Dispatchers.IO) {
            studentDao.deleteStudentByIdPermanently(id)
            try {
                val schoolId = getSchoolId()
                if (schoolId != "default_school") {
                    FirebaseFirestore.getInstance()
                        .collection("schools")
                        .document(schoolId)
                        .collection("students")
                        .document(id.toString())
                        .delete()
                }
            } catch (e: Exception) {
                Log.e("StudentRepository", "deletePermanently Firestore error: ${e.message}")
            }
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

        // Automatic background sync to Firestore (schools/{uid}/mdm_reports/all_reports)
        withContext(Dispatchers.IO) {
            try {
                val schoolId = getSchoolId()
                val firestore = FirebaseFirestore.getInstance()
                val reportsMap = hashMapOf<String, Any>(
                    "reports" to reports.map { r ->
                        hashMapOf(
                            "studentClass" to r.studentClass,
                            "mdmMale" to r.mdmMale,
                            "mdmFemale" to r.mdmFemale
                        )
                    },
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("schools")
                    .document(schoolId)
                    .collection("mdm_reports")
                    .document("all_reports")
                    .set(reportsMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("StudentRepository", "Firestore sync SUCCESS for MDM reports")
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentRepository", "Firestore sync FAILED for MDM reports: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("StudentRepository", "Firestore sync Exception for MDM reports: ${e.message}")
            }
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

        val stTotal = 0
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
                    // stTotal++ handled or can be incremented if needed
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
            stStats = CategoryStat("ST", stTotal + stMale + stFemale, stMale, stFemale),
            minorityStats = CategoryStat("MINORITY", minorityTotal, minorityMale, minorityFemale),
            aadharCount = aadharCount,
            aaparCount = aaparCount,
            dbtCount = dbtCount,
            selectedClassFilter = selectedClassFilter,
            classCounts = classCountsMap
        )
    }
}
