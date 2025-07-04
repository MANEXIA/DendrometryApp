package com.example.Dendrometry.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.Dendrometry.R
import com.example.Dendrometry.databinding.ActivityDiameterBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

class  DiameterActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDiameterBinding
    // TextViews to display angles and calculated values
    private lateinit var angleView: TextView
    private lateinit var leftRightvaltxt: TextView

    // Sensor manager and rotation vector sensor
    private lateinit var sensorM: SensorManager
    private var rotationVectorSensor: Sensor? = null
    // Variables to store left and right angles and calculated diameter
    private var leftAngle: Double = 0.0
    private var rightAngle: Double = 0.0
    private var holdDiameter: Double = 0.0

    // Threshold to filter out small angle noise for yaw calculations
    private val yawNoiseThreshold = 1.0f // You can adjust this value as needed

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiameterBinding.inflate(layoutInflater)

        // Enable edge-to-edge UI and set the content view
        enableEdgeToEdge()
        setContentView(binding.root)

        // Adjusts padding to account for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Add CameraFragment only once to avoid duplicate instances
        if (savedInstanceState == null) {
            val cameraFragment = CameraFragmet() // Replace with your actual Fragment class
            supportFragmentManager.beginTransaction()
                .replace(R.id.cam_fragment_container, cameraFragment)
                .commitNow() // Use commitNow to add it synchronously
        }

        // Initialize TextViews for angle display
        angleView = binding.textView2
        leftRightvaltxt = binding.leftrightValuetxt

        // Retrieve camera characteristics for Field of View (FOV) calculations
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
        // Set up left angle button to calculate and display tree diameter
        binding.leftWbutton.setOnClickListener(){
            val distanceText = binding.distanceValue.text.toString()
            // Validate distance input using checkDistance
            val distanceValue = checkDistance(distanceText) ?: return@setOnClickListener
            // If distance is valid, call setValueTOP
            setLeftAngleValue()
            if(rightAngle == 0.0){
                //Toast.makeText(this, "Please Set Bottom Angle", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                if (abs(leftAngle - rightAngle) < yawNoiseThreshold) {
                    Toast.makeText(this, "Please ensure the left and right angles are different", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Calculate FOV
                val fov = Math.toDegrees(2 * atan((sensorSize.width / (2 * focalLengths[0].toDouble())))).toFloat()
                Log.d("myFOV", "Calculated FOV: $fov degrees")
                // Calculate diameter
                val diameterValue = calculateTreeDiameter(leftAngle, rightAngle, distanceValue, fov.toDouble())
                val diaMtoCm = diameterValue * 100 // Convert meters to cm
                // Display calculated diameter
                binding.diameterRES.text = "Diameter: ${String.format(Locale.US,"%.2f", diaMtoCm)}cm"
                holdDiameter = diaMtoCm
                Log.d("DiameterDebug", "Calculated Diameter: $diameterValue")
            }

        }
        // Set up right angle button to calculate and display tree diameter
        binding.rightWbutton.setOnClickListener(){
            val distanceText = binding.distanceValue.text.toString()
            // Validate distance input using checkDistance
            val distanceValue = checkDistance(distanceText) ?: return@setOnClickListener
            // If distance is valid, call setValueTOP
            setRightAngleValue()
            if(leftAngle == 0.0){
                //Toast.makeText(this, "Please Set Bottom Angle", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                if (abs(leftAngle - rightAngle) < yawNoiseThreshold) {
                    Toast.makeText(this, "Please ensure the left and right angles are different", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Calculate FOV
                val fov = Math.toDegrees(2 * atan((sensorSize.width / (2 * focalLengths[0].toDouble())))).toFloat()
                Log.d("myFOV", "Calculated FOV: $fov degrees")
                // Calculate diameter
                val diameterValue = calculateTreeDiameter(leftAngle, rightAngle, distanceValue, fov.toDouble())
                val diaMtoCm = diameterValue * 100 // Convert meters to cm
                // Display calculated diameter
                binding.diameterRES.text = "Diameter: ${String.format(Locale.US,"%.2f", diaMtoCm)}cm"
                holdDiameter = diaMtoCm
                Log.d("DiameterDebug", "Calculated Diameter: $diameterValue")
            }
        }

        binding.resetDiameter.setOnClickListener(){
            leftAngle = 0.0
            rightAngle = 0.0
            leftRightvaltxt.text = "Left: ${String.format(Locale.US,"%.1f", leftAngle)}°\nRight: ${String.format(Locale.US,"%.1f", rightAngle)}°"
            binding.diameterRES.text = "Diameter:"
            binding.distanceValue.text.clear()
        }

        binding.backBtn.setOnClickListener{
            finish()
        }
        binding.applyButton.setOnClickListener {
            // Check if holdDiameter is still 0.0
            if (holdDiameter == 0.0) {
                // Optional: Show a message to the user indicating that the value cannot be 0.0
                Toast.makeText(this, "Diameter value cannot be 0.0. Please enter a valid value.", Toast.LENGTH_SHORT).show()
            } else {
                // Prepare data to send back
                val resultIntent = Intent().apply {
                    putExtra("diameterValue", holdDiameter.toString())
                }
                // Set the result for the previous activity
                setResult(Activity.RESULT_OK, resultIntent)
                // Close the current activity and return to the previous one
                finish()
            }
        }


    }//END OF ONCREATE FUNCTON

    private var isActivityFinishing = false
    private var areSensorsRegistered = false
    override fun onResume() {
        super.onResume()
        if (isActivityFinishing) {
            Log.d("BackDebug", "Activity is finishing, skipping setup")
            return
        }
        Log.d("BackDebug", "onResume called, setting up sensors and starting camera in DiameterKT")
        setupSensorStuff()
    }

    override fun onPause() {
        super.onPause()
        if (areSensorsRegistered) {
            Log.d("BackDebug", "onPause called, unregistering sensors")
            sensorM.unregisterListener(this)
            areSensorsRegistered = false
        }
    }
    override fun onStop() {
        super.onStop()
        // Unregister sensor listeners in both onPause() and onStop() for redundancy
        if (areSensorsRegistered) {
            sensorM.unregisterListener(this)
            areSensorsRegistered = false
        }
    }

    // Validate distance input and return if valid
    private fun checkDistance(distanceText: String): Double? {
        // Check if distance input is empty
        if (distanceText.isEmpty()) {
            Toast.makeText(this, "Please enter a distance value", Toast.LENGTH_SHORT).show()
            return null
        }

        // Convert distance input to float
        val distanceValue = distanceText.toDoubleOrNull()

        // Validate if the converted value is not null and greater than 0
        if (distanceValue == null || distanceValue <= 0) {
            Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show()
            return null
        }
        // Return the valid distance value
        return distanceValue
    }


    //DIAMETER MEASURING STARTS HERE
    // Variables for sensor and yaw angle calculations
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var yaw: Float = 0f

    //Register rotation vector sensor
    private fun setupSensorStuff() {
        if (!areSensorsRegistered) {
            sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            rotationVectorSensor = sensorM.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            rotationVectorSensor?.also { rotationVector ->
                sensorM.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
            }
            areSensorsRegistered = true
        }
    }
    // Handle sensor events to calculate yaw and update UI
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Get rotation matrix from the rotation vector
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val currentRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }
                // Adjust rotation matrix for current display rotation
                val rotationMatrixAdjusted = FloatArray(9)
                when (currentRotation) {
                    Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        rotationMatrixAdjusted
                    )
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted
                    )
                    Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Z,
                        rotationMatrixAdjusted
                    )
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X,
                        rotationMatrixAdjusted
                    )
                }
                // Get orientation angles from adjusted rotation matrix
                SensorManager.getOrientation(rotationMatrixAdjusted, orientationAngles)
                // Yaw (azimuth) is the angle you're interested in
                val yawInDegrees = Math.toDegrees(orientationAngles[0].toDouble()) // Intermediate calculation as Double
                yaw = yawInDegrees.toFloat() // Convert to Float if you need to maintain yaw as Float
                // Update UI with yaw
                updateUI()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //ALWAYS REMOVE T0DO IN A NEEDED FUNCTION
    }


    @SuppressLint("SetTextI18n")
    private fun setLeftAngleValue(){
        leftAngle = normalizeAngle(yaw.toDouble())
        leftRightvaltxt.text = "Left: ${String.format(Locale.US,"%.1f", leftAngle)}°\nRight: ${String.format(Locale.US,"%.1f", rightAngle)}°"
    }

    @SuppressLint("SetTextI18n")
    private fun setRightAngleValue(){
        rightAngle = normalizeAngle(yaw.toDouble())
        leftRightvaltxt.text = "Left: ${String.format(Locale.US,"%.1f", leftAngle)}°\nRight: ${String.format(Locale.US,"%.1f", rightAngle)}°"

    }

    // Function to normalize the yaw angles to 0° - 360°
    private fun normalizeAngle(angle: Double): Double {
        var normalizedAngle = angle % 360.0  // Get the angle within the 360° range
        if (normalizedAngle < 0) {
            normalizedAngle += 360.0  // If it's negative, bring it into the positive range
        }
        return normalizedAngle
    }

    // Function to get the smallest angle difference
    private fun getSmallestAngleDifference(angle1: Double, angle2: Double): Double {
        val normalizedAngle1 = normalizeAngle(angle1)
        val normalizedAngle2 = normalizeAngle(angle2)

        val diff = abs(normalizedAngle1 - normalizedAngle2)
        return if (diff > 180) {
            360 - diff  // Ensure the difference is the smallest path on the circle
        } else {
            diff
        }
    }

    private fun calculateTreeDiameter(
        yawLeft: Double,
        yawRight: Double,
        distanceToTree: Double,
        cameraFOV: Double
    ): Double {
        // Normalize the yaw angles to avoid overlap issues
        val normalizedYawLeft = normalizeAngle(yawLeft)
        val normalizedYawRight = normalizeAngle(yawRight)

        // Calculate absolute yaw difference to ensure consistency
        val yawDifference = abs(getSmallestAngleDifference(normalizedYawLeft, normalizedYawRight))

        // Notify user to adjust distance based on yaw difference
        notifyUserToAdjustDistance(yawDifference)

        // Apply a dynamic correction factor based on distance
        val correctionFactor = if (distanceToTree < 1.5) {
            0.95
        } else {
            1.0
        }

        // Calculate calibration factor to normalize the FOV
        val referenceFOV = 74.92703
        val calibrationFactor = if (cameraFOV < referenceFOV) {
            referenceFOV / cameraFOV
        } else {
            cameraFOV / referenceFOV
        }

        // Normalize the yaw difference based on the reference FOV
        val fovAdjustedYawDifference = yawDifference * calibrationFactor

        // Apply correction factor for close distances
        val adjustedYawDifference = fovAdjustedYawDifference * correctionFactor

        // Log debug information
        Log.d("DiameterDebug", "Left Angle: $normalizedYawLeft,\nRight Angle: $normalizedYawRight")
        Log.d("DiameterDebug", "distanceToTree: $distanceToTree")
        Log.d("DiameterDebug", "Reference FOV: $referenceFOV")
        Log.d("DiameterDebug", "Device FOV: $cameraFOV")
        Log.d("DiameterDebug", "CalibrationFactor: $calibrationFactor")
        Log.d("DiameterDebug", "Correction Factor: $correctionFactor")
        Log.d("DiameterDebug", "Yaw Difference: $yawDifference")
        Log.d("DiameterDebug", "Adjusted Yaw Difference: $adjustedYawDifference")

        // Calculate diameter using trigonometry
        return 2 * distanceToTree * tan(Math.toRadians(adjustedYawDifference / 2.0))
    }


    private fun notifyUserToAdjustDistance(yawDifference: Double) {
        // Define thresholds for adjusting distance
        val wideThreshold = 65f // Example threshold for wide angles
        val slimThreshold = 5f // Example threshold for slim angles

        when {
            yawDifference > wideThreshold -> {
                Snackbar.make(binding.root, "Move closer and Adjust Distance Input to the tree for better measurement.", Snackbar.LENGTH_SHORT).show()
            }
            yawDifference < slimThreshold -> {
                Snackbar.make(binding.root, "Step back and Adjust Distance Input for a clearer measurement.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateUI(){
        //angleView.text = "${String.format("%.1f", inclination)}°"
        angleView.text = "${String.format(Locale.US,"%.1f", normalizeAngle(yaw.toDouble()))}°"

    }



}




