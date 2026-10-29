package com.brijbymgt.ui.trash

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.databinding.FragmentTrashBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrashFragment : Fragment() {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrashViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao(),
            db.documentDao()
        )
        TrashViewModelFactory(repo, requireActivity().application)
    }

    private lateinit var adapter: TrashAdapter
    private var isAllSelected = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupRecyclerView()
        setupSearchAndActions()
        observeTrashList()
    }

    private fun setupSpinner() {
        val classes = listOf("All", "Class 1-8", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "CL0", "CL Pass")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classes)
        binding.spinnerClassFilter.adapter = spinnerAdapter
        binding.spinnerClassFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = classes[position]
                viewModel.setClassFilter(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        adapter = TrashAdapter(
            onRestoreClicked = { item -> confirmRestoreSingle(item) },
            onDeletePermanentlyClicked = { item -> confirmDeleteSingle(item) },
            onSelectionChanged = { selectedIds ->
                updateSelectionCounter(selectedIds.size)
            }
        )
        binding.rvTrashList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTrashList.adapter = adapter
    }

    private fun setupSearchAndActions() {
        binding.etSearchTrash.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSelectAll.setOnClickListener {
            isAllSelected = !isAllSelected
            binding.btnSelectAll.text = if (isAllSelected) "Deselect All" else "Select All"
            adapter.toggleSelectAll(isAllSelected, viewModel.trashList.value)
        }

        binding.btnRestoreSelected.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            if (selectedIds.isEmpty()) {
                Toast.makeText(requireContext(), "No students selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmRestoreBatch(selectedIds.toList())
        }

        binding.btnDeleteSelected.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            if (selectedIds.isEmpty()) {
                Toast.makeText(requireContext(), "No students selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmDeleteBatch(selectedIds.toList())
        }

        binding.btnEmptyTrash.setOnClickListener {
            val list = viewModel.trashList.value
            if (list.isEmpty()) {
                Toast.makeText(requireContext(), "Trash is already empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmEmptyTrash()
        }
    }

    private fun observeTrashList() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.trashList.collectLatest { list ->
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.rvTrashList.visibility = View.GONE
                    binding.tvEmptyTrash.visibility = View.VISIBLE
                } else {
                    binding.rvTrashList.visibility = View.VISIBLE
                    binding.tvEmptyTrash.visibility = View.GONE
                }
                updateSelectionCounter(adapter.getSelectedIds().size)
            }
        }
    }

    private fun updateSelectionCounter(count: Int) {
        binding.tvSelectedCounter.text = "$count students selected"
    }

    private fun confirmRestoreSingle(item: StudentWithDetails) {
        AlertDialog.Builder(requireContext())
            .setTitle("Restore Student")
            .setMessage("Are you sure you want to restore student '${item.student.studentName}'?")
            .setPositiveButton("Restore") { _, _ ->
                viewModel.restoreStudent(item.student.id)
                Toast.makeText(requireContext(), "Student restored successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmRestoreBatch(ids: List<Long>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Restore Selected Students")
            .setMessage("Are you sure you want to restore ${ids.size} selected student(s)?")
            .setPositiveButton("Restore All") { _, _ ->
                viewModel.restoreStudents(ids)
                adapter.setSelectedIds(emptySet())
                isAllSelected = false
                binding.btnSelectAll.text = "Select All"
                Toast.makeText(requireContext(), "${ids.size} student(s) restored", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteSingle(item: StudentWithDetails) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permanently Delete")
            .setMessage("Are you sure you want to permanently delete '${item.student.studentName}'? This action cannot be undone and will delete all associated physical documents.")
            .setPositiveButton("Delete Permanently") { _, _ ->
                viewModel.deletePermanently(item.student.id)
                Toast.makeText(requireContext(), "Student deleted permanently", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteBatch(ids: List<Long>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permanently Delete Selected")
            .setMessage("Are you sure you want to permanently delete ${ids.size} selected student(s)? This action cannot be undone and will delete all associated physical documents.")
            .setPositiveButton("Delete Permanently") { _, _ ->
                viewModel.deletePermanentlyBatch(ids)
                adapter.setSelectedIds(emptySet())
                isAllSelected = false
                binding.btnSelectAll.text = "Select All"
                Toast.makeText(requireContext(), "${ids.size} student(s) permanently deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmEmptyTrash() {
        AlertDialog.Builder(requireContext())
            .setTitle("Empty Trash")
            .setMessage("Are you sure you want to empty the trash? All deleted students and their physical documents will be permanently deleted.")
            .setPositiveButton("Empty Trash") { _, _ ->
                viewModel.emptyTrash()
                adapter.setSelectedIds(emptySet())
                isAllSelected = false
                binding.btnSelectAll.text = "Select All"
                Toast.makeText(requireContext(), "Trash emptied successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

