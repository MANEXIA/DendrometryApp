package com.example.Dendrometry.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.FragmentManager
import com.example.Dendrometry.R
import com.example.Dendrometry.databinding.FragmentHomeBinding
import com.example.Dendrometry.databinding.FragmentTutorialBinding
import com.example.Dendrometry.dbmshelpers.UserDatabaseHelper


class TutorialFragment : Fragment() {
    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: UserDatabaseHelper
    private lateinit var videoTutorial: VideoView

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

        // Call your setup method here
        setupVideoTutorial(view)

    }


    private fun setupVideoTutorial(view: View) {
        val videoTutorial: VideoView = view.findViewById(R.id.videoTutorial)

        // Set the video path or URI
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/raw/tutorial_video")
        videoTutorial.setVideoURI(videoUri)

        // Add MediaController for playback controls
        val mediaController = MediaController(requireContext())
        mediaController.setAnchorView(videoTutorial)
        videoTutorial.setMediaController(mediaController)

        // Start the video
        videoTutorial.start()
        // Enter full-screen mode when the fragment is visible
        enterFullScreenMode()
    }


    private fun enterFullScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30 and above
            requireActivity().window.setDecorFitsSystemWindows(false)
            requireActivity().window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For API levels below 30
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }


    private fun exitFullScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30 and above
            requireActivity().window.setDecorFitsSystemWindows(true)
            requireActivity().window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            // For API levels below 30
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure the app exits full-screen mode when the fragment is destroyed
        exitFullScreenMode()
    }






}