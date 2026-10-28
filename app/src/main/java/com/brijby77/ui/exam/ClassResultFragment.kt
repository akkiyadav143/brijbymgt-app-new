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
import com.brijby77.data.repository.ExamRepository
import com.brijby77.databinding.FragmentClassResultBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClassResultFragment : Fragment() {

    private var _binding: FragmentClassResultBinding? = null
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

    private lateinit var classResultAdapter: ClassResultAdapter
    private var examList: List<ExamEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClassResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadExams()
    }

    private fun setupRecyclerView() {
        classResultAdapter = ClassResultAdapter()
        binding.rvClassResultList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classResultAdapter
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
            binding.spinnerClassResultExam.adapter = adapter

            binding.spinnerClassResultExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val exam = examList[position]
                    loadClassResults(exam.examId)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadClassResults(examId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summaries = viewModel.calculateResults(examId)
            classResultAdapter.submitList(summaries)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

