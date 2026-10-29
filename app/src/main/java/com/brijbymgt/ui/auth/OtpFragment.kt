package com.brijbymgt.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.brijbymgt.MainActivity
import com.brijbymgt.databinding.FragmentOtpBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository
    private var email: String = ""
    private var name: String = ""
    private var mobile: String = ""
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
        name = arguments?.getString("name") ?: ""
        mobile = arguments?.getString("mobile") ?: ""
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
                        val finalName = if (name.isNotBlank()) name else (userData?.second ?: "User")
                        val finalMobile = if (mobile.isNotBlank()) mobile else (userData?.third ?: "")

                        if (isRegister) {
                            // Save local registration and sync to Firebase only during registration
                            authRepository.registerUser(finalName, email, finalMobile)
                        }
                        // Update local storage session for both sign-in and registration without syncing to Firebase during sign-in
                        authRepository.saveSession(uid, email, finalName, finalMobile)

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
            val resendName = if (name.isNotBlank()) name else (userData?.second ?: "")
            val resendMobile = if (mobile.isNotBlank()) mobile else (userData?.third ?: "")
            val uid = userData?.first ?: ""

            isLoading = true
            binding.btnResendOtp.isEnabled = false
            binding.btnResendOtp.text = "Resending..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    authRepository.sendOtpApi(email = email, fullName = resendName, mobile = resendMobile, uid = uid)
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
        countDownTimer = object : CountDownTimer(60000, 1000) {
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
