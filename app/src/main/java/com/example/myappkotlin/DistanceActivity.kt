package com.example.myappkotlin

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappkotlin.databinding.ActivityDistanceBinding
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.atan

class DistanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDistanceBinding
    private lateinit var previewView: PreviewView

    private var x1: Float = 0f // Initial X position (left side of tree trunk)
    private var x2: Float = 0f // Final X position (right side of tree trunk)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.distance_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if savedInstanceState is null to avoid adding the fragment multiple times
        if (savedInstanceState == null) {
            val cameraFragment = CameraFragmet() // Replace with your actual Fragment class
            supportFragmentManager.beginTransaction()
                .replace(R.id.cam_fragment_distance, cameraFragment)
                .commitNow() // Use commitNow to add it synchronously
        }

        // Set up the PreviewView
        // Handle touch events to detect tree trunk width and calculate distance
        binding.camFragmentDistance.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Capture the first touch point (left side of tree trunk)
                    x1 = motionEvent.x
                }
                MotionEvent.ACTION_UP -> {
                    // Capture the second touch point (right side of tree trunk)
                    x2 = motionEvent.x

                    // Calculate the width of the tree trunk in pixels
                    val apparentWidthInPixels = abs(x2 - x1)

                    // Call method to calculate the distance
                    calculateDistance(apparentWidthInPixels)
                }
            }
            true
        }
    }

    private fun calculateDistance(apparentWidthInPixels: Float) {
        val realTreeWidthInMeters = 0.3f // Example tree trunk width (0.3 meters)

        // Get the FOV and focal length
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0] // Use the appropriate camera ID
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

        // Check if focal lengths and sensor size are available
        if (focalLengths == null || focalLengths.isEmpty() || sensorSize == null) {
            Toast.makeText(this, "Unable to get camera characteristics", Toast.LENGTH_SHORT).show()
            return // Exit early if we can't retrieve necessary data
        }

        // Assuming focal length is in mm, use the first available focal length
        val focalLength = focalLengths[0] // Focal length in mm
        val sensorWidth = sensorSize.width.toFloat() // Sensor width in mm

        // Calculate the horizontal field of view (FOV)
        val horizontalFOV = 2 * toDegrees(atan(sensorWidth / (2 * focalLength)).toDouble())

        // Calculate distance using the distance formula
        val distance = (realTreeWidthInMeters * focalLength) / apparentWidthInPixels

        // Show the result as a Toast message
        Toast.makeText(this, "Estimated Distance: ${String.format("%.1f", distance)} meters", Toast.LENGTH_LONG).show()
    }

//    // Function to detect the width of the tree trunk in pixels
//    private fun detectTreeTrunkWidth(x: Float, y: Float): Int {
//        // You can add logic to detect the tree trunk width from the image
//        // This is a placeholder for actual detection logic
//        return 200 // Example width in pixels
//    }
//
//    // Function to calculate the distance based on the trunk width
//    private fun calculateDistance(trunkWidthInPixels: Int): Float {
//        val W_real = 0.3f // Example: 30 cm (real-world width of tree trunk)
//        val focalLength = getCameraFocalLength() // Get this value dynamically from camera parameters
//
//        // Use the formula to calculate distance
//        return (W_real * focalLength) / trunkWidthInPixels
//    }
//
//    // Dummy function to get the camera's focal length
//    private fun getCameraFocalLength(): Float {
//        // Ideally, retrieve the focal length from the camera parameters (this is just a placeholder)
//        return 4.0f // Example focal length in mm
//    }
}
