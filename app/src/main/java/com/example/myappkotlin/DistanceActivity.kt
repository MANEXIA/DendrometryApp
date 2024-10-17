package com.example.myappkotlin

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappkotlin.databinding.ActivityDistanceBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
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
            val cameraFragment = CameraFragmet() // Replace with your actual Fragment class
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
            return
        }
        setupSensorStuff()
    }

    override fun onPause() {
        super.onPause()
        if (areSensorsRegistered) {
            sensorM.unregisterListener(this)
            areSensorsRegistered = false
        }
    }

    // START FOR BODY SENSORS ACTIVITY
    private fun setupSensorStuff() {
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
        inclination = abs(pitchAngle - 90) // Ensures inclination is always positive
    }



    private fun handleGyroscope(event: SensorEvent, timestamp: Long) {
//        val wx = event.values[0]
        val wy = event.values[1]
//        val wz = event.values[2]

        if (timestamp != 0L) {
            val dt = (event.timestamp - timestamp) * 1.0f / 1_000_000_000.0f
            pitchGyro = wy * dt
        }
        this@DistanceActivity.timestamp = event.timestamp
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeightDisplay() {
        heightValueText.text = "Height: ${String.format(Locale.US, "%.2f", heightOfDevice)} m"
    }

    private fun updateCrosshairPosition() {
        val width = crosshairView.width
        val height = crosshairView.height

        // Calculate the center of the screen
        val x = (width / 2).toFloat() // Center horizontally

        // Refine the vertical position calculation based on inclination
        val adjustedInclination = inclination.coerceIn(0f, 90f) // Clamp inclination between 0 and 90 degrees
        val y = (height / 2 + (adjustedInclination * 5)).coerceIn(0f, height.toFloat()) // Adjust vertical position smoothly

        crosshairView.updatePosition(x, y)
    }


    @SuppressLint("SetTextI18n")
    private fun calculateDistance() {
        if (heightOfDevice > 0) {
            if (inclination in 5.0..85.0) { // Valid angle range for calculation
                // Convert inclination to radians for trigonometric functions
                val radInclination = Math.toRadians(inclination.toDouble())
                val distance = heightOfDevice / sin(radInclination)

                // Fine-tuning and threshold handling
                if (distance >= 0) {
                    val adjustedDistance = adjustDistanceForErrors(distance)
                    binding.distanceTextView.text = "Distance: ${String.format(Locale.US,"%.2f", adjustedDistance)} m"
                }
            } else {
                // Notify user to adjust the phone angle
                binding.distanceTextView.text = "Please adjust your phone angle"
                showAngleWarning()
            }
        } else {
            binding.distanceTextView.text = "Invalid height"
        }
    }

    private fun adjustDistanceForErrors(distance: Double): Double {
        // You can fine-tune the error correction based on real-world tests
        val correctionFactor = 0.6 // Adjust this factor based on testing
        return distance - correctionFactor
    }



    private fun showAngleWarning() {
        Snackbar.make(binding.root, "Angle too extreme! Please adjust.", Snackbar.LENGTH_SHORT).show()
    }
}
