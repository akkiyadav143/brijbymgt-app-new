package com.brijbymgt.ui.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentExamListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExamListFragment : Fragment() {

    private var _binding: FragmentExamListBinding? = null
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

    private lateinit var examAdapter: ExamAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeExams()
    }

    private fun setupRecyclerView() {
        examAdapter = ExamAdapter(
            onExamClick = { exam ->
                viewModel.setSelectedExamId(exam.examId)
                Toast.makeText(requireContext(), "Selected: ${exam.examName}", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            },
            onDeleteClick = { exam ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Exam")
                    .setMessage("Are you sure you want to delete '${exam.examName}' and all associated marks?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.deleteExam(exam.examId)
                            Toast.makeText(requireContext(), "Exam deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvExamList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = examAdapter
        }
    }

    private fun observeExams() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExams.collectLatest { exams ->
                examAdapter.submitList(exams)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

