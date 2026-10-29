package com.brijbymgt.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.brijbymgt.R
import com.brijbymgt.databinding.FragmentProfileBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())

        loadUserProfile()

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.tvProfilePrivacy.setOnClickListener {
            try {
                findNavController().navigate(R.id.privacyPolicyFragment)
            } catch (e: Exception) {
                try {
                    findNavController().navigate(R.id.action_profile_to_privacyPolicy)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        binding.tvProfileTerms.setOnClickListener {
            try {
                findNavController().navigate(R.id.termsFragment)
            } catch (e: Exception) {
                try {
                    findNavController().navigate(R.id.action_profile_to_terms)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    private fun loadUserProfile() {
        val name = authRepository.getUserName() ?: "School Admin"
        val email = authRepository.getUserEmail() ?: "admin@school.com"
        val mobile = authRepository.getUserMobile() ?: "9999999999"
        val uid = authRepository.getUserUid() ?: "UID_DEFAULT"
        val regDate = authRepository.getRegistrationDate(email)

        binding.tvProfileNameHeader.text = name
        binding.tvProfileEmailHeader.text = email
        binding.tvProfileMobile.text = mobile
        binding.tvProfileUid.text = uid
        binding.tvProfileRegDate.text = regDate
        binding.tvAccountStatus.text = "Status: Active"
    }

    private fun showEditProfileDialog() {
        val context = requireContext()
        val currentName = authRepository.getUserName() ?: ""
        val currentMobile = authRepository.getUserMobile() ?: ""

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val nameInputLayout = TextInputLayout(context).apply {
            hint = "Full Name"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }
        }
        val nameEditText = TextInputEditText(context).apply {
            setText(currentName)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            maxLines = 1
        }
        nameInputLayout.addView(nameEditText)
        layout.addView(nameInputLayout)

        val mobileInputLayout = TextInputLayout(context).apply {
            hint = "Mobile Number"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val mobileEditText = TextInputEditText(context).apply {
            setText(currentMobile)
            inputType = InputType.TYPE_CLASS_PHONE
            maxLines = 1
        }
        mobileInputLayout.addView(mobileEditText)
        layout.addView(mobileInputLayout)

        AlertDialog.Builder(context)
            .setTitle("Edit Profile")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameEditText.text.toString().trim()
                val newMobile = mobileEditText.text.toString().trim()
                val email = authRepository.getUserEmail() ?: ""

                if (newName.isBlank()) {
                    Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newMobile.length < 10) {
                    Toast.makeText(context, "Invalid mobile number (minimum 10 digits)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                authRepository.updateProfile(email, newName, newMobile)
                loadUserProfile()
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from your account?")
            .setPositiveButton("Logout") { _, _ ->
                authRepository.logout()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        val context = requireContext()
        val email = authRepository.getUserEmail() ?: ""

        AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Warning: Deleting your account is permanent and will erase all your session and registered user data. Do you wish to proceed?")
            .setPositiveButton("Proceed") { _, _ ->
                showReVerificationDialog(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReVerificationDialog(email: String) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val infoTv = TextView(context).apply {
            text = "To re-verify account ownership, please enter your registered email address:\n$email"
            textSize = 14f
            setTextColor(Color.parseColor("#495057"))
            setPadding(0, 0, 0, 24)
        }
        layout.addView(infoTv)

        val emailInputLayout = TextInputLayout(context).apply {
            hint = "Confirm Email Address"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val emailEditText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            maxLines = 1
        }
        emailInputLayout.addView(emailEditText)
        layout.addView(emailInputLayout)

        AlertDialog.Builder(context)
            .setTitle("Re-verification Required")
            .setView(layout)
            .setPositiveButton("Delete Account") { _, _ ->
                val enteredEmail = emailEditText.text.toString().trim()
                if (enteredEmail.equals(email, ignoreCase = true)) {
                    authRepository.deleteAccount(email)
                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_LONG).show()
                    val intent = Intent(context, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(context, "Email verification failed. Account deletion cancelled.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
