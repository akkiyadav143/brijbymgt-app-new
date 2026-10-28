package com.brijby77.ui.cl0pass

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
import com.brijby77.R
import com.brijby77.SchoolApp
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.data.repository.StudentRepository
import com.brijby77.databinding.DialogHeightWeightHistoryBinding
import com.brijby77.databinding.DialogImagePopupBinding
import com.brijby77.databinding.DialogStudentPreviewBinding
import com.brijby77.databinding.FragmentCl0PassBinding
import com.brijby77.ui.studentlist.StudentAdapter
import com.brijby77.util.ImageStorageHelper
import com.brijby77.util.PdfPrintHelper
import com.brijby77.util.StudentPreviewHelper
import kotlinx.coroutines.launch
import java.io.File

class Cl0PassFragment : Fragment() {

    private var _binding: FragmentCl0PassBinding? = null
    private val binding get() = _binding!!

    private val viewModel: Cl0PassViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao()
        )
        Cl0PassViewModelFactory(repo)
    }

    private lateinit var adapter: StudentAdapter
    private var currentFilter = "All"
    private var currentSearchField = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCl0PassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFiltersAndSearch()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = StudentAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            onImageClicked = { path -> showImagePopup(path) },
            onViewHistoryClicked = { item -> showHeightWeightHistoryDialog(item) },
            onPreviewClicked = { item -> showPreviewDialog(item) }
        )
        binding.rvCl0List.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCl0List.adapter = adapter
    }

    private fun setupFiltersAndSearch() {
        // Filter Buttons
        val filterButtons = listOf(
            binding.btnCl0FilterAll to "All",
            binding.btnCl0Filter0 to "Class 0",
            binding.btnCl0FilterPass to "CL Pass"
        )

        for ((btn, filterKey) in filterButtons) {
            btn.setOnClickListener {
                currentFilter = filterKey
                viewModel.setFilter(filterKey)
                updateFilterPills(filterKey)
            }
        }

        // Search Field Buttons
        val searchButtons = listOf(
            binding.btnCl0SearchAll to "All",
            binding.btnCl0SearchSrNumber to "SR Number",
            binding.btnCl0SearchStudentName to "Student Name",
            binding.btnCl0SearchAadhaar to "Student Adhar",
            binding.btnCl0SearchMobile to "Mobile No",
            binding.btnCl0SearchFather to "Father Name",
            binding.btnCl0SearchMother to "Mother Name"
        )

        for ((btn, fieldKey) in searchButtons) {
            btn.setOnClickListener {
                currentSearchField = fieldKey
                viewModel.setSearchField(fieldKey)
                updateSearchPills(fieldKey)
            }
        }

        // Search Box
        binding.etCl0SearchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        updateFilterPills(currentFilter)
        updateSearchPills(currentSearchField)
    }

    private fun updateFilterPills(selectedFilter: String) {
        val filterButtons = listOf(
            "All" to binding.btnCl0FilterAll,
            "Class 0" to binding.btnCl0Filter0,
            "CL Pass" to binding.btnCl0FilterPass
        )

        val papayaColor = Color.parseColor("#E56717")
        val unselectedBgColor = Color.parseColor("#F1F5F9")
        val unselectedTextColor = Color.parseColor("#1E293B")

        for ((key, btn) in filterButtons) {
            val label = when (key) {
                "Class 0" -> "CL 0"
                else -> key
            }
            if (key.equals(selectedFilter, ignoreCase = true)) {
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

    private fun updateSearchPills(selectedField: String) {
        val searchButtons = listOf(
            "All" to binding.btnCl0SearchAll,
            "SR Number" to binding.btnCl0SearchSrNumber,
            "Student Name" to binding.btnCl0SearchStudentName,
            "Student Adhar" to binding.btnCl0SearchAadhaar,
            "Mobile No" to binding.btnCl0SearchMobile,
            "Father Name" to binding.btnCl0SearchFather,
            "Mother Name" to binding.btnCl0SearchMother
        )

        val papayaColor = Color.parseColor("#E56717")
        val unselectedBgColor = Color.parseColor("#F1F5F9")
        val unselectedTextColor = Color.parseColor("#334155")

        for ((fieldKey, btn) in searchButtons) {
            val displayLabel = when (fieldKey) {
                "Student Adhar" -> "Aadhaar"
                else -> fieldKey
            }

            if (fieldKey.equals(selectedField, ignoreCase = true)) {
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredList.collect { list ->
                adapter.submitList(list)
                val htmlText = "Record Found: <font color='#3FFF00'><b>${list.size}</b></font>"
                binding.tvCl0Count.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
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
                val tempFile = File(getCacheDir(), "preview_cl0_${s.id}.pdf")
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

