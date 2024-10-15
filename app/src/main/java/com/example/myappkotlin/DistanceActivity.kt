package com.example.myappkotlin

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappkotlin.databinding.ActivityDistanceBinding
import com.google.android.material.snackbar.Snackbar
import kotlin.math.atan2
import kotlin.math.sqrt

class DistanceActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDistanceBinding

    private lateinit var sensorM: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var distanceView: TextView
    private var inclination: Float = 0f
    private var heightOfDevice:Double = 1.0 // Replace with actual height in meters
    private lateinit var heightSeekBar: SeekBar
    private lateinit var heightValueText: TextView

    private lateinit var crosshairView: CrosshairView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Set up UI elements
        distanceView = binding.distanceTextView // Assuming you have a TextView for displaying distance

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.distance_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ONCLICK BTN FOR ACTIVITIES
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Check if savedInstanceState is null to avoid adding the fragment multiple times
        if (savedInstanceState == null) {
            val cameraFragment = CameraFragmentDistance() // Replace with your actual Fragment class
            supportFragmentManager.beginTransaction()
                .replace(R.id.cam_fragment_distance, cameraFragment)
                .commitNow() // Use commitNow to add it synchronously
        }
        setupSensorStuff()


        heightSeekBar = binding.heightSeekBar
        heightValueText = binding.heightValueText

        // Set the initial height
        heightSeekBar.progress = 0 // Initial height (0 cm)
        updateHeightDisplay()

        // Listen for changes on the SeekBar
        heightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                heightOfDevice = progress / 100.0 // Convert cm to meters
                updateHeightDisplay()
                calculateDistance() // Recalculate distance
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        crosshairView = binding.crosshairView



    }//END OF ONCREATE

    private var areSensorsRegistered = false
    private var isActivityFinishing = false

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
        Log.d("BackDebug", "onPause called, unbinding camera in DistanceActivity")
    }

    override fun onStop() {
        super.onStop()
        // Unregister sensor listeners in both onPause() and onStop() for redundancy
        if (areSensorsRegistered) {
            sensorM.unregisterListener(this)
            areSensorsRegistered = false
        }
    }

    // START FOR BODY SENSORS ACTIVITY
    private fun setupSensorStuff() {
        Log.d("BackDebug", "setupSensorStuff called in DistanceActivity")
        if (!areSensorsRegistered) {
            sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager

            accelerometer = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) // GET TYPE SENSOR ACC
            gyroscope = sensorM.getDefaultSensor(Sensor.TYPE_GYROSCOPE)  // GET TYPE SENSOR GYRO

            accelerometer?.also { acc ->
                sensorM.registerListener(this, acc, SensorManager.SENSOR_DELAY_FASTEST)
            }

            gyroscope?.also { gyro ->
                sensorM.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST)
            }
            areSensorsRegistered = true
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event, event.timestamp)
        }
        calculateDistance()
        updateCrosshairPosition()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Handle accuracy changes if needed...
    }

    // VARIABLES FOR SENSOR ACCELEROMETER
    private var pitchAngle: Float = 0f
    private var timestamp: Long = 0
    private var pitchGyro: Float = 0f
    private var alpha: Float = 0.98f  // Complementary filter coefficient

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gravity = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val tiltY = y / gravity
        val tiltZ = z / gravity

        // Calculate the pitch angle/inclination angle from accelerometer data
        val pitchAcc = Math.toDegrees(atan2(tiltY.toDouble(), tiltZ.toDouble())).toFloat()
        pitchAngle = alpha * (pitchAngle + pitchGyro) + (1 - alpha) * pitchAcc

        // Adjust inclination to be 0 when the phone is held upright in portrait mode
        inclination = pitchAngle - 90
    }


    private fun handleGyroscope(event: SensorEvent, timestamp: Long) {
        val wx = event.values[0]
        val wy = event.values[1]
        val wz = event.values[2]

        if (timestamp != 0L) {
            val dt = (event.timestamp - timestamp) * 1.0f / 1_000_000_000.0f
            pitchGyro = wy * dt
        }
        this@DistanceActivity.timestamp = event.timestamp
    }

//    private fun calculateDistance() {
//        // Calculate distance based on inclination and height of the device
//        // Assuming heightOfDevice is in meters and inclination is in degrees
////        val heightValuetext = binding.heightValue.text.toString().toDouble()
////        heightOfDevice = binding.heightValue.text.toString().toDouble()
//        val distance = heightOfDevice / Math.sin(Math.toRadians(inclination.toDouble()))
//        distanceView.text = "Distance:${String.format("%.1f", distance)}m"
//    }

    private fun updateHeightDisplay() {
        heightValueText.text = "Height: ${String.format("%.2f", heightOfDevice)} m"
    }
    private fun updateCrosshairPosition() {
        val width = crosshairView.width
        val height = crosshairView.height

        // Calculate crosshair position based on inclination
        // Assuming a simple mapping of tilt to screen coordinates
        val x = (width / 2).toFloat() // Center horizontally
        val y = (height / 2 + (inclination * 5)).coerceIn(0f, height.toFloat()) // Adjust vertical position

        crosshairView.updatePosition(x, y)
    }

    private fun calculateDistance() {
        if (heightOfDevice > 0) {
            val distance = heightOfDevice / Math.sin(Math.toRadians(inclination.toDouble()))
//            binding.distanceTextView.text = "Distance: ${String.format("%.1f", distance)} m"

            // Check if the inclination is too close to 0 or 90 degrees
            if (distance < 1.0) {
                val distancePositive = kotlin.math.abs(distance)
                if(distancePositive in 1.0..85.0){
                    binding.distanceTextView.text = "Distance: ${String.format("%.1f", distancePositive)}m"
                }
            } else {
                // Notify user to adjust the angle
                binding.distanceTextView.text = "Please adjust your phone angle"
                // Show an alert or notification to the user
                showAngleWarning()
            }
        } else {
            binding.distanceTextView.text = "Invalid height"
        }
    }

    private fun showAngleWarning() {
        Snackbar.make(binding.root, "Angle too extreme! Please adjust.", Snackbar.LENGTH_SHORT).show()
    }
}
