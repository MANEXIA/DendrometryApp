package com.example.myappkotlin

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

class DiameterActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDiameterBinding

    private lateinit var angleView: TextView
    private lateinit var leftRightvaltxt: TextView

    private lateinit var sensorM: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private var leftAngle: Float = 0f
    private var rightAngle: Float = 0f
    private var inclination: Float = 0f


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

        binding.backBtn.setOnClickListener{
            val intentMain = Intent(this, HeightActivity::class.java)
            startActivity(intentMain)
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

                val diameterValue = calculateTreeDiameter(leftAngle, rightAngle, distanceValue)
                val diaMtoCm = diameterValue * 100 // Convert meters to cm
                //"Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}° DIAMETER: ${String.format("%.1f", diaMtoCm)}cm"
                binding.diameterRES.text = "Diameter: ${String.format("%.1f", diaMtoCm)}cm"
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
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var yaw: Float = 0f


    private fun setupSensorStuff() {
        if (!areSensorsRegistered) {
        sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //GET TYPE SENSOR ACC
        gyroscope = sensorM.getDefaultSensor(Sensor.TYPE_GYROSCOPE)  //GET TYPE SENSOR GYRO
        magnetometer = sensorM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)  // Add magnetometer

        accelerometer?.also { acc ->
            sensorM.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
        }

        gyroscope?.also { gyro ->
            sensorM.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.also{ mag ->
            sensorM.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL)
        }
            areSensorsRegistered = true
        }

    }


    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                gravity[0] = event.values[0]
                gravity[1] = event.values[1]
                gravity[2] = event.values[2]
                handleAccelerometer(event)
            }
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event, event.timestamp)

            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic[0] = event.values[0]
                geomagnetic[1] = event.values[1]
                geomagnetic[2] = event.values[2]
            }

        }
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            val rotationMatrixAdjusted = FloatArray(9)
            val currentRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For API level 30 and above (Android 11+)
                this.display?.rotation ?: Surface.ROTATION_0
            } else {
                // For older versions
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }

            when (currentRotation) {
                Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrixAdjusted)
                Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotationMatrixAdjusted)
                Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z, rotationMatrixAdjusted)
                Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, rotationMatrixAdjusted)
            }

            SensorManager.getOrientation(rotationMatrixAdjusted, orientationAngles)

            // Get yaw angle (orientationAngles[0] is the yaw)
            yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // Update UI with yaw
            updateUI()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //ALWAYS REMOVE T0DO IN A NEEDED FUNCTION
    }
    //VARIABLES FOR SENSOR ACCELEMOTER
    private var pitchAngle: Float = 0f
    private var timestamp: Long = 0
    private var pitchGyro: Float = 0f
    private var alpha: Float = 0.98f  // Complementary filter coefficient

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gravity = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        //val tiltX = x / gravity
        val tiltY = y / gravity
        val tiltZ = z / gravity

        // Calculate the pitch angle/inclination angle from accelerometer data
        val pitchAcc = Math.toDegrees(atan2(tiltY.toDouble(), tiltZ.toDouble())).toFloat()
        pitchAngle = alpha * (pitchAngle + pitchGyro) + (1 - alpha) * pitchAcc

        inclination = pitchAngle - 90
    }

    private fun handleGyroscope(event: SensorEvent, timestamp: Long) {
        //val wx = event.values[0]
        val wy = event.values[1]
        //val wz = event.values[2]

        if (timestamp != 0L) {
            val dt = (event.timestamp - timestamp) * 1.0f / 1_000_000_000.0f
            pitchGyro = wy * dt
        }
        this@DiameterActivity.timestamp = event.timestamp

    }

    private fun setLeftAngleValue(){
        leftAngle = yaw
        leftRightvaltxt.text = "Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}°"
    }

    private fun setRigtAngleValue(){
        rightAngle = yaw
        leftRightvaltxt.text = "Left: ${String.format("%.1f", leftAngle)}°\nRight: ${String.format("%.1f", rightAngle)}°"
    }
    private fun calculateTreeDiameter(yawLeft: Float, yawRight: Float, distanceToTree: Float): Double {
        val yawDifference = abs(yawRight - yawLeft)

        // Apply correction factor for very close distances
        val correctionFactor = if (distanceToTree < 1.5) {
            0.95  // Adjust this based on tests, reduce diameter slightly for close distances
        } else {
            1.0
        }

        // Adjusted yaw difference for non-linear effects at close distances
        val adjustedYawDifference = yawDifference * correctionFactor

        // Calculate diameter using trigonometry
        return 2 * distanceToTree * tan(Math.toRadians(adjustedYawDifference / 2.0))
    }

    private fun updateUI(){
//        angleView.text = "${String.format("%.1f", inclination)}°"
        angleView.text = "Yaw: ${String.format("%.1f", yaw)}°"
    }


}




