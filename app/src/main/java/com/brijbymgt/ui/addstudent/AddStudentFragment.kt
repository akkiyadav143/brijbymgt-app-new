package com.brijbymgt.ui.addstudent

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.brijbymgt.R
import com.brijbymgt.SchoolApp
import com.brijbymgt.data.entity.BankDetailsEntity
import com.brijbymgt.data.entity.DocumentEntity
import com.brijbymgt.data.entity.HealthDetailsEntity
import com.brijbymgt.data.entity.HeightWeightHistoryEntity
import com.brijbymgt.data.entity.StudentEntity
import com.brijbymgt.data.model.StudentWithDetails
import com.brijbymgt.data.repository.StudentRepository
import com.brijbymgt.databinding.DialogStudentPreviewBinding
import com.brijbymgt.databinding.FragmentAddStudentBinding
import com.brijbymgt.util.DocumentStorageHelper
import com.brijbymgt.util.ImageStorageHelper
import com.brijbymgt.util.PdfPrintHelper
import com.brijbymgt.util.StudentPreviewHelper
import com.brijbymgt.util.ValidationUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddStudentFragment : Fragment() {

    private var _binding: FragmentAddStudentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddStudentViewModel by viewModels {
        val app = requireActivity().application as SchoolApp
        val db = app.database
        val repo = StudentRepository(
            db.studentDao(),
            db.healthDetailsDao(),
            db.heightWeightHistoryDao(),
            db.bankDetailsDao(),
            db.mdmReportDao(),
            db.documentDao()
        )
        AddStudentViewModelFactory(repo)
    }

    private var selectedImagePath: String? = null
    private var selectedFatherImagePath: String? = null
    private var selectedMotherImagePath: String? = null
    private var activePhotoPicker: String = "student"
    private var editingStudentId: Long = -1L

    private val heightWeightList = mutableListOf<HeightWeightHistoryEntity>()
    private lateinit var hwAdapter: HeightWeightAdapter

    private val studentDocuments = mutableMapOf<String, MutableList<DocumentEntity>>()
    private var activeAttachingDocType: String = ""
    private var isDocumentReplacement: Boolean = false
    private var replacementDocumentId: Long = -1L
    private var currentCameraPhotoPath: String? = null
    private var currentCameraPhotoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentCameraPhotoPath?.let { path ->
                lifecycleScope.launch {
                    val file = File(path)
                    if (file.exists()) {
                        val savedInfo = DocumentStorageHelper.saveDocumentToInternalStorage(
                            requireContext(),
                            editingStudentId.takeIf { it > 0 } ?: 0L,
                            activeAttachingDocType,
                            Uri.fromFile(file)
                        )
                        try { file.delete() } catch (e: Exception) {}

                        if (savedInfo != null) {
                            val docEntity = DocumentEntity(
                                id = if (isDocumentReplacement && replacementDocumentId > 0) replacementDocumentId else 0L,
                                studentId = editingStudentId.takeIf { it > 0 } ?: 0L,
                                documentType = activeAttachingDocType,
                                fileName = savedInfo.fileName,
                                fileUri = savedInfo.absolutePath,
                                mimeType = savedInfo.mimeType,
                                fileSize = savedInfo.fileSize
                            )
                            handleSavedDocument(activeAttachingDocType, docEntity, isDocumentReplacement, replacementDocumentId)
                        }
                    }
                }
            }
        }
    }

    private val pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { sourceUri ->
            lifecycleScope.launch {
                val savedInfo = DocumentStorageHelper.saveDocumentToInternalStorage(
                    requireContext(),
                    editingStudentId.takeIf { it > 0 } ?: 0L,
                    activeAttachingDocType,
                    sourceUri
                )
                if (savedInfo != null) {
                    val docEntity = DocumentEntity(
                        id = if (isDocumentReplacement && replacementDocumentId > 0) replacementDocumentId else 0L,
                        studentId = editingStudentId.takeIf { it > 0 } ?: 0L,
                        documentType = activeAttachingDocType,
                        fileName = savedInfo.fileName,
                        fileUri = savedInfo.absolutePath,
                        mimeType = savedInfo.mimeType,
                        fileSize = savedInfo.fileSize
                    )
                    handleSavedDocument(activeAttachingDocType, docEntity, isDocumentReplacement, replacementDocumentId)
                }
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                val savedPath = ImageStorageHelper.saveImageToInternalStorage(requireContext(), it)
                if (savedPath != null) {
                    when (activePhotoPicker) {
                        "father" -> {
                            selectedFatherImagePath = savedPath
                            val bitmap = ImageStorageHelper.loadBitmapFromPath(savedPath)
                            bitmap?.let { b -> binding.ivFatherImage.setImageBitmap(b) }
                        }
                        "mother" -> {
                            selectedMotherImagePath = savedPath
                            val bitmap = ImageStorageHelper.loadBitmapFromPath(savedPath)
                            bitmap?.let { b -> binding.ivMotherImage.setImageBitmap(b) }
                        }
                        else -> {
                            selectedImagePath = savedPath
                            val bitmap = ImageStorageHelper.loadBitmapFromPath(savedPath)
                            bitmap?.let { b -> binding.ivStudentImage.setImageBitmap(b) }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupDatePickers()
        setupPhotoPicker()
        setupHwHistoryRecyclerView()
        setupDocumentSection()
        setupFormActionButtons()
        observeViewModel()

        arguments?.let { args ->
            val id = args.getLong("studentId", -1L)
            if (id != -1L) {
                editingStudentId = id
                binding.tvFormTitle.text = "EDIT STUDENT PROFILE"
                viewModel.loadStudentForEdit(id)
            }
        }
    }

    private fun setupSpinner() {
        val classes = arrayOf(
            "Class 1", "Class 2", "Class 3", "Class 4",
            "Class 5", "Class 6", "Class 7", "Class 8", "Class 0", "CL Pass"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClass.adapter = adapter
        binding.spinnerClass.setSelection(0)
    }

    private fun setupDatePickers() {
        binding.etDob.setOnClickListener { showDatePickerDialog { date -> binding.etDob.setText(date) } }
        binding.etAdmissionDate.setOnClickListener { showDatePickerDialog { date -> binding.etAdmissionDate.setText(date) } }
    }

    private fun showDatePickerDialog(onDateSet: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val date = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                onDateSet(date)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupPhotoPicker() {
        binding.btnSelectPhoto.setOnClickListener {
            activePhotoPicker = "student"
            pickImageLauncher.launch("image/*")
        }
        binding.ivStudentImage.setOnClickListener {
            activePhotoPicker = "student"
            pickImageLauncher.launch("image/*")
        }
        binding.btnSelectFatherPhoto.setOnClickListener {
            activePhotoPicker = "father"
            pickImageLauncher.launch("image/*")
        }
        binding.ivFatherImage.setOnClickListener {
            activePhotoPicker = "father"
            pickImageLauncher.launch("image/*")
        }
        binding.btnSelectMotherPhoto.setOnClickListener {
            activePhotoPicker = "mother"
            pickImageLauncher.launch("image/*")
        }
        binding.ivMotherImage.setOnClickListener {
            activePhotoPicker = "mother"
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupHwHistoryRecyclerView() {
        hwAdapter = HeightWeightAdapter(heightWeightList) { position ->
            if (position in heightWeightList.indices) {
                heightWeightList.removeAt(position)
                hwAdapter.notifyItemRemoved(position)
                hwAdapter.notifyItemRangeChanged(position, heightWeightList.size)
            }
        }
        binding.rvHwHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHwHistory.adapter = hwAdapter

        binding.btnAddMoreHwRow.setOnClickListener {
            addHwHistoryRow()
        }
    }

    private fun addHwHistoryRow() {
        val nextPeriod = (heightWeightList.size + 1).toString()
        heightWeightList.add(HeightWeightHistoryEntity(studentId = 0, datePeriod = nextPeriod, height = "", weight = ""))
        hwAdapter.notifyItemInserted(heightWeightList.size - 1)
    }

    private fun setupDocumentSection() {
        for (docType in DocumentStorageHelper.SUPPORTED_DOCUMENT_TYPES) {
            if (!studentDocuments.containsKey(docType)) {
                studentDocuments[docType] = mutableListOf()
            }
        }
        renderDocumentsUI()
    }

    private fun showDocumentSourcePicker(docType: String, isReplacement: Boolean, docId: Long) {
        activeAttachingDocType = docType
        isDocumentReplacement = isReplacement
        replacementDocumentId = docId

        val options = arrayOf("Take Photo", "Choose Image", "Choose PDF / File")
        AlertDialog.Builder(requireContext())
            .setTitle("Attach $docType")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCameraForDocument()
                    1 -> pickDocumentLauncher.launch(arrayOf("image/*"))
                    2 -> pickDocumentLauncher.launch(arrayOf("application/pdf", "image/*", "*/*"))
                }
            }
            .show()
    }

    private fun launchCameraForDocument() {
        val photoFile = File(requireContext().cacheDir, "doc_cam_${System.currentTimeMillis()}.jpg")
        currentCameraPhotoPath = photoFile.absolutePath
        currentCameraPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        currentCameraPhotoUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    private fun handleSavedDocument(docType: String, newDoc: DocumentEntity, isReplacement: Boolean, replaceId: Long) {
        val list = studentDocuments.getOrPut(docType) { mutableListOf() }
        if (isReplacement && replaceId > 0) {
            val index = list.indexOfFirst { it.id == replaceId }
            if (index != -1) {
                val oldDoc = list[index]
                if (oldDoc.fileUri != newDoc.fileUri) {
                    DocumentStorageHelper.deletePhysicalFile(oldDoc.fileUri)
                }
                list[index] = newDoc.copy(id = oldDoc.id)
            } else {
                list.add(newDoc)
            }
        } else {
            list.add(newDoc)
        }
        renderDocumentsUI()
    }

    private fun renderDocumentsUI() {
        binding.llDocumentsContainer.removeAllViews()
        val context = requireContext()
        val density = context.resources.displayMetrics.density

        for (docType in DocumentStorageHelper.SUPPORTED_DOCUMENT_TYPES) {
            val docsForType = studentDocuments[docType] ?: mutableListOf()
            val isAttached = docsForType.isNotEmpty()

            val cardLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 10) }
                setPadding(12, 12, 12, 12)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    setStroke((1 * density).toInt(), android.graphics.Color.parseColor("#E0E0E0"))
                    cornerRadius = 8f * density
                }
            }

            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvTitle = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "📄 $docType"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#1E293B"))
            }
            headerRow.addView(tvTitle)

            val tvStatus = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 0, 8, 0) }
                text = if (isAttached) "🟢 Attached" else "⚪ Not Attached"
                textSize = 11f
                setTextColor(android.graphics.Color.parseColor(if (isAttached) "#2E7D32" else "#757575"))
            }
            headerRow.addView(tvStatus)

            val btnAttach = Button(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (36 * density).toInt()
                )
                text = if (isAttached) "+ Add More" else "+ Attach Document"
                textSize = 11f
                setPadding(12, 0, 12, 0)
                setOnClickListener {
                    showDocumentSourcePicker(docType, false, -1L)
                }
            }
            headerRow.addView(btnAttach)

            cardLayout.addView(headerRow)

            if (isAttached) {
                val filesContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 8, 0, 0) }
                }

                for ((index, doc) in docsForType.withIndex()) {
                    val fileRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 4, 0, 4) }
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(8, 4, 8, 4)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#F8FAFC"))
                            setStroke((1 * density).toInt(), android.graphics.Color.parseColor("#E2E8F0"))
                            cornerRadius = 4f * density
                        }
                    }

                    val tvFileInfo = TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        text = "${doc.fileName} (${DocumentStorageHelper.formatFileSize(doc.fileSize)})"
                        textSize = 11f
                        setTextColor(android.graphics.Color.parseColor("#334155"))
                    }
                    fileRow.addView(tvFileInfo)

                    val btnPreview = Button(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (30 * density).toInt()
                        ).apply { setMargins(4, 0, 4, 0) }
                        text = "Preview"
                        textSize = 10f
                        setPadding(6, 0, 6, 0)
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
                    fileRow.addView(btnPreview)

                    val btnReplace = Button(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (30 * density).toInt()
                        ).apply { setMargins(4, 0, 4, 0) }
                        text = "Replace"
                        textSize = 10f
                        setPadding(6, 0, 6, 0)
                        setOnClickListener {
                            showDocumentSourcePicker(docType, true, doc.id)
                        }
                    }
                    fileRow.addView(btnReplace)

                    val btnDelete = Button(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (30 * density).toInt()
                        ).apply { setMargins(4, 0, 0, 0) }
                        text = "Delete"
                        textSize = 10f
                        setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                        setPadding(6, 0, 6, 0)
                        setOnClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("Delete Document")
                                .setMessage("Are you sure you want to delete ${doc.fileName}?")
                                .setPositiveButton("Delete") { _, _ ->
                                    DocumentStorageHelper.deletePhysicalFile(doc.fileUri)
                                    docsForType.removeAt(index)
                                    renderDocumentsUI()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                    fileRow.addView(btnDelete)

                    filesContainer.addView(fileRow)
                }

                cardLayout.addView(filesContainer)
            }

            binding.llDocumentsContainer.addView(cardLayout)
        }
    }

    private fun setupFormActionButtons() {
        binding.btnFormPreview.setOnClickListener {
            if (validateForm()) {
                val studentWithDetails = collectFormStudentWithDetails()
                showPreviewDialog(studentWithDetails)
            } else {
                Toast.makeText(requireContext(), "Please fix mandatory and format validation errors", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFormSubmit.setOnClickListener {
            if (validateForm()) {
                val studentWithDetails = collectFormStudentWithDetails()
                viewModel.saveStudent(
                    studentWithDetails.student,
                    studentWithDetails.healthDetails,
                    studentWithDetails.heightWeightHistory,
                    studentWithDetails.bankDetails,
                    studentWithDetails.documents
                )
            } else {
                Toast.makeText(requireContext(), "Validation failed. Please check errors.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        binding.tilStudentName.error = null
        binding.tilStudentAdhar.error = null
        binding.tilFatherAdhar.error = null
        binding.tilMotherAdhar.error = null
        binding.tilMobileNo.error = null
        binding.tilIfscCode.error = null
        binding.tilAcNo.error = null

        val nameRes = ValidationUtils.validateName(binding.etStudentName.text.toString())
        if (!nameRes.isValid) {
            binding.tilStudentName.error = nameRes.errorMessage
            isValid = false
        }

        if (binding.rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Please select Gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        val adharRes = ValidationUtils.validateAdhar(binding.etStudentAdhar.text.toString(), "Student Adhar")
        if (!adharRes.isValid) {
            binding.tilStudentAdhar.error = adharRes.errorMessage
            isValid = false
        }

        val fAdharRes = ValidationUtils.validateAdhar(binding.etFatherAdhar.text.toString(), "Father Adhar")
        if (!fAdharRes.isValid) {
            binding.tilFatherAdhar.error = fAdharRes.errorMessage
            isValid = false
        }

        val mAdharRes = ValidationUtils.validateAdhar(binding.etMotherAdhar.text.toString(), "Mother Adhar")
        if (!mAdharRes.isValid) {
            binding.tilMotherAdhar.error = mAdharRes.errorMessage
            isValid = false
        }

        val mobileRes = ValidationUtils.validateMobile(binding.etMobileNo.text.toString())
        if (!mobileRes.isValid) {
            binding.tilMobileNo.error = mobileRes.errorMessage
            isValid = false
        }

        val ifscStr = binding.etIfscCode.text.toString().trim()
        if (ifscStr.isNotBlank()) {
            val ifscRes = ValidationUtils.validateIfsc(ifscStr)
            if (!ifscRes.isValid) {
                binding.tilIfscCode.error = ifscRes.errorMessage
                isValid = false
            }
        }

        val acNoStr = binding.etAcNo.text.toString().trim()
        if (acNoStr.isNotBlank()) {
            val acRes = ValidationUtils.validateAccountNo(acNoStr)
            if (!acRes.isValid) {
                binding.tilAcNo.error = acRes.errorMessage
                isValid = false
            }
        }

        return isValid
    }

    private fun collectFormStudentWithDetails(): StudentWithDetails {
        val selectedGender = when (binding.rgGender.checkedRadioButtonId) {
            binding.rbFemale.id -> "Female"
            binding.rbMale.id -> "Male"
            else -> ""
        }
        val selectedDbt = if (binding.rbDbtYes.isChecked) "Yes" else "No"
        val selectedClass = binding.spinnerClass.selectedItem.toString()

        val selectedCaste = when (binding.rgCaste.checkedRadioButtonId) {
            binding.rbObc.id -> "OBC"
            binding.rbSc.id -> "SC"
            binding.rbSt.id -> "ST"
            binding.rbMinority.id -> "MINORITY"
            binding.rbGen.id -> "GEN"
            else -> ""
        }

        val student = StudentEntity(
            id = if (editingStudentId > 0) editingStudentId else 0,
            srNo = binding.etSrNo.text.toString().trim(),
            srNumber = binding.etSrNumber.text.toString().trim(),
            uniqueId = binding.etUniqueId.text.toString().trim(),
            penNo = binding.etPenNo.text.toString().trim(),
            aaparId = binding.etAaparId.text.toString().trim(),
            dbt = selectedDbt,
            studentClass = selectedClass,
            studentName = binding.etStudentName.text.toString().trim(),
            dob = binding.etDob.text.toString().trim(),
            admissionDate = binding.etAdmissionDate.text.toString().trim(),
            studentAdhar = binding.etStudentAdhar.text.toString().trim(),
            co = binding.etCo.text.toString().trim(),
            gender = selectedGender,
            caste = selectedCaste,
            fatherName = binding.etFatherName.text.toString().trim(),
            fatherAdhar = binding.etFatherAdhar.text.toString().trim(),
            motherName = binding.etMotherName.text.toString().trim(),
            motherAdhar = binding.etMotherAdhar.text.toString().trim(),
            mobileNo = binding.etMobileNo.text.toString().trim(),
            address = binding.etAddress.text.toString().trim(),
            other = binding.etOther.text.toString().trim(),
            rationCard = binding.etRationCard.text.toString().trim(),
            imagePath = selectedImagePath,
            fatherImagePath = selectedFatherImagePath,
            motherImagePath = selectedMotherImagePath
        )

        val heightStr = binding.etHeight.text.toString().trim()
        val weightStr = binding.etWeight.text.toString().trim()
        val bgStr = binding.etBloodGroup.text.toString().trim()
        val hbStr = binding.etHb.text.toString().trim()
        val eyeStr = binding.etEyeSight.text.toString().trim()
        val dewormStr = binding.etDeworming.text.toString().trim()

        val hasHealth = heightStr.isNotBlank() || weightStr.isNotBlank() || bgStr.isNotBlank() ||
                hbStr.isNotBlank() || eyeStr.isNotBlank() || dewormStr.isNotBlank()

        val healthDetails = if (hasHealth) {
            HealthDetailsEntity(
                studentId = student.id,
                hasHealthDetails = true,
                height = heightStr,
                weight = weightStr,
                bloodGroup = bgStr,
                hb = hbStr,
                eyeSight = eyeStr,
                deworming = dewormStr
            )
        } else null

        val historyList = if (hasHealth) heightWeightList.toList() else emptyList()

        val acHolder = binding.etAcHolderName.text.toString().trim()
        val acNo = binding.etAcNo.text.toString().trim()
        val ifsc = binding.etIfscCode.text.toString().trim()
        val branch = binding.etBranchName.text.toString().trim()

        val hasBank = acHolder.isNotBlank() || acNo.isNotBlank() || ifsc.isNotBlank() || branch.isNotBlank()

        val bankDetails = if (hasBank) {
            BankDetailsEntity(
                studentId = student.id,
                hasBankDetails = true,
                acHolderName = acHolder,
                acNo = acNo,
                ifscCode = ifsc,
                branchName = branch
            )
        } else null

        val allDocuments = mutableListOf<DocumentEntity>()
        for ((_, docs) in studentDocuments) {
            allDocuments.addAll(docs)
        }

        return StudentWithDetails(student, healthDetails, historyList, bankDetails, allDocuments)
    }

    private fun showPreviewDialog(item: StudentWithDetails) {
        val dialogBinding = DialogStudentPreviewBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val s = item.student
        StudentPreviewHelper.bindStudentPreviewDialog(dialogBinding, item, viewLifecycleOwner)

        dialogBinding.btnPreviewEdit.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnPreviewPrint.setOnClickListener {
            lifecycleScope.launch {
                val tempFile = File(getCacheProcessDir(), "preview_student.pdf")
                val stream = tempFile.outputStream()
                val success = PdfPrintHelper.generateStudentProfilePdf(requireContext(), item, stream)
                stream.close()
                if (success) {
                    PdfPrintHelper.printPdfFile(requireContext(), tempFile, "Print Student ${s.studentName}")
                }
            }
        }

        dialogBinding.btnPreviewClose.setOnClickListener {
            viewModel.saveStudent(
                item.student,
                item.healthDetails,
                item.heightWeightHistory,
                item.bankDetails,
                item.documents
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSavedStudentPreviewDialog(item: StudentWithDetails) {
        val dialogBinding = DialogStudentPreviewBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val s = item.student
        StudentPreviewHelper.bindStudentPreviewDialog(dialogBinding, item, viewLifecycleOwner)

        dialogBinding.btnPreviewEdit.text = "View in List"
        dialogBinding.btnPreviewEdit.setOnClickListener {
            dialog.dismiss()
            try {
                val bundle = Bundle().apply {
                    putLong("studentId", item.student.id)
                }
                findNavController().navigate(R.id.nav_student_list, bundle)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dialogBinding.btnPreviewPrint.setOnClickListener {
            lifecycleScope.launch {
                val tempFile = File(getCacheProcessDir(), "preview_student.pdf")
                val stream = tempFile.outputStream()
                val success = PdfPrintHelper.generateStudentProfilePdf(requireContext(), item, stream)
                stream.close()
                if (success) {
                    PdfPrintHelper.printPdfFile(requireContext(), tempFile, "Print Student ${s.studentName}")
                }
            }
        }

        dialogBinding.btnPreviewClose.text = "Add New"
        dialogBinding.btnPreviewClose.setOnClickListener {
            dialog.dismiss()
            clearForm()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savedStudentWithDetails.collect { studentWithDetails ->
                studentWithDetails?.let { item ->
                    Toast.makeText(requireContext(), "Student Record Saved Successfully!", Toast.LENGTH_SHORT).show()
                    showSavedStudentPreviewDialog(item)
                    clearForm()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveResult.collect { success ->
                if (!success) {
                    Toast.makeText(requireContext(), "Failed to save student record", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadedStudent.collect { studentWithDetails ->
                studentWithDetails?.let { populateFormForEdit(it) }
            }
        }
    }

    private fun populateFormForEdit(item: StudentWithDetails) {
        val s = item.student
        val h = item.healthDetails
        val b = item.bankDetails

        binding.etSrNo.setText(s.srNo)
        binding.etSrNumber.setText(s.srNumber)
        binding.etUniqueId.setText(s.uniqueId)
        binding.etPenNo.setText(s.penNo)
        binding.etAaparId.setText(s.aaparId)

        if (s.dbt.equals("Yes", true)) binding.rbDbtYes.isChecked = true else binding.rbDbtNo.isChecked = true

        val classes = arrayOf(
            "Class 1", "Class 2", "Class 3", "Class 4",
            "Class 5", "Class 6", "Class 7", "Class 8", "Class 0", "CL Pass"
        )
        val formattedClass = formatClassString(s.studentClass)
        val pos = classes.indexOf(formattedClass).let { if (it >= 0) it else 0 }
        binding.spinnerClass.setSelection(pos)

        binding.etStudentName.setText(s.studentName)
        binding.etDob.setText(s.dob)
        binding.etAdmissionDate.setText(s.admissionDate)
        binding.etStudentAdhar.setText(s.studentAdhar)
        binding.etCo.setText(s.co)

        if (s.gender.equals("Female", true)) binding.rbFemale.isChecked = true else if (s.gender.equals("Male", true)) binding.rbMale.isChecked = true else binding.rgGender.clearCheck()

        when (s.caste.uppercase()) {
            "OBC" -> binding.rbObc.isChecked = true
            "SC" -> binding.rbSc.isChecked = true
            "ST" -> binding.rbSt.isChecked = true
            "MINORITY" -> binding.rbMinority.isChecked = true
            "GEN" -> binding.rbGen.isChecked = true
            else -> binding.rgCaste.clearCheck()
        }

        binding.etFatherName.setText(s.fatherName)
        binding.etFatherAdhar.setText(s.fatherAdhar)
        binding.etMotherName.setText(s.motherName)
        binding.etMotherAdhar.setText(s.motherAdhar)
        binding.etMobileNo.setText(s.mobileNo)
        binding.etAddress.setText(s.address)
        binding.etOther.setText(s.other)
        binding.etRationCard.setText(s.rationCard)

        selectedImagePath = s.imagePath
        if (!selectedImagePath.isNullOrBlank()) {
            lifecycleScope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(selectedImagePath!!)
                bitmap?.let { binding.ivStudentImage.setImageBitmap(it) }
            }
        }

        selectedFatherImagePath = s.fatherImagePath
        if (!selectedFatherImagePath.isNullOrBlank()) {
            lifecycleScope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(selectedFatherImagePath!!)
                bitmap?.let { binding.ivFatherImage.setImageBitmap(it) }
            }
        }

        selectedMotherImagePath = s.motherImagePath
        if (!selectedMotherImagePath.isNullOrBlank()) {
            lifecycleScope.launch {
                val bitmap = ImageStorageHelper.loadBitmapFromPath(selectedMotherImagePath!!)
                bitmap?.let { binding.ivMotherImage.setImageBitmap(it) }
            }
        }

        // Health
        if (h != null && h.hasHealthDetails) {
            binding.etHeight.setText(h.height)
            binding.etWeight.setText(h.weight)
            binding.etBloodGroup.setText(h.bloodGroup)
            binding.etHb.setText(h.hb)
            binding.etEyeSight.setText(h.eyeSight)
            binding.etDeworming.setText(h.deworming)
        }

        // Height/Weight History
        if (item.heightWeightHistory.isNotEmpty()) {
            heightWeightList.clear()
            heightWeightList.addAll(item.heightWeightHistory)
            hwAdapter.notifyDataSetChanged()
        }

        // Bank
        if (b != null && b.hasBankDetails) {
            binding.etAcHolderName.setText(b.acHolderName)
            binding.etAcNo.setText(b.acNo)
            binding.etIfscCode.setText(b.ifscCode)
            binding.etBranchName.setText(b.branchName)
        }

        // Documents
        studentDocuments.clear()
        for (docType in DocumentStorageHelper.SUPPORTED_DOCUMENT_TYPES) {
            studentDocuments[docType] = mutableListOf()
        }
        for (doc in item.documents) {
            studentDocuments.getOrPut(doc.documentType) { mutableListOf() }.add(doc)
        }
        renderDocumentsUI()
    }

    private fun clearForm() {
        editingStudentId = -1L
        binding.tvFormTitle.text = "STUDENT REGISTRATION FORM"
        binding.etSrNo.setText("")
        binding.etSrNumber.setText("")
        binding.etUniqueId.setText("")
        binding.etPenNo.setText("")
        binding.etAaparId.setText("")
        binding.etStudentName.setText("")
        binding.etDob.setText("")
        binding.etAdmissionDate.setText("")
        binding.etStudentAdhar.setText("")
        binding.etCo.setText("")
        binding.rgGender.clearCheck()
        binding.rgCaste.clearCheck()
        binding.etFatherName.setText("")
        binding.etFatherAdhar.setText("")
        binding.etMotherName.setText("")
        binding.etMotherAdhar.setText("")
        binding.etMobileNo.setText("")
        binding.etAddress.setText("")
        binding.etOther.setText("")
        binding.etRationCard.setText("")
        binding.etHeight.setText("")
        binding.etWeight.setText("")
        binding.etBloodGroup.setText("")
        binding.etHb.setText("")
        binding.etEyeSight.setText("")
        binding.etDeworming.setText("")
        binding.etAcHolderName.setText("")
        binding.etAcNo.setText("")
        binding.etIfscCode.setText("")
        binding.etBranchName.setText("")
        selectedImagePath = null
        selectedFatherImagePath = null
        selectedMotherImagePath = null
        binding.ivStudentImage.setImageResource(0)
        binding.ivFatherImage.setImageResource(0)
        binding.ivMotherImage.setImageResource(0)
        heightWeightList.clear()
        hwAdapter.notifyDataSetChanged()

        for (docType in DocumentStorageHelper.SUPPORTED_DOCUMENT_TYPES) {
            studentDocuments[docType]?.clear()
        }
        renderDocumentsUI()
    }

    private fun formatClassString(input: String): String {
        val trimmed = input.trim()
        if (trimmed.equals("Class 0", true) || trimmed.equals("CL 0", true) || trimmed == "0") return "Class 0"
        if (trimmed.equals("CL Pass", true) || trimmed.equals("Class Pass", true) || trimmed.equals("Pass", true)) return "CL Pass"
        for (i in 1..8) {
            if (trimmed.equals("Class $i", true) || trimmed.equals("CL $i", true) || trimmed == "$i") {
                return "Class $i"
            }
        }
        return input.ifBlank { "Class 1" }
    }

    private fun getCacheProcessDir(): File {
        val dir = File(requireContext().cacheDir, "pdf_previews")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

