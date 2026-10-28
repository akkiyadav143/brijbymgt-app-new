package com.brijby77.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijby77.R
import com.brijby77.databinding.FragmentRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository
    private var num1 = 0
    private var num2 = 0
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())
        setupCaptcha()

        binding.btnRegister.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val name = binding.etRegisterName.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val mobile = binding.etRegisterMobile.text.toString().trim()
            val captchaAns = binding.etCaptchaAnswer.text.toString().trim()

            val validationError = authRepository.validateRegistration(name, email, mobile)
            if (validationError != null) {
                Toast.makeText(requireContext(), validationError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (captchaAns.toIntOrNull() != (num1 + num2)) {
                Toast.makeText(requireContext(), "Incorrect CAPTCHA answer", Toast.LENGTH_SHORT).show()
                setupCaptcha()
                return@setOnClickListener
            }

            if (!binding.cbTerms.isChecked || !binding.cbPrivacy.isChecked) {
                Toast.makeText(requireContext(), "Please accept Terms & Conditions and Privacy Policy", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (authRepository.isEmailRegistered(email)) {
                Toast.makeText(requireContext(), "Email already registered. Please login.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                return@setOnClickListener
            }

            isLoading = true
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Processing..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    authRepository.saveUserDetailsToDatabaseThenRequestOtp(name, email, mobile)
                }
                isLoading = false
                if (_binding != null) {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Register & Send OTP"
                }

                when (result) {
                    is AuthRepository.ApiResult.Success -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        val bundle = Bundle().apply {
                            putString("email", email)
                            putBoolean("is_register", true)
                        }
                        findNavController().navigate(R.id.action_registerFragment_to_otpFragment, bundle)
                    }
                    is AuthRepository.ApiResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupCaptcha() {
        num1 = (1..9).random()
        num2 = (1..9).random()
        binding.tvCaptchaQuestion.text = "Solve: $num1 + $num2 = ?"
        binding.etCaptchaAnswer.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
