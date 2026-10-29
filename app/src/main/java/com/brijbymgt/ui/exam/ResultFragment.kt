package com.brijbymgt.ui.exam

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.entity.ExamEntity
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentResultBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
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

    private lateinit var resultAdapter: ResultAdapter
    private var examList: List<ExamEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadExams()
    }

    private fun setupRecyclerView() {
        resultAdapter = ResultAdapter { summary ->
            // Navigate to ReportCardFragment with studentId and examId
            val bundle = Bundle().apply {
                putLong("examId", summary.examStudent.examId)
                putLong("studentId", summary.examStudent.studentId)
            }
            findNavController().navigate(R.id.nav_report_card, bundle)
        }

        binding.rvResultList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resultAdapter
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
            binding.spinnerResultExam.adapter = adapter

            binding.spinnerResultExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val exam = examList[position]
                    loadResults(exam.examId)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadResults(examId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summaries = viewModel.calculateResults(examId)
            resultAdapter.submitList(summaries)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

