package com.example.myappkotlin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
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

    private lateinit var treeDiameterView:TextView
    private lateinit var distanceValue: EditText

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
                    if (checkDistanceValue()) {
                        startIncreasingWidth()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (checkDistanceValue()) {
                        stopIncreasingWidth()
                    }
                    true
                }
                else -> false
            }

        }

        binding.decrWbutton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkDistanceValue()) {
                        startDecreasingWidth()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (checkDistanceValue()) {
                        stopDecreasingWidth()
                    }
                    true
                }
                else -> false
            }

        }

        treeDiameterView = binding.textWidth



        setupSensorStuff()
    }//END OF ONCREATE FUNCTON


    private fun checkDistanceValue(): Boolean {
        return try {
            val distanceText = binding.distanceValue.text.toString().trim()
            if (distanceText.isEmpty()) {
                Toast.makeText(this, "Please enter a distance value", Toast.LENGTH_SHORT).show()
                return false
            }

            val distanceValue = distanceText.toFloatOrNull()

            if (distanceValue == null) {
                Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show()
                return false
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
    //STARTING PERMISSION FOR CAMERA AND OTHERS

    @OptIn(ExperimentalCamera2Interop::class)
    fun getCameraFov(context: Context, cameraSelector: CameraSelector, callback: (Float?) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Select camera (front or back, depending on your use case)
            val camera = cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector)

            // Get Camera2 CameraInfo
            val camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)

            // Get the CameraCharacteristics
            val cameraCharacteristics = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val sensorSize = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

            if (cameraCharacteristics != null && sensorSize != null) {
                // Focal length (in millimeters)
                val focalLength = cameraCharacteristics[0]

                // Sensor size (in millimeters)
                val sensorWidth = sensorSize.width

                // Calculate the horizontal FOV (in degrees)
                val fov = Math.toDegrees(2 * Math.atan((sensorWidth / (2 * focalLength)).toDouble())).toFloat()

                callback(fov) // Return the FOV using the callback
            } else {
                callback(null) // FOV could not be retrieved
            }

        }, ContextCompat.getMainExecutor(context))
    }


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
    private var treeDiameterValue: Float = 0f
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


    private fun updateDiameterText(diameter: Float) {
       binding.diameterText.text = "${String.format("%.4f", diameter)}m"
   }

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
                // Repeat this runnable after a short delay
                binding.addWbutton.postDelayed(this, 25) // 100ms delay



                //UPDATING UI FOR DIAMTER COMPONENT
                val screenWidthMeters = 0.076f // Example: 6.8 cm screen width
                val screenWidthPixels = resources.displayMetrics.heightPixels
                val distanceToTreeMeters = binding.distanceValue.text.toString().toFloat() // Replace with the actual distance to the tree
//                treeDiameterValue = calculateRealWorldSize(this@DiameterActivity, treeDiameterView,  screenWidthPixels, distanceToTreeMeters)
                getCameraFov(this@DiameterActivity, CameraSelector.DEFAULT_BACK_CAMERA) { fovDegrees ->
                    if (fovDegrees != null) {
                        // Once we have the FOV, calculate the real-world size
                        val screenWidthPixels =
                            getScreenWidthInMeters(this@DiameterActivity) // Assuming portrait mode

                        // Calculate the real-world diameter using the FOV
                        val realWorldDiameter = calculateRealWorldSizeWithFOV(
                            context = this@DiameterActivity,
                            view = binding.textWidth, // The view width component
                            distanceToTreeMeters = distanceToTreeMeters,
                            fovDegrees = fovDegrees
                        )

                        // Update the diameter text or any other UI component
                        Log.d("FOV Value", "FOV Degrees: $fovDegrees")
                        updateDiameterText(realWorldDiameter)
                    }
                }

//                updateDiameterText()


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
                // Repeat this runnable after a short delay
                binding.decrWbutton.postDelayed(this, 25) // 100ms delay

                //UPDATING UI FOR DIAMTER COMPONENT
                val screenWidthMeters = 0.068f // Example: 6.8 cm screen width
                val screenWidthPixels = resources.displayMetrics.heightPixels
                val distanceToTreeMeters = binding.distanceValue.text.toString().toFloat() // Replace with the actual distance to the tree
//                treeDiameterValue = calculateRealWorldSize(this@DiameterActivity, treeDiameterView, screenWidthPixels, distanceToTreeMeters)
                getCameraFov(this@DiameterActivity, CameraSelector.DEFAULT_BACK_CAMERA) { fovDegrees ->
                    if (fovDegrees != null) {
                        // Once we have the FOV, calculate the real-world size
                        val screenWidthPixels =
                            getScreenWidthInMeters(this@DiameterActivity)  // Assuming portrait mode

                        // Calculate the real-world diameter using the FOV
                        val realWorldDiameter = calculateRealWorldSizeWithFOV(
                            context = this@DiameterActivity,
                            view = binding.textWidth, // The view width component
                            distanceToTreeMeters = distanceToTreeMeters,
                            fovDegrees = fovDegrees
                        )

                        // Update the diameter text or any other UI component
                        Log.d("FOV Value", "FOV Degrees: $fovDegrees")

                        updateDiameterText(realWorldDiameter)
                    }
                }


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


    fun calculateRealWorldSizeWithFOV(
        context: Context,
        view: View,
        distanceToTreeMeters: Float,
        fovDegrees: Float
    ): Float {
        // Log input data
        Log.d("data-get", "distanceToTreeMeters: $distanceToTreeMeters")
        Log.d("data-get", "fovDegrees: $fovDegrees")

        // Get the width of the view in dp and convert it to pixels
        val dpWidth = view.layoutParams.width.toFloat()
        val pixelsWidth = dpToPixels(context, dpWidth)

        // Convert FOV from degrees to radians
        val fovRadians = Math.toRadians(fovDegrees.toDouble())

        // Get the screen width in meters using the helper function
        val screenWidthMeters = getScreenWidthInMeters(context)
        Log.d("data-get", "Screen Width in meters: $screenWidthMeters")

        // Calculate the screen width in meters based on the distance to the tree and FOV
        val calculatedScreenWidthMeters = 2 * distanceToTreeMeters * Math.tan(fovRadians / 2)
        Log.d("Calculated Screen Width", "Calculated Screen Width in meters: $calculatedScreenWidthMeters")

        // Calculate meters per pixel using the calculated screen width
        val screenWidthPixels = context.resources.displayMetrics.widthPixels.toFloat()
        val metersPerPixel = calculatedScreenWidthMeters / screenWidthPixels
        Log.d("Meters Per Pixel", "Meters Per Pixel: $metersPerPixel")

        // Calculate the apparent width in meters (real-world diameter of the tree)
        val apparentWidthMeters = pixelsWidth * metersPerPixel
        Log.d("Apparent Width", "Apparent Width in meters: $apparentWidthMeters")

        // Return the apparent width as the real-world diameter of the tree
        return apparentWidthMeters.toFloat()
    }

    // Convert dp to pixels
    private fun dpToPixels(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    // Helper function to get screen width in meters
    fun getScreenWidthInMeters(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthPixels = displayMetrics.widthPixels
        val screenDensityDpi = displayMetrics.densityDpi

        // Calculate  screen width in inches
        val screenWidthInches = screenWidthPixels / screenDensityDpi.toFloat()

        // Convert inches to meters (1 inch = 0.0254 meters)
        return screenWidthInches * 0.0254f
    }



}


