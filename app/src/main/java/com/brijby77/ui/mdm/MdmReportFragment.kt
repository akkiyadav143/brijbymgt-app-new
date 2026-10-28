package com.brijby77.ui.mdm

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.brijby77.SchoolApp
import com.brijby77.data.repository.StudentRepository
import com.brijby77.databinding.FragmentMdmReportBinding
import com.brijby77.util.PdfPrintHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MdmReportFragment : Fragment() {

    private var _binding: FragmentMdmReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MdmViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao()
        )
        MdmViewModelFactory(repo)
    }

    private data class RowViewHolder(
        val tvDbTotal: TextView,
        val tvDbMF: TextView,
        val etMdmMale: EditText,
        val etMdmFemale: EditText,
        val tvMdmTotal: TextView,
        val tvDiffMF: TextView
    )

    private val rowViewsMap = mutableMapOf<String, RowViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMdmReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTable()
        setupButtons()
        setupClearFocusOnTouchOutside()
        observeData()
    }

    private fun setupClearFocusOnTouchOutside() {
        val touchListener = View.OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentFocusView = requireActivity().currentFocus
                if (currentFocusView is EditText) {
                    currentFocusView.clearFocus()
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                }
            }
            false
        }

        binding.root.setOnTouchListener(touchListener)
        binding.rootMdmLayout.setOnTouchListener(touchListener)
        binding.tableMdm.setOnTouchListener(touchListener)
    }

    private fun initTable() {
        val table = binding.tableMdm
        if (table.childCount > 1) return

        rowViewsMap.clear()
        val classNames = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8")

        for (i in classNames.indices) {
            val className = classNames[i]
            val tableRow = TableRow(requireContext()).apply {
                setPadding(4, 8, 4, 8)
            }

            val tvClass = TextView(requireContext()).apply {
                text = className
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
            }

            val tvDbTotal = TextView(requireContext()).apply {
                text = "0"
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#2E7D32"))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            val tvDbMF = TextView(requireContext()).apply {
                text = Html.fromHtml("<font color='#014EB1'><b>0</b></font>/<font color='#D92200'><b>0</b></font>", Html.FROM_HTML_MODE_LEGACY)
                textSize = 12.5f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            val etMdmMale = EditText(requireContext()).apply {
                hint = "0"
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#014EB1"))
                inputType = InputType.TYPE_CLASS_NUMBER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setBackgroundResource(android.R.drawable.editbox_background)
                imeOptions = EditorInfo.IME_ACTION_NEXT
            }

            val etMdmFemale = EditText(requireContext()).apply {
                hint = "0"
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#D92200"))
                inputType = InputType.TYPE_CLASS_NUMBER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setBackgroundResource(android.R.drawable.editbox_background)
                imeOptions = if (i < classNames.size - 1) EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_DONE
            }

            val tvMdmTotal = TextView(requireContext()).apply {
                text = "0"
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1565C0"))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            val tvDiffMF = TextView(requireContext()).apply {
                text = Html.fromHtml("<font color='#014EB1'><b>0</b></font>/<font color='#D92200'><b>0</b></font>", Html.FROM_HTML_MODE_LEGACY)
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            // Focus behavior: Auto-clear "0" or select all on focus
            etMdmMale.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if (etMdmMale.text.toString() == "0") {
                        etMdmMale.setText("")
                    } else {
                        etMdmMale.selectAll()
                    }
                } else if (etMdmMale.text.isNullOrBlank()) {
                    etMdmMale.setText("0")
                }
            }

            etMdmFemale.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if (etMdmFemale.text.toString() == "0") {
                        etMdmFemale.setText("")
                    } else {
                        etMdmFemale.selectAll()
                    }
                } else if (etMdmFemale.text.isNullOrBlank()) {
                    etMdmFemale.setText("0")
                }
            }

            // Action Next: Male -> Female
            etMdmMale.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    etMdmFemale.requestFocus()
                    true
                } else false
            }

            // Action Next: Female -> Next Class Male
            if (i < classNames.size - 1) {
                val nextClassName = classNames[i + 1]
                etMdmFemale.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        rowViewsMap[nextClassName]?.etMdmMale?.requestFocus()
                        true
                    } else false
                }
            }

            etMdmMale.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val maleVal = s?.toString()?.toIntOrNull() ?: 0
                    val femaleVal = etMdmFemale.text.toString().toIntOrNull() ?: 0
                    tvMdmTotal.text = (maleVal + femaleVal).toString()
                    if (etMdmMale.hasFocus()) {
                        viewModel.updateMdmInput(className, maleVal, femaleVal)
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            etMdmFemale.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val maleVal = etMdmMale.text.toString().toIntOrNull() ?: 0
                    val femaleVal = s?.toString()?.toIntOrNull() ?: 0
                    tvMdmTotal.text = (maleVal + femaleVal).toString()
                    if (etMdmFemale.hasFocus()) {
                        viewModel.updateMdmInput(className, maleVal, femaleVal)
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            tableRow.addView(tvClass)
            tableRow.addView(tvDbTotal)
            tableRow.addView(tvDbMF)
            tableRow.addView(etMdmMale)
            tableRow.addView(etMdmFemale)
            tableRow.addView(tvMdmTotal)
            tableRow.addView(tvDiffMF)

            table.addView(tableRow)

            rowViewsMap[className] = RowViewHolder(tvDbTotal, tvDbMF, etMdmMale, etMdmFemale, tvMdmTotal, tvDiffMF)
        }
    }

    private fun setupButtons() {
        binding.btnSaveMdm.setOnClickListener {
            viewModel.saveMdmReports()
            Toast.makeText(requireContext(), "MDM Report Saved Successfully!", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetMdm.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset MDM Inputs")
                .setMessage("Are you sure you want to clear all MDM input fields for re-inputting?")
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.resetMdmInputs()
                    for (holder in rowViewsMap.values) {
                        holder.etMdmMale.setText("0")
                        holder.etMdmFemale.setText("0")
                        holder.tvMdmTotal.text = "0"
                    }
                    Toast.makeText(requireContext(), "MDM inputs reset for re-input", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnShareMdmPdf.setOnClickListener {
            generatePdfFile { pdfFile ->
                PdfPrintHelper.sharePdfFile(requireContext(), pdfFile)
            }
        }

        binding.btnPrintMdmPdf.setOnClickListener {
            generatePdfFile { pdfFile ->
                PdfPrintHelper.printPdfFile(requireContext(), pdfFile, "Print MDM Report")
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.tvDbCount1to5.text = state.dbTotal1to5.toString()
                binding.tvDbCount6to8.text = state.dbTotal6to8.toString()
                binding.tvDbCount1to8.text = state.dbTotal1to8.toString()

                // Summaries formatted as requested:
                // Class (1-5) (db male) male MDM | (db female) female mdm | (db Total) Total mdm
                // Male in Blue #014EB1, Female in Red #D92200, Total in Green #2E7D32

                val html1to5 = "Class (1-5)  " +
                        "<font color='#014EB1'><b>(${state.dbSummary1to5.first}) ${state.summary1to5.first} male MDM</b></font> | " +
                        "<font color='#D92200'><b>(${state.dbSummary1to5.second}) ${state.summary1to5.second} female mdm</b></font> | " +
                        "<font color='#2E7D32'><b>(${state.dbSummary1to5.third}) ${state.summary1to5.third} Total mdm</b></font>"

                val html6to8 = "Class (6-8)  " +
                        "<font color='#014EB1'><b>(${state.dbSummary6to8.first}) ${state.summary6to8.first} male MDM</b></font> | " +
                        "<font color='#D92200'><b>(${state.dbSummary6to8.second}) ${state.summary6to8.second} female mdm</b></font> | " +
                        "<font color='#2E7D32'><b>(${state.dbSummary6to8.third}) ${state.summary6to8.third} Total mdm</b></font>"

                val html1to8 = "Class (1-8)  " +
                        "<font color='#014EB1'><b>(${state.dbSummary1to8.first}) ${state.summary1to8.first} male MDM</b></font> | " +
                        "<font color='#D92200'><b>(${state.dbSummary1to8.second}) ${state.summary1to8.second} female mdm</b></font> | " +
                        "<font color='#2E7D32'><b>(${state.dbSummary1to8.third}) ${state.summary1to8.third} Total mdm</b></font>"

                binding.tvMdmSummary1to5.text = Html.fromHtml(html1to5, Html.FROM_HTML_MODE_LEGACY)
                binding.tvMdmSummary6to8.text = Html.fromHtml(html6to8, Html.FROM_HTML_MODE_LEGACY)
                binding.tvMdmSummary1to8.text = Html.fromHtml(html1to8, Html.FROM_HTML_MODE_LEGACY)

                updateTableValues(state.classRows)
            }
        }
    }

    private fun updateTableValues(rows: List<ClassMdmData>) {
        for (row in rows) {
            val holder = rowViewsMap[row.className] ?: continue
            holder.tvDbTotal.text = row.dbTotal.toString()

            val dbMfHtml = "<font color='#014EB1'><b>${row.dbMale}</b></font>/<font color='#D92200'><b>${row.dbFemale}</b></font>"
            holder.tvDbMF.text = Html.fromHtml(dbMfHtml, Html.FROM_HTML_MODE_LEGACY)

            holder.tvMdmTotal.text = row.mdmTotal.toString()

            val diffMale = row.dbMale - row.mdmMale
            val diffFemale = row.dbFemale - row.mdmFemale
            val diffHtml = "<font color='#014EB1'><b>$diffMale</b></font>/<font color='#D92200'><b>$diffFemale</b></font>"
            holder.tvDiffMF.text = Html.fromHtml(diffHtml, Html.FROM_HTML_MODE_LEGACY)

            if (!holder.etMdmMale.hasFocus()) {
                val maleStr = row.mdmMale.toString()
                if (holder.etMdmMale.text.toString() != maleStr) {
                    holder.etMdmMale.setText(maleStr)
                }
            }

            if (!holder.etMdmFemale.hasFocus()) {
                val femaleStr = row.mdmFemale.toString()
                if (holder.etMdmFemale.text.toString() != femaleStr) {
                    holder.etMdmFemale.setText(femaleStr)
                }
            }
        }
    }

    private fun generatePdfFile(onGenerated: (File) -> Unit) {
        lifecycleScope.launch {
            val state = viewModel.uiState.value
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val mdmPdfRows = state.classRows.map { row ->
                PdfPrintHelper.MdmRowPdfData(
                    studentClass = row.className,
                    dbTotal = row.dbTotal,
                    dbMale = row.dbMale,
                    dbFemale = row.dbFemale,
                    mdmMale = row.mdmMale,
                    mdmFemale = row.mdmFemale,
                    mdmTotal = row.mdmTotal
                )
            }

            val pdfFile = File(requireContext().cacheDir, "MDM_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = pdfFile.outputStream()

            val success = PdfPrintHelper.generateMdmReportPdf(
                context = requireContext(),
                reportTitle = "MDM SCHOOL REPORT (CLASSES 1–8)",
                dateString = dateStr,
                mdmRows = mdmPdfRows,
                summary1to5 = state.summary1to5,
                summary6to8 = state.summary6to8,
                summary1to8 = state.summary1to8,
                outputStream = outputStream
            )
            outputStream.close()

            if (success) {
                onGenerated(pdfFile)
            } else {
                Toast.makeText(requireContext(), "Failed to generate MDM PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rowViewsMap.clear()
        _binding = null
    }
}

