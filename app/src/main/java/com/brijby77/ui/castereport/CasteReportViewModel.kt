package com.brijby77.ui.castereport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijby77.data.model.StudentWithDetails
import com.brijby77.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class ClassCasteRowData(
    val className: String,
    val scMale: Int = 0,
    val scFemale: Int = 0,
    val stMale: Int = 0,
    val stFemale: Int = 0,
    val obcMale: Int = 0,
    val obcFemale: Int = 0,
    val minMale: Int = 0,
    val minFemale: Int = 0,
    val genMale: Int = 0,
    val genFemale: Int = 0
) {
    val scTotal: Int get() = scMale + scFemale
    val stTotal: Int get() = stMale + stFemale
    val obcTotal: Int get() = obcMale + obcFemale
    val minTotal: Int get() = minMale + minFemale
    val genTotal: Int get() = genMale + genFemale

    val rowMaleTotal: Int get() = scMale + stMale + obcMale + minMale + genMale
    val rowFemaleTotal: Int get() = scFemale + stFemale + obcFemale + minFemale + genFemale
    val rowGrandTotal: Int get() = rowMaleTotal + rowFemaleTotal
}

data class CasteReportUiState(
    val classRows: List<ClassCasteRowData> = emptyList(),
    val summary1to5: ClassCasteRowData = ClassCasteRowData("योग (1-5)"),
    val summary6to8: ClassCasteRowData = ClassCasteRowData("योग (6-8)"),
    val grandTotal: ClassCasteRowData = ClassCasteRowData("महायोग (1-8)")
)

class CasteReportViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CasteReportUiState())
    val uiState: StateFlow<CasteReportUiState> = _uiState

    init {
        repository.mainClassActiveStudents.onEach { students ->
            _uiState.value = buildReportState(students)
        }.launchIn(viewModelScope)
    }

    private fun buildReportState(students: List<StudentWithDetails>): CasteReportUiState {
        val classNames = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8")
        val classRows = mutableListOf<ClassCasteRowData>()

        for (cls in classNames) {
            val clsNum = cls.replace("Class ", "").trim()
            val clsStudents = students.filter {
                val sc = it.student.studentClass
                val rawSc = sc.replace("Class ", "").replace("CL ", "").trim()
                sc.equals(cls, true) || rawSc.equals(clsNum, true)
            }

            var scM = 0; var scF = 0
            var stM = 0; var stF = 0
            var obcM = 0; var obcF = 0
            var minM = 0; var minF = 0
            var genM = 0; var genF = 0

            for (item in clsStudents) {
                val s = item.student
                val isMale = s.gender.equals("Male", ignoreCase = true)

                when (s.caste.uppercase().trim()) {
                    "SC" -> if (isMale) scM++ else scF++
                    "ST" -> if (isMale) stM++ else stF++
                    "OBC" -> if (isMale) obcM++ else obcF++
                    "MINORITY", "MIN" -> if (isMale) minM++ else minF++
                    else -> if (isMale) genM++ else genF++ // GEN
                }
            }

            classRows.add(
                ClassCasteRowData(
                    className = clsNum,
                    scMale = scM, scFemale = scF,
                    stMale = stM, stFemale = stF,
                    obcMale = obcM, obcFemale = obcF,
                    minMale = minM, minFemale = minF,
                    genMale = genM, genFemale = genF
                )
            )
        }

        // Summary 1-5
        val rows1to5 = classRows.take(5)
        val sum1to5 = ClassCasteRowData(
            className = "योग (1-5)",
            scMale = rows1to5.sumOf { it.scMale }, scFemale = rows1to5.sumOf { it.scFemale },
            stMale = rows1to5.sumOf { it.stMale }, stFemale = rows1to5.sumOf { it.stFemale },
            obcMale = rows1to5.sumOf { it.obcMale }, obcFemale = rows1to5.sumOf { it.obcFemale },
            minMale = rows1to5.sumOf { it.minMale }, minFemale = rows1to5.sumOf { it.minFemale },
            genMale = rows1to5.sumOf { it.genMale }, genFemale = rows1to5.sumOf { it.genFemale }
        )

        // Summary 6-8
        val rows6to8 = classRows.drop(5)
        val sum6to8 = ClassCasteRowData(
            className = "योग (6-8)",
            scMale = rows6to8.sumOf { it.scMale }, scFemale = rows6to8.sumOf { it.scFemale },
            stMale = rows6to8.sumOf { it.stMale }, stFemale = rows6to8.sumOf { it.stFemale },
            obcMale = rows6to8.sumOf { it.obcMale }, obcFemale = rows6to8.sumOf { it.obcFemale },
            minMale = rows6to8.sumOf { it.minMale }, minFemale = rows6to8.sumOf { it.minFemale },
            genMale = rows6to8.sumOf { it.genMale }, genFemale = rows6to8.sumOf { it.genFemale }
        )

        // Grand Total
        val grand = ClassCasteRowData(
            className = "महायोग (1-8)",
            scMale = classRows.sumOf { it.scMale }, scFemale = classRows.sumOf { it.scFemale },
            stMale = classRows.sumOf { it.stMale }, stFemale = classRows.sumOf { it.stFemale },
            obcMale = classRows.sumOf { it.obcMale }, obcFemale = classRows.sumOf { it.obcFemale },
            minMale = classRows.sumOf { it.minMale }, minFemale = classRows.sumOf { it.minFemale },
            genMale = classRows.sumOf { it.genMale }, genFemale = classRows.sumOf { it.genFemale }
        )

        return CasteReportUiState(
            classRows = classRows,
            summary1to5 = sum1to5,
            summary6to8 = sum6to8,
            grandTotal = grand
        )
    }
}

class CasteReportViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CasteReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CasteReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

