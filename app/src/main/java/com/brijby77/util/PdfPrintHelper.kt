package com.brijby77.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.brijby77.data.model.StudentWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfPrintHelper {

    private const val A4_WIDTH = 595 // A4 size in points (72 dpi): 595 x 842
    private const val A4_HEIGHT = 842

    suspend fun generateStudentProfilePdf(
        context: Context,
        item: StudentWithDetails,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val s = item.student
                val h = item.healthDetails
                val b = item.bankDetails

                val paintNormal = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val paintBold = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintHeaderBanner = Paint().apply {
                    color = Color.parseColor("#1565C0") // Deep Blue Header
                    style = Paint.Style.FILL
                }

                val paintHeaderTitle = Paint().apply {
                    color = Color.WHITE
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintSectionHeaderBg = Paint().apply {
                    style = Paint.Style.FILL
                }

                val paintSectionTitle = Paint().apply {
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintBoxBg = Paint().apply {
                    color = Color.parseColor("#F1F5F9")
                    style = Paint.Style.FILL
                }

                val paintBorder = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                var y = 25f
                val margin = 25f
                val contentWidth = A4_WIDTH - (margin * 2)

                // 1. TOP HEADER BANNER
                val bannerRect = RectF(margin, y, A4_WIDTH - margin, y + 36f)
                canvas.drawRoundRect(bannerRect, 6f, 6f, paintHeaderBanner)
                canvas.drawText("STUDENT ADMISSION PREVIEW", margin + 15f, y + 23f, paintHeaderTitle)
                y += 48f

                // 2. BASIC DETAILS SECTION
                val basicSecStart = y
                paintSectionHeaderBg.color = Color.parseColor("#E3F2FD")
                canvas.drawRoundRect(RectF(margin, y, A4_WIDTH - margin, y + 20f), 4f, 4f, paintSectionHeaderBg)
                paintSectionTitle.color = Color.parseColor("#1565C0")
                canvas.drawText("BASIC DETAILS", margin + 8f, y + 14f, paintSectionTitle)
                y += 28f

                // Student Photo
                if (!s.imagePath.isNullOrBlank()) {
                    val bitmap = ImageStorageHelper.loadBitmapFromPath(s.imagePath)
                    bitmap?.let {
                        val photoRect = RectF(margin + 10f, y, margin + 80f, y + 85f)
                        canvas.drawBitmap(it, null, photoRect, null)
                    }
                }

                // Basic Details Fields Table
                val textX = margin + 95f
                var textY = y + 10f

                val basicFieldsLeft = listOf(
                    "Class" to s.studentClass,
                    "SR Number" to s.srNumber.ifBlank { "-" },
                    "Unique ID" to s.uniqueId.ifBlank { "-" },
                    "PEN No" to s.penNo.ifBlank { "-" },
                    "APAAR ID" to s.aaparId.ifBlank { "-" },
                    "Student Name" to s.studentName.ifBlank { "-" },
                    "Date of Birth" to s.dob.ifBlank { "-" },
                    "Admission Date" to s.admissionDate.ifBlank { "-" }
                )

                for (bf in basicFieldsLeft) {
                    canvas.drawText("${bf.first} :", textX, textY, paintBold)
                    canvas.drawText(bf.second, textX + 110f, textY, paintNormal)
                    textY += 13f
                }

                y = maxOf(y + 90f, textY + 5f)

                // Pastel Bottom Box for Basic Details
                val pastelBoxRect = RectF(margin + 6f, y, A4_WIDTH - margin - 6f, y + 42f)
                canvas.drawRoundRect(pastelBoxRect, 4f, 4f, paintBoxBg)

                val py = y + 14f
                canvas.drawText("Student Aadhaar :", margin + 12f, py, paintBold)
                canvas.drawText(s.studentAdhar.ifBlank { "-" }, margin + 110f, py, paintNormal)
                canvas.drawText("Caste Category :", margin + 280f, py, paintBold)
                canvas.drawText(s.caste.ifBlank { "-" }, margin + 370f, py, paintBold)

                val py2 = y + 30f
                canvas.drawText("Care of (C/o) :", margin + 12f, py2, paintBold)
                canvas.drawText(s.co.ifBlank { "-" }, margin + 110f, py2, paintNormal)
                canvas.drawText("DBT Status :", margin + 280f, py2, paintBold)
                canvas.drawText(s.dbt, margin + 370f, py2, paintBold)

                y += 54f
                canvas.drawRoundRect(RectF(margin, basicSecStart, A4_WIDTH - margin, y), 6f, 6f, paintBorder)
                y += 14f

                // 3. PARENT DETAILS SECTION
                val parentSecStart = y
                paintSectionHeaderBg.color = Color.parseColor("#EDE7F6")
                canvas.drawRoundRect(RectF(margin, y, A4_WIDTH - margin, y + 20f), 4f, 4f, paintSectionHeaderBg)
                paintSectionTitle.color = Color.parseColor("#5E35B1")
                canvas.drawText("PARENT DETAILS", margin + 8f, y + 14f, paintSectionTitle)
                y += 28f

                val halfWidth = (contentWidth - 12f) / 2f
                val fatherBox = RectF(margin + 4f, y, margin + 4f + halfWidth, y + 60f)
                val motherBox = RectF(margin + 8f + halfWidth, y, A4_WIDTH - margin - 4f, y + 60f)

                canvas.drawRoundRect(fatherBox, 4f, 4f, paintBoxBg)
                canvas.drawRoundRect(motherBox, 4f, 4f, paintBoxBg)

                // Father Photo & Text
                if (!s.fatherImagePath.isNullOrBlank()) {
                    val fBitmap = ImageStorageHelper.loadBitmapFromPath(s.fatherImagePath)
                    fBitmap?.let {
                        canvas.drawBitmap(it, null, RectF(fatherBox.left + 6f, y + 5f, fatherBox.left + 50f, y + 55f), null)
                    }
                }
                val fTextX = fatherBox.left + 56f
                canvas.drawText("Father Name :", fTextX, y + 18f, paintBold)
                canvas.drawText(s.fatherName.ifBlank { "-" }, fTextX + 75f, y + 18f, paintNormal)
                canvas.drawText("Father Aadhaar :", fTextX, y + 36f, paintBold)
                canvas.drawText(s.fatherAdhar.ifBlank { "-" }, fTextX + 75f, y + 36f, paintNormal)

                // Mother Photo & Text
                if (!s.motherImagePath.isNullOrBlank()) {
                    val mBitmap = ImageStorageHelper.loadBitmapFromPath(s.motherImagePath)
                    mBitmap?.let {
                        canvas.drawBitmap(it, null, RectF(motherBox.left + 6f, y + 5f, motherBox.left + 50f, y + 55f), null)
                    }
                }
                val mTextX = motherBox.left + 56f
                canvas.drawText("Mother Name :", mTextX, y + 18f, paintBold)
                canvas.drawText(s.motherName.ifBlank { "-" }, mTextX + 75f, y + 18f, paintNormal)
                canvas.drawText("Mother Aadhaar :", mTextX, y + 36f, paintBold)
                canvas.drawText(s.motherAdhar.ifBlank { "-" }, mTextX + 75f, y + 36f, paintNormal)

                y += 68f
                canvas.drawRoundRect(RectF(margin, parentSecStart, A4_WIDTH - margin, y), 6f, 6f, paintBorder)
                y += 14f

                // 4. HEALTH DETAILS SECTION
                val healthSecStart = y
                paintSectionHeaderBg.color = Color.parseColor("#E8F5E9")
                canvas.drawRoundRect(RectF(margin, y, A4_WIDTH - margin, y + 20f), 4f, 4f, paintSectionHeaderBg)
                paintSectionTitle.color = Color.parseColor("#2E7D32")
                canvas.drawText("HEALTH DETAILS", margin + 8f, y + 14f, paintSectionTitle)
                val isHealth = h != null && h.hasHealthDetails
                canvas.drawText("Included : ${if (isHealth) "Yes" else "No"}", A4_WIDTH - margin - 90f, y + 14f, paintSectionTitle)
                y += 28f

                val healthBox = RectF(margin + 4f, y, A4_WIDTH - margin - 4f, y + 45f)
                canvas.drawRoundRect(healthBox, 4f, 4f, paintBoxBg)

                val hy1 = y + 16f
                canvas.drawText("Height :", healthBox.left + 8f, hy1, paintBold)
                canvas.drawText(if (isHealth && h.height.isNotBlank()) "${h.height} cm" else "-", healthBox.left + 70f, hy1, paintNormal)
                canvas.drawText("Hb Level :", healthBox.left + 260f, hy1, paintBold)
                canvas.drawText(if (isHealth && h.hb.isNotBlank()) "${h.hb} g/dL" else "-", healthBox.left + 330f, hy1, paintNormal)

                val hy2 = y + 32f
                canvas.drawText("Weight :", healthBox.left + 8f, hy2, paintBold)
                canvas.drawText(if (isHealth && h.weight.isNotBlank()) "${h.weight} kg" else "-", healthBox.left + 70f, hy2, paintNormal)
                canvas.drawText("Eye Sight :", healthBox.left + 260f, hy2, paintBold)
                canvas.drawText(if (isHealth && h.eyeSight.isNotBlank()) h.eyeSight else "-", healthBox.left + 330f, hy2, paintNormal)

                y += 54f
                canvas.drawRoundRect(RectF(margin, healthSecStart, A4_WIDTH - margin, y), 6f, 6f, paintBorder)
                y += 14f

                // 5. BANK DETAILS SECTION
                val bankSecStart = y
                paintSectionHeaderBg.color = Color.parseColor("#E3F2FD")
                canvas.drawRoundRect(RectF(margin, y, A4_WIDTH - margin, y + 20f), 4f, 4f, paintSectionHeaderBg)
                paintSectionTitle.color = Color.parseColor("#1565C0")
                canvas.drawText("BANK DETAILS", margin + 8f, y + 14f, paintSectionTitle)
                val isBank = b != null && b.hasBankDetails
                canvas.drawText("Included : ${if (isBank) "Yes" else "No"}", A4_WIDTH - margin - 90f, y + 14f, paintSectionTitle)
                y += 28f

                val bankBox = RectF(margin + 4f, y, A4_WIDTH - margin - 4f, y + 45f)
                canvas.drawRoundRect(bankBox, 4f, 4f, paintBoxBg)

                val by1 = y + 16f
                canvas.drawText("A/c Holder :", bankBox.left + 8f, by1, paintBold)
                canvas.drawText(if (isBank) b.acHolderName.ifBlank { "-" } else "-", bankBox.left + 80f, by1, paintNormal)
                canvas.drawText("IFSC Code :", bankBox.left + 260f, by1, paintBold)
                canvas.drawText(if (isBank) b.ifscCode.ifBlank { "-" } else "-", bankBox.left + 330f, by1, paintNormal)

                val by2 = y + 32f
                canvas.drawText("A/c Number :", bankBox.left + 8f, by2, paintBold)
                canvas.drawText(if (isBank) b.acNo.ifBlank { "-" } else "-", bankBox.left + 80f, by2, paintNormal)
                canvas.drawText("Branch :", bankBox.left + 260f, by2, paintBold)
                canvas.drawText(if (isBank) b.branchName.ifBlank { "-" } else "-", bankBox.left + 330f, by2, paintNormal)

                y += 54f
                canvas.drawRoundRect(RectF(margin, bankSecStart, A4_WIDTH - margin, y), 6f, 6f, paintBorder)
                y += 14f

                // 6. ADDRESS & REMARKS SECTION
                val addrSecStart = y
                paintSectionHeaderBg.color = Color.parseColor("#FFF3E0")
                canvas.drawRoundRect(RectF(margin, y, A4_WIDTH - margin, y + 20f), 4f, 4f, paintSectionHeaderBg)
                paintSectionTitle.color = Color.parseColor("#E65100")
                canvas.drawText("ADDRESS & REMARKS", margin + 8f, y + 14f, paintSectionTitle)
                y += 28f

                val addrBox = RectF(margin + 4f, y, A4_WIDTH - margin - 4f, y + 40f)
                canvas.drawRoundRect(addrBox, 4f, 4f, paintBoxBg)

                canvas.drawText("Address :", addrBox.left + 8f, y + 15f, paintBold)
                canvas.drawText(s.address.ifBlank { "-" }, addrBox.left + 90f, y + 15f, paintNormal)

                canvas.drawText("Other :", addrBox.left + 8f, y + 30f, paintBold)
                canvas.drawText(s.other.ifBlank { "No additional remarks." }, addrBox.left + 90f, y + 30f, paintNormal)

                y += 48f
                canvas.drawRoundRect(RectF(margin, addrSecStart, A4_WIDTH - margin, y), 6f, 6f, paintBorder)

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

    suspend fun generateMdmReportPdf(
        context: Context,
        reportTitle: String,
        dateString: String,
        mdmRows: List<MdmRowPdfData>,
        summary1to5: Triple<Int, Int, Int>, // Male, Female, Total
        summary6to8: Triple<Int, Int, Int>,
        summary1to8: Triple<Int, Int, Int>,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paintNormal = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val paintBold = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintHeader = Paint().apply {
                    color = Color.parseColor("#0D47A1")
                    textSize = 16f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintSection = Paint().apply {
                    color = Color.parseColor("#1B5E20")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintLine = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }

                var y = 40f
                val margin = 35f

                // Title
                canvas.drawText(reportTitle, margin, y, paintHeader)
                canvas.drawText("Date: $dateString", A4_WIDTH - margin - 120f, y, paintBold)
                y += 30f

                // Table Header
                canvas.drawText("CLASS-WISE MDM REPORT TABLE", margin, y, paintSection)
                y += 20f

                // Table columns
                val colX = floatArrayOf(margin, margin + 65f, margin + 135f, margin + 215f, margin + 295f, margin + 375f, margin + 455f)
                canvas.drawLine(margin, y - 5f, A4_WIDTH - margin, y - 5f, paintLine)
                canvas.drawText("Class", colX[0], y, paintBold)
                canvas.drawText("DB Total", colX[1], y, paintBold)
                canvas.drawText("DB Male", colX[2], y, paintBold)
                canvas.drawText("DB Female", colX[3], y, paintBold)
                canvas.drawText("MDM Male", colX[4], y, paintBold)
                canvas.drawText("MDM Female", colX[5], y, paintBold)
                canvas.drawText("MDM Total", colX[6], y, paintBold)
                y += 12f
                canvas.drawLine(margin, y, A4_WIDTH - margin, y, paintLine)
                y += 15f

                for (row in mdmRows) {
                    canvas.drawText(row.studentClass, colX[0], y, paintNormal)
                    canvas.drawText(row.dbTotal.toString(), colX[1], y, paintNormal)
                    canvas.drawText(row.dbMale.toString(), colX[2], y, paintNormal)
                    canvas.drawText(row.dbFemale.toString(), colX[3], y, paintNormal)
                    canvas.drawText(row.mdmMale.toString(), colX[4], y, paintBold)
                    canvas.drawText(row.mdmFemale.toString(), colX[5], y, paintBold)
                    canvas.drawText(row.mdmTotal.toString(), colX[6], y, paintBold)
                    y += 18f
                    canvas.drawLine(margin, y - 10f, A4_WIDTH - margin, y - 10f, paintLine)
                }

                y += 25f

                // SUMMARIES
                canvas.drawText("MDM ABSTRACT SUMMARIES", margin, y, paintSection)
                y += 20f

                val summaries = listOf(
                    "CLASS 1–5" to summary1to5,
                    "CLASS 6–8" to summary6to8,
                    "CLASS 1–8" to summary1to8
                )

                for (sum in summaries) {
                    val label = sum.first
                    val counts = sum.second // Male, Female, Total
                    canvas.drawText("$label Summary:", margin, y, paintBold)
                    y += 16f
                    canvas.drawText("Male: ${counts.first}   |   Female: ${counts.second}   |   Total: ${counts.third}", margin + 20f, y, paintNormal)
                    y += 22f
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

    data class MdmRowPdfData(
        val studentClass: String,
        val dbTotal: Int,
        val dbMale: Int,
        val dbFemale: Int,
        val mdmMale: Int,
        val mdmFemale: Int,
        val mdmTotal: Int = mdmMale + mdmFemale
    )

    data class CasteRowPdfData(
        val className: String,
        val scMale: Int, val scFemale: Int, val scTotal: Int = scMale + scFemale,
        val stMale: Int, val stFemale: Int, val stTotal: Int = stMale + stFemale,
        val obcMale: Int, val obcFemale: Int, val obcTotal: Int = obcMale + obcFemale,
        val minMale: Int, val minFemale: Int, val minTotal: Int = minMale + minFemale,
        val genMale: Int, val genFemale: Int, val genTotal: Int = genMale + genFemale,
        val rowMaleTotal: Int = scMale + stMale + obcMale + minMale + genMale,
        val rowFemaleTotal: Int = scFemale + stFemale + obcFemale + minFemale + genFemale,
        val rowGrandTotal: Int = rowMaleTotal + rowFemaleTotal
    )

    suspend fun generateCasteReportPdf(
        context: Context,
        reportTitle: String,
        schoolName: String,
        dateString: String,
        casteRows: List<CasteRowPdfData>,
        summary1to5: CasteRowPdfData,
        summary6to8: CasteRowPdfData,
        grandTotal: CasteRowPdfData,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageWidth = 842 // Landscape A4 width
                val pageHeight = 595 // Landscape A4 height
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val margin = 25f
                var y = 25f

                // 1. Header Banner (White Background)
                val paintHeaderBanner = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(RectF(margin, y, pageWidth - margin, y + 36f), paintHeaderBanner)

                val paintTitle = Paint().apply {
                    color = Color.parseColor("#1E3A8A")
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(reportTitle, margin + 15f, y + 23f, paintTitle)

                val kalamTypeface = try {
                    Typeface.createFromAsset(context.assets, "fonts/Kalam-Regular.ttf")
                } catch (e: Exception) {
                    Typeface.SERIF
                }

                val paintSubTitleNormal = Paint().apply {
                    color = Color.parseColor("#334155")
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintSubTitleKalamBlue = Paint().apply {
                    color = Color.parseColor("#0D47A1") // Royal Blue
                    textSize = 10.5f
                    typeface = kalamTypeface
                    isAntiAlias = true
                }

                val school = schoolName.ifBlank { "_________________" }
                val suffix = " - जातिवार विवरण सूची (सत्र 2026-27)"

                canvas.drawText(school, margin, y + 50f, paintSubTitleKalamBlue)
                val wSchool = paintSubTitleKalamBlue.measureText(school)

                canvas.drawText(suffix, margin + wSchool, y + 50f, paintSubTitleNormal)

                canvas.drawText("Date: $dateString", pageWidth - margin - 110f, y + 50f, paintSubTitleNormal)

                y += 60f

                val colWidths = floatArrayOf(
                    54f, // Class
                    35f, 35f, 37f, // SC
                    35f, 35f, 37f, // ST
                    35f, 35f, 37f, // OBC
                    35f, 35f, 37f, // Min
                    35f, 35f, 37f, // Gen
                    37f, 37f, 41f  // Total
                )
                val totalTableWidth = colWidths.sum()
                val startX = (pageWidth - totalTableWidth) / 2f

                val paintTableBg = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
                val paintCellBg = Paint().apply { style = Paint.Style.FILL }
                val paintTextNormal = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 9f; isAntiAlias = true }
                val paintTextBlue = Paint().apply { color = Color.parseColor("#014EB1"); textSize = 13f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
                val paintTextRed = Paint().apply { color = Color.parseColor("#D92200"); textSize = 13f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
                val paintTextSummaryRed = Paint().apply { color = Color.parseColor("#D32F2F"); textSize = 13.5f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
                val paintHeaderCategoryText = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                val paintBorder = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    isAntiAlias = true
                }
                val paintHeaderBg1 = Paint().apply { color = Color.parseColor("#ECEFF1"); style = Paint.Style.FILL }
                val paintHeaderBg2 = Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }

                fun fmt(v: Int): String {
                    return if (v <= 0) "-" else String.format(java.util.Locale.getDefault(), "%02d", v)
                }

                var currentY = y
                val rowHeight = 24f
                val tableHeader1Height = 24f
                val tableHeader2Height = 20f

                // 1. Draw Table Header Row 1 (Categories)
                var curX = startX
                val classHeaderRect = RectF(curX, currentY, curX + colWidths[0], currentY + tableHeader1Height)
                canvas.drawRect(classHeaderRect, paintHeaderBg1)
                canvas.drawRect(classHeaderRect, paintBorder)
                canvas.drawText("कक्षा", curX + 10f, currentY + 16f, paintHeaderCategoryText)
                curX += colWidths[0]

                val catWidth = colWidths[1] + colWidths[2] + colWidths[3]
                val categories = listOf(
                    "अनुसूचित जाति (SC)" to Color.parseColor("#0D47A1"),
                    "अनुसूचित जनजाति (ST)" to Color.parseColor("#4A148C"),
                    "पिछड़ी जाति (OBC)" to Color.parseColor("#1B5E20"),
                    "अल्पसंख्यक (MIN)" to Color.parseColor("#E65100"),
                    "सामान्य (GEN)" to Color.parseColor("#B71C1C"),
                    "कुल योग (TOTAL)" to Color.parseColor("#0F172A")
                )

                for (cat in categories) {
                    val catRect = RectF(curX, currentY, curX + catWidth, currentY + tableHeader1Height)
                    canvas.drawRect(catRect, paintHeaderBg1)
                    canvas.drawRect(catRect, paintBorder)
                    val p = Paint(paintHeaderCategoryText)
                    p.color = cat.second
                    canvas.drawText(cat.first, curX + 10f, currentY + 16f, p)
                    curX += catWidth
                }
                currentY += tableHeader1Height

                // 2. Draw Header Row 2 (Subcols)
                curX = startX
                val classSubRect = RectF(curX, currentY, curX + colWidths[0], currentY + tableHeader2Height)
                canvas.drawRect(classSubRect, paintHeaderBg2)
                canvas.drawRect(classSubRect, paintBorder)
                canvas.drawText("(Class)", curX + 6f, currentY + 14f, paintTextNormal)
                curX += colWidths[0]

                val subCols = listOf("बालक", "बालिका", "योग")
                for (catIndex in 0 until 6) {
                    for (scIndex in 0 until 3) {
                        val w = colWidths[1 + catIndex * 3 + scIndex]
                        val subCellRect = RectF(curX, currentY, curX + w, currentY + tableHeader2Height)
                        canvas.drawRect(subCellRect, paintHeaderBg2)
                        canvas.drawRect(subCellRect, paintBorder)
                        canvas.drawText(subCols[scIndex], curX + 6f, currentY + 14f, paintTextNormal)
                        curX += w
                    }
                }
                currentY += tableHeader2Height

                // 3. Data Rows
                val allRowsToDraw = mutableListOf<Pair<CasteRowPdfData, Int>>().apply {
                    casteRows.take(5).forEach { add(it to 0) }
                    add(summary1to5 to 1)
                    casteRows.drop(5).forEach { add(it to 0) }
                    add(summary6to8 to 1)
                    add(grandTotal to 2)
                }

                for ((rowData, type) in allRowsToDraw) {
                    val bgCol = when (type) {
                        2 -> Color.parseColor("#FFE0B2")
                        1 -> Color.parseColor("#FFF3E0")
                        else -> Color.WHITE
                    }
                    paintCellBg.color = bgCol

                    curX = startX
                    val classCellRect = RectF(curX, currentY, curX + colWidths[0], currentY + rowHeight)
                    canvas.drawRect(classCellRect, paintCellBg)
                    canvas.drawRect(classCellRect, paintBorder)

                    val classTextPaint = if (type > 0) Paint(paintTextSummaryRed).apply { textSize = 8f } else Paint(paintTextNormal).apply { textSize = 9f }
                    canvas.drawText(rowData.className, curX + 4f, currentY + 16f, classTextPaint)
                    curX += colWidths[0]

                    val values = listOf(
                        rowData.scMale, rowData.scFemale, rowData.scTotal,
                        rowData.stMale, rowData.stFemale, rowData.stTotal,
                        rowData.obcMale, rowData.obcFemale, rowData.obcTotal,
                        rowData.minMale, rowData.minFemale, rowData.minTotal,
                        rowData.genMale, rowData.genFemale, rowData.genTotal,
                        rowData.rowMaleTotal, rowData.rowFemaleTotal, rowData.rowGrandTotal
                    )

                    for (i in values.indices) {
                        val w = colWidths[1 + i]
                        val cellRect = RectF(curX, currentY, curX + w, currentY + rowHeight)
                        canvas.drawRect(cellRect, paintCellBg)
                        canvas.drawRect(cellRect, paintBorder)

                        val v = values[i]
                        val textStr = fmt(v)
                        val isTotalCol = (i % 3 == 2)
                        val p = when {
                            type > 0 -> paintTextSummaryRed
                            isTotalCol -> paintTextRed
                            i % 3 == 0 -> paintTextBlue
                            else -> paintTextRed
                        }
                        canvas.drawText(textStr, curX + 8f, currentY + 16f, p)
                        curX += w
                    }

                    currentY += rowHeight
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

    fun printPdfFile(context: Context, pdfFile: File, jobName: String = "Print Student Record") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val builder = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    val pInfo = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(pInfo, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val inputStream = pdfFile.inputStream()
                        val outputStream = FileOutputStream(destination?.fileDescriptor)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape()).build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.brijby77.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Student Document PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class So3PdfData(
        val academicYear: String,
        val udiseCode: String,
        val schoolName: String,
        val stateName: String,
        val districtName: String,
        val blockName: String,
        val studentPen: String,
        val studentName: String,
        val mobileNumber: String,
        val existName: String,
        val existDob: String,
        val existGender: String,
        val existAadhar: String,
        val existNameAsAadhar: String,
        val existClass: String,
        val existMother: String,
        val existFather: String,
        val updateName: String,
        val updateDob: String,
        val updateGender: String,
        val updateAadhar: String,
        val updateNameAsAadhar: String,
        val updateClass: String,
        val updateMother: String,
        val updateFather: String,
        val docAadharAttached: Boolean,
        val docBirthCertAttached: Boolean,
        val docParentIdAttached: Boolean,
        val undertakingChecked: Boolean
    )

    suspend fun generateSo3Pdf(
        context: Context,
        data: So3PdfData,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                val paintNormal = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val paintBold = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintHeaderTitle = Paint().apply {
                    color = Color.parseColor("#0D47A1")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintSectionTitle = Paint().apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintWhiteBg = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }

                val paintBorder = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                val margin = 25f
                var y = 25f
                val contentWidth = A4_WIDTH - (margin * 2)

                // 1. Header
                canvas.drawText("FORM S03/UDISE / Format to update details of Active Student", margin, y, paintHeaderTitle)
                y += 20f

                // 2. Metadata Box
                val metaRect = RectF(margin, y, A4_WIDTH - margin, y + 65f)
                canvas.drawRect(metaRect, paintWhiteBg)
                canvas.drawRect(metaRect, paintBorder)

                var mx = margin + 8f
                var my = y + 16f
                canvas.drawText("Academic Year: ${data.academicYear}", mx, my, paintBold)
                canvas.drawText("UDISE Code: ${data.udiseCode}", mx + 180f, my, paintBold)
                canvas.drawText("School Name: ${data.schoolName}", mx + 350f, my, paintBold)

                my += 16f
                canvas.drawText("State: ${data.stateName}", mx, my, paintNormal)
                canvas.drawText("District: ${data.districtName}", mx + 150f, my, paintNormal)
                canvas.drawText("Block: ${data.blockName}", mx + 310f, my, paintNormal)

                my += 16f
                canvas.drawText("Student PEN: ${data.studentPen}", mx, my, paintBold)
                canvas.drawText("Student Name: ${data.studentName}", mx + 180f, my, paintBold)
                canvas.drawText("Mobile No: ${data.mobileNumber}", mx + 350f, my, paintNormal)

                y += 75f

                // 3. Demographic Details Update Table (Existing vs Updated)
                canvas.drawText("Demographic Details Update in UDISE+", margin, y, paintSectionTitle)
                y += 16f

                val col0 = margin
                val col1 = margin + 140f
                val col2 = margin + 280f
                val tableWidth = contentWidth
                val rowHeight = 20f

                val tableHeaderRect = RectF(col0, y, col0 + tableWidth, y + rowHeight)
                canvas.drawRect(tableHeaderRect, paintWhiteBg)
                canvas.drawRect(tableHeaderRect, paintBorder)

                canvas.drawText("Field Description", col0 + 6f, y + 14f, paintBold)
                canvas.drawText("Existing Details in UDISE+", col1 + 6f, y + 14f, paintBold)
                canvas.drawText("Details to be Updated", col2 + 6f, y + 14f, paintBold)
                y += rowHeight

                val fields = listOf(
                    Triple("Student Name", data.existName, data.updateName),
                    Triple("Date of Birth (DOB)", data.existDob, data.updateDob),
                    Triple("Gender", data.existGender, data.updateGender),
                    Triple("Aadhaar Number", data.existAadhar, data.updateAadhar),
                    Triple("Name as per Aadhaar", data.existNameAsAadhar, data.updateNameAsAadhar),
                    Triple("Class & Section", data.existClass, data.updateClass),
                    Triple("Mother's Name", data.existMother, data.updateMother),
                    Triple("Father's Name", data.existFather, data.updateFather)
                )

                for (field in fields) {
                    val rowRect = RectF(col0, y, col0 + tableWidth, y + rowHeight)
                    canvas.drawRect(rowRect, paintWhiteBg)
                    canvas.drawRect(rowRect, paintBorder)

                    canvas.drawText(field.first, col0 + 6f, y + 14f, paintNormal)
                    canvas.drawText(field.second.ifBlank { "-" }, col1 + 6f, y + 14f, paintNormal)
                    canvas.drawText(field.third.ifBlank { "-" }, col2 + 6f, y + 14f, paintBold)
                    y += rowHeight
                }

                y += 15f

                // 4. Supporting Documents & Undertaking
                canvas.drawText("Supporting Documents Attached:", margin, y, paintBold)
                y += 16f
                val docText = "• Aadhaar Card Copy: ${if (data.docAadharAttached) "Yes" else "No"}   |   • Birth Cert/TC: ${if (data.docBirthCertAttached) "Yes" else "No"}   |   • Parent ID: ${if (data.docParentIdAttached) "Yes" else "No"}"
                canvas.drawText(docText, margin + 10f, y, paintNormal)
                y += 20f

                val undertakingText = "Undertaking: ${if (data.undertakingChecked) "I/We hereby certify that updated information is correct as per official records." else "N/A"}"
                canvas.drawText(undertakingText, margin, y, paintNormal)
                y += 35f

                // 5. Signatures
                canvas.drawLine(margin, y, margin + 200f, y, paintBorder)
                canvas.drawLine(A4_WIDTH - margin - 200f, y, A4_WIDTH - margin, y, paintBorder)
                y += 14f
                canvas.drawText("Head of School Signature & Seal", margin, y, paintBold)
                canvas.drawText("Verifying Officer Signature", A4_WIDTH - margin - 200f, y, paintBold)

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

    data class So2PdfData(

        val udiseCode: String,
        val schoolName: String,
        val districtName: String,
        val blockName: String,
        val students: List<So2StudentData>
    )

    data class So2StudentData(
        val srNo: Int,
        val studentName: String,
        val studentClass: String,
        val gender: String,
        val dob: String,
        val penNo: String,
        val fatherName: String,
        val motherName: String,
        val aadharNo: String,
        val nameAsAadhar: String,
        val reasonNotAdded: String
    )

    suspend fun generateSo2Pdf(
        context: Context,
        data: So2PdfData,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                // A4 Landscape: 842 x 595
                val width = 842
                val height = 595
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Set white background for the entire page
                canvas.drawColor(Color.WHITE)

                val paintNormal = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val paintBold = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintHeaderTitle = Paint().apply {
                    color = Color.parseColor("#0D47A1")
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintTableHeader = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintWhiteBg = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }

                val paintBorder = Paint().apply {
                    color = Color.parseColor("#000000")
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                }

                val margin = 20f
                var y = 20f

                // 1. Header Title
                canvas.drawText("Form S02/UDISE / Format for Student ADDITION in UDISE+ (Class-2 to Class-12)", margin, y, paintHeaderTitle)
                y += 16f

                // 2. Metadata Box (White background)
                val metaRect = RectF(margin, y, width - margin, y + 36f)
                canvas.drawRect(metaRect, paintWhiteBg)
                canvas.drawRect(metaRect, paintBorder)

                canvas.drawText("UDISE Code: ${data.udiseCode}", margin + 6f, y + 13f, paintBold)
                canvas.drawText("School Name: ${data.schoolName}", margin + 230f, y + 13f, paintBold)
                canvas.drawText("District: ${data.districtName}", margin + 6f, y + 27f, paintNormal)
                canvas.drawText("Block: ${data.blockName}", margin + 230f, y + 27f, paintNormal)
                canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", margin + 580f, y + 27f, paintNormal)

                y += 44f

                // 3. Table Headers (White background, black text/borders)
                val startX = margin
                val colWidths = listOf(26f, 140f, 32f, 35f, 55f, 55f, 80f, 80f, 72f, 85f, 142f)
                // S.No, Student Name, Class, Gender, DOB, PEN/UDISE, Father, Mother, Aadhar, Name as Aadhar, Reason

                val headers = listOf("S.No", "Student Name", "Class", "Gender", "DOB", "PEN / UDISE", "Father Name", "Mother Name", "Student Aadhar No", "Name As per Aadhar", "Reason Not Added / Remarks")
                var curX = startX
                val headerHeight = 22f

                for (i in headers.indices) {
                    val w = colWidths[i]
                    val cellRect = RectF(curX, y, curX + w, y + headerHeight)
                    canvas.drawRect(cellRect, paintWhiteBg)
                    canvas.drawRect(cellRect, paintBorder)
                    drawFittedText(canvas, headers[i], cellRect, paintTableHeader)
                    curX += w
                }
                y += headerHeight

                // 4. Student Rows (Single row per student record with auto-adjusted fitted text)
                val rowHeight = 22f
                for (student in data.students) {
                    if (y + rowHeight > height - 35f) {
                        break
                    }
                    curX = startX
                    val rowValues = listOf(
                        student.srNo.toString(),
                        student.studentName,
                        student.studentClass,
                        student.gender,
                        student.dob,
                        student.penNo,
                        student.fatherName,
                        student.motherName,
                        student.aadharNo,
                        student.nameAsAadhar,
                        student.reasonNotAdded
                    )

                    for (i in rowValues.indices) {
                        val w = colWidths[i]
                        val cellRect = RectF(curX, y, curX + w, y + rowHeight)
                        canvas.drawRect(cellRect, paintWhiteBg)
                        canvas.drawRect(cellRect, paintBorder)

                        val cellText = if (rowValues[i].isBlank()) "" else rowValues[i]
                        drawFittedText(canvas, cellText, cellRect, paintNormal)
                        curX += w
                    }
                    y += rowHeight
                }

                // 5. Signature Block
                y = maxOf(y + 15f, height - 30f)
                canvas.drawText("Teacher / Headmaster Signature & Seal", margin, y, paintBold)
                canvas.drawText("Block / District Verifying Officer Signature", width - 260f, y, paintBold)

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

    private fun drawFittedText(canvas: Canvas, text: String, rect: RectF, basePaint: Paint, padding: Float = 3f) {
        if (text.isBlank()) return
        val maxWidth = rect.width() - (padding * 2)
        val paint = Paint(basePaint)
        var textSize = paint.textSize

        while (paint.measureText(text) > maxWidth && textSize > 5f) {
            textSize -= 0.5f
            paint.textSize = textSize
        }

        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val y = rect.top + (rect.height() / 2f) + (textHeight / 2f) - fontMetrics.descent
        canvas.drawText(text, rect.left + padding, y, paint)
    }

    data class FurnitureItemData(
        val srNo: Int,
        val blockName: String,
        val schoolName: String,
        val udiseCode: String,
        val availableFurniture: Int,
        val totalEnrolled: Int,
        val furnitureRequired: Int,
        val remarks: String
    )

    data class FurniturePdfData(
        val schoolName: String,
        val blockName: String,
        val udiseCode: String,
        val districtName: String,
        val items: List<FurnitureItemData>
    )

    suspend fun generateFurniturePdf(
        context: Context,
        data: FurniturePdfData,
        outputStream: OutputStream
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val width = 842
                val height = 595
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                val paintNormal = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val paintBold = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintHeaderTitle = Paint().apply {
                    color = Color.parseColor("#0D47A1")
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintTableHeader = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val paintWhiteBg = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }

                val paintBorder = Paint().apply {
                    color = Color.parseColor("#000000")
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                }

                val margin = 20f
                var y = 20f

                // 1. Header Title
                canvas.drawText("फर्नीचर सूचना / Furniture Information Proforma (सत्र 2026-27)", margin, y, paintHeaderTitle)
                y += 16f

                // 2. Metadata Box
                val metaRect = RectF(margin, y, width - margin, y + 36f)
                canvas.drawRect(metaRect, paintWhiteBg)
                canvas.drawRect(metaRect, paintBorder)

                canvas.drawText("UDISE Code: ${data.udiseCode}", margin + 6f, y + 13f, paintBold)
                canvas.drawText("School Name: ${data.schoolName}", margin + 230f, y + 13f, paintBold)
                canvas.drawText("District: ${data.districtName}", margin + 6f, y + 27f, paintNormal)
                canvas.drawText("Block: ${data.blockName}", margin + 230f, y + 27f, paintNormal)
                canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", margin + 580f, y + 27f, paintNormal)

                y += 44f

                // 3. Table Headers
                val startX = margin
                val colWidths = listOf(35f, 105f, 135f, 85f, 85f, 85f, 85f, 120f)
                // S.No, Block Name, School Name, UDISE Code, Available Furn., Total Enrolled, Required Furn., Remarks

                val headers = listOf("S.No", "Block Name", "School Name", "UDISE Code", "Available Furn.", "Total Enrolled", "Required Furn.", "Remarks")
                var curX = startX
                val headerHeight = 22f

                for (i in headers.indices) {
                    val w = colWidths[i]
                    val cellRect = RectF(curX, y, curX + w, y + headerHeight)
                    canvas.drawRect(cellRect, paintWhiteBg)
                    canvas.drawRect(cellRect, paintBorder)
                    canvas.drawText(headers[i], curX + 3f, y + 15f, paintTableHeader)
                    curX += w
                }
                y += headerHeight

                // 4. Data Rows
                val rowHeight = 20f
                var totalAvail = 0
                var totalEnrolled = 0
                var totalReq = 0

                for (item in data.items) {
                    if (y + rowHeight > height - 60f) {
                        break
                    }

                    totalAvail += item.availableFurniture
                    totalEnrolled += item.totalEnrolled
                    totalReq += item.furnitureRequired

                    var rowX = startX
                    val rowValues = listOf(
                        item.srNo.toString(),
                        item.blockName,
                        item.schoolName,
                        item.udiseCode,
                        if (item.availableFurniture > 0) item.availableFurniture.toString() else "0",
                        if (item.totalEnrolled > 0) item.totalEnrolled.toString() else "0",
                        if (item.furnitureRequired > 0) item.furnitureRequired.toString() else "0",
                        item.remarks
                    )

                    for (i in rowValues.indices) {
                        val w = colWidths[i]
                        val cellR = RectF(rowX, y, rowX + w, y + rowHeight)
                        canvas.drawRect(cellR, paintWhiteBg)
                        canvas.drawRect(cellR, paintBorder)
                        val txt = rowValues[i]
                        if (txt.isNotBlank()) {
                            canvas.drawText(txt, rowX + 3f, y + 14f, paintNormal)
                        }
                        rowX += w
                    }
                    y += rowHeight
                }

                // 5. Summary Row
                if (y + rowHeight <= height - 50f) {
                    var sumX = startX
                    val sumValues = listOf("TOTAL", "", "", "", totalAvail.toString(), totalEnrolled.toString(), totalReq.toString(), "")
                    for (i in sumValues.indices) {
                        val w = colWidths[i]
                        val cellR = RectF(sumX, y, sumX + w, y + rowHeight)
                        canvas.drawRect(cellR, paintWhiteBg)
                        canvas.drawRect(cellR, paintBorder)
                        val txt = sumValues[i]
                        if (txt.isNotBlank()) {
                            canvas.drawText(txt, sumX + 3f, y + 14f, paintBold)
                        }
                        sumX += w
                    }
                    y += rowHeight + 10f
                }

                // 6. Signature Block
                y = maxOf(y + 10f, height - 40f)
                canvas.drawText("Teacher / Headmaster Signature & Seal", margin, y, paintBold)
                canvas.drawText("Block / District Verifying Officer Signature", width - 260f, y, paintBold)

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
}

