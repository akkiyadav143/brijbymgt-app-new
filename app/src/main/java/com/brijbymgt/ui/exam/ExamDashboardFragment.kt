package com.brijbymgt.ui.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentExamDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExamDashboardFragment : Fragment() {

    private var _binding: FragmentExamDashboardBinding? = null
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeStats()
    }

    private fun setupClickListeners() {
        binding.btnSubjectManagement.setOnClickListener {
            findNavController().navigate(R.id.nav_subject_management)
        }
        binding.btnNewExam.setOnClickListener {
            findNavController().navigate(R.id.nav_create_exam)
        }
        binding.btnExamList.setOnClickListener {
            findNavController().navigate(R.id.nav_exam_list)
        }
        binding.btnMarksEntry.setOnClickListener {
            findNavController().navigate(R.id.nav_marks_entry)
        }
        binding.btnResultReportCard.setOnClickListener {
            findNavController().navigate(R.id.nav_result)
        }
        binding.btnClassResult.setOnClickListener {
            findNavController().navigate(R.id.nav_class_result)
        }
        binding.btnSubjectResult.setOnClickListener {
            findNavController().navigate(R.id.nav_subject_result)
        }
        binding.btnResultSummary.setOnClickListener {
            findNavController().navigate(R.id.nav_result_summary)
        }
        binding.btnExamSettings.setOnClickListener {
            findNavController().navigate(R.id.nav_exam_settings)
        }
        binding.btnFullReport.setOnClickListener {
            findNavController().navigate(R.id.nav_full_report)
        }
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExams.collectLatest { exams ->
                val totalExams = exams.size
                val activeExams = exams.count { it.status.equals("Active", true) }
                val completedExams = exams.count { it.status.equals("Completed", true) }
                val finalizedExams = exams.count { it.isFinalized }
                val pendingExams = totalExams - completedExams

                binding.tvStatTotalExams.text = totalExams.toString()
                binding.tvStatActiveExams.text = activeExams.toString()
                binding.tvStatCompletedExams.text = completedExams.toString()
                binding.tvStatFinalized.text = finalizedExams.toString()
                binding.tvStatPending.text = pendingExams.toString()
                binding.tvStatTotalStudents.text = "—"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

