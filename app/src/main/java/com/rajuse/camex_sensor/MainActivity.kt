package com.rajuse.camex_sensor

import VideoListAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.rajuse.camex_sensor.AlerUtils.Companion.showAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var transparentAlert: TextView
    private lateinit var previewView: PreviewView
    private lateinit var recordButton: ImageView
    private lateinit var timerText: TextView
    private lateinit var gallery: ImageButton
    private lateinit var videoList: RecyclerView
    private lateinit var btnClose: ImageButton
    private lateinit var videoContainer: LinearLayout

    private var isRecording = false
    private var seconds = 0

    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraExecutor: ExecutorService
    lateinit var videoListAdapter: VideoListAdapter

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor


    lateinit var mainViewModel: MainViewModel


    @SuppressLint("MissingInflatedId", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_camera)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        previewView = findViewById(R.id.previewView)
        recordButton = findViewById(R.id.recordButton)
        timerText = findViewById(R.id.timerText)
        videoList = findViewById(R.id.videoList)
        gallery = findViewById(R.id.gallery)


        videoContainer = findViewById(R.id.videoContainer)
        btnClose = findViewById(R.id.btnClose)
        createTransparentAlert()



        btnClose.setOnClickListener {
            hideVideoList()
        }

        gallery.setOnClickListener {
            showVideoList()
        }

        videoListAdapter = VideoListAdapter(emptyList(),
            onVideoItemClick = {
                hideVideoList()
            })

        videoList.hasFixedSize()
        videoList.adapter = videoListAdapter

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        cameraExecutor = Executors.newSingleThreadExecutor()

        requestCameraPermissions()

        recordButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

    }

    private fun createTransparentAlert() {
        transparentAlert = TextView(this).apply {
            text = context.getString(R.string.wrong_direction_detected)
            textSize = 30f
            setTextColor(Color.GREEN)
            setBackgroundColor(Color.argb(100, 0, 0, 0)) // Transparent background
            visibility = TextView.GONE // Initially hidden
            gravity = Gravity.CENTER
        }

        // Add the alert view to the layout
        addContentView(
            transparentAlert,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun hideVideoList() {
        videoContainer.visibility = View.GONE
    }

    private fun showVideoList() {
        videoContainer.visibility = View.VISIBLE
        loadSavedVideos()
    }

    private fun requestCameraPermissions() {
        val cameraPermission = Manifest.permission.CAMERA
        val recordAudioPermission = Manifest.permission.RECORD_AUDIO

        if (ContextCompat.checkSelfPermission(
                this,
                cameraPermission
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                recordAudioPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions.launch(arrayOf(cameraPermission, recordAudioPermission))
        } else {
            startCamera()
        }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera and Audio permissions are required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Set up the video capture use case
            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    var rec: Recording? = null

    @SuppressLint("CheckResult")
    private fun startRecording() {
        isRecording = true
        recordButton.setImageDrawable(resources.getDrawable(R.drawable.iconstop, theme))
        startTimer()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        val videoFile = FileUtils.createVideoFile(this)

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        rec = videoCapture.output
            .prepareRecording(this, outputOptions)
            .start(
                cameraExecutor,
                (Consumer<VideoRecordEvent> { value ->

                    if (value is Finalize) {

                        if (!value.hasError())
                            loadSavedVideos()
                    }
                })
            )
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.setImageDrawable(resources.getDrawable(R.drawable.iconrecord, theme))
        stopTimer()
        sensorManager.unregisterListener(this)
        rec?.stop()
    }


    @SuppressLint("DefaultLocale")
    private fun startTimer() {
        lifecycleScope.launch {
            while (isRecording && seconds < 30) {
                delay(1000L)
                seconds++
                if (isRecording)
                    String.format("%02d:%02d", seconds / 60, seconds % 60)
                        .also { timerText.text = it }
            }
            if (seconds >= 30) stopRecording() // Stop after 30 seconds
        }
    }

    private fun stopTimer() {
        seconds = 0
        timerText.text = getString(R.string._00_00)
    }


    private fun loadSavedVideos() {

        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        val outputDir = if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
        val savedVideos = outputDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()

        runOnUiThread {
            videoContainer.visibility = View.VISIBLE
            println("called $savedVideos")
            println("size ${savedVideos.size}")
            videoListAdapter.submitList(savedVideos)
            Handler(Looper.getMainLooper()).postDelayed({
                if (savedVideos.size > 5)
                    videoList.smoothScrollToPosition(savedVideos.size - 1)
            }, 500)


        }
    }


    @SuppressLint("SuspiciousIndentation")
    override fun onSensorChanged(event: SensorEvent?) {

        mainViewModel.onSensorChanged(event, onMoveFastDetect = {
            println("MOVINGFAST $isRecording")
            if (isRecording) {
                runOnUiThread {
                    showAlert(
                        context = this,
                        message = "Please move device with slow speed while recording"
                    )
                }
            }
        })

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            if(isRecording)
            detectDirection(event.values)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private var isMovingInCorrectDirection = true

    private fun detectDirection(values: FloatArray) {
        val x = values[0]
        val y = values[1]

        // If user is moving in the opposite direction, show alert
        if (x > 0 && y < 0) { // This would indicate reverse movement (bottom-left to top-right)
            if (isMovingInCorrectDirection) {
                transparentAlert.visibility = View.VISIBLE
                isMovingInCorrectDirection = false
            }
        } else {
            isMovingInCorrectDirection = true
            transparentAlert.visibility = TextView.GONE
        }
    }
}


