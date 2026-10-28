package com.brijby77.data.model

data class CategoryStat(
    val categoryName: String,
    val total: Int = 0,
    val male: Int = 0,
    val female: Int = 0
)

data class DashboardStats(
    val totalStudents: Int = 0,
    val totalMale: Int = 0,
    val totalFemale: Int = 0,
    val genStats: CategoryStat = CategoryStat("GEN"),
    val obcStats: CategoryStat = CategoryStat("OBC"),
    val scStats: CategoryStat = CategoryStat("SC"),
    val stStats: CategoryStat = CategoryStat("ST"),
    val minorityStats: CategoryStat = CategoryStat("MINORITY"),
    val aadharCount: Int = 0,
    val aaparCount: Int = 0,
    val dbtCount: Int = 0,
    val selectedClassFilter: String = "ALL CL",
    val classCounts: Map<String, Int> = emptyMap()
)

