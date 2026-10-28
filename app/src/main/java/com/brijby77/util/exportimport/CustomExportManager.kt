package com.brijby77.util.exportimport

import com.brijby77.data.model.StudentWithDetails

object CustomExportManager {

    data class FilterCriteria(
        val selectedClass: String = "All", // "All", "Class 0".."Class 8", "CL Pass"
        val selectedGender: String = "All", // "All", "Male", "Female"
        val selectedCaste: String = "All", // "All", "GEN", "OBC", "SC", "ST", "MINORITY"
        val selectedDbt: String = "All", // "All", "Yes", "No"
        val searchQuery: String = "",
        val selectedStudentIds: Set<Long>? = null // null means all students matching criteria
    )

    fun filterStudents(
        allStudents: List<StudentWithDetails>,
        criteria: FilterCriteria
    ): List<StudentWithDetails> {
        return allStudents.filter { item ->
            val s = item.student

            // 1. Specific Student IDs
            if (criteria.selectedStudentIds != null && !criteria.selectedStudentIds.contains(s.id)) {
                return@filter false
            }

            // 2. Class Filter
            if (criteria.selectedClass != "All") {
                val normalizedClass = normalizeClass(s.studentClass)
                val targetClass = normalizeClass(criteria.selectedClass)
                if (normalizedClass != targetClass) return@filter false
            }

            // 3. Gender Filter
            if (criteria.selectedGender != "All") {
                if (!s.gender.equals(criteria.selectedGender, ignoreCase = true)) return@filter false
            }

            // 4. Caste Filter
            if (criteria.selectedCaste != "All") {
                if (!s.caste.equals(criteria.selectedCaste, ignoreCase = true)) return@filter false
            }

            // 5. DBT Filter
            if (criteria.selectedDbt != "All") {
                if (!s.dbt.equals(criteria.selectedDbt, ignoreCase = true)) return@filter false
            }

            // 6. Search Query Filter
            if (criteria.searchQuery.isNotBlank()) {
                val q = criteria.searchQuery.trim().lowercase()
                val match = s.studentName.lowercase().contains(q) ||
                        s.srNumber.lowercase().contains(q) ||
                        s.srNo.lowercase().contains(q) ||
                        s.mobileNo.lowercase().contains(q) ||
                        s.penNo.lowercase().contains(q) ||
                        s.aaparId.lowercase().contains(q) ||
                        s.studentAdhar.lowercase().contains(q)
                if (!match) return@filter false
            }

            true
        }
    }

    private fun normalizeClass(rawClass: String): String {
        val trimmed = rawClass.trim()
        if (trimmed.equals("CL Pass", true) || trimmed.equals("Pass", true)) return "CL Pass"
        val num = trimmed.replace("Class ", "").replace("CL ", "").trim()
        return "Class $num"
    }
}

