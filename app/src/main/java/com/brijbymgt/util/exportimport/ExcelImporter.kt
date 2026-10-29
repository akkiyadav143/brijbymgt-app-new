package com.brijbymgt.util.exportimport

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object ExcelImporter {

    suspend fun parseXlsxFile(
        context: Context,
        uri: Uri
    ): List<List<String>> {
        return withContext(Dispatchers.IO) {
            val rows = mutableListOf<List<String>>()
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
                val zipIn = ZipInputStream(inputStream)

                var sharedStringsXml: ByteArray? = null
                var sheet1Xml: ByteArray? = null

                var entry = zipIn.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "xl/sharedStrings.xml" -> sharedStringsXml = readEntryBytes(zipIn)
                        "xl/worksheets/sheet1.xml" -> sheet1Xml = readEntryBytes(zipIn)
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                zipIn.close()

                if (sheet1Xml == null) return@withContext emptyList()

                val sharedStrings = if (sharedStringsXml != null) parseSharedStrings(sharedStringsXml) else emptyList()
                rows.addAll(parseSheetXml(sheet1Xml, sharedStrings))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rows
        }
    }

    private fun readEntryBytes(zipIn: ZipInputStream): ByteArray {
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var count = zipIn.read(buffer)
        while (count != -1) {
            bos.write(buffer, 0, count)
            count = zipIn.read(buffer)
        }
        return bos.toByteArray()
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            var inT = false
            val currentSb = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag.equals("t", true)) {
                            inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            currentSb.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag.equals("t", true)) {
                            inT = false
                        } else if (tag.equals("si", true)) {
                            strings.add(currentSb.toString())
                            currentSb.clear()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strings
    }

    private fun parseSheetXml(sheetXmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(sheetXmlBytes), "UTF-8")

            var eventType = parser.eventType
            var currentRow = mutableMapOf<Int, String>()
            var cellRef = ""
            var cellType = ""
            var inV = false
            var inInlineStrT = false
            var cellValSb = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag.equals("row", true)) {
                            currentRow = mutableMapOf()
                        } else if (tag.equals("c", true)) {
                            cellRef = parser.getAttributeValue(null, "r") ?: ""
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValSb = StringBuilder()
                        } else if (tag.equals("v", true)) {
                            inV = true
                        } else if (tag.equals("t", true)) {
                            inInlineStrT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV || inInlineStrT) {
                            cellValSb.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag.equals("v", true)) {
                            inV = false
                        } else if (tag.equals("t", true)) {
                            inInlineStrT = false
                        } else if (tag.equals("c", true)) {
                            val colIdx = getColumnIndexFromRef(cellRef)
                            val finalVal = when (cellType) {
                                "s" -> {
                                    val idx = cellValSb.toString().toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else ""
                                }
                                "inlineStr" -> cellValSb.toString()
                                else -> cellValSb.toString()
                            }
                            if (colIdx >= 0) {
                                currentRow[colIdx] = finalVal
                            }
                        } else if (tag.equals("row", true)) {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = currentRow.keys.maxOrNull() ?: 0
                                val rowList = mutableListOf<String>()
                                for (i in 0..maxCol) {
                                    rowList.add(currentRow[i] ?: "")
                                }
                                result.add(rowList)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun getColumnIndexFromRef(ref: String): Int {
        val colLetters = ref.takeWhile { it.isLetter() }.uppercase()
        if (colLetters.isEmpty()) return -1
        var result = 0
        for (c in colLetters) {
            result = result * 26 + (c.code - 'A'.code + 1)
        }
        return result - 1
    }
}

