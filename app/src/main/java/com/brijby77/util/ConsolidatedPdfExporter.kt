package com.brijby77.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.brijby77.data.repository.ConsolidatedStudentReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object ConsolidatedPdfExporter {

    private const val A4_LANDSCAPE_WIDTH = 842
    private const val A4_LANDSCAPE_HEIGHT = 595

    suspend fun generateConsolidatedPdf(
        context: Context,
        report: ConsolidatedStudentReport,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(A4_LANDSCAPE_WIDTH, A4_LANDSCAPE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                drawStudentMarksheetOnCanvas(page.canvas, report)
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

    suspend fun generateAllConsolidatedPdf(
        context: Context,
        reports: List<ConsolidatedStudentReport>,
        className: String,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                for ((index, report) in reports.withIndex()) {
                    val pageInfo = PdfDocument.PageInfo.Builder(A4_LANDSCAPE_WIDTH, A4_LANDSCAPE_HEIGHT, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    drawStudentMarksheetOnCanvas(page.canvas, report)
                    pdfDocument.finishPage(page)
                }
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun drawStudentMarksheetOnCanvas(canvas: Canvas, report: ConsolidatedStudentReport) {
        val margin = 30f
        val width = A4_LANDSCAPE_WIDTH.toFloat()
        val height = A4_LANDSCAPE_HEIGHT.toFloat()

        val paintBorder = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val paintHeaderBg = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.FILL
        }

        val paintHeaderTitle = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTextNormal = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintTextBold = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTableBgHeader = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
        }

        val paintTableGrid = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        // Draw outer rectangular border
        canvas.drawRect(RectF(margin, margin, width - margin, height - margin), paintBorder)

        var y = margin + 10f

        // 1. Header Banner
        val bannerRect = RectF(margin + 5f, y, width - margin - 5f, y + 34f)
        canvas.drawRoundRect(bannerRect, 4f, 4f, paintHeaderBg)
        canvas.drawText("CONSOLIDATED ANNUAL EXAMINATION MARKSHET", margin + 15f, y + 21f, paintHeaderTitle)

        val sessionText = "Academic Session: ${report.academicSession}"
        val sessionWidth = paintHeaderTitle.measureText(sessionText)
        canvas.drawText(sessionText, width - margin - sessionWidth - 15f, y + 21f, paintHeaderTitle)

        y += 42f

        // 2. Student Info Box
        val student = report.student
        val infoBoxRect = RectF(margin + 5f, y, width - margin - 5f, y + 45f)
        canvas.drawRoundRect(infoBoxRect, 4f, 4f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
        canvas.drawRoundRect(infoBoxRect, 4f, 4f, paintTableGrid)

        val col1X = margin + 15f
        val col2X = margin + 280f
        val col3X = margin + 550f

        canvas.drawText("Student Name :", col1X, y + 18f, paintTextBold)
        canvas.drawText(student.studentName.ifBlank { "-" }, col1X + 95f, y + 18f, paintTextNormal)

        canvas.drawText("SR Number :", col2X, y + 18f, paintTextBold)
        canvas.drawText(student.srNumber.ifBlank { "-" }, col2X + 75f, y + 18f, paintTextNormal)

        canvas.drawText("Class :", col3X, y + 18f, paintTextBold)
        canvas.drawText(student.className.ifBlank { "-" }, col3X + 45f, y + 18f, paintTextNormal)

        canvas.drawText("Father Name :", col1X, y + 34f, paintTextBold)
        canvas.drawText(student.fatherName.ifBlank { "-" }, col1X + 95f, y + 34f, paintTextNormal)

        canvas.drawText("Gender :", col2X, y + 34f, paintTextBold)
        canvas.drawText(student.gender.ifBlank { "-" }, col2X + 75f, y + 34f, paintTextNormal)

        canvas.drawText("Rank :", col3X, y + 34f, paintTextBold)
        canvas.drawText("#${report.rank}", col3X + 45f, y + 34f, paintTextBold)

        y += 55f

        // 3. Subject Table Headers
        val tableLeft = margin + 5f
        val tableRight = width - margin - 5f
        val tableWidth = tableRight - tableLeft

        val colWeights = listOf(2.2f, 0.9f, 1.1f, 0.9f, 0.9f, 1.4f)
        val totalWeight = colWeights.sum()
        val colWidths = colWeights.map { (it / totalWeight) * tableWidth }

        val tableHeaderHeight = 22f
        canvas.drawRect(RectF(tableLeft, y, tableRight, y + tableHeaderHeight), paintTableBgHeader)
        canvas.drawRect(RectF(tableLeft, y, tableRight, y + tableHeaderHeight), paintTableGrid)

        val headers = listOf("Subject", "Unit I", "Half Yearly", "Unit II", "Yearly", "Subject Total")
        var curX = tableLeft
        for (i in headers.indices) {
            val w = colWidths[i]
            canvas.drawRect(RectF(curX, y, curX + w, y + tableHeaderHeight), paintTableGrid)
            val textX = if (i == 0) curX + 8f else curX + (w / 2f) - (paintTextBold.measureText(headers[i]) / 2f)
            canvas.drawText(headers[i], textX, y + 15f, paintTextBold)
            curX += w
        }
        y += tableHeaderHeight

        // 4. Subject Rows
        val rowHeight = 20f
        for (subj in report.subjectMarksList) {
            curX = tableLeft
            canvas.drawRect(RectF(tableLeft, y, tableRight, y + rowHeight), paintTableGrid)

            val rowData = listOf(
                subj.subjectName,
                subj.unit1,
                subj.halfYearly,
                subj.unit2,
                subj.yearly,
                "${subj.subjectTotalObtained.toInt()}/${subj.subjectTotalMax}"
            )

            for (i in rowData.indices) {
                val w = colWidths[i]
                canvas.drawRect(RectF(curX, y, curX + w, y + rowHeight), paintTableGrid)
                val cellText = rowData[i]
                val isAbsentOrDash = cellText == "Ab" || cellText == "-"
                val paintToUse = if (i > 0 && isAbsentOrDash) {
                    Paint(paintTextNormal).apply { color = Color.parseColor("#DC2626") }
                } else if (i == rowData.size - 1) {
                    paintTextBold
                } else {
                    paintTextNormal
                }
                val textX = if (i == 0) curX + 8f else curX + (w / 2f) - (paintToUse.measureText(cellText) / 2f)
                canvas.drawText(cellText, textX, y + 14f, paintToUse)
                curX += w
            }
            y += rowHeight
        }

        // 5. GRAND TOTAL Row
        canvas.drawRect(RectF(tableLeft, y, tableRight, y + rowHeight), Paint().apply { color = Color.parseColor("#E2E8F0"); style = Paint.Style.FILL })
        canvas.drawRect(RectF(tableLeft, y, tableRight, y + rowHeight), paintTableGrid)

        val grandTotalData = listOf(
            "GRAND TOTAL",
            "${report.unit1TotalObtained.toInt()}/${report.unit1TotalMax}",
            "${report.halfYearlyTotalObtained.toInt()}/${report.halfYearlyTotalMax}",
            "${report.unit2TotalObtained.toInt()}/${report.unit2TotalMax}",
            "${report.yearlyTotalObtained.toInt()}/${report.yearlyTotalMax}",
            "${report.overallObtained.toInt()}/${report.overallMax}"
        )

        curX = tableLeft
        for (i in grandTotalData.indices) {
            val w = colWidths[i]
            canvas.drawRect(RectF(curX, y, curX + w, y + rowHeight), paintTableGrid)
            val cellText = grandTotalData[i]
            val textX = if (i == 0) curX + 8f else curX + (w / 2f) - (paintTextBold.measureText(cellText) / 2f)
            canvas.drawText(cellText, textX, y + 14f, paintTextBold)
            curX += w
        }
        y += rowHeight + 15f

        // 6. Overall Summary & Result Box
        val summaryBoxRect = RectF(tableLeft, y, tableRight, y + 45f)
        canvas.drawRoundRect(summaryBoxRect, 4f, 4f, Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL })
        canvas.drawRoundRect(summaryBoxRect, 4f, 4f, paintTableGrid)

        val s1X = summaryBoxRect.left + 15f
        val s2X = summaryBoxRect.left + 260f
        val s3X = summaryBoxRect.left + 470f
        val s4X = summaryBoxRect.left + 650f

        canvas.drawText("Overall Total :", s1X, y + 27f, paintTextBold)
        canvas.drawText("${report.overallObtained.toInt()} / ${report.overallMax}", s1X + 90f, y + 27f, paintTextNormal)

        canvas.drawText("Percentage :", s2X, y + 27f, paintTextBold)
        canvas.drawText("${String.format("%.2f", report.percentage)}%", s2X + 80f, y + 27f, paintTextNormal)

        canvas.drawText("Grade :", s3X, y + 27f, paintTextBold)
        canvas.drawText(report.grade, s3X + 45f, y + 27f, paintTextBold)

        // Pass / Fail badge
        val isPass = report.passStatus == "PASS"
        val badgeRect = RectF(s4X, y + 10f, s4X + 85f, y + 35f)
        canvas.drawRoundRect(badgeRect, 4f, 4f, Paint().apply {
            color = if (isPass) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
            style = Paint.Style.FILL
        })
        val paintBadgeText = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val badgeTextWidth = paintBadgeText.measureText(report.passStatus)
        canvas.drawText(report.passStatus, badgeRect.left + (badgeRect.width() - badgeTextWidth) / 2f, badgeRect.top + 17f, paintBadgeText)
    }
}

