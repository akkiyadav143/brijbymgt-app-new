package com.brijby77.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.brijby77.MainActivity
import com.brijby77.databinding.FragmentOtpBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository
    private var email: String = ""
    private var isRegister: Boolean = false
    private var countDownTimer: CountDownTimer? = null
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())

        email = arguments?.getString("email") ?: ""
        isRegister = arguments?.getBoolean("is_register", false) ?: false

        binding.tvOtpSubtitle.text = "Enter the 6-digit verification code sent to:\n$email"

        startResendTimer()

        binding.btnVerifyAndLogin.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val otp = binding.etOtpInput.text.toString().trim()
            if (otp.length != 6) {
                binding.etOtpInput.error = "Enter valid 6-digit OTP"
                return@setOnClickListener
            }

            isLoading = true
            binding.btnVerifyAndLogin.isEnabled = false
            binding.btnVerifyAndLogin.text = "Verifying..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    authRepository.verifyOtpApi(email, otp)
                }
                isLoading = false
                if (_binding != null) {
                    binding.btnVerifyAndLogin.isEnabled = true
                    binding.btnVerifyAndLogin.text = "Verify & Login"
                }

                when (result) {
                    is AuthRepository.ApiResult.Success -> {
                        val userData = authRepository.getRegisteredUserData(email)
                        val uid = userData?.first ?: authRepository.generateUniqueUid()
                        val name = userData?.second ?: (if (isRegister) "New User" else "School Admin")
                        val mobile = userData?.third ?: "9999999999"

                        authRepository.saveSession(uid, email, name, mobile)
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is AuthRepository.ApiResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.btnResendOtp.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val userData = authRepository.getRegisteredUserData(email)
            val name = userData?.second ?: "School Admin"
            val mobile = userData?.third ?: "9999999999"

            isLoading = true
            binding.btnResendOtp.isEnabled = false
            binding.btnResendOtp.text = "Resending..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    authRepository.saveUserDetailsToDatabaseThenRequestOtp(name, email, mobile)
                }
                isLoading = false

                when (result) {
                    is AuthRepository.ApiResult.Success -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        startResendTimer()
                    }
                    is AuthRepository.ApiResult.Error -> {
                        if (_binding != null) {
                            binding.btnResendOtp.isEnabled = true
                            binding.btnResendOtp.text = "Resend OTP"
                        }
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun startResendTimer() {
        binding.btnResendOtp.isEnabled = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding != null) {
                    binding.btnResendOtp.text = "Resend OTP in ${millisUntilFinished / 1000}s"
                }
            }

            override fun onFinish() {
                if (_binding != null) {
                    binding.btnResendOtp.isEnabled = true
                    binding.btnResendOtp.text = "Resend OTP"
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }
}
