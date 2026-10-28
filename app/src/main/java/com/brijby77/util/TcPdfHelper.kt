package com.brijby77.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
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
import java.util.Locale

data class TcDetails(
    val tcNumber: String,
    val struckOffClass: String,
    val lastAdmissionDate: String,
    val udiseCode: String,
    val schoolName: String,
    val schoolAddress: String,
    val tehsil: String,
    val district: String,
    val durationOfStay: String,
    val language: String,
    val workingDays: String,
    val attendance: String,
    val fatherOccupation: String,
    val dateOfLeaving: String,
    val reasonForLeaving: String,
    val conduct: String,
    val remarks: String,
    val dateOfIssue: String
)

object TcPdfHelper {

    private const val A4_WIDTH = 595 // A4 width in points (72 dpi)
    private const val A4_HEIGHT = 842 // A4 height in points

    suspend fun generateTcPdfFile(
        context: Context,
        item: StudentWithDetails,
        tc: TcDetails
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "Transfer_Certificate_${item.student.srNumber.ifBlank { "student" }}.pdf")
                val outputStream = FileOutputStream(file)
                val success = generateTransferCertificatePdf(item, tc, outputStream)
                outputStream.close()
                if (success) file else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun generateTransferCertificatePdf(
        item: StudentWithDetails,
        tc: TcDetails,
        outputStream: OutputStream
    ): Boolean {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paintGreenBold = Paint().apply {
                color = Color.parseColor("#2E7D32") // Dark Green for Student Copy
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val paintRedBold = Paint().apply {
                color = Color.parseColor("#D32F2F") // Dark Red for Record Copy
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Draw Two Copies on A4 (Top Half & Bottom Half)
            drawTcCopy(canvas, item, tc, 14f, "Student Copy", paintGreenBold)
            
            // Dashed Divider Line in the middle
            val paintDash = Paint().apply {
                color = Color.parseColor("#94A3B8")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            canvas.drawLine(20f, 424f, (A4_WIDTH - 20).toFloat(), 424f, paintDash)

            drawTcCopy(canvas, item, tc, 436f, "Record Copy", paintRedBold)

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun drawTcCopy(canvas: Canvas, item: StudentWithDetails, tc: TcDetails, startY: Float, copyLabel: String, labelPaint: Paint) {
        val s = item.student

        val paintBlackBold = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBlackNormal = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Normal (non-italic) font style for blue text as requested
        val paintBlueNormal = Paint().apply {
            color = Color.parseColor("#0D47A1") // Royal Blue / Pen Ink Blue
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintBlueBold = Paint().apply {
            color = Color.parseColor("#0D47A1") // Royal Blue
            textSize = 9f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTitle = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBg = Paint().apply {
            color = Color.parseColor("#DCECC9") // Light Pistachio Background (#DCECC9)
            style = Paint.Style.FILL
        }

        val paintBorder = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val paintBoxBorder = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }

        // Draw background and outer border for single copy
        canvas.drawRect(20f, startY, (A4_WIDTH - 20).toFloat(), startY + 404f, paintBg)
        canvas.drawRect(20f, startY, (A4_WIDTH - 20).toFloat(), startY + 404f, paintBorder)

        var y = startY + 16f
        val marginX = 26f

        // Top Certificate No. and Copy Label
        canvas.drawText("प्रमाण पत्र की सं0 : ${tc.tcNumber}", marginX, y, paintBlackBold)
        canvas.drawText(copyLabel, A4_WIDTH - 90f, y, labelPaint)
        y += 11f
        if (tc.udiseCode.isNotBlank()) {
            canvas.drawText(tc.udiseCode, A4_WIDTH - 90f, y, labelPaint)
        }
        y += 13f

        // Header Title
        canvas.drawText("बेसिक शिक्षा परिषद", (A4_WIDTH / 2f) - 55f, y, paintTitle)
        y += 12f
        canvas.drawText("(लिविंग सर्टिफिकेट) प्रस्थान प्रमाण- पत्र", (A4_WIDTH / 2f) - 80f, y, paintSubtitle)
        y += 15f

        // UDISE Code (Bold, Proper Case "Udise Code : ") right above School Name
        if (tc.udiseCode.isNotBlank()) {
            canvas.drawText("Udise Code : ", marginX, y, paintBlackBold)
            canvas.drawText(tc.udiseCode, marginX + 65f, y - 1f, paintBlueBold)
            y += 14f
        }

        // School Name & Address
        canvas.drawText("विद्यालय का नाम : __________________________________ पता : __________________________________", marginX, y, paintBlackNormal)
        canvas.drawText(toProperCase(tc.schoolName), marginX + 75f, y - 2f, paintBlueNormal)
        canvas.drawText(toProperCase(tc.schoolAddress), marginX + 275f, y - 2f, paintBlueNormal)
        y += 14f

        // विवरण Heading
        canvas.drawText("विवरण", (A4_WIDTH / 2f) - 12f, y, paintBlackBold)
        y += 13f

        fun drawField(label: String, value: String, x: Float, yPos: Float, labelWidth: Float = 120f) {
            canvas.drawText(label, x, yPos, paintBlackNormal)
            canvas.drawText(value, x + labelWidth, yPos - 1f, paintBlueNormal)
        }

        fun drawBoxedDigits(label: String, digits: String, x: Float, yPos: Float, boxCount: Int, isBold: Boolean = false) {
            canvas.drawText(label, x, yPos, paintBlackNormal)
            val startX = x + 72f
            val boxWidth = if (boxCount > 10) 9f else 9.5f
            val boxHeight = 12f
            val paddedDigits = digits.padStart(boxCount, ' ').takeLast(boxCount)
            val digitPaint = if (isBold) paintBlueBold else paintBlueNormal
            for (i in 0 until boxCount) {
                val boxLeft = startX + (i * boxWidth)
                val boxTop = yPos - 9.5f
                canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, paintBoxBorder)
                val char = paddedDigits[i].toString()
                if (char.isNotBlank()) {
                    canvas.drawText(char, boxLeft + 2f, yPos - 1f, digitPaint)
                }
            }
        }

        // Row 1: Student Name in UPPERCASE (Capital Letters)
        drawField("नाम विद्यार्थी:", s.studentName.uppercase(Locale.getDefault()), marginX, y, 65f)
        drawField("(अ) स्कूल छोड़ने का कारण:", tc.reasonForLeaving, marginX + 245f, y, 110f)
        y += 14f

        // Row 2: Aadhar & Mobile (12 and 10 boxes)
        drawBoxedDigits("आधार कार्ड नं0:", s.studentAdhar, marginX, y, 12, isBold = false)
        drawBoxedDigits("मोबाइल नं0:", s.mobileNo, marginX + 235f, y, 10, isBold = false)
        y += 14f

        // Row 3: Unique ID & PEN No (12 and 11 boxes) - PEN No is Bold
        drawBoxedDigits("यूनिक आईडी नं0:", s.uniqueId, marginX, y, 12, isBold = false)
        drawBoxedDigits("पेन नं0:", s.penNo, marginX + 235f, y, 11, isBold = true)
        y += 14f

        // Row 4: Aapar ID & Father Name (Proper Case)
        drawBoxedDigits("अपार आई.डी.नं0:", s.aaparId, marginX, y, 12, isBold = false)
        drawField("पिता का नाम:", toProperCase(s.fatherName), marginX + 235f, y, 55f)
        y += 14f

        // Row 5: Mother Name (Proper Case) & Conduct
        drawField("माता का नाम:", toProperCase(s.motherName), marginX, y, 60f)
        drawField("(ब) आचरण:", tc.conduct, marginX + 245f, y, 45f)
        y += 14f

        // Row 6: DOB & Exam Passed
        drawField("जन्म तिथि:", s.dob, marginX, y, 55f)
        drawField("नाम उच्च उत्तीर्ण:", "-", marginX + 245f, y, 75f)
        y += 14f

        // Row 7: Age & Passing Date
        canvas.drawText("स्कूल छोड़ते समय की आयु:", marginX, y, paintBlackNormal)
        canvas.drawText("वर्ष: ____ मास: ____", marginX + 105f, y, paintBlackNormal)
        canvas.drawBlueTextIfPresent("11", marginX + 125f, y - 1f, paintBlueNormal)
        canvas.drawBlueTextIfPresent("4", marginX + 168f, y - 1f, paintBlueNormal)
        drawField("उत्तीर्ण तिथि:", "-", marginX + 245f, y, 55f)
        y += 14f

        // Row 8: Class (Struck off class from editable input or fallback to student class)
        drawField("तथा कक्षा जिसमें नाम कटा:", tc.struckOffClass.ifBlank { s.studentClass }, marginX, y, 120f)
        y += 14f

        // Row 9: Caste
        drawField("जाति (यदि हिन्दू हो) अन्यथा धर्म अन्य आवश्यक सूचना:", s.caste, marginX, y, 195f)
        y += 14f

        // Row 10: Address & Language (Editable)
        drawField("निवास स्थान:", s.address, marginX, y, 60f)
        drawField("(द) विद्यार्थी की भाषा हिन्दी या उर्दू:", tc.language, marginX + 245f, y, 125f)
        y += 14f

        // Row 11: Tehsil (Editable), District (Editable), Free student
        drawField("तहसील:", tc.tehsil, marginX, y, 35f)
        drawField("जिला:", tc.district, marginX + 130f, y, 30f)
        drawField("(1) क्या छात्र निःशुल्क था ?", "हाँ (Yes)", marginX + 245f, y, 110f)
        y += 14f

        // Row 12: Duration of Stay (Editable)
        drawField("उत्तर प्रदेश में रहने की अवधि:", tc.durationOfStay, marginX, y, 120f)
        drawField("(2) माध्यम उप कक्षा निर्धारित तिथि से:", "-", marginX + 245f, y, 135f)
        y += 14f

        // Row 13: First admission date & Working Days (Editable)
        drawField("प्रथम स्कूल में प्रवेश का दिनांक:", s.admissionDate, marginX, y, 130f)
        drawField("(अ) स्कूल खुलने की दिनों की संख्या:", tc.workingDays, marginX + 245f, y, 135f)
        y += 14f

        // Row 14: Last admission date (Editable) & Attendance (Editable)
        drawField("स्कूल में प्रवेश का अन्तिम दिनांक:", tc.lastAdmissionDate.ifBlank { s.admissionDate }, marginX, y, 130f)
        drawField("(ब) विद्यार्थी की उपस्थिति सं0:", tc.attendance, marginX + 245f, y, 125f)
        y += 14f

        // Row 15: SR Number & Leave
        drawField("प्रवेश रजिस्टर की क्रम सं0:", s.srNumber, marginX, y, 120f)
        drawField("(स) बीमारी के कारण अवकाश सं0:", "0", marginX + 245f, y, 125f)
        y += 14f

        // Row 16: Last SR & Father Occupation (Editable)
        drawField("स्कूल में प्रवेश का अन्तिम क्रम सं0:", s.srNumber, marginX, y, 135f)
        drawField("(द) पिता का व्यवसाय:", tc.fatherOccupation, marginX + 245f, y, 90f)
        y += 14f

        // Row 17: Date of leaving
        drawField("स्कूल छोड़ने की तिथि:", tc.dateOfLeaving, marginX, y, 95f)
        y += 18f

        // Row 18: Date, Remarks & Signature in the same row
        val dateStr = "दिनांक: ${tc.dateOfIssue.take(2)}   मास: ${tc.dateOfIssue.drop(3).take(2)}   सत्र: 2025-26"
        canvas.drawText(dateStr, marginX, y, paintBlackNormal)
        if (tc.remarks.isNotBlank()) {
            canvas.drawText("विशेष विवरण:", marginX + 175f, y, paintBlackNormal)
            canvas.drawText(tc.remarks, marginX + 230f, y - 1f, paintBlueNormal)
        }
        canvas.drawText("हस्ताक्षर प्रधानाध्यापक / प्रधानाध्यापिका", A4_WIDTH - 165f, y, paintBlackBold)
        y += 14f

        // Footer Note
        val notePaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 6f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("नोट:- 1. उपर्युक्त उल्लेख शुद्ध और सुन्दर हो तथा प्रधानाध्यापक/प्रधानाध्यापिका अपने हस्ताक्षर करें। 2. प्रमाण-पत्र छात्र रजिस्टर के अनुसार हो।", marginX, y, notePaint)
    }

    private fun toProperCase(text: String): String {
        if (text.isBlank()) return "-"
        return text.lowercase().split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
        }
    }

    private fun Canvas.drawBlueTextIfPresent(text: String, x: Float, y: Float, paint: Paint) {
        if (text.isNotBlank() && text != "-") {
            drawText(text, x, y, paint)
        }
    }

    fun printTcPdf(context: Context, pdfFile: File, jobName: String) {
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
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareTcPdf(context: Context, pdfFile: File) {
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
            context.startActivity(Intent.createChooser(shareIntent, "Share Transfer Certificate PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

