package com.brijbymgt.util.exportimport

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.brijbymgt.data.model.StudentWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    suspend fun exportToPdf(
        context: Context,
        uri: Uri,
        students: List<StudentWithDetails>,
        selectedFields: List<String>,
        filterSummary: String = "All Active Records"
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
                val pdfDocument = PdfDocument()

                // Adjustable A4 Dimensions
                val isLandscape = selectedFields.size > 7
                val pageWidth = if (isLandscape) 842 else 595 // A4 Landscape vs Portrait
                val pageHeight = if (isLandscape) 595 else 842

                val margin = 20f
                val availableWidth = pageWidth - (margin * 2)
                val numCols = selectedFields.size.coerceAtLeast(1)
                val colWidth = availableWidth / numCols

                val paintNormal = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val paintHeader = Paint().apply {
                    color = Color.parseColor("#1E3A8A")
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintSubHeader = Paint().apply {
                    color = Color.parseColor("#475569")
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintTableHeadBg = Paint().apply {
                    color = Color.parseColor("#1565C0")
                    style = Paint.Style.FILL
                }

                val paintTableHeadText = Paint().apply {
                    color = Color.WHITE
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintLine = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    strokeWidth = 0.5f
                }

                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                var pageNum = 1
                var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                // Pre-wrap table headers
                val headerLinesList = selectedFields.map { wrapText(it, colWidth - 6f, paintTableHeadText) }
                val maxHeaderLines = headerLinesList.maxOfOrNull { it.size } ?: 1
                val headerHeight = maxOf(22f, maxHeaderLines * 10.5f + 6f)

                fun drawPageHeader(c: Canvas, pNum: Int) {
                    c.drawText("SCHOOL STUDENT RECORD MANAGEMENT SYSTEM", margin, 22f, paintHeader)
                    c.drawText("Export Date: $dateStr | Filters: $filterSummary | Total Records: ${students.size} | Page $pNum", margin, 34f, paintSubHeader)

                    // Draw Table Header Box
                    c.drawRect(margin, 42f, pageWidth - margin, 42f + headerHeight, paintTableHeadBg)
                    for (colIdx in selectedFields.indices) {
                        val cx = margin + (colIdx * colWidth) + 3f
                        val lines = headerLinesList[colIdx]
                        var hy = 52f
                        for (line in lines) {
                            c.drawText(line, cx, hy, paintTableHeadText)
                            hy += 10f
                        }
                    }
                }

                drawPageHeader(canvas, pageNum)

                var y = 42f + headerHeight + 12f
                val lineHeight = 11f

                for (rowIdx in students.indices) {
                    val item = students[rowIdx]

                    // Wrap all cell values for this row
                    val cellLinesList = selectedFields.map { fieldName ->
                        val rawVal = FieldExtractor.getFieldValue(fieldName, item)
                        wrapText(rawVal, colWidth - 6f, paintNormal)
                    }

                    val maxCellLines = cellLinesList.maxOfOrNull { it.size } ?: 1
                    val calculatedRowHeight = maxOf(18f, maxCellLines * lineHeight + 6f)

                    // Page Break Check
                    if (y + calculatedRowHeight > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        drawPageHeader(canvas, pageNum)
                        y = 42f + headerHeight + 12f
                    }

                    // Zebra Striping
                    if (rowIdx % 2 == 1) {
                        val rowBg = Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL }
                        canvas.drawRect(margin, y - 9f, pageWidth - margin, y + calculatedRowHeight - 9f, rowBg)
                    }

                    // Draw Row Cells
                    for (colIdx in selectedFields.indices) {
                        val lines = cellLinesList[colIdx]
                        val cx = margin + (colIdx * colWidth) + 3f
                        var lineY = y
                        for (line in lines) {
                            canvas.drawText(line, cx, lineY, paintNormal)
                            lineY += lineHeight
                        }
                    }

                    canvas.drawLine(margin, y + calculatedRowHeight - 9f, pageWidth - margin, y + calculatedRowHeight - 9f, paintLine)
                    y += calculatedRowHeight
                }

                pdfDocument.finishPage(page)
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val str = text.trim()
        if (str.isBlank()) return listOf("-")
        if (paint.measureText(str) <= maxWidth) return listOf(str)

        val words = str.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                if (paint.measureText(word) > maxWidth) {
                    var subWord = word
                    while (subWord.isNotEmpty() && paint.measureText(subWord) > maxWidth) {
                        var charCount = subWord.length
                        while (charCount > 1 && paint.measureText(subWord.substring(0, charCount)) > maxWidth) {
                            charCount--
                        }
                        lines.add(subWord.substring(0, charCount))
                        subWord = subWord.substring(charCount)
                    }
                    if (subWord.isNotEmpty()) {
                        currentLine.append(subWord)
                    }
                } else {
                    currentLine.append(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return if (lines.isEmpty()) listOf(str) else lines
    }
}

