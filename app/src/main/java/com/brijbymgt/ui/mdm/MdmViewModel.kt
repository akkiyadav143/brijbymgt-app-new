package com.brijbymgt.ui.mdm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijbymgt.data.entity.MdmReportEntity
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ClassMdmData(
    val className: String,
    val dbTotal: Int = 0,
    val dbMale: Int = 0,
    val dbFemale: Int = 0,
    var mdmMale: Int = 0,
    var mdmFemale: Int = 0
) {
    val mdmTotal: Int get() = mdmMale + mdmFemale
}

data class MdmUiState(
    val dbTotal1to5: Int = 0,
    val dbTotal6to8: Int = 0,
    val dbTotal1to8: Int = 0,
    val classRows: List<ClassMdmData> = emptyList(),
    val dbSummary1to5: Triple<Int, Int, Int> = Triple(0, 0, 0), // DB Male, DB Female, DB Total
    val dbSummary6to8: Triple<Int, Int, Int> = Triple(0, 0, 0),
    val dbSummary1to8: Triple<Int, Int, Int> = Triple(0, 0, 0),
    val summary1to5: Triple<Int, Int, Int> = Triple(0, 0, 0), // MDM Male, MDM Female, MDM Total
    val summary6to8: Triple<Int, Int, Int> = Triple(0, 0, 0),
    val summary1to8: Triple<Int, Int, Int> = Triple(0, 0, 0)
)

class MdmViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _manualInputs = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap()) // className -> Pair(mdmMale, mdmFemale)

    val uiState: StateFlow<MdmUiState> = combine(
        repository.mainClassActiveStudents,
        repository.mdmReports,
        _manualInputs
    ) { students, savedReports, manualMap ->
        buildUiState(students, savedReports, manualMap)
    }.let { flow ->
        val state = MutableStateFlow(MdmUiState())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state
    }

    fun updateMdmInput(className: String, mdmMale: Int, mdmFemale: Int) {
        val current = _manualInputs.value.toMutableMap()
        current[className] = Pair(mdmMale, mdmFemale)
        _manualInputs.value = current
    }

    fun resetMdmInputs() {
        val classNames = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8")
        val resetMap = classNames.associateWith { Pair(0, 0) }
        _manualInputs.value = resetMap

        viewModelScope.launch {
            val resetEntities = classNames.map { cls ->
                MdmReportEntity(
                    studentClass = cls,
                    mdmMale = 0,
                    mdmFemale = 0
                )
            }
            repository.saveMdmReports(resetEntities)
        }
    }

    fun saveMdmReports() {
        viewModelScope.launch {
            val currentState = uiState.value
            val entities = currentState.classRows.map { row ->
                MdmReportEntity(
                    studentClass = row.className,
                    mdmMale = row.mdmMale,
                    mdmFemale = row.mdmFemale
                )
            }
            repository.saveMdmReports(entities)
        }
    }

    private fun buildUiState(
        students: List<StudentWithDetails>,
        savedReports: List<MdmReportEntity>,
        manualMap: Map<String, Pair<Int, Int>>
    ): MdmUiState {
        val savedMap = savedReports.associate { it.studentClass to Pair(it.mdmMale, it.mdmFemale) }

        val classNames = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8")
        val classRows = mutableListOf<ClassMdmData>()

        var total1to5 = 0
        var total6to8 = 0

        for (cls in classNames) {
            val clsNum = cls.replace("Class ", "").trim()
            val classStudents = students.filter {
                val sc = it.student.studentClass
                val rawSc = sc.replace("Class ", "").replace("CL ", "").trim()
                sc.equals(cls, true) || rawSc.equals(clsNum, true)
            }

            val dbTotal = classStudents.size
            val dbMale = classStudents.count { it.student.gender.equals("Male", true) }
            val dbFemale = dbTotal - dbMale

            if (cls in listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5")) {
                total1to5 += dbTotal
            } else {
                total6to8 += dbTotal
            }

            val savedPair = savedMap[cls] ?: Pair(0, 0)
            val manualPair = manualMap[cls] ?: savedPair

            classRows.add(
                ClassMdmData(
                    className = cls,
                    dbTotal = dbTotal,
                    dbMale = dbMale,
                    dbFemale = dbFemale,
                    mdmMale = manualPair.first,
                    mdmFemale = manualPair.second
                )
            )
        }

        // Calculate DB & MDM Summaries
        var dbSum1to5Male = 0
        var dbSum1to5Female = 0
        var dbSum6to8Male = 0
        var dbSum6to8Female = 0

        var sum1to5Male = 0
        var sum1to5Female = 0
        var sum6to8Male = 0
        var sum6to8Female = 0

        for (row in classRows) {
            if (row.className in listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5")) {
                dbSum1to5Male += row.dbMale
                dbSum1to5Female += row.dbFemale
                sum1to5Male += row.mdmMale
                sum1to5Female += row.mdmFemale
            } else {
                dbSum6to8Male += row.dbMale
                dbSum6to8Female += row.dbFemale
                sum6to8Male += row.mdmMale
                sum6to8Female += row.mdmFemale
            }
        }

        val dbSum1to5 = Triple(dbSum1to5Male, dbSum1to5Female, dbSum1to5Male + dbSum1to5Female)
        val dbSum6to8 = Triple(dbSum6to8Male, dbSum6to8Female, dbSum6to8Male + dbSum6to8Female)
        val dbSum1to8 = Triple(dbSum1to5Male + dbSum6to8Male, dbSum1to5Female + dbSum6to8Female, dbSum1to5Male + dbSum1to5Female + dbSum6to8Male + dbSum6to8Female)

        val sum1to5 = Triple(sum1to5Male, sum1to5Female, sum1to5Male + sum1to5Female)
        val sum6to8 = Triple(sum6to8Male, sum6to8Female, sum6to8Male + sum6to8Female)
        val sum1to8 = Triple(sum1to5Male + sum6to8Male, sum1to5Female + sum6to8Female, sum1to5Male + sum1to5Female + sum6to8Male + sum6to8Female)

        return MdmUiState(
            dbTotal1to5 = total1to5,
            dbTotal6to8 = total6to8,
            dbTotal1to8 = total1to5 + total6to8,
            classRows = classRows,
            dbSummary1to5 = dbSum1to5,
            dbSummary6to8 = dbSum6to8,
            dbSummary1to8 = dbSum1to8,
            summary1to5 = sum1to5,
            summary6to8 = sum6to8,
            summary1to8 = sum1to8
        )
    }
}

class MdmViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MdmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MdmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

