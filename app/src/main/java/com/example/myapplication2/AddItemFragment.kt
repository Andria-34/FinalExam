package com.example.myapplication2

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication2.databinding.FragmentAddItemBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            _binding?.ivItemPreview?.setImageURI(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSelectImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        binding.rgStatus.setOnCheckedChangeListener { _, checkedId -> binding.tilSecretQuestion.isVisible = checkedId == R.id.rbFound }
        binding.btnSubmit.setOnClickListener { submitReport() }
    }

    private fun submitReport() {
        val title = binding.etItemTitle.text.toString().trim()
        val description = binding.etItemDescription.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val location = binding.etLocation.text.toString().trim()
        val status = if (binding.rbLost.isChecked) "LOST" else "FOUND"
        val secretQuestion = binding.etSecretQuestion.text.toString().trim()

        if (imageUri == null || title.isEmpty() || description.isEmpty() || location.isEmpty() || (status == "FOUND" && secretQuestion.isEmpty())) {
            Toast.makeText(requireContext(), "Please fill all required fields and select an image.", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true, binding)

        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}")
        imageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveItemToFirestore(title, description, category, location, status, secretQuestion, downloadUrl.toString())
                }
            }
            .addOnFailureListener { e ->
                _binding?.let { safeBinding -> setLoading(false, safeBinding) }
                Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveItemToFirestore(title: String, description: String, category: String, location: String, status: String, secretQuestion: String, imageUrl: String) {
        val firestore = Firebase.firestore
        val newItem = Item(
            userId = Firebase.auth.currentUser?.uid ?: "",
            title = title, description = description, category = category, location = location,
            imageUrl = imageUrl, status = status, secretQuestion = if (status == "FOUND") secretQuestion else null
        )

        firestore.collection("items").add(newItem)
            .addOnSuccessListener {

                val safeBinding = _binding ?: return@addOnSuccessListener

                setLoading(false, safeBinding)
                Toast.makeText(requireContext(), "Item reported successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                val safeBinding = _binding ?: return@addOnFailureListener
                setLoading(false, safeBinding)
                Toast.makeText(requireContext(), "Error saving item: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(isLoading: Boolean, binding: FragmentAddItemBinding) {
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnSubmit.text = if (isLoading) "Submitting..." else "Report Item"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}