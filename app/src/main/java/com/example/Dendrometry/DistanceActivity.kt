package com.example.Dendrometry

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
import com.example.Dendrometry.databinding.ActivityDistanceBinding
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
    private var heightOfDevice: Double = 1.0 // Replace with actual height in meters
    private lateinit var heightSeekBar: SeekBar
    private lateinit var heightValueText: TextView
    private lateinit var crosshairView: CrosshairView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Set up UI elements
        distanceView = binding.distanceTextView

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.distance_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // OnClick Button for activities
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Add CameraFragment once if savedInstanceState is null
        if (savedInstanceState == null) {
            val cameraFragment = CameraFragmet() // Replace with your actual Fragment class
            supportFragmentManager.beginTransaction()
                .replace(R.id.cam_fragment_distance, cameraFragment)
                .commitNow()
        }

        setupSeekBar()
        crosshairView = binding.crosshairView
    }

    private var areSensorsRegistered = false
    private var isActivityFinishing = false

    override fun onResume() {
        super.onResume()
        if (!isActivityFinishing && !areSensorsRegistered) {
            sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorM.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            accelerometer?.let { sensorM.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            gyroscope?.let { sensorM.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

            areSensorsRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (areSensorsRegistered) {
            sensorM.unregisterListener(this)
            areSensorsRegistered = false
        }
    }

    private fun setupSeekBar() {
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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometer(event)
                calculateDistance() // Only recalculate distance if accelerometer updates
                updateCrosshairPosition()
            }
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event, event.timestamp)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Handle accuracy changes if needed...
    }

    // Variables for sensor data
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

    private fun handleGyroscope(event: SensorEvent, currentTimestamp: Long) {
        if (timestamp != 0L) {
            val dt = (currentTimestamp - timestamp) * 1.0f / 1_000_000_000.0f
            pitchGyro = event.values[1] * dt
        }
        timestamp = currentTimestamp
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeightDisplay() {
        heightValueText.text = "Height: ${String.format(Locale.US, "%.2f", heightOfDevice)}m"
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
                    binding.distanceTextView.text = "Distance: ${String.format(Locale.US,"%.1f", adjustedDistance)}m"
                }
            } else {
                // Notify user to adjust the phone angle
                binding.distanceTextView.text = "Please adjust your phone Angle/Height"
                showAngleWarning()
            }
        } else {
            binding.distanceTextView.text = "Invalid height"
        }
    }

    private fun adjustDistanceForErrors(distance: Double): Double {
        // Fine-tune error correction based on real-world tests
        val correctionFactor = 0.6 // Adjust this factor based on testing
        return distance - correctionFactor
    }

    private fun showAngleWarning() {
        Snackbar.make(binding.root, "Angle too extreme! Please adjust.", Snackbar.LENGTH_SHORT).show()
    }
}
