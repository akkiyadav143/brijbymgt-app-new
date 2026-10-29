package com.brijbymgt.ui.furniture

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.databinding.FragmentFurnitureBinding
import com.brijbymgt.util.PdfPrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FurnitureFragment : Fragment() {

    private var _binding: FragmentFurnitureBinding? = null
    private val binding get() = _binding!!

    private val furnitureRowViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFurnitureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCachedInputs()
        setupListeners()

        if (furnitureRowViews.isEmpty()) {
            addFurnitureRow()
        }
        updateSummaryTotals()
    }

    override fun onPause() {
        super.onPause()
        saveCachedInputs()
    }

    private fun loadCachedInputs() {
        val prefs = requireContext().getSharedPreferences("furniture_prefs", Context.MODE_PRIVATE)
        binding.etFurnitureTotalStudents.setText(prefs.getString("total_students", ""))
        binding.etFurnitureRatio.setText(prefs.getString("ratio", "3"))
        binding.etFurnitureRequirement.setText(prefs.getString("requirement", ""))
        binding.etFurnitureUdiseCode.setText(prefs.getString("udise_code", ""))
        binding.etFurnitureDistrictName.setText(prefs.getString("district_name", ""))

        if (binding.etFurnitureTotalStudents.text.isNullOrBlank()) {
            fetchTotalStudentsFromDatabase()
        }

        val count = prefs.getInt("row_count", 0)
        if (count > 0) {
            binding.llFurnitureRowsContainer.removeAllViews()
            furnitureRowViews.clear()
            for (i in 0 until count) {
                addFurnitureRow(
                    totalStudentsVal = prefs.getString("row_total_students_$i", "") ?: "",
                    ratioVal = prefs.getString("row_ratio_$i", "") ?: "",
                    requiredVal = prefs.getString("row_required_$i", "") ?: "",
                    available = prefs.getString("row_available_$i", "") ?: "",
                    enrolled = prefs.getString("row_enrolled_$i", "") ?: "",
                    remarks = prefs.getString("row_remarks_$i", "") ?: ""
                )
            }
        }
    }

    private fun fetchTotalStudentsFromDatabase() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SchoolApp
                val dao = app.database.studentDao()
                val allStudents = withContext(Dispatchers.IO) {
                    dao.getAllActiveStudentsSync()
                }
                binding.etFurnitureTotalStudents.setText(allStudents.size.toString())
                calculateRequirement()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateRequirement() {
        val totalStudents = binding.etFurnitureTotalStudents.text?.toString()?.toIntOrNull() ?: 0
        val ratio = binding.etFurnitureRatio.text?.toString()?.toIntOrNull() ?: 3
        if (ratio > 0) {
            val req = Math.ceil(totalStudents.toDouble() / ratio.toDouble()).toInt()
            binding.etFurnitureRequirement.setText(req.toString())
        }
    }

    private fun saveCachedInputs() {
        val prefs = requireContext().getSharedPreferences("furniture_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("total_students", binding.etFurnitureTotalStudents.text?.toString() ?: "")
            putString("ratio", binding.etFurnitureRatio.text?.toString() ?: "")
            putString("requirement", binding.etFurnitureRequirement.text?.toString() ?: "")
            putString("udise_code", binding.etFurnitureUdiseCode.text?.toString() ?: "")
            putString("district_name", binding.etFurnitureDistrictName.text?.toString() ?: "")

            putInt("row_count", furnitureRowViews.size)
            for ((index, rowView) in furnitureRowViews.withIndex()) {
                putString("row_total_students_$index", rowView.findViewById<EditText>(R.id.etRowTotalStudents).text?.toString() ?: "")
                putString("row_ratio_$index", rowView.findViewById<EditText>(R.id.etRowRatio).text?.toString() ?: "")
                putString("row_required_$index", rowView.findViewById<EditText>(R.id.etRowFurnitureRequired).text?.toString() ?: "")
                putString("row_available_$index", rowView.findViewById<EditText>(R.id.etRowAvailableFurniture).text?.toString() ?: "")
                putString("row_enrolled_$index", rowView.findViewById<EditText>(R.id.etRowTotalEnrolled).text?.toString() ?: "")
                putString("row_remarks_$index", rowView.findViewById<EditText>(R.id.etRowRemarks).text?.toString() ?: "")
            }
        }
    }

    private fun setupListeners() {
        binding.etFurnitureTotalStudents.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateRequirement()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etFurnitureRatio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateRequirement()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnAddFurnitureRow.setOnClickListener {
            addFurnitureRow()
            updateSummaryTotals()
        }

        binding.btnSaveFurniture.setOnClickListener {
            saveCachedInputs()
            updateSummaryTotals()
            Toast.makeText(requireContext(), "Furniture information saved successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetFurniture.setOnClickListener {
            binding.etFurnitureTotalStudents.setText("")
            binding.etFurnitureRatio.setText("3")
            binding.etFurnitureRequirement.setText("")
            binding.etFurnitureUdiseCode.setText("")
            binding.etFurnitureDistrictName.setText("")
            binding.llFurnitureRowsContainer.removeAllViews()
            furnitureRowViews.clear()
            val prefs = requireContext().getSharedPreferences("furniture_prefs", Context.MODE_PRIVATE)
            prefs.edit { clear() }
            fetchTotalStudentsFromDatabase()
            addFurnitureRow()
            updateSummaryTotals()
            Toast.makeText(requireContext(), "Reset successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrintFurniturePdf.setOnClickListener {
            generatePdf { pdfFile ->
                PdfPrintHelper.printPdfFile(requireContext(), pdfFile, "Print Furniture Report")
            }
        }

        binding.btnShareFurniturePdf.setOnClickListener {
            generatePdf { pdfFile ->
                PdfPrintHelper.sharePdfFile(requireContext(), pdfFile)
            }
        }
    }

    private fun addFurnitureRow(
        totalStudentsVal: String = "",
        ratioVal: String = "",
        requiredVal: String = "",
        available: String = "",
        enrolled: String = "",
        remarks: String = ""
    ) {
        val rowView = layoutInflater.inflate(R.layout.item_furniture_row, binding.llFurnitureRowsContainer, false)

        val etTotalStudents = rowView.findViewById<EditText>(R.id.etRowTotalStudents)
        val etRatio = rowView.findViewById<EditText>(R.id.etRowRatio)
        val etRequired = rowView.findViewById<EditText>(R.id.etRowFurnitureRequired)
        val etAvailable = rowView.findViewById<EditText>(R.id.etRowAvailableFurniture)
        val etEnrolled = rowView.findViewById<EditText>(R.id.etRowTotalEnrolled)

        if (totalStudentsVal.isNotBlank()) {
            etTotalStudents.setText(totalStudentsVal)
        } else {
            val parentTotal = binding.etFurnitureTotalStudents.text?.toString() ?: ""
            if (parentTotal.isNotBlank()) {
                etTotalStudents.setText(parentTotal)
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val app = requireActivity().application as SchoolApp
                        val dao = app.database.studentDao()
                        val allStudents = withContext(Dispatchers.IO) {
                            dao.getAllActiveStudentsSync()
                        }
                        etTotalStudents.setText(allStudents.size.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (ratioVal.isNotBlank()) {
            etRatio.setText(ratioVal)
        } else {
            val parentRatio = binding.etFurnitureRatio.text?.toString() ?: "3"
            etRatio.setText(parentRatio)
        }

        etAvailable.setText(available)
        etEnrolled.setText(enrolled)
        etRequired.setText(requiredVal)
        rowView.findViewById<EditText>(R.id.etRowRemarks).setText(remarks)

        fun calcRowReq() {
            val ts = etTotalStudents.text?.toString()?.toIntOrNull() ?: 0
            val rat = etRatio.text?.toString()?.toIntOrNull() ?: 3
            if (rat > 0) {
                val req = Math.ceil(ts.toDouble() / rat.toDouble()).toInt()
                etRequired.setText(req.toString())
            }
        }

        if (etRequired.text.isNullOrBlank()) {
            calcRowReq()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcRowReq()
                updateSummaryTotals()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etTotalStudents.addTextChangedListener(textWatcher)
        etRatio.addTextChangedListener(textWatcher)
        etAvailable.addTextChangedListener(textWatcher)
        etEnrolled.addTextChangedListener(textWatcher)
        etRequired.addTextChangedListener(textWatcher)

        rowView.findViewById<ImageButton>(R.id.btnDeleteFurnitureRow).setOnClickListener {
            if (furnitureRowViews.size > 1) {
                binding.llFurnitureRowsContainer.removeView(rowView)
                furnitureRowViews.remove(rowView)
                updateRowNumbers()
                updateSummaryTotals()
                saveCachedInputs()
            } else {
                Toast.makeText(requireContext(), "At least one row is required", Toast.LENGTH_SHORT).show()
            }
        }

        furnitureRowViews.add(rowView)
        binding.llFurnitureRowsContainer.addView(rowView)
        updateRowNumbers()
    }

    private fun updateRowNumbers() {
        for ((index, view) in furnitureRowViews.withIndex()) {
            view.findViewById<TextView>(R.id.tvFurnitureRowTitle).text = "Row #${index + 1}"
        }
    }

    private fun updateSummaryTotals() {
        var totalAvailable = 0
        var totalEnrolled = 0
        var totalRequired = 0

        for (view in furnitureRowViews) {
            val avail = view.findViewById<EditText>(R.id.etRowAvailableFurniture).text?.toString()?.toIntOrNull() ?: 0
            val enr = view.findViewById<EditText>(R.id.etRowTotalEnrolled).text?.toString()?.toIntOrNull()?.let { if (it > 0) it else view.findViewById<EditText>(R.id.etRowTotalStudents).text?.toString()?.toIntOrNull() ?: 0 } ?: 0
            val req = view.findViewById<EditText>(R.id.etRowFurnitureRequired).text?.toString()?.toIntOrNull() ?: 0

            totalAvailable += avail
            totalEnrolled += enr
            totalRequired += req
        }

        binding.tvTotalAvailableFurnitureSummary.text = "Total Available Furniture: $totalAvailable"
        binding.tvTotalEnrolledSummary.text = "Total Enrolled Students: $totalEnrolled"
        binding.tvTotalFurnitureRequiredSummary.text = "Total Furniture Required: $totalRequired"
    }

    private fun generatePdf(onReady: (File) -> Unit) {
        saveCachedInputs()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val totalStudents = binding.etFurnitureTotalStudents.text?.toString() ?: ""
                val ratio = binding.etFurnitureRatio.text?.toString() ?: ""
                val udise = binding.etFurnitureUdiseCode.text?.toString() ?: ""
                val district = binding.etFurnitureDistrictName.text?.toString() ?: ""

                val items = mutableListOf<PdfPrintHelper.FurnitureItemData>()
                for ((index, view) in furnitureRowViews.withIndex()) {
                    val rowTot = view.findViewById<EditText>(R.id.etRowTotalStudents).text?.toString() ?: ""
                    val rowRat = view.findViewById<EditText>(R.id.etRowRatio).text?.toString() ?: ""
                    items.add(
                        PdfPrintHelper.FurnitureItemData(
                            srNo = index + 1,
                            blockName = rowTot,
                            schoolName = rowRat,
                            udiseCode = "",
                            availableFurniture = view.findViewById<EditText>(R.id.etRowAvailableFurniture).text?.toString()?.toIntOrNull() ?: 0,
                            totalEnrolled = rowTot.toIntOrNull() ?: (view.findViewById<EditText>(R.id.etRowTotalEnrolled).text?.toString()?.toIntOrNull() ?: 0),
                            furnitureRequired = view.findViewById<EditText>(R.id.etRowFurnitureRequired).text?.toString()?.toIntOrNull() ?: 0,
                            remarks = view.findViewById<EditText>(R.id.etRowRemarks).text?.toString() ?: ""
                        )
                    )
                }

                val pdfData = PdfPrintHelper.FurniturePdfData(
                    schoolName = totalStudents,
                    blockName = ratio,
                    udiseCode = udise,
                    districtName = district,
                    items = items
                )

                val pdfFile = File(requireContext().cacheDir, "Furniture_Report_${System.currentTimeMillis()}.pdf")
                val outputStream = pdfFile.outputStream()
                val success = PdfPrintHelper.generateFurniturePdf(requireContext(), pdfData, outputStream)
                outputStream.close()

                if (success && pdfFile.exists()) {
                    onReady(pdfFile)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate Furniture PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
