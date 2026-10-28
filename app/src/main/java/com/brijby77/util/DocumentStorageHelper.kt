package com.brijby77.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object DocumentStorageHelper {

    val SUPPORTED_DOCUMENT_TYPES = listOf(
        "Student Aadhaar",
        "Father Aadhaar",
        "Mother Aadhaar",
        "Rashan Card",
        "Birth Certificate",
        "Transfer Certificate (TC)",
        "Caste Certificate",
        "Domicile Certificate",
        "Income Certificate",
        "Bank Passbook",
        "Disability Certificate",
        "Other Document"
    )

    data class SavedDocumentInfo(
        val absolutePath: String,
        val fileName: String,
        val mimeType: String,
        val fileSize: Long
    )

    suspend fun saveDocumentToInternalStorage(
        context: Context,
        studentId: Long,
        documentType: String,
        uri: Uri
    ): SavedDocumentInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedType = documentType.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val studentDir = File(context.filesDir, "StudentDocuments/Student_$studentId/$sanitizedType")
                if (!studentDir.exists()) {
                    studentDir.mkdirs()
                }

                var originalName = "doc_${UUID.randomUUID()}"
                var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            originalName = name
                        }
                    }
                }

                val ext = getExtensionFromMimeType(mimeType)
                val finalFileName = if (!originalName.contains(".") && ext.isNotBlank()) {
                    "$originalName.$ext"
                } else {
                    originalName
                }

                val targetFile = File(studentDir, "${System.currentTimeMillis()}_$finalFileName")
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(targetFile)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                val fileSize = targetFile.length()
                SavedDocumentInfo(
                    absolutePath = targetFile.absolutePath,
                    fileName = finalFileName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getFileProviderUri(context: Context, filePath: String): Uri? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deletePhysicalFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteAllStudentDocumentsFolder(context: Context, studentId: Long) {
        try {
            val studentDir = File(context.filesDir, "StudentDocuments/Student_$studentId")
            if (studentDir.exists()) {
                studentDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 KB"
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            String.format("%.1f KB", kb)
        }
    }

    private fun getExtensionFromMimeType(mimeType: String): String {
        return when {
            mimeType.contains("pdf", true) -> "pdf"
            mimeType.contains("image/jpeg", true) || mimeType.contains("jpg", true) -> "jpg"
            mimeType.contains("image/png", true) -> "png"
            mimeType.contains("image/webp", true) -> "webp"
            mimeType.contains("image/", true) -> "jpg"
            else -> ""
        }
    }
}

