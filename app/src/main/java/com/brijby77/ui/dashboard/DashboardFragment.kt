package com.brijby77.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijby77.R
import com.brijby77.SchoolApp
import com.brijby77.data.repository.StudentRepository
import com.brijby77.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao()
        )
        DashboardViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateStr = SimpleDateFormat("dd EEEE MMMM yyyy", Locale.getDefault()).format(Date())
        binding.tvDashboardDate.text = dateStr

        setupClassFilterButtons()
        setupReportButtons()
        observeStats()
    }

    private fun setupClassFilterButtons() {
        binding.btnAllCl.setOnClickListener { viewModel.setFilter("ALL CL") }
        binding.btnCl1.setOnClickListener { viewModel.setFilter("Class 1") }
        binding.btnCl2.setOnClickListener { viewModel.setFilter("Class 2") }
        binding.btnCl3.setOnClickListener { viewModel.setFilter("Class 3") }
        binding.btnCl4.setOnClickListener { viewModel.setFilter("Class 4") }
        binding.btnCl5.setOnClickListener { viewModel.setFilter("Class 5") }
        binding.btnCl6.setOnClickListener { viewModel.setFilter("Class 6") }
        binding.btnCl7.setOnClickListener { viewModel.setFilter("Class 7") }
        binding.btnCl8.setOnClickListener { viewModel.setFilter("Class 8") }
    }

    private fun setupReportButtons() {
        binding.btnMdmReport.setOnClickListener {
            findNavController().navigate(R.id.nav_mdm)
        }
        binding.btnCasteReport.setOnClickListener {
            findNavController().navigate(R.id.nav_caste_report)
        }
        binding.btnTcPrint.setOnClickListener {
            findNavController().navigate(R.id.nav_tc_print)
        }
        binding.btnExamDashboard.setOnClickListener {
            findNavController().navigate(R.id.nav_exam_dashboard)
        }
        binding.btnTrash.setOnClickListener {
            findNavController().navigate(R.id.nav_trash)
        }
        binding.btnExportImportTab.setOnClickListener {
            findNavController().navigate(R.id.nav_export_import)
        }
        binding.btnSelectedExport.setOnClickListener {
            findNavController().navigate(R.id.nav_selected_export)
        }
        binding.btnSo2Report.setOnClickListener {
            findNavController().navigate(R.id.nav_so2)
        }
        binding.btnSo3Report.setOnClickListener {
            findNavController().navigate(R.id.nav_so3)
        }
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dashboardStats.collect { stats ->
                binding.tvActiveFilterLabel.text = "Showing Statistics for: ${stats.selectedClassFilter}"

                // 1. Category Cards + Gender Breakdown
                bindCategoryGenderCard(
                    binding.cardTotal.root,
                    "TOTAL",
                    stats.totalStudents,
                    stats.totalMale,
                    stats.totalFemale
                )

                bindCategoryGenderCard(
                    binding.cardGen.root,
                    "GEN",
                    stats.genStats.total,
                    stats.genStats.male,
                    stats.genStats.female
                )

                bindCategoryGenderCard(
                    binding.cardObc.root,
                    "OBC",
                    stats.obcStats.total,
                    stats.obcStats.male,
                    stats.obcStats.female
                )

                bindCategoryGenderCard(
                    binding.cardSc.root,
                    "SC",
                    stats.scStats.total,
                    stats.scStats.male,
                    stats.scStats.female
                )

                bindCategoryGenderCard(
                    binding.cardSt.root,
                    "ST",
                    stats.stStats.total,
                    stats.stStats.male,
                    stats.stStats.female
                )

                bindCategoryGenderCard(
                    binding.cardMinority.root,
                    "MINORITY",
                    stats.minorityStats.total,
                    stats.minorityStats.male,
                    stats.minorityStats.female
                )

                // 2. AADHAR, APAAR, DBT
                binding.tvAadharCount.text = stats.aadharCount.toString()
                binding.tvAaparCount.text = stats.aaparCount.toString()
                binding.tvDbtCount.text = stats.dbtCount.toString()

                // 3. Class-wise counts banner (Count Number ONLY +4pt larger in Harlequin Green #3FFF00)
                val map = stats.classCounts
                fun formatPillText(label: String, valKey: String): CharSequence {
                    val count = map[valKey] ?: 0
                    val prefix = "$label : "
                    val numStr = "$count"
                    val fullText = prefix + numStr

                    val spannable = SpannableStringBuilder(fullText)
                    val numStart = prefix.length
                    val numEnd = fullText.length

                    // Increase font size of count number ONLY by +4pt (14sp)
                    spannable.setSpan(
                        AbsoluteSizeSpan(14, true),
                        numStart, numEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#3FFF00")),
                        numStart, numEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        numStart, numEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    return spannable
                }

                binding.tvCountTotal.text = formatPillText("TOTAL", "TOTAL")
                binding.tvCountAllCl.text = formatPillText("ALL", "ALL CL")
                binding.tvCountCl1.text = formatPillText("Class 1", "CL 1")
                binding.tvCountCl2.text = formatPillText("Class 2", "CL 2")
                binding.tvCountCl3.text = formatPillText("Class 3", "CL 3")
                binding.tvCountCl4.text = formatPillText("Class 4", "CL 4")
                binding.tvCountCl5.text = formatPillText("Class 5", "CL 5")
                binding.tvCountCl6.text = formatPillText("Class 6", "CL 6")
                binding.tvCountCl7.text = formatPillText("Class 7", "CL 7")
                binding.tvCountCl8.text = formatPillText("Class 8", "CL 8")
                binding.tvCountCl0.text = formatPillText("Class 0", "CL 0")
                binding.tvCountClPass.text = formatPillText("Pass", "CL Pass")

                // 4. Class Filter Buttons
                updateClassFilterPillButtons(stats.selectedClassFilter)
            }
        }
    }

    private fun bindCategoryGenderCard(
        cardView: View,
        title: String,
        total: Int,
        male: Int,
        female: Int
    ) {
        val tvTitle = cardView.findViewById<TextView>(R.id.tvCategoryTitle)
        val tvCount = cardView.findViewById<TextView>(R.id.tvCategoryCount)
        val tvMale = cardView.findViewById<TextView>(R.id.tvMaleCount)
        val tvFemale = cardView.findViewById<TextView>(R.id.tvFemaleCount)

        tvTitle.text = title
        tvCount.text = total.toString()
        tvMale.text = male.toString()
        tvFemale.text = female.toString()
    }

    private fun updateClassFilterPillButtons(selectedFilter: String) {
        val buttonMap: List<Pair<String, Button>> = listOf(
            "ALL CL" to binding.btnAllCl,
            "Class 1" to binding.btnCl1,
            "Class 2" to binding.btnCl2,
            "Class 3" to binding.btnCl3,
            "Class 4" to binding.btnCl4,
            "Class 5" to binding.btnCl5,
            "Class 6" to binding.btnCl6,
            "Class 7" to binding.btnCl7,
            "Class 8" to binding.btnCl8
        )

        val selectedBgColor = Color.parseColor("#1E3A8A")
        val selectedTextColor = Color.parseColor("#FFFFFF")
        val unselectedBgColor = Color.parseColor("#F1F5F9")
        val unselectedTextColor = Color.parseColor("#1E293B")

        for ((filterKey, btn) in buttonMap) {
            val labelName = filterKey.replace("Class ", "CL ")
            if (filterKey.equals(selectedFilter, ignoreCase = true) ||
                (selectedFilter == "ALL CL" && filterKey == "ALL CL")
            ) {
                // SELECTED -> Dark Blue bg, White text, ◉ radio
                btn.text = "◉ $labelName"
                btn.backgroundTintList = ColorStateList.valueOf(selectedBgColor)
                btn.setTextColor(selectedTextColor)
            } else {
                // UNSELECTED -> Light Slate bg, Dark text, ○ radio
                btn.text = "○ $labelName"
                btn.backgroundTintList = ColorStateList.valueOf(unselectedBgColor)
                btn.setTextColor(unselectedTextColor)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

