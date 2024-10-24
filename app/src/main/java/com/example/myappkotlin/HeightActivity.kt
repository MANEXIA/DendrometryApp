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
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappkotlin.databinding.ActivityHeightBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

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

        private var treeHeightValue = 0.0
        private var diameterValue = 0.0
        private var volumeValue = 0.0


        //DATABASE
        private lateinit var db: ClassificationDatabaseHelper

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

            binding.bottomBtn.setOnClickListener{
                val distanceText = binding.distanceValue.text.toString()
                // Validate distance input using checkDistance
                val distanceValue = checkDistance(distanceText) ?: return@setOnClickListener
                // If distance is valid, call setValueBOTTOM
                setValueBOTTOM()
                if(topAngle == 0f){
                    //Toast.makeText(this, "Please Set Top Angle", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }else{
                    treeHeightValue = calculateTreeHeight(distanceValue, bottomAngle, topAngle)
                    treeHeight.text = "Height: ${String.format(Locale.US,"%.1f", treeHeightValue)}m"
                }
            }

            binding.topBtn.setOnClickListener{
                val distanceText = binding.distanceValue.text.toString()
                // Validate distance input using checkDistance
                val distanceValue = checkDistance(distanceText) ?: return@setOnClickListener
                // If distance is valid, call setValueTOP
                setValueTOP()
                if(bottomAngle == 0f){
                    //Toast.makeText(this, "Please Set Bottom Angle", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }else{
                    treeHeightValue = calculateTreeHeight(distanceValue, bottomAngle, topAngle)
                    treeHeight.text = "Height: ${String.format(Locale.US,"%.1f", treeHeightValue)}m"
                }

            }

            binding.calBtn.setOnClickListener{
                Log.d("setHEIGHTndDIAMATER", "H: $treeHeightValue D: $diameterValue")
                if (treeHeightValue != 0.0 && diameterValue != 0.0) {
                    if (treeHeightValue > 0 && diameterValue > 0) {
                        // Proceed to calculation using the formula
                        val diameterInMeters = diameterValue / 100 // Convert diameter to meters
                        volumeValue = 0.7854 * (treeHeightValue / 2) * (diameterInMeters * diameterInMeters)
                        // Display the calculated volume in a TextView
                        binding.volumeResult.text = "Volume: ${String.format(Locale.US,"%.1f", volumeValue)}m³"
                        binding.ViewClass.visibility = View.VISIBLE
                    } else {
                        // Handle invalid or zero values
                        Toast.makeText(this, "Please enter valid non-zero height and diameter", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle empty input fields
                    Toast.makeText(this, "Please fill in both height and diameter", Toast.LENGTH_SHORT).show()
                }

            }


            binding.ViewClass.setOnClickListener(){
                // Check if holdDiameter is still 0.0
                if (volumeValue == 0.0) {
                    // Optional: Show a message to the user indicating that the value cannot be 0.0
                    Toast.makeText(this, "Please Calculate Volume First", Toast.LENGTH_SHORT).show()
                } else {
                    showCurvedAlertDialog()
                }
            }


            binding.resetBtn.setOnClickListener{
               resetsValue()
            }


            //SWITCH BUTTONS VISIBILITY
            val visibilityBottomBtn = binding.bottomBtn
            val visibilityTopBtn = binding.topBtn
            binding.arrowButtonRight.setOnClickListener {
                if (visibilityBottomBtn.visibility == View.VISIBLE) {
                    toggleVisibility(visibilityTopBtn, visibilityBottomBtn)
                } else {
                    toggleVisibility(visibilityBottomBtn, visibilityTopBtn)
                }
            }

            binding.arrowButtonLeft.setOnClickListener {
                if (visibilityTopBtn.visibility == View.VISIBLE) {
                    toggleVisibility(visibilityBottomBtn, visibilityTopBtn)
                } else {
                    toggleVisibility(visibilityTopBtn, visibilityBottomBtn)
                }
            }

            //Register ActivityResultLauncher based on API level
            //Register ActivityResultLauncher for all API levels
            resultLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val diameterTxt = result.data?.getStringExtra("diameterValue")
                    Log.d("ETORESULT", "Received diameter value: $diameterTxt")
                    diameterValue = diameterTxt?.toDouble() ?: 0.0
                    binding.DiamterValue.text = "Diameter: ${String.format(Locale.US,"%.1f", diameterValue)}cm"
                }
            }

            // Start DiameterActivity
            binding.diameterStartBtn.setOnClickListener {
                val intent3rdAct = Intent(this, DiameterActivity::class.java)
                resultLauncher.launch(intent3rdAct)
            }

            db = ClassificationDatabaseHelper(this)

        }//END OF ONCREATE FUNCTIONS

        //SHOW CALCULATION FOR VOLUME
        @SuppressLint("SetTextI18n")
        private fun showCurvedAlertDialog(){
         val dialog: AlertDialog = MaterialAlertDialogBuilder(this, R.style.RoundedMaterialDialog)
             .setView(R.layout.volume_dialog).show()

             //DATA TO SAVE IN SQLITE DATABASE
             //HEIGHT, DIAMETER, VOLUME, DIAMETER CLASS, DATE
            // Get current date and time
            val currentDateTime = LocalDateTime.now()
            // Define a date-time formatter to format the output
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss") // Customize the format as needed

         var diameterClass : String = ""
         if(diameterValue in 1.0..30.0){
             dialog.findViewById<TextView>(R.id.diameterClass)?.text = getString(R.string.dclass_small_tree)
             diameterClass = "Small Tree"
         }else if(diameterValue in 30.0..60.0){
             dialog.findViewById<TextView>(R.id.diameterClass)?.text = getString(R.string.dclass_medium_sized_tree)
             diameterClass = "Medium-sized Tree"
         }else if(diameterValue > 60.0){
             dialog.findViewById<TextView>(R.id.diameterClass)?.text = getString(R.string.dclass_large_tree)
             diameterClass = "Large Tree"
         }

         dialog.findViewById<TextView>(R.id.heightResult)?.text = "Height: ${String.format(Locale.US,"%.1f", treeHeightValue)}m"
         dialog.findViewById<TextView>(R.id.diameterResult)?.text = "Diameter: ${String.format(Locale.US,"%.1f", diameterValue)}cm"
         dialog.findViewById<TextView>(R.id.volumeResult)?.text = "Volume: ${String.format(Locale.US,"%.1f", volumeValue)}m³"
         dialog.findViewById<TextView>(R.id.dateVolumeClass)?.text = currentDateTime.format(formatter)

         dialog.findViewById<View>(R.id.closeDialog)?.setOnClickListener{
             dialog.dismiss()
         }

         dialog.findViewById<View>(R.id.addClassification)?.setOnClickListener{
               val height = treeHeightValue
               val diameter = diameterValue
               val volume = volumeValue
               val diametersize = diameterClass
               val date = currentDateTime.format(formatter)
               val data = DataClassification(0, height, diameter, volume, diametersize, date)
               db.insertClassification(data)
               dialog.dismiss()
               Toast.makeText(this, "Classification Added", Toast.LENGTH_SHORT).show()
         }

        }

        private fun checkDistance(distanceText: String): Float? {
            // Check if distance input is empty
            if (distanceText.isEmpty()) {
                Toast.makeText(this, "Please enter a distance value", Toast.LENGTH_SHORT).show()
                return null
            }

            // Convert distance input to float
            val distanceValue = distanceText.toFloatOrNull()

            // Validate if the converted value is not null and greater than 0
            if (distanceValue == null || distanceValue <= 0) {
                Toast.makeText(this, "Invalid distance value", Toast.LENGTH_SHORT).show()
                return null
            }

            // Return the valid distance value
            return distanceValue
        }

        @SuppressLint("SetTextI18n")
        private fun resetsValue() {
            topAngle = 0f
            bottomAngle = 0f
            resText.text = "Top:\nBottom:"
            treeHeight.text = "Height:"
            diameterValue = 0.0
            treeHeightValue = 0.0
            volumeValue = 0.0
            binding.DiamterValue.text = "Diameter:"
            binding.volumeResult.text = "Volume:"
            binding.distanceValue.text.clear()
        }

        fun toggleVisibility(btnToShow: View, btnToHide: View) {
            btnToShow.visibility = View.VISIBLE
            btnToHide.visibility = View.GONE
        }

        // Handle the result when HeightActivity finishes
        @SuppressLint("SetTextI18n")
        @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.let {
                    val diameterTxt = it.getStringExtra("diameterValue")
                    if (diameterTxt != null) {
                        diameterValue = diameterTxt.toDouble()
                        binding.DiamterValue.text = "Diameter: ${String.format(Locale.US,"%.1f", diameterValue)} cm"
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

    @SuppressLint("SetTextI18n")
    private fun updateUI(){
            angleView.text = "${String.format(Locale.US,"%.1f", inclination)}°"
        }

        //CALCULATION FOR TREE HEIGHT
    @SuppressLint("SetTextI18n")
    private fun setValueTOP() {
            topAngle = inclination
            resText.text = "Top: ${String.format(Locale.US,"%.1f", topAngle)}°\nBottom: ${String.format(Locale.US,"%.1f", bottomAngle)}°"
        }
    @SuppressLint("SetTextI18n")
    private fun setValueBOTTOM() {
            bottomAngle = inclination
            resText.text = "Top: ${String.format(Locale.US,"%.1f", topAngle)}°\nBottom: ${String.format(Locale.US,"%.1f", bottomAngle)}°"
        }

    private fun calculateTreeHeight(distance: Float, bottomAngle: Float, topAngle: Float): Double {
            // Convert angles from degrees to radians
            val bottomAngleRad = Math.toRadians(bottomAngle.toDouble())
            val topAngleRad = Math.toRadians(topAngle.toDouble())

            // Calculate heights
            val heightBottom = distance * tan(bottomAngleRad)
            val heightTop = distance * tan(topAngleRad)

            // Calculate tree height
            return heightTop - heightBottom

        }

    }














