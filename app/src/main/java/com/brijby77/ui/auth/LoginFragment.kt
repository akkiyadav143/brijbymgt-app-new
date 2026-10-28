package com.brijby77.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brijby77.R
import com.brijby77.databinding.FragmentLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())

        binding.btnSendOtp.setOnClickListener {
            if (isLoading) return@setOnClickListener

            val email = binding.etLoginEmail.text.toString().trim()
            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etLoginEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }

            if (!authRepository.isEmailRegistered(email)) {
                Toast.makeText(requireContext(), "Email not registered. Please register first.", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                return@setOnClickListener
            }

            val userData = authRepository.getRegisteredUserData(email)
            val name = userData?.second ?: "School Admin"
            val mobile = userData?.third ?: "9999999999"

            isLoading = true
            binding.btnSendOtp.isEnabled = false
            binding.btnSendOtp.text = "Sending OTP..."

            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    authRepository.saveUserDetailsToDatabaseThenRequestOtp(name, email, mobile)
                }
                isLoading = false
                if (_binding != null) {
                    binding.btnSendOtp.isEnabled = true
                    binding.btnSendOtp.text = "Send OTP"
                }

                when (result) {
                    is AuthRepository.ApiResult.Success -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        val bundle = Bundle().apply {
                            putString("email", email)
                            putBoolean("is_register", false)
                        }
                        findNavController().navigate(R.id.action_loginFragment_to_otpFragment, bundle)
                    }
                    is AuthRepository.ApiResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvLoginPrivacy.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_privacyPolicyFragment)
        }

        binding.tvLoginTerms.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_termsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
