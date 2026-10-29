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
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.entity.ExamEntity
import com.brijbymgt.data.entity.MarksEntity
import com.brijbymgt.data.entity.SubjectEntity
import com.brijbymgt.data.repository.ExamRepository
import com.brijbymgt.databinding.FragmentMarksEntryBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MarksEntryFragment : Fragment() {

    private var _binding: FragmentMarksEntryBinding? = null
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

    private lateinit var marksAdapter: MarksEntryAdapter
    private var availableClasses: List<String> = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8")
    private var examListForClass: List<ExamEntity> = emptyList()
    private var currentExamSubjects: List<SubjectEntity> = emptyList()
    private var currentStudentMarksMap = mutableMapOf<Long, StudentMarkItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarksEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClassDropdown()

        binding.btnSaveMarks.setOnClickListener {
            saveMarksAction(false)
        }
        binding.btnSaveAllMarks.setOnClickListener {
            saveMarksAction(true)
        }
    }

    private fun setupRecyclerView() {
        marksAdapter = MarksEntryAdapter { item ->
            currentStudentMarksMap[item.examStudent.studentId] = item
        }
        binding.rvMarksEntry.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = marksAdapter
        }
    }

    private fun setupClassDropdown() {
        val classAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, availableClasses)
        binding.spinnerSelectClass.adapter = classAdapter

        binding.spinnerSelectClass.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in availableClasses.indices) {
                    val selectedClass = availableClasses[position]
                    onClassSelected(selectedClass)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun onClassSelected(className: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Step 2: Ensure standard exams exist for this class & load into Examination dropdown
            examListForClass = viewModel.ensureStandardExamsForClass(className)

            if (examListForClass.isEmpty()) {
                binding.spinnerSelectExam.isEnabled = false
                binding.spinnerSelectSubject.isEnabled = false
                marksAdapter.submitList(emptyList())
                Toast.makeText(requireContext(), "कोई परीक्षा उपलब्ध नहीं है।", Toast.LENGTH_SHORT).show()
                return@launch
            }

            binding.spinnerSelectExam.isEnabled = true
            val examNames = examListForClass.map { "${it.examName} (${it.examType})" }
            val examAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, examNames)
            binding.spinnerSelectExam.adapter = examAdapter

            binding.spinnerSelectExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position in examListForClass.indices) {
                        val selectedExam = examListForClass[position]
                        onExaminationSelected(selectedExam)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun onExaminationSelected(exam: ExamEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Step 3: Load active subjects for the selected Examination
            val subjects = viewModel.getActiveSubjects(exam.examId).first()
            currentExamSubjects = subjects

            if (subjects.isEmpty()) {
                binding.spinnerSelectSubject.isEnabled = false
                marksAdapter.submitList(emptyList())
                binding.tvSubjectMarksInfo.text = "Maximum Marks: - | Passing Marks: -"
                Toast.makeText(requireContext(), "इस परीक्षा के लिए कोई विषय उपलब्ध नहीं है।", Toast.LENGTH_SHORT).show()
                return@launch
            }

            binding.spinnerSelectSubject.isEnabled = true
            val subjectNames = subjects.map { "${it.subjectName} (Max: ${it.maxMarks}, Pass: ${it.passingMarks})" }
            val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, subjectNames)
            binding.spinnerSelectSubject.adapter = subAdapter

            binding.spinnerSelectSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position in currentExamSubjects.indices) {
                        val selectedSubject = currentExamSubjects[position]
                        binding.tvSubjectMarksInfo.text = "Maximum Marks: ${selectedSubject.maxMarks} | Passing Marks: ${selectedSubject.passingMarks}"
                        loadStudentListForSubject(exam.examId, selectedSubject.subjectId, selectedSubject.maxMarks, selectedSubject.passingMarks)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadStudentListForSubject(examId: Long, subjectId: Long, maxMarks: Int, passingMarks: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Step 4: Complete Student List in RecyclerView
            val examStudents = viewModel.getExamStudents(examId).first()
            val existingMarks = viewModel.getMarks(examId).first().filter { it.subjectId == subjectId }
            val marksMap = existingMarks.associateBy { it.studentId }

            currentStudentMarksMap.clear()
            val items = examStudents.mapIndexed { index, student ->
                val m = marksMap[student.studentId]
                val item = StudentMarkItem(
                    examStudent = student,
                    obtainedMarks = m?.obtainedMarks ?: 0.0,
                    isAbsent = m?.isAbsent ?: false,
                    maxMarks = maxMarks,
                    passingMarks = passingMarks,
                    srIndex = index + 1,
                    isSaved = m != null
                )
                currentStudentMarksMap[student.studentId] = item
                item
            }
            marksAdapter.submitList(items)
        }
    }

    private fun saveMarksAction(isSaveAll: Boolean) {
        val examPosition = binding.spinnerSelectExam.selectedItemPosition
        val subjectPosition = binding.spinnerSelectSubject.selectedItemPosition

        if (examPosition < 0 || examPosition >= examListForClass.size) {
            Toast.makeText(requireContext(), "कृपया वैध परीक्षा चुनें।", Toast.LENGTH_SHORT).show()
            return
        }

        if (subjectPosition < 0 || subjectPosition >= currentExamSubjects.size || !binding.spinnerSelectSubject.isEnabled) {
            Toast.makeText(requireContext(), "कृपया वैध विषय चुनें।", Toast.LENGTH_SHORT).show()
            return
        }

        val examId = examListForClass[examPosition].examId
        val subjectId = currentExamSubjects[subjectPosition].subjectId
        val maxMarks = currentExamSubjects[subjectPosition].maxMarks.toDouble()

        // Validation: 0 <= obtainedMarks <= maxMarks
        for ((_, item) in currentStudentMarksMap) {
            if (!item.isAbsent) {
                if (item.obtainedMarks < 0.0 || item.obtainedMarks > maxMarks) {
                    Toast.makeText(requireContext(), "अंक 0 और अधिकतम अंक ($maxMarks) के बीच होने चाहिए। विद्यार्थी: ${item.examStudent.studentName}", Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

        val marksList = currentStudentMarksMap.values.map { item ->
            MarksEntity(
                examId = examId,
                studentId = item.examStudent.studentId,
                subjectId = subjectId,
                obtainedMarks = item.obtainedMarks,
                isAbsent = item.isAbsent
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveMarksList(marksList)
            // Mark items as saved
            currentStudentMarksMap.values.forEach { it.isSaved = true }
            marksAdapter.notifyDataSetChanged()

            val msg = if (isSaveAll) "All marks saved & validated successfully!" else "Marks saved successfully!"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

