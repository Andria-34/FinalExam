package com.example.myapplication2

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication2.databinding.FragmentHomeBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.example.myapplication2.HomeFragmentDirections
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var itemAdapter: ItemAdapter
    private val itemList = mutableListOf<Item>()

    private var firestoreListener: ListenerRegistration? = null

    // --- Fragment Lifecycle ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupMenu()
        setupRecyclerView()
        fetchItems()

        binding.fabAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addItemFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        fetchItems()
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Menu Handling ---
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        Firebase.auth.signOut()
                        Toast.makeText(requireContext(), "You have been logged out.", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // --- UI and Data ---
    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(itemList) { clickedItem ->
            val action = HomeFragmentDirections.actionHomeFragmentToItemDetailFragment(clickedItem.id)
            findNavController().navigate(action)
        }
        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = itemAdapter
        }
    }

    private fun fetchItems() {
        binding.progressBar.isVisible = true
        binding.tvEmptyState.isVisible = false
        binding.rvItems.isVisible = false

        firestoreListener = firestore.collection("items")
            .orderBy("datePosted", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->

                val safeBinding = _binding ?: return@addSnapshotListener

                safeBinding.progressBar.isVisible = false

                if (error != null) {
                    Log.w("HomeFragment", "Listen failed.", error)
                    Toast.makeText(context, "Failed to load items.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                itemList.clear()
                snapshots?.forEach { document ->
                    val item = document.toObject(Item::class.java).apply { id = document.id }
                    itemList.add(item)
                }
                itemAdapter.notifyDataSetChanged()

                safeBinding.tvEmptyState.isVisible = itemList.isEmpty()
                safeBinding.rvItems.isVisible = itemList.isNotEmpty()
            }
    }
}