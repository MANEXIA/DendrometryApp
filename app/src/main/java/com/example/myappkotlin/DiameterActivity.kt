package com.example.myappkotlin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappkotlin.databinding.ActivityDiameterBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.sqrt

class DiameterActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDiameterBinding
    //CAMERA THINGS
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var angleView: TextView
//    private lateinit var diameterView: TextView
    private lateinit var sensorM: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var inclination: Float = 0f

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
            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
        }


        //REQUEST CAMERA PERMISSION BY CALLING FUNCTIONS
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
       //START ANGLE SETUP
        angleView = binding.textView2
        val density = resources.displayMetrics.density // Screen density


        binding.addWbutton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startIncreasingWidth()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopIncreasingWidth()
                    true
                }
                else -> false
            }

        }

        binding.decrWbutton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startDecreasingWidth()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopDecreasingWidth()
                    true
                }
                else -> false
            }

        }



        setupSensorStuff()
    }//END OF ONCREATE FUNCTON




    //STARTING PERMISSION FOR CAMERA AND OTHERS
    //ASKING FOR PERMISSION ALL NEEDED FOR CAMERA
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                android.Manifest.permission.CAMERA,
                ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }
//END FOR PERMISSIONS

    //START CAMERA ACTIVITY


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)


                // Example of calculating tree diameter after binding the preview
                // Assuming you have a UI component like an ImageView for the diameter
                val treeDiameterView = binding.textWidth // Replace with your actual view
                val screenWidthMeters = 0.068f // Example: 6.8 cm screen width
                val screenWidthPixels = resources.displayMetrics.widthPixels
                val distanceToTreeMeters = 5f // Replace with the actual distance to the tree

                val treeDiameter = calculateRealWorldSize(
                    this,
                    treeDiameterView,
                    screenWidthMeters,
                    screenWidthPixels,
                    distanceToTreeMeters
                )
//
//                binding.diameterText = "${String.format("%.1f", treeDiameter)}°"



            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    } //END FOR CAMERA ACTIVITY


    //DIAMETER MEASURING STARTS HERE
    private fun setupSensorStuff() {
        sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //GET TYPE SENSOR ACC
        gyroscope = sensorM.getDefaultSensor(Sensor.TYPE_GYROSCOPE)  //GET TYPE SENSOR GYRO

        accelerometer?.also { acc ->
            sensorM.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
        }

        gyroscope?.also { gyro ->
            sensorM.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event, event.timestamp)
        }
        updateUI()
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //ALWAYS REMOVE T0DO IN A NEEDED FUNCTION
    }

    //VARIABLES FOR SENSOR ACCELEMOTER

    private var pitchAngle: Float = 0f
    private var timestamp: Long = 0
    private var pitchGyro: Float = 0f
    private var alpha: Float = 0.98f  // Complementary filter coefficient
    // private var isMeasuringTop: Boolean = true // Toggle to switch between measuring top and base

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gravity = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val tiltX = x / gravity
        val tiltY = y / gravity
        val tiltZ = z / gravity

        // Calculate the pitch angle/inclination angle from accelerometer data
        val pitchAcc = Math.toDegrees(atan2(tiltX.toDouble(), tiltZ.toDouble())).toFloat()
        pitchAngle = alpha * (pitchAngle + pitchGyro) + (1 - alpha) * pitchAcc

        // Adjust inclination to be 0 when the phone is held upright in portrait mode
//        inclination = pitchAngle - 90
        inclination = pitchAngle
    }

    private fun handleGyroscope(event: SensorEvent, timestamp: Long) {
        val wx = event.values[0]
        val wy = event.values[1]
        val wz = event.values[2]

        if (timestamp != 0L) {
            val dt = (event.timestamp - timestamp) * 1.0f / 1_000_000_000.0f
            pitchGyro = wx * dt
        }
        this@DiameterActivity.timestamp = event.timestamp

    }

    private fun updateUI(){
        angleView.text = "${String.format("%.1f", inclination)}°"
    }


   //GET WIDTH FOR DIAMETER PIXEL TO REAL WORLDS SIZE


    private var increaseWidthRunnable: Runnable? = null
    private var isIncreasing = false

    private fun startIncreasingWidth() {
        if (isIncreasing) return
        isIncreasing = true
        increaseWidthRunnable = object : Runnable {
            override fun run() {
                if (!isIncreasing) return
                val currentWidth = binding.textWidth.width
                val newWidth = currentWidth + 5 // Increment width by 10 pixels
                val params = binding.textWidth.layoutParams
                params.width = newWidth
                binding.textWidth.layoutParams = params
                binding.textWidth.text = "$newWidth"


                // Repeat this runnable after a short delay
                binding.addWbutton.postDelayed(this, 25) // 100ms delay
            }
        }
        binding.addWbutton.post(increaseWidthRunnable)
    }
    private fun stopIncreasingWidth() {
        isIncreasing = false
        increaseWidthRunnable?.let {
            binding.addWbutton.removeCallbacks(it)
        }
    }


    private var decreaseWidthRunnable: Runnable? = null
    private var isDecreasing = false


    private fun startDecreasingWidth() {
        if (isDecreasing) return
        isDecreasing = true
        decreaseWidthRunnable = object : Runnable {
            override fun run() {
                if (!isDecreasing) return
                val currentWidth = binding.textWidth.width
                val newWidth = currentWidth - 5 // Increment width by 10 pixels
                val params = binding.textWidth.layoutParams
                params.width = newWidth
                binding.textWidth.layoutParams = params
                binding.textWidth.text = "$newWidth"

                // Repeat this runnable after a short delay
                binding.decrWbutton.postDelayed(this, 25) // 100ms delay
            }
        }
        binding.decrWbutton.post(decreaseWidthRunnable)
    }

    private fun stopDecreasingWidth() {
        isDecreasing = false
        decreaseWidthRunnable?.let {
            binding.decrWbutton.removeCallbacks(it)
        }
    }
    fun dpToPixels(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }
    //CALCULATION OF DIAMETER
    fun calculateRealWorldSize(context: Context, view: View, screenWidthMeters: Float, screenWidthPixels: Int, distanceToTreeMeters: Float): Float {
        val dpWidth = view.layoutParams.width.toFloat()
        val pixelsWidth = dpToPixels(context, dpWidth)
        val metersPerPixel = screenWidthMeters / screenWidthPixels
        val apparentWidthMeters = pixelsWidth * metersPerPixel
        val realWorldDiameter = apparentWidthMeters * distanceToTreeMeters / screenWidthMeters
        return realWorldDiameter
    }





}


