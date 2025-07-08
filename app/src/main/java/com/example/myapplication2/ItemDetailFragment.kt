package com.example.myapplication2

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.myapplication2.databinding.FragmentItemDetailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ItemDetailFragmentArgs by navArgs()
    private var currentItem: Item? = null // To hold the fetched item

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchItemDetails(args.itemId)
    }

    private fun fetchItemDetails(itemId: String) {
        val firestore = Firebase.firestore
        firestore.collection("items").document(itemId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentItem = document.toObject(Item::class.java)?.apply { id = document.id }
                    currentItem?.let {
                        populateUi(it)
                        // Setup the menu after we have the item data
                        setupMenu()
                    }
                } else {
                    Toast.makeText(requireContext(), "Item not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUi(item: Item) {
        binding.detailProgressBar.isVisible = false
        binding.contentGroup.isVisible = true

        binding.ivDetailImage.load(item.imageUrl) {
            placeholder(R.drawable.ic_image_placeholder)
        }

        binding.tvDetailTitle.text = item.title
        binding.tvDetailDescription.text = item.description
        binding.tvDetailLocation.text = item.location

        if (item.datePosted != null) {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            binding.tvDetailDate.text = sdf.format(item.datePosted)
        } else {

            binding.tvDetailDate.text = "Date not available"
        }

        binding.tvDetailStatus.text = item.status
        val statusColor = if (item.status == "LOST") R.color.color_lost_red else R.color.color_found_green
        binding.tvDetailStatus.background.setTint(ContextCompat.getColor(requireContext(), statusColor))

        binding.btnClaimItem.isVisible = item.status == "FOUND"
        binding.btnClaimItem.setOnClickListener {
            Toast.makeText(requireContext(), "Claim logic to be implemented!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.detail_menu, menu)
            }

            // This is called just before the menu is displayed
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                val deleteMenuItem = menu.findItem(R.id.action_delete)

                val currentUserId = Firebase.auth.currentUser?.uid
                deleteMenuItem.isVisible = currentItem != null && currentItem?.userId == currentUserId
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to permanently delete this item?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItemFromFirebase()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItemFromFirebase() {
        val itemToDelete = currentItem ?: return

        Toast.makeText(requireContext(), "Deleting item...", Toast.LENGTH_SHORT).show()

        val storage = Firebase.storage
        val firestore = Firebase.firestore

        fun deleteFirestoreDocument() {
            firestore.collection("items").document(itemToDelete.id).delete()
                .addOnSuccessListener {
                    if (view == null) return@addOnSuccessListener
                    Toast.makeText(requireContext(), "Item successfully deleted.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    if (view == null) return@addOnFailureListener
                    Toast.makeText(requireContext(), "Error deleting item data: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        if (itemToDelete.imageUrl.isEmpty()) {
            deleteFirestoreDocument()
            return
        }

        val imageRef = storage.getReferenceFromUrl(itemToDelete.imageUrl)

        imageRef.delete()
            .addOnSuccessListener {
                deleteFirestoreDocument()
            }
            .addOnFailureListener { exception ->
                val errorCode = (exception as? StorageException)?.errorCode
                if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    deleteFirestoreDocument()
                } else {
                    if (view == null) return@addOnFailureListener
                    Toast.makeText(requireContext(), "Error deleting image: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}