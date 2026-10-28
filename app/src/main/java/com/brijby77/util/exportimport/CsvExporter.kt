package com.brijby77.util.exportimport

import android.content.Context
import android.net.Uri
import com.brijby77.data.model.StudentWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

object CsvExporter {

    suspend fun exportToCsv(
        context: Context,
        uri: Uri,
        students: List<StudentWithDetails>,
        selectedFields: List<String>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
                // Write UTF-8 BOM for Excel compatibility with Hindi/Unicode
                outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

                // Write Header Line
                writer.write(selectedFields.joinToString(",") { escapeCsv(it) } + "\r\n")

                // Write Data Rows
                for (item in students) {
                    val rowValues = selectedFields.map { fieldName ->
                        FieldExtractor.getFieldValue(fieldName, item)
                    }
                    writer.write(rowValues.joinToString(",") { escapeCsv(it) } + "\r\n")
                }

                writer.flush()
                writer.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun escapeCsv(text: String): String {
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }
}

