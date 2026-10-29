package com.brijbymgt.util.exportimport

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object CsvImporter {

    suspend fun parseCsvFile(
        context: Context,
        uri: Uri
    ): List<List<String>> {
        return withContext(Dispatchers.IO) {
            val rows = mutableListOf<List<String>>()
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))

                var line = reader.readLine()
                // Strip UTF-8 BOM if present on line 1
                if (line != null && line.startsWith("\uFEFF")) {
                    line = line.substring(1)
                }

                while (line != null) {
                    if (line.isNotBlank()) {
                        val parsed = parseCsvLine(line)
                        rows.add(parsed)
                    }
                    line = reader.readLine()
                }
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rows
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (c in line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}

