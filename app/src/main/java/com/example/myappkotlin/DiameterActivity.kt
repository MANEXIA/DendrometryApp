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
import android.view.Surface
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
    private lateinit var leftRightvaltxt: TextView
    private lateinit var sensorM: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var LeftAngle: Float = 0f
    private var RightAngle: Float = 0f

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
        setupSensorStuff()
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

                if (distanceValue == null) {
                    Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val diameterValue = calculateTreeDiameter(LeftAngle, RightAngle, distanceValue)
                val diaMtoInch = diameterValue * 39.3701
                binding.diameterRES.text = "Diamter: ${String.format("%.1f", diaMtoInch)}inch"
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        val density = resources.displayMetrics.density // Screen density

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
//    private var treeDiameterValue: Float = 0f
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

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var yaw: Float = 0f


    private fun setupSensorStuff() {
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
        val tiltX = x / gravity
        val tiltY = y / gravity
        val tiltZ = z / gravity

        // Calculate the pitch angle/inclination angle from accelerometer data
        val pitchAcc = Math.toDegrees(atan2(tiltY.toDouble(), tiltZ.toDouble())).toFloat()
        pitchAngle = alpha * (pitchAngle + pitchGyro) + (1 - alpha) * pitchAcc

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
        this@DiameterActivity.timestamp = event.timestamp

    }



    private fun setLeftAngleValue(){
        LeftAngle = yaw
        leftRightvaltxt.text = "Left: ${String.format("%.1f", LeftAngle)}°\nRight: ${String.format("%.1f", RightAngle)}°"
    }

    private fun setRigtAngleValue(){
        RightAngle = yaw
        leftRightvaltxt.text = "Left: ${String.format("%.1f", LeftAngle)}°\nRight: ${String.format("%.1f", RightAngle)}°"
    }
    private fun calculateTreeDiameter(yawLeft: Float, yawRight: Float, distanceToTree: Float): Double {
        val yawDifference = Math.abs(yawRight - yawLeft)
        return 2 * distanceToTree * Math.tan(Math.toRadians(yawDifference / 2.0))
    }

    private fun updateUI(){
//        angleView.text = "${String.format("%.1f", inclination)}°"

        angleView.text = "Yaw: ${String.format("%.1f", yaw)}°"
    }






}


