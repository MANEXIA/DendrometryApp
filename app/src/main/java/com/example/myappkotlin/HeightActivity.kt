package com.example.myappkotlin


import android.annotation.SuppressLint
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
import com.example.myappkotlin.databinding.ActivityHeightBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.sqrt

//typealias LumaListener = (luma: Double) -> Unit

class HeightActivity : AppCompatActivity(), SensorEventListener{
    //BINDING ID/THIS IS THE SECOND ACTIVITY XML
    private lateinit var binding: ActivityHeightBinding

    //CAMERA THINGS
    private lateinit var cameraExecutor: ExecutorService

    //SENSOR THINGS
    private lateinit var sensorM: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var angleView: TextView
    private lateinit var resText: TextView

   //VARIABLES FOR CALCULATIONS TREE HEIGHT
    private var inclination: Float = 0f
    private var bottomAngle: Float = 0f
    private var topAngle: Float = 0f

    private lateinit var treeHeight: TextView

    //STARTING FUNCTION ON CREATE/DISPLAYING APPLICATION AND RUNNING FUNCTIONS
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeightBinding.inflate(layoutInflater)//BINDING XML COMPONENT
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        //ONCLICK BTN FOR GOING BACK TO THE LAST ACTIVITY
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

        //CALL FOR SENSORS AND TEXTVIEWS
        angleView = binding.angleTextView
        resText = binding.resultTextview 
        treeHeight = binding.heightResult


        binding.bottomBtn.setOnClickListener{
             setValueBOTTOM()
        }
        binding.topBtn.setOnClickListener{
             setValueTOP()
        }
        binding.calBtn.setOnClickListener{
//            val distanceValue = binding.distanceValue.text.toString().toFloat()
//            treeHeight.text = "Height: ${String.format("%.1f", calculateTreeHeight(distanceValue, bottomAngle, topAngle))}m"
            try {
                val distanceText = binding.distanceValue.text.toString()

                if (distanceText.isEmpty()) {
                    Toast.makeText(this, "Please enter a distance value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val distanceValue = distanceText.toFloatOrNull()

                if (distanceValue == null) {
                    Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val treeHeightValue = calculateTreeHeight(distanceValue, bottomAngle, topAngle)
                treeHeight.text = "Height: ${String.format("%.1f", treeHeightValue)}m"
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.resetBtn.setOnClickListener{
            topAngle = 0f
            bottomAngle = 0f
            resText.text = "Top: ${String.format("%.1f", topAngle)}°\nBottom: ${String.format("%.1f", bottomAngle)}°"
            treeHeight.text = ""
            binding.distanceValue.text.clear()
        }

        setupSensorStuff()


    }//END OF ONCREATE FUNCTIONS

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

        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(this))
} //END FOR CAMERA ACTIVITY



//START FOR BODY SENSORS ACTIVITY
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
       this@HeightActivity.timestamp = event.timestamp

    }

    private fun updateUI(){
        angleView.text = "${String.format("%.1f", inclination)}°"
    }

    //CALCULATION FOR TREE HEIGHT
    private fun setValueTOP() {
        topAngle = inclination
        resText.text = "Top: ${String.format("%.1f", topAngle)}°\nBottom: ${String.format("%.1f", bottomAngle)}°"
    }
    private fun setValueBOTTOM() {
        bottomAngle = inclination
        resText.text = "Top: ${String.format("%.1f", topAngle)}°\nBottom: ${String.format("%.1f", bottomAngle)}°"
    }

    fun calculateTreeHeight(distance: Float, bottomAngle: Float, topAngle: Float): Double {
        // Convert angles from degrees to radians
        val bottomAngleRad = Math.toRadians(bottomAngle.toDouble())
        val topAngleRad = Math.toRadians(topAngle.toDouble())

        // Calculate heights
        val heightBottom = distance * Math.tan(bottomAngleRad)
        val heightTop = distance * Math.tan(topAngleRad)

        // Calculate tree height
        return heightTop - heightBottom

    }


    override fun onDestroy() {
        sensorM.unregisterListener(this)
        super.onDestroy()
        cameraExecutor.shutdown()

    }









}














