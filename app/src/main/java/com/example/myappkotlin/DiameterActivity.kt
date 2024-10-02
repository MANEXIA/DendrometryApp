package com.example.myappkotlin

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
import com.example.myappkotlin.databinding.ActivityDiameterBinding
import kotlin.math.abs
import kotlin.math.tan

class DiameterActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDiameterBinding

    private lateinit var angleView: TextView
    private lateinit var leftRightvaltxt: TextView

    private lateinit var sensorM: SensorManager

    //ROTATION VERCTOR
    private var rotationVectorSensor: Sensor? = null
    private var leftAngle: Float = 0f
    private var rightAngle: Float = 0f
    private var holdDiameter: Double = 0.0

    // Yaw threshold to filter out noise (small angle differences)
    private val yawNoiseThreshold = 1.0f // You can adjust this value as needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiameterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if savedInstanceState is null to avoid adding the fragment multiple times
        if (savedInstanceState == null) {
            val cameraFragment = CameraFragmet() // Replace with your actual Fragment class
            supportFragmentManager.beginTransaction()
                .replace(R.id.cam_fragment_container, cameraFragment)
                .commitNow() // Use commitNow to add it synchronously
        }

       //START ANGLE SETUP
        angleView = binding.textView2
        leftRightvaltxt = binding.leftrightValuetxt

        binding.leftWbutton.setOnClickListener(){
            setLeftAngleValue()
        }
        binding.rightWbutton.setOnClickListener(){
            setRigtAngleValue()
        }

        //GET FOV OF PHONE
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
        binding.calculateDiameter.setOnClickListener(){
            try {
                val distanceText = binding.distanceValue.text.toString().trim()

                if (distanceText.isEmpty()) {
                    Toast.makeText(this, "Please enter a distance value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val distanceValue = distanceText.toFloatOrNull()

                if (distanceValue == null || distanceValue <= 0) {
                    Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (abs(leftAngle - rightAngle) < yawNoiseThreshold) {
                    Toast.makeText(this, "Please ensure the left and right angles are different", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Calculate the FOV in degrees (for simplicity, assuming focalLengths[0] is the current focal length)
                val fov = Math.toDegrees(2 * Math.atan((sensorSize.width / (2 * focalLengths[0].toDouble())))).toFloat()

                Log.d("myFOV", "Calculated FOV: $fov degrees")

                val diameterValue = calculateTreeDiameter(leftAngle, rightAngle, distanceValue, fov)
                val diaMtoCm = diameterValue * 100 // Convert meters to cm
                //"Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}° DIAMETER: ${String.format("%.1f", diaMtoCm)}cm"
                binding.diameterRES.text = "Diameter: ${String.format("%.1f", diaMtoCm)}cm"
                holdDiameter = diaMtoCm
                Log.d("DiameterDebug", "Calculated Diameter: $diameterValue")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        //CROSSHAIR CHANGE
        binding.crosshairSwitch.setOnCheckedChangeListener{ _, isChecked ->

            if (isChecked){
                binding.imageView.setImageResource(R.drawable.blackcrosshair)
            }else{
                binding.imageView.setImageResource(R.drawable.whitecrosshair)
            }
        }

        binding.resetDiameter.setOnClickListener(){
            Log.d("resetClick", "wow")
            leftAngle = 0f
            rightAngle = 0f
            leftRightvaltxt.text = "Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}°"
            binding.diameterRES.text = "Diameter:"
            binding.distanceValue.text.clear()

        }

        binding.backBtn.setOnClickListener{
            finish()
        }
        binding.applyButton.setOnClickListener {
            // Prepare data to send back
            val resultIntent = Intent()
            resultIntent.putExtra("diameterValue", holdDiameter.toString())

            // Set the result for the previous activity
            setResult(Activity.RESULT_OK, resultIntent)

            // Close the current activity and return to the previous one
            finish()
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
        Log.d("BackDebug", "onResume called, setting up sensors and starting camera")
        setupSensorStuff()
    }

    override fun onPause() {
        super.onPause()
        if (areSensorsRegistered) {
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


    //DIAMETER MEASURING STARTS HERE
    //YAW VARIABLES FOR SENSOR
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var yaw: Float = 0f

    private fun setupSensorStuff() {
        if (!areSensorsRegistered) {
            sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            rotationVectorSensor = sensorM.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            rotationVectorSensor?.also { rotationVector ->
                sensorM.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
            }
            areSensorsRegistered = true
        }

    }

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
                yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

                // Update UI with yaw
                updateUI()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //ALWAYS REMOVE T0DO IN A NEEDED FUNCTION
    }
    // Function to normalize the yaw angles to 0° - 360°
    private fun normalizeAngle(angle: Float): Float {
        var normalizedAngle = angle % 360f  // Get the angle within the 360° range
        if (normalizedAngle < 0) {
            normalizedAngle += 360f  // If it's negative, bring it into the positive range
        }
        return normalizedAngle
    }

    private fun setLeftAngleValue(){
        leftAngle = normalizeAngle(yaw)
        leftRightvaltxt.text = "Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}°"
    }

    private fun setRigtAngleValue(){
        rightAngle = normalizeAngle(yaw)
        leftRightvaltxt.text = "Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}°"
    }


    private fun calculateTreeDiameter(yawLeft: Float, yawRight: Float, distanceToTree: Float, cameraFOV: Float): Double {
        val yawDifference = getSmallestAngleDifference(yawLeft, yawRight)

        // Apply correction factor for very close distances
        val correctionFactor = if (distanceToTree < 1.5) {
            0.95  // Adjust this based on tests, reduce diameter slightly for close distances
        } else {
            1.0
        }

        // Adjust the yaw difference based on the camera's FOV
        val referenceFOV = 74.92703f // Set this to the FOV of the device you used for testing
//        val referenceFOV = 65.0f // Set this to the FOV of the device you used for testing
        val fovAdjustedYawDifference = (yawDifference * cameraFOV) / referenceFOV

        // Apply the correction factor for non-linear effects at close distances
        val adjustedYawDifference = fovAdjustedYawDifference * correctionFactor

        // Calculate diameter using trigonometry
        Log.d("DiameterDebug", "Left Angle: $leftAngle, Right Angle: $rightAngle")
        Log.d("DiameterDebug", "Yaw Difference: $yawDifference")
        return 2 * distanceToTree * tan(Math.toRadians(adjustedYawDifference / 2.0))

    }

    // Function to get the smallest angle difference
    private fun getSmallestAngleDifference(angle1: Float, angle2: Float): Float {
        val normalizedAngle1 = normalizeAngle(angle1)
        val normalizedAngle2 = normalizeAngle(angle2)

        val diff = abs(normalizedAngle1 - normalizedAngle2)
        return if (diff > 180f) {
            360f - diff  // Ensure the difference is the smallest path on the circle
        } else {
            diff
        }
    }


    private fun updateUI(){
        //angleView.text = "${String.format("%.1f", inclination)}°"
        angleView.text = "Yaw: ${String.format("%.1f", normalizeAngle(yaw))}°"
    }


}




