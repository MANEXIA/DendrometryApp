    package com.example.myappkotlin


    import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappkotlin.databinding.ActivityHeightBinding
import kotlin.math.atan2
import kotlin.math.sqrt

    //typealias LumaListener = (luma: Double) -> Unit

    class HeightActivity : AppCompatActivity(), SensorEventListener{
        //BINDING ID/THIS IS THE SECOND ACTIVITY XML
        private lateinit var binding: ActivityHeightBinding

        //VARIABLES FOR SENSOR THINGS
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

        //VARIABLES FOR GETTING DIAMETER VALUE FROM DIAMETER ACTIVITY
        private lateinit var resultLauncher: ActivityResultLauncher<Intent>
        private val REQUEST_CODE = 1001
        private var diameterValue = 0.0

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

            //ONCLICK BTN FOR ACTIVITIES
            binding.backBtn.setOnClickListener{
                finish()
            }
            // Check if savedInstanceState is null to avoid adding the fragment multiple times
            if (savedInstanceState == null) {
                val cameraFragment = CameraFragmet() // Replace with your actual Fragment class
                supportFragmentManager.beginTransaction()
                    .replace(R.id.cam_fragment_container, cameraFragment)
                    .commitNow() // Use commitNow to add it synchronously
            }

            //CALL FOR SENSORS AND TEXTVIEWS
            setupSensorStuff()
            angleView = binding.angleTextView
            resText = binding.resultTextview
            treeHeight = binding.heightResult

            binding.bottomBtn.setOnClickListener{ setValueBOTTOM() }
            binding.topBtn.setOnClickListener{ setValueTOP() }

            binding.calBtn.setOnClickListener{
                try {
                    val distanceText = binding.distanceValue.text.toString()

                    if (distanceText.isEmpty()) {
                        Toast.makeText(this, "Please enter a distance value", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val distanceValue = distanceText.toFloatOrNull()

                    if (distanceValue == null || distanceValue <= 0) {
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
               resetsValue()
            }

            //CROSSHAIR CHANGE
            binding.crosshairSwitch.setOnCheckedChangeListener{ _, isChecked ->
                if (isChecked){
                    binding.imageView.setImageResource(R.drawable.blackcrosshair)
                }else{
                    binding.imageView.setImageResource(R.drawable.whitecrosshair)
                }
            }

            // Register ActivityResultLauncher based on API level
            // Register ActivityResultLauncher for all API levels
            resultLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val diameterTxt = result.data?.getStringExtra("diameterValue")
                    Log.d("ETORESULT", "Received diameter value: $diameterTxt")
                    diameterValue = diameterTxt?.toDouble() ?: 0.0
                    binding.DiamterValue.text = "Diameter: ${String.format("%.1f", diameterValue)}cm"
                }
            }

            // Start DiameterActivity
            binding.diameterStartBtn.setOnClickListener {
                val intent3rdAct = Intent(this, DiameterActivity::class.java)
                resultLauncher.launch(intent3rdAct)
            }



        }//END OF ONCREATE FUNCTIONS

        private fun resetsValue() {
            topAngle = 0f
            bottomAngle = 0f
            resText.text = "Top:\nBottom:"
            treeHeight.text = "Height:"
            diameterValue = 0.0
            binding.DiamterValue.text = "Diameter:"
            binding.distanceValue.text.clear()
        }

        // Handle the result when HeightActivity finishes
//        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//            super.onActivityResult(requestCode, resultCode, data)
//
//            // This will only work on API 29 and below
//            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
//                if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//                    val diametertxt = data?.getStringExtra("diameterValue")
//                    Log.d("ETORESULT", "Received diameter value: $diametertxt")
//                    diameterValue = diametertxt?.toDouble()!!
//                    binding.DiamterValue.text = "Diameter: ${String.format("%.1f", diameterValue)}cm"
//                }
//            }
//        }

        @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.let {
                    val diameterTxt = it.getStringExtra("diameterValue")
                    if (diameterTxt != null) {
                        diameterValue = diameterTxt.toDouble()
                        binding.DiamterValue.text = "Diameter: ${String.format("%.1f", diameterValue)} cm"
                    } else {
                        Log.e("Error", "Diameter value is null")
                    }
                } ?: Log.e("Error", "Intent data is null")
            }
        }



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


        private var areSensorsRegistered = false
        override fun onPause() {
            super.onPause()
            if (areSensorsRegistered) {
                sensorM.unregisterListener(this)
                areSensorsRegistered = false
            }
            Log.d("BackDebug", "onPause called, unbinding camera in Height KT")


        }

        override fun onStop() {
            super.onStop()
            // Unregister sensor listeners in both onPause() and onStop() for redundancy
            if (areSensorsRegistered) {
                sensorM.unregisterListener(this)
                areSensorsRegistered = false
            }
        }


    //START FOR BODY SENSORS ACTIVITY
        private fun setupSensorStuff() {
            Log.d("BackDebug", "setupSensorStuff called in  HeightKT")
            if (!areSensorsRegistered) {
                sensorM = getSystemService(Context.SENSOR_SERVICE) as SensorManager

                accelerometer = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //GET TYPE SENSOR ACC
                gyroscope = sensorM.getDefaultSensor(Sensor.TYPE_GYROSCOPE)  //GET TYPE SENSOR GYRO

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

    }














