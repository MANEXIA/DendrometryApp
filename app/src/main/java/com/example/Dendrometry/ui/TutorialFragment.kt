package com.example.Dendrometry.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.example.Dendrometry.R
import com.example.Dendrometry.databinding.FragmentHomeBinding
import com.example.Dendrometry.databinding.FragmentTutorialBinding
import com.example.Dendrometry.dbmshelpers.UserDatabaseHelper


class TutorialFragment : Fragment() {
    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using ViewBinding
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseHelper = UserDatabaseHelper(requireContext())
        // Access the SharedPreferences where the data was saved
        val sharedPreferences = requireActivity().getSharedPreferences("userSession", Context.MODE_PRIVATE)
        // Retrieve the stored values
        val loggedInUsername = sharedPreferences.getString("loggedInUsername", null)

        binding.skipBtn.setOnClickListener {
            // Update the user's status to "Old" before navigating
            if (loggedInUsername != null) {
                databaseHelper.updateUserStatus(loggedInUsername, "Old")
            }

            // Replace the current fragment with the desired fragment (e.g., HomeFragment)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment()) // Replace with your desired fragment
                .commit()

            // Clear the back stack to prevent returning to the TutorialFragment
            requireActivity().supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }


    }





}