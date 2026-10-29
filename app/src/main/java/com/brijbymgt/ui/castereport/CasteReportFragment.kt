package com.brijbymgt.ui.castereport

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.databinding.FragmentCasteReportBinding
import com.brijbymgt.util.PdfPrintHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CasteReportFragment : Fragment() {

    private var _binding: FragmentCasteReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CasteReportViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao()
        )
        CasteReportViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCasteReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("caste_prefs", Context.MODE_PRIVATE)
        binding.etCasteSchoolName.setText(prefs.getString("school_name", ""))

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        binding.btnShareCastePdf.setOnClickListener {
            generatePdfFile { pdfFile ->
                PdfPrintHelper.sharePdfFile(requireContext(), pdfFile)
            }
        }

        binding.btnPrintCastePdf.setOnClickListener {
            generatePdfFile { pdfFile ->
                PdfPrintHelper.printPdfFile(requireContext(), pdfFile, "Print Caste Report")
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                buildCasteTable(state)
            }
        }
    }

    private fun buildCasteTable(state: CasteReportUiState) {
        val table = binding.tableCasteReport
        // Keep first 2 header rows
        while (table.childCount > 2) {
            table.removeViewAt(2)
        }

        val rows = state.classRows
        if (rows.isEmpty()) return

        // 1. Class 1 to 5 Rows
        val rows1to5 = rows.take(5)
        for (rowData in rows1to5) {
            table.addView(createDataTableRow(rowData, isSummary = false, isGrandTotal = false))
        }

        // 2. Summary 1-5 Row (Red numbers)
        table.addView(createDataTableRow(state.summary1to5, isSummary = true, isGrandTotal = false))

        // 3. Class 6 to 8 Rows
        val rows6to8 = rows.drop(5)
        for (rowData in rows6to8) {
            table.addView(createDataTableRow(rowData, isSummary = false, isGrandTotal = false))
        }

        // 4. Summary 6-8 Row (Red numbers)
        table.addView(createDataTableRow(state.summary6to8, isSummary = true, isGrandTotal = false))

        // 5. Grand Total Row (Peach Background #FFE0B2, Red numbers)
        table.addView(createDataTableRow(state.grandTotal, isSummary = false, isGrandTotal = true))
    }

    private fun createDataTableRow(
        data: ClassCasteRowData,
        isSummary: Boolean,
        isGrandTotal: Boolean
    ): TableRow {
        val row = TableRow(requireContext()).apply {
            setPadding(2, 6, 2, 6)
            when {
                isGrandTotal -> setBackgroundColor(Color.parseColor("#FFE0B2")) // Peach background
                isSummary -> setBackgroundColor(Color.parseColor("#FFF3E0")) // Light summary bg
                else -> setBackgroundColor(Color.WHITE)
            }
        }

        fun fmt(valNum: Int): String {
            if (valNum <= 0) return "-"
            return String.format(Locale.getDefault(), "%02d", valNum)
        }

        val textColorNormal = Color.parseColor("#0F172A")
        val textColorBlue = Color.parseColor("#014EB1")
        val textColorRed = Color.parseColor("#D92200")
        val textColorSummaryRed = Color.parseColor("#D32F2F")

        fun addCell(textStr: String, textColor: Int, isBold: Boolean) {
            val tv = TextView(requireContext()).apply {
                text = textStr
                textSize = if (isGrandTotal) 12f else 11f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(textColor)
                if (isBold || isSummary || isGrandTotal) {
                    setTypeface(null, Typeface.BOLD)
                }
                setPadding(6, 6, 6, 6)
            }
            row.addView(tv)
        }

        val totalColor = if (isSummary || isGrandTotal) textColorSummaryRed else textColorRed

        // Class name
        addCell(data.className, if (isSummary || isGrandTotal) textColorSummaryRed else textColorNormal, isBold = true)

        // SC (Male, Female, Total)
        addCell(fmt(data.scMale), textColorBlue, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.scFemale), textColorRed, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.scTotal), totalColor, isBold = true)

        // ST (Male, Female, Total)
        addCell(fmt(data.stMale), textColorBlue, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.stFemale), textColorRed, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.stTotal), totalColor, isBold = true)

        // OBC (Male, Female, Total)
        addCell(fmt(data.obcMale), textColorBlue, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.obcFemale), textColorRed, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.obcTotal), totalColor, isBold = true)

        // MINORITY (Male, Female, Total)
        addCell(fmt(data.minMale), textColorBlue, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.minFemale), textColorRed, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.minTotal), totalColor, isBold = true)

        // GEN (Male, Female, Total)
        addCell(fmt(data.genMale), textColorBlue, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.genFemale), textColorRed, isBold = isSummary || isGrandTotal)
        addCell(fmt(data.genTotal), totalColor, isBold = true)

        // GRAND TOTAL (Male, Female, Total)
        addCell(fmt(data.rowMaleTotal), textColorBlue, isBold = true)
        addCell(fmt(data.rowFemaleTotal), textColorRed, isBold = true)
        addCell(fmt(data.rowGrandTotal), textColorSummaryRed, isBold = true)

        return row
    }

    private fun generatePdfFile(onGenerated: (File) -> Unit) {
        lifecycleScope.launch {
            val schoolName = binding.etCasteSchoolName.text?.toString() ?: ""
            requireContext().getSharedPreferences("caste_prefs", Context.MODE_PRIVATE).edit {
                putString("school_name", schoolName)
            }

            val state = viewModel.uiState.value
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            fun mapToPdfData(d: ClassCasteRowData) = PdfPrintHelper.CasteRowPdfData(
                className = d.className,
                scMale = d.scMale, scFemale = d.scFemale,
                stMale = d.stMale, stFemale = d.stFemale,
                obcMale = d.obcMale, obcFemale = d.obcFemale,
                minMale = d.minMale, minFemale = d.minFemale,
                genMale = d.genMale, genFemale = d.genFemale
            )

            val castePdfRows = state.classRows.map { mapToPdfData(it) }
            val pdfFile = File(requireContext().cacheDir, "Caste_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = pdfFile.outputStream()

            val success = PdfPrintHelper.generateCasteReportPdf(
                context = requireContext(),
                reportTitle = "CASTE-WISE STUDENT REPORT",
                schoolName = schoolName,
                dateString = dateStr,
                casteRows = castePdfRows,
                summary1to5 = mapToPdfData(state.summary1to5),
                summary6to8 = mapToPdfData(state.summary6to8),
                grandTotal = mapToPdfData(state.grandTotal),
                outputStream = outputStream
            )
            outputStream.close()

            if (success) {
                onGenerated(pdfFile)
            } else {
                Toast.makeText(requireContext(), "Failed to generate Caste Report PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

