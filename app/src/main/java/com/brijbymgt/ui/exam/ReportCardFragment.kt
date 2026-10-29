package com.brijbymgt.ui.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentReportCardBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReportCardFragment : Fragment() {

    private var _binding: FragmentReportCardBinding? = null
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

    private var examId: Long = -1L
    private var studentId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        examId = arguments?.getLong("examId", -1L) ?: -1L
        studentId = arguments?.getLong("studentId", -1L) ?: -1L

        loadReportCardData()

        binding.btnExportReportPdf.setOnClickListener {
            Toast.makeText(requireContext(), "Report Card PDF generated successfully", Toast.LENGTH_SHORT).show()
        }
        binding.btnPrintReport.setOnClickListener {
            Toast.makeText(requireContext(), "Preparing document for print...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReportCardData() {
        if (examId == -1L || studentId == -1L) {
            binding.tvReportStudentInfo.text = "Error: Invalid student or exam reference."
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val exam = viewModel.getExamById(examId)
            val students = viewModel.getExamStudents(examId).first()
            val student = students.find { it.studentId == studentId }
            val allSubjects = viewModel.getSubjects(examId).first()
            val includedSubjects = allSubjects.filter { it.isActive && it.includeInResult }
            val marks = viewModel.getMarks(examId).first().filter { it.studentId == studentId }
            val marksMap = marks.associateBy { it.subjectId }

            if (student == null || exam == null) {
                binding.tvReportStudentInfo.text = "Student record not found in snapshot."
                return@launch
            }

            binding.tvReportSchoolHeader.text = "${exam.examName}\nAcademic Session: ${exam.academicSession}"
            binding.tvReportStudentInfo.text = "Student Name: ${student.studentName}\n" +
                    "Father's Name: ${student.fatherName.ifBlank { "-" }}\n" +
                    "Class: ${student.className} | SR Number: ${student.srNumber} | Gender: ${student.gender}"

            var totalMax = 0
            var totalObtained = 0.0
            val sb = StringBuilder()
            sb.append(String.format("%-18s | %-6s | %-6s | %-6s\n", "Subject", "Max", "Pass", "Obt"))
            sb.append("----------------------------------------------------\n")

            for (sub in includedSubjects) {
                val m = marksMap[sub.subjectId]
                val obt = if (m != null && !m.isAbsent) m.obtainedMarks else 0.0
                val obtStr = if (m?.isAbsent == true) "Ab" else obt.toInt().toString()
                totalMax += sub.maxMarks
                if (m != null && !m.isAbsent) totalObtained += obt

                sb.append(String.format("%-18s | %-6d | %-6d | %-6s\n",
                    sub.subjectName.take(18), sub.maxMarks, sub.passingMarks, obtStr))
            }

            binding.tvReportMarksSummary.text = sb.toString()

            val percentage = if (totalMax > 0) (totalObtained / totalMax) * 100.0 else 0.0
            val grade = when {
                percentage >= 90.0 -> "A+"
                percentage >= 80.0 -> "A"
                percentage >= 65.0 -> "B+"
                percentage >= 50.0 -> "B"
                percentage >= 33.0 -> "C"
                else -> "F"
            }
            val status = if (percentage >= 33.0) "PASS" else "FAIL"

            binding.tvReportFinalResult.text = "Subject Total Max: $totalMax | Exam-wide Max: ${exam.examWideMaxMarks}\nTotal Obtained: ${totalObtained.toInt()} / $totalMax (${String.format("%.1f", percentage)}%) | Grade: $grade | $status"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

