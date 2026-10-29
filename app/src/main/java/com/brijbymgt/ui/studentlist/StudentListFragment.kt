package com.brijbymgt.ui.studentlist

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.databinding.DialogHeightWeightHistoryBinding
import com.brijbymgt.databinding.DialogImagePopupBinding
import com.brijbymgt.databinding.DialogStudentPreviewBinding
import com.brijbymgt.databinding.FragmentStudentListBinding
import com.brijbymgt.util.ImageStorageHelper
import com.brijbymgt.util.PdfPrintHelper
import com.brijbymgt.util.StudentPreviewHelper
import kotlinx.coroutines.launch
import java.io.File

class StudentListFragment : Fragment() {

    private var _binding: FragmentStudentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentListViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao()
        )
        StudentListViewModelFactory(repo)
    }

    private lateinit var adapter: StudentAdapter
    private var currentSearchField = "All"
    private var currentSelectedClass = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { args ->
            val caste = args.getString("caste")
            val gender = args.getString("gender")
            val scheme = args.getString("scheme")
            if (!caste.isNullOrBlank()) viewModel.setCasteFilter(caste)
            if (!gender.isNullOrBlank()) viewModel.setGenderFilter(gender)
            if (!scheme.isNullOrBlank()) viewModel.setSchemeFilter(scheme)
        }

        setupRecyclerView()
        setupFiltersAndSearch()
        setupOrderAndSortByChips()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = StudentAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            onImageClicked = { imagePath -> showImagePopup(imagePath) },
            onViewHistoryClicked = { item -> showHeightWeightHistoryDialog(item) },
            onPreviewClicked = { item -> showPreviewDialog(item) }
        )
        binding.rvStudentList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudentList.adapter = adapter
    }

    private fun setupFiltersAndSearch() {
        // Class Filter Click Listeners
        val classClickMap = listOf(
            binding.btnListFilterAll to "All",
            binding.btnListFilterCl1 to "Class 1",
            binding.btnListFilterCl2 to "Class 2",
            binding.btnListFilterCl3 to "Class 3",
            binding.btnListFilterCl4 to "Class 4",
            binding.btnListFilterCl5 to "Class 5",
            binding.btnListFilterCl6 to "Class 6",
            binding.btnListFilterCl7 to "Class 7",
            binding.btnListFilterCl8 to "Class 8"
        )

        for ((btn, classVal) in classClickMap) {
            btn.setOnClickListener {
                currentSelectedClass = classVal
                viewModel.setClassFilter(classVal)
                updateClassFilterPills(classVal)
                binding.rvStudentList.scrollToPosition(0)
            }
        }

        // Search Field Click Listeners (2 Rows)
        val searchFieldClickMap = listOf(
            binding.btnSearchFieldAll to "All",
            binding.btnSearchFieldSrNumber to "SR Number",
            binding.btnSearchFieldStudentName to "Student Name",
            binding.btnSearchFieldAadhaar to "Student Adhar",
            binding.btnSearchFieldMobile to "Mobile No",
            binding.btnSearchFieldFather to "Father Name",
            binding.btnSearchFieldMother to "Mother Name"
        )

        for ((btn, fieldVal) in searchFieldClickMap) {
            btn.setOnClickListener {
                currentSearchField = fieldVal
                viewModel.setSearchField(fieldVal)
                updateSearchFieldRadioButtons(fieldVal)
                binding.rvStudentList.scrollToPosition(0)
            }
        }

        // Search Box
        binding.etSearchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
                binding.rvStudentList.scrollToPosition(0)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        updateClassFilterPills(currentSelectedClass)
        updateSearchFieldRadioButtons(currentSearchField)
    }

    private fun setupOrderAndSortByChips() {
        // Order Chips (Ascending / Descending)
        binding.chipGroupOrder.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(binding.chipOrderDesc.id)) {
                viewModel.setSortOrder("Descending")
            } else {
                viewModel.setSortOrder("Ascending")
            }
            binding.rvStudentList.scrollToPosition(0)
        }

        // Sort By Chips
        val chipMap = mapOf(
            binding.chipSortName.id to "Student Name",
            binding.chipSortAdhar.id to "Student Aadhaar",
            binding.chipSortSrNumber.id to "Sr Number",
            binding.chipSortSrNo.id to "Sr No",
            binding.chipSortUid.id to "Unique ID",
            binding.chipSortPen.id to "PEN No",
            binding.chipSortAapar.id to "AAPAR ID",
            binding.chipSortClass.id to "Class",
            binding.chipSortMale.id to "Male",
            binding.chipSortFemale.id to "Female",
            binding.chipSortGen.id to "GEN",
            binding.chipSortObc.id to "OBC",
            binding.chipSortSc.id to "SC",
            binding.chipSortSt.id to "ST",
            binding.chipSortMinority.id to "MINORITY"
        )

        binding.chipGroupSortBy.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedId = checkedIds.firstOrNull()
            val sortField = chipMap[selectedId] ?: "None"
            viewModel.setSortByField(sortField)
            binding.rvStudentList.scrollToPosition(0)
        }
    }

    private fun updateClassFilterPills(selectedClass: String) {
        val classButtons = listOf(
            "All" to binding.btnListFilterAll,
            "Class 1" to binding.btnListFilterCl1,
            "Class 2" to binding.btnListFilterCl2,
            "Class 3" to binding.btnListFilterCl3,
            "Class 4" to binding.btnListFilterCl4,
            "Class 5" to binding.btnListFilterCl5,
            "Class 6" to binding.btnListFilterCl6,
            "Class 7" to binding.btnListFilterCl7,
            "Class 8" to binding.btnListFilterCl8
        )

        val papayaColor = Color.parseColor("#E56717")
        val unselectedBgColor = Color.parseColor("#F1F5F9")
        val unselectedTextColor = Color.parseColor("#1E293B")

        for ((key, btn) in classButtons) {
            val label = key.replace("Class ", "CL ")
            if (key.equals(selectedClass, true) || (selectedClass == "All" && key == "All")) {
                btn.text = "◉ $label"
                btn.backgroundTintList = ColorStateList.valueOf(papayaColor)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.text = "○ $label"
                btn.backgroundTintList = ColorStateList.valueOf(unselectedBgColor)
                btn.setTextColor(unselectedTextColor)
            }
        }
    }

    private fun updateSearchFieldRadioButtons(selectedField: String) {
        val fieldButtons = listOf(
            "All" to binding.btnSearchFieldAll,
            "SR Number" to binding.btnSearchFieldSrNumber,
            "Student Name" to binding.btnSearchFieldStudentName,
            "Student Adhar" to binding.btnSearchFieldAadhaar,
            "Mobile No" to binding.btnSearchFieldMobile,
            "Father Name" to binding.btnSearchFieldFather,
            "Mother Name" to binding.btnSearchFieldMother
        )

        val papayaColor = Color.parseColor("#E56717")
        val unselectedBgColor = Color.parseColor("#F1F5F9")
        val unselectedTextColor = Color.parseColor("#334155")

        for ((fieldKey, btn) in fieldButtons) {
            val displayLabel = when (fieldKey) {
                "Student Adhar" -> "Aadhaar"
                "Mobile No" -> "Mobile No"
                else -> fieldKey
            }

            if (fieldKey.equals(selectedField, true) ||
                (selectedField == "Student Adhar" && fieldKey == "Student Adhar")
            ) {
                btn.text = "🔘 $displayLabel"
                btn.backgroundTintList = ColorStateList.valueOf(papayaColor)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.text = "○ $displayLabel"
                btn.backgroundTintList = ColorStateList.valueOf(unselectedBgColor)
                btn.setTextColor(unselectedTextColor)
            }
        }
    }

    private fun observeData() {
        val targetStudentId = arguments?.getLong("studentId", -1L) ?: -1L
        var hasScrolledToTarget = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredStudentList.collect { list ->
                adapter.submitList(list) {
                    if (!hasScrolledToTarget && targetStudentId != -1L) {
                        val index = list.indexOfFirst { it.student.id == targetStudentId }
                        if (index != -1) {
                            hasScrolledToTarget = true
                            binding.rvStudentList.scrollToPosition(index)
                            val item = list[index]
                            showPreviewDialog(item)
                        } else {
                            binding.rvStudentList.scrollToPosition(0)
                        }
                    } else {
                        binding.rvStudentList.scrollToPosition(0)
                    }
                }
                val htmlText = "Record Found: <font color='#3FFF00'><b>${list.size}</b></font>"
                binding.tvListRecordCount.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    private fun showImagePopup(path: String) {
        val popupBinding = DialogImagePopupBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(popupBinding.root)
            .create()

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = ImageStorageHelper.loadBitmapFromPath(path, 800, 800)
            bitmap?.let { popupBinding.ivFullImage.setImageBitmap(it) }
        }

        popupBinding.btnClosePopup.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHeightWeightHistoryDialog(item: StudentWithDetails) {
        val dialogBinding = DialogHeightWeightHistoryBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val container = dialogBinding.llHistoryListContainer
        container.removeAllViews()

        for (hw in item.heightWeightHistory) {
            val rowView = layoutInflater.inflate(R.layout.item_height_weight_row, container, false)
            rowView.findViewById<TextView>(R.id.tvRowNumber).text = hw.datePeriod
            rowView.findViewById<EditText>(R.id.etDatePeriod).setText(hw.datePeriod)
            rowView.findViewById<EditText>(R.id.etHeight).setText(hw.height)
            rowView.findViewById<EditText>(R.id.etWeight).setText(hw.weight)
            rowView.findViewById<ImageView>(R.id.btnDeleteRow).visibility = View.GONE
            container.addView(rowView)
        }

        dialogBinding.btnCloseHistory.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPreviewDialog(item: StudentWithDetails) {
        val dialogBinding = DialogStudentPreviewBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val s = item.student
        StudentPreviewHelper.bindStudentPreviewDialog(dialogBinding, item, viewLifecycleOwner)

        dialogBinding.btnPreviewEdit.setOnClickListener {
            dialog.dismiss()
            navigateToEdit(item)
        }

        dialogBinding.btnPreviewPrint.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val tempFile = File(getCacheDir(), "preview_student_${s.id}.pdf")
                val stream = tempFile.outputStream()
                val success = PdfPrintHelper.generateStudentProfilePdf(requireContext(), item, stream)
                stream.close()
                if (success) {
                    PdfPrintHelper.printPdfFile(requireContext(), tempFile, "Print ${s.studentName}")
                }
            }
        }

        dialogBinding.btnPreviewDelete.setOnClickListener {
            dialog.dismiss()
            confirmDelete(item)
        }

        dialogBinding.btnPreviewClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun navigateToEdit(item: StudentWithDetails) {
        val bundle = Bundle().apply {
            putLong("studentId", item.student.id)
        }
        findNavController().navigate(R.id.nav_add_student, bundle)
    }

    private fun confirmDelete(item: StudentWithDetails) {
        AlertDialog.Builder(requireContext())
            .setTitle("Move to Trash")
            .setMessage("Are you sure you want to move student '${item.student.studentName}' to Trash?")
            .setPositiveButton("Move to Trash") { _, _ ->
                viewModel.moveToTrash(item.student.id)
                Toast.makeText(requireContext(), "Student moved to Trash", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCacheDir(): File {
        val dir = File(requireContext().cacheDir, "pdf_previews")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

