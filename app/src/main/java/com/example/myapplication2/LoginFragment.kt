package com.example.myapplication2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication2.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            Log.d("LoginFragment", "User already logged in. Navigating to HomeFragment.")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        binding.btnLogin.setOnClickListener {
            Log.d("LoginFragment", "Login Button Clicked!")
            loginUser()
        }

        binding.tvGoToRegister.setOnClickListener {
            Log.d("LoginFragment", "Register text clicked. Navigating to RegisterFragment.")
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun loginUser() {
        Log.d("LoginFragment", "loginUser function started.")

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        Log.d("LoginFragment", "Attempting to log in with Email: [$email]") // Password omitted for security

        if (email.isEmpty() || password.isEmpty()) {
            Log.w("LoginFragment", "Validation failed: Email or Password is empty.")
            Toast.makeText(requireContext(), "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFragment", "Firebase login task was successful.")
                    Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    Log.e("LoginFragment", "Firebase login task failed.", task.exception)
                    Toast.makeText(requireContext(), "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}