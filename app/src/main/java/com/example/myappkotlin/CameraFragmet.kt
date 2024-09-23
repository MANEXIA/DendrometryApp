package com.example.myappkotlin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myappkotlin.databinding.FragmentCameraFragmetBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragmet : Fragment() {

    private var _binding: FragmentCameraFragmetBinding? = null
    private val binding get() = _binding!!

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService

    // Permission launcher
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (!it.value) permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(requireContext(), "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraFragmetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize cameraExecutor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request permissions when fragment view is created
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("BackDebug", "onResume called")

        // Check permissions and restart the camera if needed
        if (allPermissionsGranted()) {
            startCamera() // Restart the camera if necessary
        }
    }

    // Request camera permissions
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    // Check if all required permissions are granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // Setup and start the camera
    private fun startCamera() {
        Log.d("BackDebug", "startCamera called in CameraFragment")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            Log.d("BackDebug", "CameraProvider is ready")
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    Log.d("BackDebug", "Attaching surfaceProvider to viewFinder")
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any existing use cases and bind new ones
                cameraProvider?.unbindAll()
                Log.d("BackDebug", "Unbind all previous use cases")

                // Bind preview use case to lifecycle
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview
                )
                Log.d("BackDebug", "Camera use case bound successfully")

            } catch (exc: Exception) {
                Log.e("BackDebug", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Unbind camera use cases when fragment is paused
    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
        Log.d("BackDebug", "Camera unbound in onPause")
    }

    // Clean up resources when fragment is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // Shutdown camera executor
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
