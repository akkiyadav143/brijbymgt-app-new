package com.brijby77.ui.exam

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijby77.R
import com.brijby77.SchoolApp
import com.brijby77.data.entity.ExamEntity
import com.brijby77.data.entity.SubjectEntity
import com.brijby77.data.repository.ExamRepository
import com.brijby77.databinding.FragmentSubjectManagementBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubjectManagementFragment : Fragment() {

    private var _binding: FragmentSubjectManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExamViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = ExamRepository(
            db.examDao(),
            db.subjectDao(),
            db.examStudentDao(),
            db.marksDao(),
            db.studentDao()
        )
        ExamViewModelFactory(repo)
    }

    private lateinit var subjectAdapter: SubjectManagementAdapter
    private var examList: List<ExamEntity> = emptyList()
    private var selectedExam: ExamEntity? = null
    private var subjectList: List<SubjectEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadExams()

        binding.btnAddSubject.setOnClickListener {
            selectedExam?.let { exam ->
                showAddEditSubjectDialog(exam.examId, null)
            } ?: Toast.makeText(requireContext(), "Please select an exam first", Toast.LENGTH_SHORT).show()
        }

        binding.btnUpdateExamWideMax.setOnClickListener {
            selectedExam?.let { exam ->
                val text = binding.etExamWideMaxMarks.text.toString()
                val maxMarks = text.toIntOrNull() ?: 100
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.updateExamWideMaxMarks(exam.examId, maxMarks)
                    Toast.makeText(requireContext(), "Exam-wide max marks updated!", Toast.LENGTH_SHORT).show()
                    selectedExam = viewModel.getExamById(exam.examId)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        subjectAdapter = SubjectManagementAdapter(
            onEdit = { subject ->
                showAddEditSubjectDialog(subject.examId, subject)
            },
            onDeleteOrDeactivate = { subject ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val msg = viewModel.deleteOrDeactivateSubject(subject.examId, subject.subjectId)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    loadSubjects(subject.examId)
                }
            },
            onMoveUp = { subject ->
                moveSubjectOrder(subject, -1)
            },
            onMoveDown = { subject ->
                moveSubjectOrder(subject, 1)
            }
        )
        binding.rvSubjectMgmtList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subjectAdapter
        }
    }

    private fun moveSubjectOrder(subject: SubjectEntity, direction: Int) {
        val index = subjectList.indexOfFirst { it.subjectId == subject.subjectId }
        val newIndex = index + direction
        if (newIndex >= 0 && newIndex < subjectList.size) {
            val targetSubject = subjectList[newIndex]
            viewLifecycleOwner.lifecycleScope.launch {
                val tempOrder = subject.subjectOrder
                val targetOrder = targetSubject.subjectOrder
                // Swap orders
                viewModel.updateSubjectOrder(subject.subjectId, targetOrder)
                viewModel.updateSubjectOrder(targetSubject.subjectId, tempOrder)
                loadSubjects(subject.examId)
            }
        }
    }

    private fun loadExams() {
        viewLifecycleOwner.lifecycleScope.launch {
            val exams = viewModel.allExams.first()
            examList = exams
            if (exams.isEmpty()) {
                Toast.makeText(requireContext(), "No exams found.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = exams.map { "${it.examName} (${it.className})" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerSubjectMgmtExam.adapter = adapter

            binding.spinnerSubjectMgmtExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedExam = examList[position]
                    selectedExam?.let { exam ->
                        binding.etExamWideMaxMarks.setText(exam.examWideMaxMarks.toString())
                        loadSubjects(exam.examId)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadSubjects(examId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getSubjects(examId).collect { subjects ->
                subjectList = subjects
                subjectAdapter.submitList(subjects)
            }
        }
    }

    private fun showAddEditSubjectDialog(examId: Long, existingSubject: SubjectEntity?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_subject, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogSubName)
        val etMax = dialogView.findViewById<EditText>(R.id.etDialogMaxMarks)
        val etPass = dialogView.findViewById<EditText>(R.id.etDialogPassingMarks)
        val etOrder = dialogView.findViewById<EditText>(R.id.etDialogSubjectOrder)
        val cbInclude = dialogView.findViewById<CheckBox>(R.id.cbDialogIncludeResult)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnDialogSave)

        if (existingSubject != null) {
            tvTitle.text = "Edit Subject"
            etName.setText(existingSubject.subjectName)
            etMax.setText(existingSubject.maxMarks.toString())
            etPass.setText(existingSubject.passingMarks.toString())
            etOrder.setText(existingSubject.subjectOrder.toString())
            cbInclude.isChecked = existingSubject.includeInResult
        } else {
            tvTitle.text = "Add New Subject"
            etOrder.setText((subjectList.size + 1).toString())
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val max = etMax.text.toString().toIntOrNull() ?: 100
            val pass = etPass.text.toString().toIntOrNull() ?: 33
            val order = etOrder.text.toString().toIntOrNull() ?: (subjectList.size + 1)
            val include = cbInclude.isChecked

            if (name.isBlank()) {
                etName.error = "Enter subject name"
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                if (existingSubject != null) {
                    val updated = existingSubject.copy(
                        subjectName = name,
                        maxMarks = max,
                        passingMarks = pass,
                        subjectOrder = order,
                        includeInResult = include
                    )
                    viewModel.updateSubject(updated)
                    Toast.makeText(requireContext(), "Subject updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addSubject(examId, name, max, pass, order, include)
                    Toast.makeText(requireContext(), "Subject added successfully!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                loadSubjects(examId)
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

