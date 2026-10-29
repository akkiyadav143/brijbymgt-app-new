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
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.entity.ExamEntity
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentResultSummaryBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ResultSummaryFragment : Fragment() {

    private var _binding: FragmentResultSummaryBinding? = null
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

    private var examList: List<ExamEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadExams()
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
            binding.spinnerSummaryExam.adapter = adapter

            binding.spinnerSummaryExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val exam = examList[position]
                    loadSummary(exam.examId)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadSummary(examId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summaries = viewModel.calculateResults(examId)
            val totalStudents = summaries.size
            val passedCount = summaries.count { it.passStatus == "PASS" }
            val failedCount = totalStudents - passedCount
            val passPercentage = if (totalStudents > 0) (passedCount.toDouble() / totalStudents) * 100.0 else 0.0
            val topper = summaries.maxByOrNull { it.totalObtainedMarks }

            val sb = StringBuilder()
            sb.append("• Total Enrolled Students: $totalStudents\n")
            sb.append("• Total Passed: $passedCount\n")
            sb.append("• Total Failed / Absent: $failedCount\n")
            sb.append("• Overall Pass Percentage: ${String.format("%.1f", passPercentage)}%\n\n")
            if (topper != null) {
                sb.append("🏆 Class Topper:\n")
                sb.append("  - Name: ${topper.examStudent.studentName}\n")
                sb.append("  - SR Number: ${topper.examStudent.srNumber}\n")
                sb.append("  - Score: ${topper.totalObtainedMarks.toInt()} / ${topper.totalMaxMarks} (${String.format("%.1f", topper.percentage)}%)\n")
                sb.append("  - Grade: ${topper.grade}")
            } else {
                sb.append("🏆 Class Topper: None")
            }

            binding.tvSummaryContent.text = sb.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

