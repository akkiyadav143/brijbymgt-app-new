package com.brijbymgt.util

import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.databinding.DialogStudentPreviewBinding
import kotlinx.coroutines.launch

object StudentPreviewHelper {

    fun bindStudentPreviewDialog(
        binding: DialogStudentPreviewBinding,
        item: StudentWithDetails,
        lifecycleOwner: LifecycleOwner
    ) {
        val s = item.student
        val h = item.healthDetails
        val b = item.bankDetails

        // Student Photo
        if (!s.imagePath.isNullOrBlank()) {
            lifecycleOwner.lifecycleScope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(s.imagePath)
                bitmap?.let { binding.ivPreviewPhoto.setImageBitmap(it) }
            }
            binding.tvStudentPhotoBadge.visibility = View.VISIBLE
        } else {
            binding.tvStudentPhotoBadge.visibility = View.GONE
        }

        // Basic Info
        binding.tvPreviewClass.text = s.studentClass
        binding.tvPreviewSrNumber.text = s.srNumber.ifBlank { "-" }
        binding.tvPreviewUniqueId.text = s.uniqueId.ifBlank { "-" }
        binding.tvPreviewPenNo.text = s.penNo.ifBlank { "-" }
        binding.tvPreviewAaparId.text = s.aaparId.ifBlank { "-" }
        binding.tvPreviewStudentName.text = s.studentName.ifBlank { "-" }
        binding.tvPreviewDob.text = s.dob.ifBlank { "-" }
        binding.tvPreviewAdmissionDate.text = s.admissionDate.ifBlank { "-" }

        binding.tvPreviewStudentAdhar.text = s.studentAdhar.ifBlank { "-" }
        binding.tvPreviewCaste.text = s.caste.ifBlank { "-" }
        binding.tvPreviewCo.text = s.co.ifBlank { "-" }
        binding.tvPreviewDbt.text = s.dbt
        binding.tvPreviewGender.text = s.gender.ifBlank { "-" }

        // Father Photo & Info
        if (!s.fatherImagePath.isNullOrBlank()) {
            lifecycleOwner.lifecycleScope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(s.fatherImagePath)
                bitmap?.let { binding.ivPreviewFatherPhoto.setImageBitmap(it) }
            }
            binding.tvFatherPhotoBadge.visibility = View.VISIBLE
        } else {
            binding.tvFatherPhotoBadge.visibility = View.GONE
        }
        binding.tvPreviewFatherName.text = s.fatherName.ifBlank { "-" }
        binding.tvPreviewFatherAdhar.text = s.fatherAdhar.ifBlank { "-" }

        // Mother Photo & Info
        if (!s.motherImagePath.isNullOrBlank()) {
            lifecycleOwner.lifecycleScope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(s.motherImagePath)
                bitmap?.let { binding.ivPreviewMotherPhoto.setImageBitmap(it) }
            }
            binding.tvMotherPhotoBadge.visibility = View.VISIBLE
        } else {
            binding.tvMotherPhotoBadge.visibility = View.GONE
        }
        binding.tvPreviewMotherName.text = s.motherName.ifBlank { "-" }
        binding.tvPreviewMotherAdhar.text = s.motherAdhar.ifBlank { "-" }

        // Health Details
        if (h != null && h.hasHealthDetails) {
            binding.tvHealthIncluded.text = "Included : Yes"
            binding.tvHealthIncluded.setBackgroundColor(Color.parseColor("#2E7D32"))
            binding.tvPreviewHeight.text = if (h.height.isNotBlank()) "${h.height} cm" else "-"
            binding.tvPreviewWeight.text = if (h.weight.isNotBlank()) "${h.weight} kg" else "-"
            binding.tvPreviewBloodGroup.text = h.bloodGroup.ifBlank { "-" }
            binding.tvPreviewHb.text = if (h.hb.isNotBlank()) "${h.hb} g/dL" else "-"
            binding.tvPreviewEyeSight.text = h.eyeSight.ifBlank { "-" }
            binding.tvPreviewDeworming.text = h.deworming.ifBlank { "-" }
        } else {
            binding.tvHealthIncluded.text = "Included : No"
            binding.tvHealthIncluded.setBackgroundColor(Color.parseColor("#757575"))
            binding.tvPreviewHeight.text = "-"
            binding.tvPreviewWeight.text = "-"
            binding.tvPreviewBloodGroup.text = "-"
            binding.tvPreviewHb.text = "-"
            binding.tvPreviewEyeSight.text = "-"
            binding.tvPreviewDeworming.text = "-"
        }

        // Bank Details
        if (b != null && b.hasBankDetails) {
            binding.tvBankIncluded.text = "Included : Yes"
            binding.tvBankIncluded.setBackgroundColor(Color.parseColor("#2E7D32"))
            binding.tvPreviewAcHolderName.text = b.acHolderName.ifBlank { "-" }
            binding.tvPreviewAcNo.text = b.acNo.ifBlank { "-" }
            binding.tvPreviewIfsc.text = b.ifscCode.ifBlank { "-" }
            binding.tvPreviewBranch.text = b.branchName.ifBlank { "-" }
        } else {
            binding.tvBankIncluded.text = "Included : No"
            binding.tvBankIncluded.setBackgroundColor(Color.parseColor("#757575"))
            binding.tvPreviewAcHolderName.text = "-"
            binding.tvPreviewAcNo.text = "-"
            binding.tvPreviewIfsc.text = "-"
            binding.tvPreviewBranch.text = "-"
        }

        // Documents Preview
        binding.llPreviewDocumentsContainer.removeAllViews()
        val context = binding.root.context
        if (item.documents.isNotEmpty()) {
            for (doc in item.documents) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 4, 0, 4) }
                    setPadding(4, 4, 4, 4)
                }

                val tvName = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = "${doc.documentType}: ${doc.fileName} (${DocumentStorageHelper.formatFileSize(doc.fileSize)})"
                    textSize = 11f
                    setTextColor(Color.parseColor("#0F172A"))
                }
                row.addView(tvName)

                val btnPreview = Button(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (32 * context.resources.displayMetrics.density).toInt()
                    )
                    text = "Preview"
                    textSize = 10f
                    setPadding(8, 0, 8, 0)
                    setOnClickListener {
                        val fileUri = DocumentStorageHelper.getFileProviderUri(context, doc.fileUri)
                        if (fileUri != null) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(fileUri, doc.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to open this document", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Document file not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                row.addView(btnPreview)

                binding.llPreviewDocumentsContainer.addView(row)
            }
        } else {
            val tvNone = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "No documents attached."
                textSize = 11f
                setTextColor(Color.parseColor("#64748B"))
            }
            binding.llPreviewDocumentsContainer.addView(tvNone)
        }

        // Address & Remarks
        binding.tvPreviewAddress.text = s.address.ifBlank { "-" }
        binding.tvPreviewOther.text = s.other.ifBlank { "No additional remarks." }
    }
}

