package com.brijby77.ui.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijby77.SchoolApp
import com.brijby77.data.entity.ExamEntity
import com.brijby77.data.entity.SubjectEntity
import com.brijby77.data.repository.ExamRepository
import com.brijby77.databinding.FragmentSubjectResultBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubjectResultFragment : Fragment() {

    private var _binding: FragmentSubjectResultBinding? = null
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

    private lateinit var subjectResultAdapter: SubjectResultAdapter
    private var examList: List<ExamEntity> = emptyList()
    private var subjectList: List<SubjectEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadExams()
    }

    private val setupRecyclerView: () -> Unit = {
        subjectResultAdapter = SubjectResultAdapter()
        binding.rvSubjectResultList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subjectResultAdapter
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
            binding.spinnerSubjectExam.adapter = adapter

            binding.spinnerSubjectExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val exam = examList[position]
                    loadSubjects(exam.examId)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadSubjects(examId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val subjects = viewModel.getSubjects(examId).first()
            subjectList = subjects
            val names = subjects.map { "${it.subjectName} (Max: ${it.maxMarks})" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerSubjectSelect.adapter = adapter

            binding.spinnerSubjectSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position < subjectList.size) {
                        val sub = subjectList[position]
                        loadSubjectMarks(examId, sub)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadSubjectMarks(examId: Long, subject: SubjectEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val students = viewModel.getExamStudents(examId).first()
            val marks = viewModel.getMarks(examId).first().filter { it.subjectId == subject.subjectId }
            val marksMap = marks.associateBy { it.studentId }

            val items = students.map { student ->
                SubjectStudentMark(
                    student = student,
                    mark = marksMap[student.studentId],
                    maxMarks = subject.maxMarks,
                    passingMarks = subject.passingMarks
                )
            }
            subjectResultAdapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

