package com.brijby77.ui.so3

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
import com.brijby77.databinding.FragmentSo3Binding
import com.brijby77.ui.tc.StudentSearchAdapter
import com.brijby77.util.PdfPrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class So3Fragment : Fragment() {

    private var _binding: FragmentSo3Binding? = null
    private val binding get() = _binding!!

    private var allStudents: List<StudentEntity> = emptyList()
    private lateinit var searchAdapter: StudentSearchAdapter
    private var searchTypeIsSr = false // true = SR Number, false = Name

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSo3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchRecyclerView()
        loadStudents()
        loadCachedInputs()
        setupButtons()
    }

    override fun onPause() {
        super.onPause()
        saveCachedInputs()
    }

    private fun setupSearchRecyclerView() {
        searchAdapter = StudentSearchAdapter(emptyList()) { student ->
            onStudentSelected(student)
        }
        binding.rvSo3StudentSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSo3StudentSearchResults.adapter = searchAdapter
    }

    private fun loadStudents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SchoolApp
                val dao = app.database.studentDao()
                allStudents = withContext(Dispatchers.IO) {
                    dao.getAllActiveStudentsSync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error loading students: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCachedInputs() {
        val prefs = requireContext().getSharedPreferences("so3_prefs", Context.MODE_PRIVATE)
        binding.etSo3AcademicYear.setText(prefs.getString("academic_year", "2024-2025"))
        binding.etSo3UdiseCode.setText(prefs.getString("udise_code", ""))
        binding.etSo3SchoolName.setText(prefs.getString("school_name", ""))
        binding.etSo3StateName.setText(prefs.getString("state_name", ""))
        binding.etSo3DistrictName.setText(prefs.getString("district_name", ""))
        binding.etSo3BlockName.setText(prefs.getString("block_name", ""))
        binding.etSo3StudentPen.setText(prefs.getString("student_pen", ""))
        binding.etSo3StudentName.setText(prefs.getString("student_name", ""))
        binding.etSo3MobileNumber.setText(prefs.getString("mobile_number", ""))

        // Existing details
        binding.etSo3ExistName.setText(prefs.getString("exist_name", ""))
        binding.etSo3ExistDob.setText(prefs.getString("exist_dob", ""))
        binding.etSo3ExistGender.setText(prefs.getString("exist_gender", ""))
        binding.etSo3ExistAadhar.setText(prefs.getString("exist_aadhar", ""))
        binding.etSo3ExistNameAsAadhar.setText(prefs.getString("exist_name_aadhar", ""))
        binding.etSo3ExistClass.setText(prefs.getString("exist_class", ""))
        binding.etSo3ExistMother.setText(prefs.getString("exist_mother", ""))
        binding.etSo3ExistFather.setText(prefs.getString("exist_father", ""))

        // Updated details
        binding.etSo3UpdateName.setText(prefs.getString("update_name", ""))
        binding.etSo3UpdateDob.setText(prefs.getString("update_dob", ""))
        binding.etSo3UpdateGender.setText(prefs.getString("update_gender", ""))
        binding.etSo3UpdateAadhar.setText(prefs.getString("update_aadhar", ""))
        binding.etSo3UpdateNameAsAadhar.setText(prefs.getString("update_name_aadhar", ""))
        binding.etSo3UpdateClass.setText(prefs.getString("update_class", ""))
        binding.etSo3UpdateMother.setText(prefs.getString("update_mother", ""))
        binding.etSo3UpdateFather.setText(prefs.getString("update_father", ""))

        binding.cbSo3DocAadhar.isChecked = prefs.getBoolean("doc_aadhar", false)
        binding.cbSo3DocBirthCert.isChecked = prefs.getBoolean("doc_birth", false)
        binding.cbSo3DocParentId.isChecked = prefs.getBoolean("doc_parent", false)
        binding.cbSo3Undertaking.isChecked = prefs.getBoolean("undertaking", true)
    }

    private fun saveCachedInputs() {
        val prefs = requireContext().getSharedPreferences("so3_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("academic_year", binding.etSo3AcademicYear.text?.toString() ?: "")
            putString("udise_code", binding.etSo3UdiseCode.text?.toString() ?: "")
            putString("school_name", binding.etSo3SchoolName.text?.toString() ?: "")
            putString("state_name", binding.etSo3StateName.text?.toString() ?: "")
            putString("district_name", binding.etSo3DistrictName.text?.toString() ?: "")
            putString("block_name", binding.etSo3BlockName.text?.toString() ?: "")
            putString("student_pen", binding.etSo3StudentPen.text?.toString() ?: "")
            putString("student_name", binding.etSo3StudentName.text?.toString() ?: "")
            putString("mobile_number", binding.etSo3MobileNumber.text?.toString() ?: "")

            putString("exist_name", binding.etSo3ExistName.text?.toString() ?: "")
            putString("exist_dob", binding.etSo3ExistDob.text?.toString() ?: "")
            putString("exist_gender", binding.etSo3ExistGender.text?.toString() ?: "")
            putString("exist_aadhar", binding.etSo3ExistAadhar.text?.toString() ?: "")
            putString("exist_name_aadhar", binding.etSo3ExistNameAsAadhar.text?.toString() ?: "")
            putString("exist_class", binding.etSo3ExistClass.text?.toString() ?: "")
            putString("exist_mother", binding.etSo3ExistMother.text?.toString() ?: "")
            putString("exist_father", binding.etSo3ExistFather.text?.toString() ?: "")

            putString("update_name", binding.etSo3UpdateName.text?.toString() ?: "")
            putString("update_dob", binding.etSo3UpdateDob.text?.toString() ?: "")
            putString("update_gender", binding.etSo3UpdateGender.text?.toString() ?: "")
            putString("update_aadhar", binding.etSo3UpdateAadhar.text?.toString() ?: "")
            putString("update_name_aadhar", binding.etSo3UpdateNameAsAadhar.text?.toString() ?: "")
            putString("update_class", binding.etSo3UpdateClass.text?.toString() ?: "")
            putString("update_mother", binding.etSo3UpdateMother.text?.toString() ?: "")
            putString("update_father", binding.etSo3UpdateFather.text?.toString() ?: "")

            putBoolean("doc_aadhar", binding.cbSo3DocAadhar.isChecked)
            putBoolean("doc_birth", binding.cbSo3DocBirthCert.isChecked)
            putBoolean("doc_parent", binding.cbSo3DocParentId.isChecked)
            putBoolean("undertaking", binding.cbSo3Undertaking.isChecked)
        }
    }

    private fun setupButtons() {
        binding.rgSo3SearchType.setOnCheckedChangeListener { _, checkedId ->
            searchTypeIsSr = (checkedId == R.id.rbSo3SrNumber)
            if (searchTypeIsSr) {
                binding.etSo3StudentSearch.hint = "Search student by SR Number..."
            } else {
                binding.etSo3StudentSearch.hint = "Search student by Name..."
            }
            filterStudents(binding.etSo3StudentSearch.text.toString())
        }

        binding.etSo3StudentSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStudents(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSaveSo3.setOnClickListener {
            saveCachedInputs()
            Toast.makeText(requireContext(), "Form S03 details saved successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetSo3.setOnClickListener {
            binding.etSo3StudentPen.setText("")
            binding.etSo3StudentName.setText("")
            binding.etSo3MobileNumber.setText("")

            binding.etSo3ExistName.setText("")
            binding.etSo3ExistDob.setText("")
            binding.etSo3ExistGender.setText("")
            binding.etSo3ExistAadhar.setText("")
            binding.etSo3ExistNameAsAadhar.setText("")
            binding.etSo3ExistClass.setText("")
            binding.etSo3ExistMother.setText("")
            binding.etSo3ExistFather.setText("")

            binding.etSo3UpdateName.setText("")
            binding.etSo3UpdateDob.setText("")
            binding.etSo3UpdateGender.setText("")
            binding.etSo3UpdateAadhar.setText("")
            binding.etSo3UpdateNameAsAadhar.setText("")
            binding.etSo3UpdateClass.setText("")
            binding.etSo3UpdateMother.setText("")
            binding.etSo3UpdateFather.setText("")

            binding.cbSo3DocAadhar.isChecked = false
            binding.cbSo3DocBirthCert.isChecked = false
            binding.cbSo3DocParentId.isChecked = false
            binding.cbSo3Undertaking.isChecked = true

            val prefs = requireContext().getSharedPreferences("so3_prefs", Context.MODE_PRIVATE)
            prefs.edit {
                remove("student_pen")
                remove("student_name")
                remove("mobile_number")
                remove("exist_name")
                remove("exist_dob")
                remove("exist_gender")
                remove("exist_aadhar")
                remove("exist_name_aadhar")
                remove("exist_class")
                remove("exist_mother")
                remove("exist_father")
                remove("update_name")
                remove("update_dob")
                remove("update_gender")
                remove("update_aadhar")
                remove("update_name_aadhar")
                remove("update_class")
                remove("update_mother")
                remove("update_father")
                remove("doc_aadhar")
                remove("doc_birth")
                remove("doc_parent")
                remove("undertaking")
            }
            Toast.makeText(requireContext(), "Student details reset successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrintSo3Pdf.setOnClickListener {
            generatePdf { pdfFile ->
                PdfPrintHelper.printPdfFile(requireContext(), pdfFile, "Print Form S03")
            }
        }

        binding.btnShareSo3Pdf.setOnClickListener {
            generatePdf { pdfFile ->
                PdfPrintHelper.sharePdfFile(requireContext(), pdfFile)
            }
        }
    }

    private fun filterStudents(query: String) {
        if (query.isBlank()) {
            binding.rvSo3StudentSearchResults.visibility = View.GONE
            binding.tvSo3NoStudentFound.visibility = View.GONE
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
            binding.rvSo3StudentSearchResults.visibility = View.VISIBLE
            binding.tvSo3NoStudentFound.visibility = View.GONE
        } else {
            binding.rvSo3StudentSearchResults.visibility = View.GONE
            binding.tvSo3NoStudentFound.visibility = View.VISIBLE
        }
    }

    private fun onStudentSelected(student: StudentEntity) {
        binding.etSo3StudentSearch.setText("")
        binding.rvSo3StudentSearchResults.visibility = View.GONE
        binding.tvSo3NoStudentFound.visibility = View.GONE

        // Auto-fill metadata, mobile number and existing details
        binding.etSo3StudentPen.setText(student.penNo)
        binding.etSo3StudentName.setText(student.studentName)
        binding.etSo3MobileNumber.setText(student.mobileNo)

        binding.etSo3ExistName.setText(student.studentName)
        binding.etSo3ExistDob.setText(student.dob)
        binding.etSo3ExistGender.setText(student.gender)
        binding.etSo3ExistAadhar.setText(student.studentAdhar)
        binding.etSo3ExistNameAsAadhar.setText(student.studentName)
        binding.etSo3ExistClass.setText(student.studentClass)
        binding.etSo3ExistMother.setText(student.motherName)
        binding.etSo3ExistFather.setText(student.fatherName)

        saveCachedInputs()
        Toast.makeText(requireContext(), "Student details auto-filled successfully", Toast.LENGTH_SHORT).show()
    }

    private fun generatePdf(onReady: (File) -> Unit) {
        saveCachedInputs()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pdfData = PdfPrintHelper.So3PdfData(
                    academicYear = binding.etSo3AcademicYear.text?.toString() ?: "",
                    udiseCode = binding.etSo3UdiseCode.text?.toString() ?: "",
                    schoolName = binding.etSo3SchoolName.text?.toString() ?: "",
                    stateName = binding.etSo3StateName.text?.toString() ?: "",
                    districtName = binding.etSo3DistrictName.text?.toString() ?: "",
                    blockName = binding.etSo3BlockName.text?.toString() ?: "",
                    studentPen = binding.etSo3StudentPen.text?.toString() ?: "",
                    studentName = binding.etSo3StudentName.text?.toString() ?: "",
                    mobileNumber = binding.etSo3MobileNumber.text?.toString() ?: "",

                    existName = binding.etSo3ExistName.text?.toString() ?: "",
                    existDob = binding.etSo3ExistDob.text?.toString() ?: "",
                    existGender = binding.etSo3ExistGender.text?.toString() ?: "",
                    existAadhar = binding.etSo3ExistAadhar.text?.toString() ?: "",
                    existNameAsAadhar = binding.etSo3ExistNameAsAadhar.text?.toString() ?: "",
                    existClass = binding.etSo3ExistClass.text?.toString() ?: "",
                    existMother = binding.etSo3ExistMother.text?.toString() ?: "",
                    existFather = binding.etSo3ExistFather.text?.toString() ?: "",

                    updateName = binding.etSo3UpdateName.text?.toString() ?: "",
                    updateDob = binding.etSo3UpdateDob.text?.toString() ?: "",
                    updateGender = binding.etSo3UpdateGender.text?.toString() ?: "",
                    updateAadhar = binding.etSo3UpdateAadhar.text?.toString() ?: "",
                    updateNameAsAadhar = binding.etSo3UpdateNameAsAadhar.text?.toString() ?: "",
                    updateClass = binding.etSo3UpdateClass.text?.toString() ?: "",
                    updateMother = binding.etSo3UpdateMother.text?.toString() ?: "",
                    updateFather = binding.etSo3UpdateFather.text?.toString() ?: "",

                    docAadharAttached = binding.cbSo3DocAadhar.isChecked,
                    docBirthCertAttached = binding.cbSo3DocBirthCert.isChecked,
                    docParentIdAttached = binding.cbSo3DocParentId.isChecked,
                    undertakingChecked = binding.cbSo3Undertaking.isChecked
                )

                val pdfFile = File(requireContext().cacheDir, "Form_S03_${System.currentTimeMillis()}.pdf")
                val outputStream = pdfFile.outputStream()
                val success = PdfPrintHelper.generateSo3Pdf(requireContext(), pdfData, outputStream)
                outputStream.close()

                if (success && pdfFile.exists()) {
                    onReady(pdfFile)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate Form S03 PDF", Toast.LENGTH_SHORT).show()
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
