package com.brijby77.ui.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijby77.SchoolApp
import com.brijby77.data.repository.ExamRepository
import com.brijby77.databinding.FragmentCreateExamBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateExamFragment : Fragment() {

    private var _binding: FragmentCreateExamBinding? = null
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
        _binding = FragmentCreateExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()

        binding.btnSubmitCreateExam.setOnClickListener {
            createExamAction()
        }
    }

    private fun setupSpinners() {
        val examTypes = listOf("Half Yearly", "Annual", "Unit Test", "Quarterly", "Monthly Test")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, examTypes)
        binding.spinnerExamType.adapter = typeAdapter

        val classes = listOf("All Classes", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 0")
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, classes)
        binding.spinnerClassName.adapter = classAdapter
    }

    private fun createExamAction() {
        val name = binding.etExamName.text.toString().trim()
        val session = binding.etAcademicSession.text.toString().trim()
        val type = binding.spinnerExamType.selectedItem.toString()
        val className = binding.spinnerClassName.selectedItem.toString()

        if (name.isEmpty()) {
            binding.etExamName.error = "Enter exam name"
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val examId = viewModel.createExam(
                    examName = name,
                    academicSession = session.ifEmpty { "2024-2025" },
                    examType = type,
                    className = className,
                    startDate = currentDate,
                    endDate = currentDate,
                    customSubjects = emptyList() // Uses default subjects
                )
                Toast.makeText(requireContext(), "Exam created & students synced successfully!", Toast.LENGTH_SHORT).show()
                viewModel.setSelectedExamId(examId)
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

