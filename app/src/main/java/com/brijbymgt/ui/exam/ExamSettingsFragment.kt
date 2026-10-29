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
import com.brijbymgt.databinding.FragmentExamSettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExamSettingsFragment : Fragment() {

    private var _binding: FragmentExamSettingsBinding? = null
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
    private var selectedExam: ExamEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadExams()

        binding.btnToggleFinalize.setOnClickListener {
            selectedExam?.let { exam ->
                val newStatus = !exam.isFinalized
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.finalizeExam(exam.examId, newStatus)
                    Toast.makeText(requireContext(), if (newStatus) "Exam finalized successfully!" else "Exam unfinalized.", Toast.LENGTH_SHORT).show()
                    loadExams()
                }
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

            val names = exams.map { "${it.examName} (${if (it.isFinalized) "Finalized" else "Active"})" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerSettingsExam.adapter = adapter

            binding.spinnerSettingsExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedExam = examList[position]
                    binding.btnToggleFinalize.text = if (selectedExam?.isFinalized == true) "🔓 Unfinalize Exam Result" else "🔒 Finalize Exam Result"
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

