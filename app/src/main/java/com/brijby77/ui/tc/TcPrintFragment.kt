package com.brijby77.ui.tc

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijby77.R
import com.brijby77.SchoolApp
import com.brijby77.data.entity.StudentEntity
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.databinding.FragmentTcPrintBinding
import com.brijby77.util.TcDetails
import com.brijby77.util.TcPdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TcPrintFragment : Fragment() {

    private var _binding: FragmentTcPrintBinding? = null
    private val binding get() = _binding!!

    private var allStudents: List<StudentEntity> = emptyList()
    private var selectedStudent: StudentEntity? = null
    private lateinit var searchAdapter: StudentSearchAdapter

    private var searchTypeIsSr = false // true = SR Number, false = Name

    private val prefs by lazy {
        requireContext().getSharedPreferences("tc_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTcPrintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchRecyclerView()
        loadStudents()
        setupListeners()
        setDefaultValues()
    }

    private fun setupSearchRecyclerView() {
        searchAdapter = StudentSearchAdapter(emptyList()) { student ->
            onStudentSelected(student)
        }
        binding.rvStudentSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudentSearchResults.adapter = searchAdapter
    }

    private fun loadStudents() {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SchoolApp
                val dao = app.database.studentDao()
                allStudents = withContext(Dispatchers.IO) {
                    dao.getAllActiveStudentsSync()
                }
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Unable to load student data. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.rgSearchType.setOnCheckedChangeListener { _, checkedId ->
            searchTypeIsSr = (checkedId == R.id.rbSrNumber)
            if (searchTypeIsSr) {
                binding.etStudentSearch.hint = "Search by SR Number..."
            } else {
                binding.etStudentSearch.hint = "Search by Student Name..."
            }
            filterStudents(binding.etStudentSearch.text.toString())
        }

        binding.etStudentSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStudents(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnPreviewTc.setOnClickListener {
            generateAndActionPdf(ActionType.PREVIEW)
        }

        binding.btnSaveTc.setOnClickListener {
            if (selectedStudent == null) {
                Toast.makeText(requireContext(), "Please select a student first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveTcFormPrefs()
            Toast.makeText(requireContext(), "TC Saved Successfully!", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrintTc.setOnClickListener {
            generateAndActionPdf(ActionType.PRINT)
        }

        binding.btnPdfTc.setOnClickListener {
            generateAndActionPdf(ActionType.PDF)
        }

        binding.btnShareTc.setOnClickListener {
            generateAndActionPdf(ActionType.SHARE)
        }
    }

    private fun setDefaultValues() {
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.etDateOfIssue.setText(currentDate)
        binding.etDateOfLeaving.setText(currentDate)

        // Load cached values
        binding.etTcNumber.setText(prefs.getString("tc_number", getString(R.string.tc_number_format, System.currentTimeMillis().toString().takeLast(5))))
        binding.etStruckOffClass.setText(prefs.getString("struck_off_class", ""))
        binding.etLastAdmissionDate.setText(prefs.getString("last_admission_date", currentDate))
        binding.etReason.setText(prefs.getString("reason_for_leaving", getString(R.string.completed_course_reason)))
        binding.etUdiseCode.setText(prefs.getString("udise_code", ""))
        binding.etSchoolName.setText(prefs.getString("school_name", getString(R.string.default_school_name)))
        binding.etSchoolAddress.setText(prefs.getString("school_address", getString(R.string.default_school_address)))
        binding.etTehsil.setText(prefs.getString("tehsil", "सदर"))
        binding.etDistrict.setText(prefs.getString("district", "उत्तर प्रदेश"))
        binding.etDurationOfStay.setText(prefs.getString("duration_of_stay", "पूर्ण अवधि"))
        binding.etStudentLanguage.setText(prefs.getString("student_language", "हिन्दी (Hindi)"))
        binding.etWorkingDays.setText(prefs.getString("working_days", "220"))
        binding.etAttendance.setText(prefs.getString("attendance", "205"))
        binding.etFatherOccupation.setText(prefs.getString("father_occupation", "कृषि / सेवा"))
        binding.etConduct.setText(prefs.getString("conduct", "Good / उत्तम"))
        binding.etRemarks.setText(prefs.getString("remarks", "Passed & Promoted"))
    }

    private fun saveTcFormPrefs() {
        prefs.edit {
            putString("udise_code", binding.etUdiseCode.text?.toString() ?: "")
            putString("school_name", binding.etSchoolName.text?.toString() ?: "")
            putString("school_address", binding.etSchoolAddress.text?.toString() ?: "")
            putString("tehsil", binding.etTehsil.text?.toString() ?: "")
            putString("district", binding.etDistrict.text?.toString() ?: "")
            putString("duration_of_stay", binding.etDurationOfStay.text?.toString() ?: "")
            putString("student_language", binding.etStudentLanguage.text?.toString() ?: "")
            putString("working_days", binding.etWorkingDays.text?.toString() ?: "")
            putString("attendance", binding.etAttendance.text?.toString() ?: "")
            putString("father_occupation", binding.etFatherOccupation.text?.toString() ?: "")
            putString("tc_number", binding.etTcNumber.text?.toString() ?: "")
            putString("struck_off_class", binding.etStruckOffClass.text?.toString() ?: "")
            putString("last_admission_date", binding.etLastAdmissionDate.text?.toString() ?: "")
            putString("reason_for_leaving", binding.etReason.text?.toString() ?: "")
            putString("conduct", binding.etConduct.text?.toString() ?: "")
            putString("remarks", binding.etRemarks.text?.toString() ?: "")
        }
    }

    private fun filterStudents(query: String) {
        if (query.isBlank()) {
            binding.rvStudentSearchResults.visibility = View.GONE
            binding.tvNoStudentFound.visibility = View.GONE
            return
        }

        val filtered = allStudents.filter { student ->
            if (searchTypeIsSr) {
                student.srNumber.contains(query, ignoreCase = true)
            } else {
                student.studentName.contains(query, ignoreCase = true) ||
                        student.fatherName.contains(query, ignoreCase = true)
            }
        }

        if (filtered.isNotEmpty()) {
            searchAdapter.updateList(filtered)
            binding.rvStudentSearchResults.visibility = View.VISIBLE
            binding.tvNoStudentFound.visibility = View.GONE
        } else {
            binding.rvStudentSearchResults.visibility = View.GONE
            binding.tvNoStudentFound.visibility = View.VISIBLE
        }
    }

    private fun onStudentSelected(student: StudentEntity) {
        selectedStudent = student
        binding.etStudentSearch.setText(student.studentName)
        binding.rvStudentSearchResults.visibility = View.GONE
        binding.tvNoStudentFound.visibility = View.GONE

        if (binding.etStruckOffClass.text.isNullOrBlank()) {
            binding.etStruckOffClass.setText(student.studentClass)
        }

        binding.cardSelectedStudent.visibility = View.VISIBLE
        binding.tvSelectedStudentDetails.text = getString(
            R.string.selected_student_details_format,
            student.studentName,
            student.srNumber,
            student.studentClass,
            student.fatherName
        )
    }

    private enum class ActionType { PREVIEW, PRINT, PDF, SHARE }

    private fun generateAndActionPdf(action: ActionType) {
        val student = selectedStudent
        if (student == null) {
            Toast.makeText(requireContext(), "Please search and select a student first.", Toast.LENGTH_SHORT).show()
            return
        }

        saveTcFormPrefs()

        val tcDetails = TcDetails(
            tcNumber = binding.etTcNumber.text?.toString() ?: "",
            struckOffClass = binding.etStruckOffClass.text?.toString() ?: "",
            lastAdmissionDate = binding.etLastAdmissionDate.text?.toString() ?: "",
            udiseCode = binding.etUdiseCode.text?.toString() ?: "",
            schoolName = binding.etSchoolName.text?.toString() ?: "",
            schoolAddress = binding.etSchoolAddress.text?.toString() ?: "",
            tehsil = binding.etTehsil.text?.toString() ?: "सदर",
            district = binding.etDistrict.text?.toString() ?: "उत्तर प्रदेश",
            durationOfStay = binding.etDurationOfStay.text?.toString() ?: "पूर्ण अवधि",
            language = binding.etStudentLanguage.text?.toString() ?: "हिन्दी (Hindi)",
            workingDays = binding.etWorkingDays.text?.toString() ?: "220",
            attendance = binding.etAttendance.text?.toString() ?: "205",
            fatherOccupation = binding.etFatherOccupation.text?.toString() ?: "कृषि / सेवा",
            dateOfLeaving = binding.etDateOfLeaving.text?.toString() ?: "",
            reasonForLeaving = binding.etReason.text?.toString() ?: "",
            conduct = binding.etConduct.text?.toString() ?: "Good / उत्तम",
            remarks = binding.etRemarks.text?.toString() ?: "",
            dateOfIssue = binding.etDateOfIssue.text?.toString() ?: ""
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SchoolApp
                val db = app.database
                val studentWithDetails = withContext(Dispatchers.IO) {
                    db.studentDao().getStudentWithDetailsById(student.id) ?: StudentWithDetails(
                        student = student,
                        healthDetails = null,
                        heightWeightHistory = emptyList(),
                        bankDetails = null
                    )
                }

                val pdfFile = TcPdfHelper.generateTcPdfFile(requireContext(), studentWithDetails, tcDetails)
                if (pdfFile != null && pdfFile.exists()) {
                    when (action) {
                        ActionType.PREVIEW, ActionType.PDF -> {
                            TcPdfHelper.shareTcPdf(requireContext(), pdfFile)
                        }
                        ActionType.PRINT -> {
                            TcPdfHelper.printTcPdf(requireContext(), pdfFile, "TC_${student.srNumber}")
                        }
                        ActionType.SHARE -> {
                            TcPdfHelper.shareTcPdf(requireContext(), pdfFile)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to generate TC PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

