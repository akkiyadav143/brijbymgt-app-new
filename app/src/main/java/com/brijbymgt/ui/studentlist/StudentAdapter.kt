package com.brijbymgt.ui.studentlist

import android.R
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.databinding.ItemStudentCardBinding
import com.brijbymgt.util.ImageStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentAdapter(
    private val scope: CoroutineScope,
    private val onImageClicked: (String) -> Unit,
    private val onViewHistoryClicked: (StudentWithDetails) -> Unit,
    private val onPreviewClicked: (StudentWithDetails) -> Unit
) : ListAdapter<StudentWithDetails, StudentAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemStudentCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val s = item.student
        val h = item.healthDetails
        val b = item.bankDetails

        // Photo
        if (!s.imagePath.isNullOrBlank()) {
            scope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(s.imagePath)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        holder.binding.ivStudentPhoto.setImageBitmap(bitmap)
                    } else {
                        holder.binding.ivStudentPhoto.setImageResource(R.drawable.ic_menu_camera)
                    }
                }
            }
            holder.binding.ivStudentPhoto.setOnClickListener {
                onImageClicked(s.imagePath)
            }
        } else {
            holder.binding.ivStudentPhoto.setImageResource(R.drawable.ic_menu_camera)
            holder.binding.ivStudentPhoto.setOnClickListener(null)
        }

        // Sr No Badge directly below photo in Dark Pink #C2185B
        holder.binding.tvSrNoBelowPhoto.text = "Sr No : ${s.srNo.ifBlank { "-" }}"

        // Student Name in Bold Blue (#1565C0)
        holder.binding.tvStudentName.text = s.studentName.ifBlank { "NO NAME" }.uppercase()
        holder.binding.tvStudentName.setTextColor(Color.parseColor("#1565C0"))

        // Class Tab (ensure "Class " string)
        val formattedClass = if (s.studentClass.startsWith("Class ", ignoreCase = true) || s.studentClass.startsWith("CL ", ignoreCase = true)) {
            s.studentClass.replace("CL ", "Class ")
        } else if (s.studentClass.equals("CL Pass", true) || s.studentClass.equals("Pass", true)) {
            "CL Pass"
        } else {
            "Class ${s.studentClass}"
        }
        holder.binding.tvClassTab.text = formattedClass
        holder.binding.tvCategoryTab.text = s.caste.ifBlank { "GEN" }

        // Category Badge Background Color
        val catBgColor = when (s.caste.uppercase().trim()) {
            "GEN" -> Color.parseColor("#D32F2F") // Red
            "OBC" -> Color.parseColor("#388E3C") // Green
            "SC" -> Color.parseColor("#1976D2")  // Blue
            "ST" -> Color.parseColor("#FBC02D")  // Yellow
            "MINORITY" -> Color.parseColor("#F57C00") // Orange
            else -> Color.parseColor("#4E342E")
        }
        holder.binding.tvCategoryTab.setBackgroundColor(catBgColor)

        // Gender Tab: Male = Blue #014EB1, Female = Maximum Red #D92200
        val isFemale = s.gender.equals("Female", ignoreCase = true)
        val genderBgColor = if (isFemale) Color.parseColor("#D92200") else Color.parseColor("#014EB1")
        holder.binding.tvGenderTab.text = if (s.gender.isNotBlank()) s.gender else "Male"
        holder.binding.tvGenderTab.setBackgroundColor(genderBgColor)
        holder.binding.tvGenderTab.setTextColor(Color.WHITE)

        // Below Gender: SR NUMBER value ONLY in Rosewood #5A0006 background card
        holder.binding.tvSrNumberVal.text = s.srNumber.ifBlank { "-" }
        holder.binding.tvSrNumberVal.setBackgroundColor(Color.parseColor("#5A0006"))
        holder.binding.tvSrNumberVal.setTextColor(Color.WHITE)

        // Call Button on right side of Adm Date & DOB row
        holder.binding.btnCallStudent.setOnClickListener {
            val mobile = s.mobileNo.trim()
            if (mobile.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$mobile"))
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(holder.itemView.context, "No mobile number available to call", Toast.LENGTH_SHORT).show()
            }
        }

        // Details Section Rows
        holder.binding.tvSrInfo.text = "SR Number : ${s.srNumber.ifBlank { "-" }}   C/o : ${s.co.ifBlank { "-" }}" // Row 1
        holder.binding.tvAdmDob.text = "Adm Date : ${s.admissionDate.ifBlank { "-" }}   DOB : ${s.dob.ifBlank { "-" }}" // Row 2 (WITH CALL BUTTON ON RIGHT)
        holder.binding.tvAdrUid.text = "ADR : ${s.studentAdhar.ifBlank { "-" }}   UID : ${s.uniqueId.ifBlank { "-" }}" // Row 3

        // Row 4: PEN VALUE ONLY WITH BACKGROUND DARK BLACK #0F172A & HARLEQUIN GREEN #3FFF00 TEXT
        val penVal = s.penNo.ifBlank { "-" }
        val aprVal = s.aaparId.ifBlank { "-" }

        val prefixPen = "PEN : "
        val valPen = " $penVal "
        val suffixPen = "   APR : $aprVal"

        val fullPenText = prefixPen + valPen + suffixPen
        val spannablePen = SpannableStringBuilder(fullPenText)

        val penStart = prefixPen.length
        val penEnd = penStart + valPen.length

        // Dark Black Background #0F172A for PEN value
        spannablePen.setSpan(
            BackgroundColorSpan(Color.parseColor("#0F172A")),
            penStart, penEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Harlequin Green Text #3FFF00
        spannablePen.setSpan(
            ForegroundColorSpan(Color.parseColor("#3FFF00")),
            penStart, penEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannablePen.setSpan(
            StyleSpan(Typeface.BOLD),
            penStart, penEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        holder.binding.tvPenApr.text = spannablePen

        holder.binding.tvFaInfo.text = "Fa_N : ${s.fatherName.ifBlank { "-" }.uppercase()}   Fa_A : ${s.fatherAdhar.ifBlank { "-" }}"
        holder.binding.tvMoInfo.text = "M_N : ${s.motherName.ifBlank { "-" }.uppercase()}   M_A : ${s.motherAdhar.ifBlank { "-" }}"
        holder.binding.tvMobInfo.text = "Mob : ${s.mobileNo.ifBlank { "-" }}   Adrs : ${s.address.ifBlank { "-" }}"

        // Health Details
        if (h != null && h.hasHealthDetails) {
            val hText = "Height: ${h.height.ifBlank { "-" }} | Weight: ${h.weight.ifBlank { "-" }} | Blood Group: ${h.bloodGroup.ifBlank { "-" }}\nHb: ${h.hb.ifBlank { "-" }} | Eye Sight: ${h.eyeSight.ifBlank { "-" }} | Deworming: ${h.deworming.ifBlank { "-" }}"
            holder.binding.tvHealthDetailsContent.text = hText

            if (item.heightWeightHistory.isNotEmpty()) {
                holder.binding.btnViewHistory.visibility = View.VISIBLE
                holder.binding.btnViewHistory.setOnClickListener {
                    onViewHistoryClicked(item)
                }
            } else {
                holder.binding.btnViewHistory.visibility = View.GONE
            }
        } else {
            holder.binding.tvHealthDetailsContent.text = "-"
            holder.binding.btnViewHistory.visibility = View.GONE
        }

        // Bank Details
        if (b != null && b.hasBankDetails) {
            val bText = "A/c Holder: ${b.acHolderName.ifBlank { "-" }}\nA/c No: ${b.acNo.ifBlank { "-" }}\nIFSC: ${b.ifscCode.ifBlank { "-" }} | Branch: ${b.branchName.ifBlank { "-" }}"
            holder.binding.tvBankDetailsContent.text = bText
        } else {
            holder.binding.tvBankDetailsContent.text = "-"
        }

        // Actions (Only One Preview Profile Button)
        holder.binding.btnCardPreview.setOnClickListener { onPreviewClicked(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<StudentWithDetails>() {
        override fun areItemsTheSame(oldItem: StudentWithDetails, newItem: StudentWithDetails): Boolean {
            return oldItem.student.id == newItem.student.id
        }

        override fun areContentsTheSame(oldItem: StudentWithDetails, newItem: StudentWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}

