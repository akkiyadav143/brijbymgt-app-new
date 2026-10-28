package com.brijby77.ui.so2

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijby77.R
import com.brijby77.SchoolApp
import com.brijby77.data.entity.StudentEntity
import com.brijby77.databinding.FragmentSo2Binding
import com.brijby77.ui.tc.StudentSearchAdapter
import com.brijby77.util.PdfPrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class So2Fragment : Fragment() {

    private var _binding: FragmentSo2Binding? = null
    private val binding get() = _binding!!

    private val studentRowViews = mutableListOf<View>()
    private var allStudents: List<StudentEntity> = emptyList()
    private lateinit var searchAdapter: StudentSearchAdapter
    private var searchTypeIsSr = false // true = SR Number, false = Name

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSo2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchRecyclerView()
        loadStudents()
        loadCachedInputs()
        setupButtons()

        if (studentRowViews.isEmpty()) {
            addStudentRow()
        }
    }

    override fun onPause() {
        super.onPause()
        saveCachedInputs()
    }

    private fun setupSearchRecyclerView() {
        searchAdapter = StudentSearchAdapter(emptyList()) { student ->
            onStudentSelected(student)
        }
        binding.rvSo2StudentSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSo2StudentSearchResults.adapter = searchAdapter
    }

    private fun loadStudents() {
        binding.progressBarSo2.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SchoolApp
                val dao = app.database.studentDao()
                allStudents = withContext(Dispatchers.IO) {
                    dao.getAllActiveStudentsSync()
                }
                binding.progressBarSo2.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBarSo2.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading students: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCachedInputs() {
        val prefs = requireContext().getSharedPreferences("so2_prefs", Context.MODE_PRIVATE)
        binding.etSo2UdiseCode.setText(prefs.getString("udise_code", ""))
        binding.etSo2SchoolName.setText(prefs.getString("school_name", ""))
        binding.etSo2DistrictName.setText(prefs.getString("district_name", ""))
        binding.etSo2BlockName.setText(prefs.getString("block_name", ""))

        val count = prefs.getInt("student_count", 0)
        if (count > 0) {
            binding.llSo2StudentRowsContainer.removeAllViews()
            studentRowViews.clear()
            for (i in 0 until count) {
                addStudentRow(
                    name = prefs.getString("student_name_$i", "") ?: "",
                    studentClass = prefs.getString("student_class_$i", "") ?: "",
                    gender = prefs.getString("student_gender_$i", "") ?: "",
                    dob = prefs.getString("student_dob_$i", "") ?: "",
                    pen = prefs.getString("student_pen_$i", "") ?: "",
                    father = prefs.getString("student_father_$i", "") ?: "",
                    mother = prefs.getString("student_mother_$i", "") ?: "",
                    aadharNo = prefs.getString("student_aadhar_$i", "") ?: "",
                    nameAsAadhar = prefs.getString("student_name_aadhar_$i", "") ?: "",
                    reason = prefs.getString("student_reason_$i", "") ?: ""
                )
            }
        }
    }

    private fun saveCachedInputs() {
        val prefs = requireContext().getSharedPreferences("so2_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("udise_code", binding.etSo2UdiseCode.text?.toString() ?: "")
            putString("school_name", binding.etSo2SchoolName.text?.toString() ?: "")
            putString("district_name", binding.etSo2DistrictName.text?.toString() ?: "")
            putString("block_name", binding.etSo2BlockName.text?.toString() ?: "")

            putInt("student_count", studentRowViews.size)
            for ((index, rowView) in studentRowViews.withIndex()) {
                putString("student_name_$index", rowView.findViewById<EditText>(R.id.etSo2StudentName).text?.toString() ?: "")
                putString("student_class_$index", rowView.findViewById<EditText>(R.id.etSo2Class).text?.toString() ?: "")
                putString("student_gender_$index", rowView.findViewById<EditText>(R.id.etSo2Gender).text?.toString() ?: "")
                putString("student_dob_$index", rowView.findViewById<EditText>(R.id.etSo2Dob).text?.toString() ?: "")
                putString("student_pen_$index", rowView.findViewById<EditText>(R.id.etSo2Pen).text?.toString() ?: "")
                putString("student_father_$index", rowView.findViewById<EditText>(R.id.etSo2FatherName).text?.toString() ?: "")
                putString("student_mother_$index", rowView.findViewById<EditText>(R.id.etSo2MotherName).text?.toString() ?: "")
                putString("student_aadhar_$index", rowView.findViewById<EditText>(R.id.etSo2AadharNo).text?.toString() ?: "")
                putString("student_name_aadhar_$index", rowView.findViewById<EditText>(R.id.etSo2NameAsAadhar).text?.toString() ?: "")
                putString("student_reason_$index", rowView.findViewById<EditText>(R.id.etSo2ReasonNotAdded).text?.toString() ?: "")
            }
        }
    }

    private fun setupButtons() {
        binding.rgSo2SearchType.setOnCheckedChangeListener { _, checkedId ->
            searchTypeIsSr = (checkedId == R.id.rbSo2SrNumber)
            if (searchTypeIsSr) {
                binding.etSo2StudentSearch.hint = "Search student by SR Number..."
            } else {
                binding.etSo2StudentSearch.hint = "Search student by Name..."
            }
            filterStudents(binding.etSo2StudentSearch.text.toString())
        }

        binding.etSo2StudentSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStudents(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.cbSo2FilterNoPen.setOnCheckedChangeListener { _, _ ->
            filterStudents(binding.etSo2StudentSearch.text.toString())
        }

        binding.btnAddSo2Student.setOnClickListener {
            addStudentRow()
        }

        binding.btnSaveSo2.setOnClickListener {
            saveCachedInputs()
            Toast.makeText(requireContext(), "SO2 details and entries saved successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetSo2.setOnClickListener {
            binding.etSo2UdiseCode.setText("")
            binding.etSo2SchoolName.setText("")
            binding.etSo2DistrictName.setText("")
            binding.etSo2BlockName.setText("")
            binding.llSo2StudentRowsContainer.removeAllViews()
            studentRowViews.clear()
            val prefs = requireContext().getSharedPreferences("so2_prefs", Context.MODE_PRIVATE)
            prefs.edit { clear() }
            addStudentRow()
            Toast.makeText(requireContext(), "Reset successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrintSo2Pdf.setOnClickListener {
            generatePdf { pdfFile ->
                PdfPrintHelper.printPdfFile(requireContext(), pdfFile, "Print SO2 Form")
            }
        }

        binding.btnShareSo2Pdf.setOnClickListener {
            generatePdf { pdfFile ->
                PdfPrintHelper.sharePdfFile(requireContext(), pdfFile)
            }
        }
    }

    private fun filterStudents(query: String) {
        val onlyNoPen = binding.cbSo2FilterNoPen.isChecked
        if (query.isBlank() && !onlyNoPen) {
            binding.rvSo2StudentSearchResults.visibility = View.GONE
            binding.tvSo2NoStudentFound.visibility = View.GONE
            return
        }

        val filtered = allStudents.filter { student ->
            val matchesPenFilter = if (onlyNoPen) student.penNo.isBlank() else true
            val matchesQuery = if (query.isBlank()) {
                true
            } else if (searchTypeIsSr) {
                student.srNumber.contains(query, ignoreCase = true)
            } else {
                student.studentName.contains(query, ignoreCase = true) ||
                        student.fatherName.contains(query, ignoreCase = true)
            }
            matchesPenFilter && matchesQuery
        }

        if (filtered.isNotEmpty()) {
            searchAdapter.updateList(filtered)
            binding.rvSo2StudentSearchResults.visibility = View.VISIBLE
            binding.tvSo2NoStudentFound.visibility = View.GONE
        } else {
            binding.rvSo2StudentSearchResults.visibility = View.GONE
            binding.tvSo2NoStudentFound.visibility = View.VISIBLE
        }
    }

    private fun onStudentSelected(student: StudentEntity) {
        binding.etSo2StudentSearch.setText("")
        binding.rvSo2StudentSearchResults.visibility = View.GONE
        binding.tvSo2NoStudentFound.visibility = View.GONE

        // Check if PEN No already generated
        if (student.penNo.isNotBlank()) {
            Toast.makeText(requireContext(), "Pen No Generated Already", Toast.LENGTH_LONG).show()
            return
        }

        // Populate into the latest active student row (or add a new one if all filled)
        val targetRow = studentRowViews.lastOrNull() ?: run {
            addStudentRow()
            studentRowViews.last()
        }

        targetRow.findViewById<EditText>(R.id.etSo2StudentName).setText(student.studentName)
        targetRow.findViewById<EditText>(R.id.etSo2Class).setText(student.studentClass)
        targetRow.findViewById<EditText>(R.id.etSo2Gender).setText(student.gender)
        targetRow.findViewById<EditText>(R.id.etSo2Dob).setText(student.dob)
        targetRow.findViewById<EditText>(R.id.etSo2Pen).setText(student.penNo)
        targetRow.findViewById<EditText>(R.id.etSo2FatherName).setText(student.fatherName)
        targetRow.findViewById<EditText>(R.id.etSo2MotherName).setText(student.motherName)
        targetRow.findViewById<EditText>(R.id.etSo2AadharNo).setText(student.studentAdhar)
        targetRow.findViewById<EditText>(R.id.etSo2NameAsAadhar).setText(student.studentName)

        saveCachedInputs()
        Toast.makeText(requireContext(), "Student data updated from database", Toast.LENGTH_SHORT).show()
    }

    private fun addStudentRow(
        name: String = "",
        studentClass: String = "",
        gender: String = "",
        dob: String = "",
        pen: String = "",
        father: String = "",
        mother: String = "",
        aadharNo: String = "",
        nameAsAadhar: String = "",
        reason: String = ""
    ) {
        val rowView = layoutInflater.inflate(R.layout.item_so2_student_row, binding.llSo2StudentRowsContainer, false)

        rowView.findViewById<EditText>(R.id.etSo2StudentName).setText(name)
        rowView.findViewById<EditText>(R.id.etSo2Class).setText(studentClass)
        rowView.findViewById<EditText>(R.id.etSo2Gender).setText(gender)
        rowView.findViewById<EditText>(R.id.etSo2Dob).setText(dob)
        rowView.findViewById<EditText>(R.id.etSo2Pen).setText(pen)
        rowView.findViewById<EditText>(R.id.etSo2FatherName).setText(father)
        rowView.findViewById<EditText>(R.id.etSo2MotherName).setText(mother)
        rowView.findViewById<EditText>(R.id.etSo2AadharNo).setText(aadharNo)
        rowView.findViewById<EditText>(R.id.etSo2NameAsAadhar).setText(nameAsAadhar)
        rowView.findViewById<EditText>(R.id.etSo2ReasonNotAdded).setText(reason)

        rowView.findViewById<ImageButton>(R.id.btnDeleteSo2Row).setOnClickListener {
            if (studentRowViews.size > 1) {
                binding.llSo2StudentRowsContainer.removeView(rowView)
                studentRowViews.remove(rowView)
                updateRowTitles()
                saveCachedInputs()
            } else {
                Toast.makeText(requireContext(), "At least one student entry is required", Toast.LENGTH_SHORT).show()
            }
        }

        studentRowViews.add(rowView)
        binding.llSo2StudentRowsContainer.addView(rowView)
        updateRowTitles()
    }

    private fun updateRowTitles() {
        for ((index, view) in studentRowViews.withIndex()) {
            view.findViewById<TextView>(R.id.tvSo2RowTitle).text = "Student #${index + 1}"
        }
    }

    private fun generatePdf(onReady: (File) -> Unit) {
        saveCachedInputs()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val udise = binding.etSo2UdiseCode.text?.toString() ?: ""
                val school = binding.etSo2SchoolName.text?.toString() ?: ""
                val district = binding.etSo2DistrictName.text?.toString() ?: ""
                val block = binding.etSo2BlockName.text?.toString() ?: ""

                val students = mutableListOf<PdfPrintHelper.So2StudentData>()
                for ((index, view) in studentRowViews.withIndex()) {
                    students.add(
                        PdfPrintHelper.So2StudentData(
                            srNo = index + 1,
                            studentName = view.findViewById<EditText>(R.id.etSo2StudentName).text?.toString() ?: "",
                            studentClass = view.findViewById<EditText>(R.id.etSo2Class).text?.toString() ?: "",
                            gender = view.findViewById<EditText>(R.id.etSo2Gender).text?.toString() ?: "",
                            dob = view.findViewById<EditText>(R.id.etSo2Dob).text?.toString() ?: "",
                            penNo = view.findViewById<EditText>(R.id.etSo2Pen).text?.toString() ?: "",
                            fatherName = view.findViewById<EditText>(R.id.etSo2FatherName).text?.toString() ?: "",
                            motherName = view.findViewById<EditText>(R.id.etSo2MotherName).text?.toString() ?: "",
                            aadharNo = view.findViewById<EditText>(R.id.etSo2AadharNo).text?.toString() ?: "",
                            nameAsAadhar = view.findViewById<EditText>(R.id.etSo2NameAsAadhar).text?.toString() ?: "",
                            reasonNotAdded = view.findViewById<EditText>(R.id.etSo2ReasonNotAdded).text?.toString() ?: ""
                        )
                    )
                }

                val pdfData = PdfPrintHelper.So2PdfData(
                    udiseCode = udise,
                    schoolName = school,
                    districtName = district,
                    blockName = block,
                    students = students
                )

                val pdfFile = File(requireContext().cacheDir, "Form_S02_${System.currentTimeMillis()}.pdf")
                val outputStream = pdfFile.outputStream()
                val success = PdfPrintHelper.generateSo2Pdf(requireContext(), pdfData, outputStream)
                outputStream.close()

                if (success && pdfFile.exists()) {
                    onReady(pdfFile)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate Form S02 PDF", Toast.LENGTH_SHORT).show()
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
