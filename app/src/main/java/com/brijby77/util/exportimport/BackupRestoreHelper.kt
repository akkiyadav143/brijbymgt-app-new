package com.brijby77.util.exportimport

import android.content.Context
import android.net.Uri
import com.brijby77.data.entity.BankDetailsEntity
import com.brijby77.data.entity.DocumentEntity
import com.brijby77.data.entity.HealthDetailsEntity
import com.brijby77.data.entity.HeightWeightHistoryEntity
import com.brijby77.data.entity.StudentEntity
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreHelper {

    enum class BackupScope {
        ALL_STUDENTS,
        SELECTED_CLASS,
        CUSTOM_SELECTED
    }

    enum class ConflictResolution {
        SKIP,
        UPDATE_EXISTING,
        RESTORE_AS_NEW
    }

    data class RestorePreview(
        val isValid: Boolean,
        val errorMessage: String?,
        val manifestVersion: Int,
        val createdAt: Long,
        val studentCount: Int,
        val photoCount: Int,
        val attachmentCount: Int,
        val conflictCount: Int,
        val conflicts: List<ConflictInfo>
    )

    data class ConflictInfo(
        val backupStudentId: Long,
        val studentName: String,
        val srNumber: String,
        val uniqueId: String,
        val penNo: String,
        val aaparId: String,
        val studentAdhar: String,
        val matchedField: String,
        val existingStudentId: Long,
        val existingStudentName: String
    )

    data class BackupPayload(
        val studentWithDetails: StudentWithDetails,
        val photoFiles: Map<String, File>,
        val attachmentFiles: List<Pair<DocumentEntity, File>>
    )

    // 1. EXPORT BACKUP ZIP
    suspend fun exportBackupZip(
        context: Context,
        destinationUri: Uri,
        allStudents: List<StudentWithDetails>,
        scope: BackupScope,
        selectedClass: String?,
        customSelectedIds: List<Long>?,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val studentsToBackup = when (scope) {
                    BackupScope.ALL_STUDENTS -> allStudents
                    BackupScope.SELECTED_CLASS -> {
                        if (selectedClass.isNullOrBlank()) allStudents
                        else allStudents.filter {
                            val c = it.student.studentClass
                            val rawC = c.replace("Class ", "").replace("CL ", "").trim()
                            val rawTarget = selectedClass.replace("Class ", "").replace("CL ", "").trim()
                            c.equals(selectedClass, true) || rawC.equals(rawTarget, true)
                        }
                    }
                    BackupScope.CUSTOM_SELECTED -> {
                        val ids = customSelectedIds ?: emptyList()
                        allStudents.filter { ids.contains(it.student.id) }
                    }
                }

                if (studentsToBackup.isEmpty()) return@withContext false

                val outputStream = context.contentResolver.openOutputStream(destinationUri) ?: return@withContext false
                val zipOut = ZipOutputStream(outputStream)

                var photoCount = 0
                var attachmentCount = 0
                val totalSteps = studentsToBackup.size
                var currentStep = 0

                val payloads = mutableListOf<BackupPayload>()
                for (item in studentsToBackup) {
                    val photoMap = mutableMapOf<String, File>()

                    item.student.imagePath?.let { path ->
                        if (path.isNotBlank()) {
                            val f = File(path)
                            if (f.exists()) {
                                photoMap["photo_1"] = f
                                photoCount++
                            }
                        }
                    }
                    item.student.fatherImagePath?.let { path ->
                        if (path.isNotBlank()) {
                            val f = File(path)
                            if (f.exists()) {
                                photoMap["photo_2"] = f
                                photoCount++
                            }
                        }
                    }
                    item.student.motherImagePath?.let { path ->
                        if (path.isNotBlank()) {
                            val f = File(path)
                            if (f.exists()) {
                                photoMap["photo_3"] = f
                                photoCount++
                            }
                        }
                    }

                    val attachments = mutableListOf<Pair<DocumentEntity, File>>()
                    for (doc in item.documents) {
                        if (doc.fileUri.isNotBlank()) {
                            val f = File(doc.fileUri)
                            if (f.exists()) {
                                attachments.add(doc to f)
                                attachmentCount++
                            }
                        }
                    }

                    payloads.add(BackupPayload(item, photoMap, attachments))
                }

                val manifestJson = JSONObject().apply {
                    put("version", 1)
                    put("backupVersion", "1.0")
                    put("createdAt", System.currentTimeMillis())
                    put("creationTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    put("studentCount", studentsToBackup.size)
                    put("photoCount", photoCount)
                    put("attachmentCount", attachmentCount)
                    put("scope", scope.name)
                }

                zipOut.putNextEntry(ZipEntry("manifest.json"))
                zipOut.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                for (payload in payloads) {
                    currentStep++
                    val student = payload.studentWithDetails.student
                    onProgress(currentStep, totalSteps, "Backing up: ${student.studentName}")

                    val studentBaseDir = "students/${student.id}"

                    val studentObj = JSONObject().apply {
                        put("student", studentToJson(student))
                        put("healthDetails", healthToJson(payload.studentWithDetails.healthDetails))
                        put("bankDetails", bankToJson(payload.studentWithDetails.bankDetails))

                        val historyArr = JSONArray()
                        for (h in payload.studentWithDetails.heightWeightHistory) {
                            historyArr.put(historyToJson(h))
                        }
                        put("heightWeightHistory", historyArr)

                        val docsArr = JSONArray()
                        for (d in payload.studentWithDetails.documents) {
                            docsArr.put(documentToJson(d))
                        }
                        put("documents", docsArr)
                    }

                    zipOut.putNextEntry(ZipEntry("$studentBaseDir/student.json"))
                    zipOut.write(studentObj.toString(2).toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    for ((slotName, file) in payload.photoFiles) {
                        val entryName = "$studentBaseDir/photos/$slotName${getExtension(file.name)}"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { input -> input.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }

                    for ((doc, file) in payload.attachmentFiles) {
                        val sanitizedType = doc.documentType.replace("[^a-zA-Z0-9]".toRegex(), "_")
                        val entryName = "$studentBaseDir/attachments/${sanitizedType}_${file.name}"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { input -> input.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }

                zipOut.flush()
                zipOut.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // 2. PREVIEW RESTORE ZIP
    suspend fun previewRestoreZip(
        context: Context,
        sourceUri: Uri,
        repository: StudentRepository
    ): RestorePreview {
        return withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "preview_temp_${System.currentTimeMillis()}")
            try {
                tempDir.mkdirs()

                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            val outFile = File(tempDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { out ->
                                    zipIn.copyTo(out)
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                } ?: return@withContext RestorePreview(false, "Could not open backup file", 0, 0L, 0, 0, 0, 0, emptyList())

                val manifestFile = File(tempDir, "manifest.json")
                if (!manifestFile.exists()) {
                    tempDir.deleteRecursively()
                    return@withContext RestorePreview(false, "Invalid backup: missing manifest.json", 0, 0L, 0, 0, 0, 0, emptyList())
                }

                val manifestObj = JSONObject(manifestFile.readText())
                val version = manifestObj.optInt("version", 1)
                val createdAt = manifestObj.optLong("createdAt", 0L)

                val studentsDir = File(tempDir, "students")
                val studentFolders = if (studentsDir.exists() && studentsDir.isDirectory) studentsDir.listFiles() ?: emptyArray() else emptyArray()
                val studentCount = studentFolders.size

                var photoCount = 0
                var attachmentCount = 0

                val existingStudents = repository.getAllActiveStudentsSync()
                val conflicts = mutableListOf<ConflictInfo>()

                for (folder in studentFolders) {
                    if (!folder.isDirectory) continue
                    val photosDir = File(folder, "photos")
                    if (photosDir.exists()) photoCount += (photosDir.listFiles()?.size ?: 0)

                    val attDir = File(folder, "attachments")
                    if (attDir.exists()) attachmentCount += (attDir.listFiles()?.size ?: 0)

                    val studentJsonFile = File(folder, "student.json")
                    if (!studentJsonFile.exists()) continue

                    val rootObj = JSONObject(studentJsonFile.readText())
                    val sObj = rootObj.optJSONObject("student") ?: continue
                    val backupId = sObj.optLong("id", 0)
                    val name = sObj.optString("studentName", "")
                    val srNum = sObj.optString("srNumber", "").trim()
                    val uid = sObj.optString("uniqueId", "").trim()
                    val pen = sObj.optString("penNo", "").trim()
                    val aapar = sObj.optString("aaparId", "").trim()
                    val adhar = sObj.optString("studentAdhar", "").trim()

                    for (existing in existingStudents) {
                        var matchedField: String? = null
                        if (uid.isNotBlank() && existing.uniqueId.isNotBlank() && uid.equals(existing.uniqueId, true)) {
                            matchedField = "Unique ID ($uid)"
                        } else if (pen.isNotBlank() && existing.penNo.isNotBlank() && pen.equals(existing.penNo, true)) {
                            matchedField = "PEN No ($pen)"
                        } else if (srNum.isNotBlank() && existing.srNumber.isNotBlank() && srNum.equals(existing.srNumber, true)) {
                            matchedField = "SR Number ($srNum)"
                        } else if (aapar.isNotBlank() && existing.aaparId.isNotBlank() && aapar.equals(existing.aaparId, true)) {
                            matchedField = "AAPAR ID ($aapar)"
                        } else if (adhar.isNotBlank() && existing.studentAdhar.isNotBlank() && adhar.equals(existing.studentAdhar, true)) {
                            matchedField = "Student Aadhaar ($adhar)"
                        }

                        if (matchedField != null) {
                            conflicts.add(
                                ConflictInfo(
                                    backupStudentId = backupId,
                                    studentName = name,
                                    srNumber = srNum,
                                    uniqueId = uid,
                                    penNo = pen,
                                    aaparId = aapar,
                                    studentAdhar = adhar,
                                    matchedField = matchedField,
                                    existingStudentId = existing.id,
                                    existingStudentName = existing.studentName
                                )
                            )
                            break
                        }
                    }
                }

                tempDir.deleteRecursively()
                RestorePreview(
                    isValid = true,
                    errorMessage = null,
                    manifestVersion = version,
                    createdAt = createdAt,
                    studentCount = studentCount,
                    photoCount = photoCount,
                    attachmentCount = attachmentCount,
                    conflictCount = conflicts.size,
                    conflicts = conflicts
                )
            } catch (e: Exception) {
                e.printStackTrace()
                RestorePreview(false, "Error reading backup ZIP: ${e.localizedMessage}", 0, 0L, 0, 0, 0, 0, emptyList())
            }
        }
    }

    // 3. EXECUTE RESTORE ZIP
    suspend fun importBackupZip(
        context: Context,
        sourceUri: Uri,
        repository: StudentRepository,
        conflictResolution: ConflictResolution,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            try {
                tempDir.mkdirs()

                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            val outFile = File(tempDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { out ->
                                    zipIn.copyTo(out)
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                } ?: return@withContext false

                val studentsDir = File(tempDir, "students")
                if (!studentsDir.exists() || !studentsDir.isDirectory) {
                    tempDir.deleteRecursively()
                    return@withContext false
                }

                val studentFolders = studentsDir.listFiles() ?: emptyArray()
                val existingStudents = repository.getAllActiveStudentsSync()
                val totalSteps = studentFolders.size
                var currentStep = 0

                for (folder in studentFolders) {
                    if (!folder.isDirectory) continue
                    currentStep++
                    onProgress(currentStep, totalSteps, "Restoring student records...")

                    val studentJsonFile = File(folder, "student.json")
                    if (!studentJsonFile.exists()) continue

                    val jsonStr = studentJsonFile.readText()
                    val rootObj = JSONObject(jsonStr)

                    val studentEntity = jsonToStudent(rootObj.optJSONObject("student") ?: continue)
                    val healthObj = rootObj.optJSONObject("healthDetails")
                    val bankObj = rootObj.optJSONObject("bankDetails")
                    val historyArr = rootObj.optJSONArray("heightWeightHistory") ?: JSONArray()
                    val docsArr = rootObj.optJSONArray("documents") ?: JSONArray()

                    val uid = studentEntity.uniqueId.trim()
                    val pen = studentEntity.penNo.trim()
                    val srNum = studentEntity.srNumber.trim()
                    val aapar = studentEntity.aaparId.trim()
                    val adhar = studentEntity.studentAdhar.trim()

                    var existingMatch: StudentEntity? = null
                    for (existing in existingStudents) {
                        if (uid.isNotBlank() && existing.uniqueId.isNotBlank() && uid.equals(existing.uniqueId, true)) {
                            existingMatch = existing
                            break
                        }
                        if (pen.isNotBlank() && existing.penNo.isNotBlank() && pen.equals(existing.penNo, true)) {
                            existingMatch = existing
                            break
                        }
                        if (srNum.isNotBlank() && existing.srNumber.isNotBlank() && srNum.equals(existing.srNumber, true)) {
                            existingMatch = existing
                            break
                        }
                        if (aapar.isNotBlank() && existing.aaparId.isNotBlank() && aapar.equals(existing.aaparId, true)) {
                            existingMatch = existing
                            break
                        }
                        if (adhar.isNotBlank() && existing.studentAdhar.isNotBlank() && adhar.equals(existing.studentAdhar, true)) {
                            existingMatch = existing
                            break
                        }
                    }

                    val resolution = if (existingMatch != null) conflictResolution else ConflictResolution.RESTORE_AS_NEW

                    if (resolution == ConflictResolution.SKIP) {
                        continue
                    }

                    val targetStudentId = if (resolution == ConflictResolution.UPDATE_EXISTING && existingMatch != null) {
                        existingMatch.id
                    } else {
                        0L
                    }

                    val photosDir = File(folder, "photos")
                    var restoredImagePath: String? = if (resolution == ConflictResolution.UPDATE_EXISTING && existingMatch != null) existingMatch.imagePath else null
                    var restoredFatherImagePath: String? = if (resolution == ConflictResolution.UPDATE_EXISTING && existingMatch != null) existingMatch.fatherImagePath else null
                    var restoredMotherImagePath: String? = if (resolution == ConflictResolution.UPDATE_EXISTING && existingMatch != null) existingMatch.motherImagePath else null

                    if (photosDir.exists()) {
                        val photoFiles = photosDir.listFiles() ?: emptyArray()
                        val imagesDir = File(context.filesDir, "student_images")
                        if (!imagesDir.exists()) imagesDir.mkdirs()

                        for (pfile in photoFiles) {
                            val pName = pfile.name.lowercase()
                            val destFile = File(imagesDir, "img_${UUID.randomUUID()}.jpg")
                            pfile.copyTo(destFile, overwrite = true)
                            val destPath = destFile.absolutePath

                            if (pName.contains("photo_1")) {
                                restoredImagePath = destPath
                            } else if (pName.contains("photo_2")) {
                                restoredFatherImagePath = destPath
                            } else if (pName.contains("photo_3")) {
                                restoredMotherImagePath = destPath
                            }
                        }
                    }

                    val finalStudent = studentEntity.copy(
                        id = targetStudentId,
                        imagePath = restoredImagePath,
                        fatherImagePath = restoredFatherImagePath,
                        motherImagePath = restoredMotherImagePath
                    )

                    val healthDetails = jsonToHealth(healthObj, targetStudentId)
                    val bankDetails = jsonToBank(bankObj, targetStudentId)

                    val historyList = mutableListOf<HeightWeightHistoryEntity>()
                    for (i in 0 until historyArr.length()) {
                        historyList.add(jsonToHistory(historyArr.getJSONObject(i), targetStudentId))
                    }

                    val savedStudentId = repository.saveStudent(
                        finalStudent,
                        healthDetails,
                        historyList,
                        bankDetails,
                        emptyList()
                    )

                    val attachmentsDir = File(folder, "attachments")
                    val restoredDocuments = mutableListOf<DocumentEntity>()

                    if (attachmentsDir.exists()) {
                        val attFiles = attachmentsDir.listFiles() ?: emptyArray()
                        for (afile in attFiles) {
                            val fileName = afile.name
                            var matchedDocObj: JSONObject? = null
                            for (i in 0 until docsArr.length()) {
                                val dObj = docsArr.getJSONObject(i)
                                val dName = dObj.optString("fileName", "")
                                if (fileName.contains(dName, true)) {
                                    matchedDocObj = dObj
                                    break
                                }
                            }

                            val docType = matchedDocObj?.optString("documentType", "Other Document") ?: "Other Document"
                            val sanitizedType = docType.replace("[^a-zA-Z0-9]".toRegex(), "_")
                            val studentDocsDir = File(context.filesDir, "StudentDocuments/Student_$savedStudentId/$sanitizedType")
                            if (!studentDocsDir.exists()) studentDocsDir.mkdirs()

                            val destDocFile = File(studentDocsDir, "${System.currentTimeMillis()}_$fileName")
                            afile.copyTo(destDocFile, overwrite = true)

                            val mimeType = matchedDocObj?.optString("mimeType", "application/octet-stream") ?: "application/octet-stream"
                            val fileSize = destDocFile.length()

                            val docEntity = DocumentEntity(
                                id = 0,
                                studentId = savedStudentId,
                                documentType = docType,
                                fileName = fileName,
                                fileUri = destDocFile.absolutePath,
                                mimeType = mimeType,
                                fileSize = fileSize,
                                createdAt = matchedDocObj?.optLong("createdAt", System.currentTimeMillis()) ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            restoredDocuments.add(docEntity)
                        }
                    }

                    if (restoredDocuments.isNotEmpty()) {
                        val studentWithNewId = finalStudent.copy(id = savedStudentId)
                        repository.saveStudent(
                            studentWithNewId,
                            healthDetails,
                            historyList,
                            bankDetails,
                            restoredDocuments
                        )
                    }
                }

                tempDir.deleteRecursively()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                tempDir.deleteRecursively()
                false
            }
        }
    }

    // HELPER JSON SERIALIZATION
    private fun studentToJson(s: StudentEntity): JSONObject {
        return JSONObject().apply {
            put("id", s.id)
            put("srNo", s.srNo)
            put("srNumber", s.srNumber)
            put("uniqueId", s.uniqueId)
            put("penNo", s.penNo)
            put("aaparId", s.aaparId)
            put("dbt", s.dbt)
            put("studentClass", s.studentClass)
            put("studentName", s.studentName)
            put("dob", s.dob)
            put("admissionDate", s.admissionDate)
            put("studentAdhar", s.studentAdhar)
            put("co", s.co)
            put("gender", s.gender)
            put("caste", s.caste)
            put("fatherName", s.fatherName)
            put("fatherAdhar", s.fatherAdhar)
            put("motherName", s.motherName)
            put("motherAdhar", s.motherAdhar)
            put("mobileNo", s.mobileNo)
            put("address", s.address)
            put("other", s.other)
            put("rationCard", s.rationCard)
            put("isDeleted", s.isDeleted)
            put("deletedAt", if (s.deletedAt != null) s.deletedAt!! else JSONObject.NULL)
        }
    }

    private fun jsonToStudent(obj: JSONObject): StudentEntity {
        return StudentEntity(
            id = 0,
            srNo = obj.optString("srNo", ""),
            srNumber = obj.optString("srNumber", ""),
            uniqueId = obj.optString("uniqueId", ""),
            penNo = obj.optString("penNo", ""),
            aaparId = obj.optString("aaparId", ""),
            dbt = obj.optString("dbt", "No"),
            studentClass = obj.optString("studentClass", "Class 1"),
            studentName = obj.optString("studentName", ""),
            dob = obj.optString("dob", ""),
            admissionDate = obj.optString("admissionDate", ""),
            studentAdhar = obj.optString("studentAdhar", ""),
            co = obj.optString("co", ""),
            gender = obj.optString("gender", "Male"),
            caste = obj.optString("caste", "GEN"),
            fatherName = obj.optString("fatherName", ""),
            fatherAdhar = obj.optString("fatherAdhar", ""),
            motherName = obj.optString("motherName", ""),
            motherAdhar = obj.optString("motherAdhar", ""),
            mobileNo = obj.optString("mobileNo", ""),
            address = obj.optString("address", ""),
            other = obj.optString("other", ""),
            rationCard = obj.optString("rationCard", ""),
            isDeleted = obj.optBoolean("isDeleted", false),
            deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.optLong("deletedAt") else null
        )
    }

    private fun healthToJson(h: HealthDetailsEntity?): JSONObject? {
        if (h == null) return null
        return JSONObject().apply {
            put("hasHealthDetails", h.hasHealthDetails)
            put("height", h.height)
            put("weight", h.weight)
            put("bloodGroup", h.bloodGroup)
            put("hb", h.hb)
            put("eyeSight", h.eyeSight)
            put("deworming", h.deworming)
        }
    }

    private fun jsonToHealth(obj: JSONObject?, studentId: Long): HealthDetailsEntity? {
        if (obj == null) return null
        return HealthDetailsEntity(
            studentId = studentId,
            hasHealthDetails = obj.optBoolean("hasHealthDetails", false),
            height = obj.optString("height", ""),
            weight = obj.optString("weight", ""),
            bloodGroup = obj.optString("bloodGroup", ""),
            hb = obj.optString("hb", ""),
            eyeSight = obj.optString("eyeSight", ""),
            deworming = obj.optString("deworming", "")
        )
    }

    private fun bankToJson(b: BankDetailsEntity?): JSONObject? {
        if (b == null) return null
        return JSONObject().apply {
            put("hasBankDetails", b.hasBankDetails)
            put("acHolderName", b.acHolderName)
            put("acNo", b.acNo)
            put("ifscCode", b.ifscCode)
            put("branchName", b.branchName)
        }
    }

    private fun jsonToBank(obj: JSONObject?, studentId: Long): BankDetailsEntity? {
        if (obj == null) return null
        return BankDetailsEntity(
            studentId = studentId,
            hasBankDetails = obj.optBoolean("hasBankDetails", false),
            acHolderName = obj.optString("acHolderName", ""),
            acNo = obj.optString("acNo", ""),
            ifscCode = obj.optString("ifscCode", ""),
            branchName = obj.optString("branchName", "")
        )
    }

    private fun historyToJson(h: HeightWeightHistoryEntity): JSONObject {
        return JSONObject().apply {
            put("datePeriod", h.datePeriod)
            put("height", h.height)
            put("weight", h.weight)
        }
    }

    private fun jsonToHistory(obj: JSONObject, studentId: Long): HeightWeightHistoryEntity {
        return HeightWeightHistoryEntity(
            id = 0,
            studentId = studentId,
            datePeriod = obj.optString("datePeriod", ""),
            height = obj.optString("height", ""),
            weight = obj.optString("weight", "")
        )
    }

    private fun documentToJson(d: DocumentEntity): JSONObject {
        return JSONObject().apply {
            put("documentType", d.documentType)
            put("fileName", d.fileName)
            put("mimeType", d.mimeType)
            put("fileSize", d.fileSize)
            put("createdAt", d.createdAt)
            put("updatedAt", d.updatedAt)
        }
    }

    private fun getExtension(fileName: String): String {
        val idx = fileName.lastIndexOf('.')
        return if (idx != -1) fileName.substring(idx) else ".jpg"
    }
}

