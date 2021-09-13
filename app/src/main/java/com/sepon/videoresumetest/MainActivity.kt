package com.sepon.videoresumetest

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sepon.videoresumetest.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files.createFile
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import android.R
import android.R.attr

import android.content.ContextWrapper
import android.view.View
import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent
import androidx.annotation.RequiresApi
import android.R.attr.data

import android.app.Activity
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import android.content.pm.ActivityInfo

import android.media.MediaRecorder








class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var imagePreview: Preview? = null
    private var videoCapture: VideoCapture? = null
    private lateinit var outputDirectory: File
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var linearZoom = 0f
    private var recording = false
    var sdk = 0
    private val VIDEO_CAPTURE = 101
    var videoUri: Uri? = null


    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

       sdk =  Integer.valueOf(android.os.Build.VERSION.SDK_INT);
        Toast.makeText(this, "SDK : "+sdk, Toast.LENGTH_SHORT).show()

        if (allPermissionsGranted()) {
            lifecycleScope.launch(Dispatchers.IO) {

                startCamera()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        outputDirectory = getOutputDirectory()



        binding.cameraCaptureButton.setOnClickListener {

            dispatchTakeVideoIntent()

            if (sdk > 23){


                if (recording) {
                    videoCapture?.stopRecording()
                    it.isSelected = false
                    recording = false
                } else {
                    lifecycleScope.launch (Dispatchers.IO) {
                        recordVideo()
                    }

                    it.isSelected = true
                    recording = true
                }

            }else{

                Toast.makeText(this@MainActivity, "Old Device!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, VideoCaptureActivity::class.java)
                startActivity(intent)
            }


        }


    }


    private fun dispatchTakeVideoIntent() {

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Companion.REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "DOne", Toast.LENGTH_SHORT).show()
//            val videoUri: Uri = intent.data
//            videoView.setVideoURI(videoUri)
        }
    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
//        super.onActivityResult(requestCode, resultCode, intent)
//        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
////            val videoUri: Uri = intent.data
////            videoView.setVideoURI(videoUri)
//        }
//    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        if (resultCode == RESULT_OK && resultCode == 101) {
            val result: String = attr.data.toString()
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
            // ...
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener({
            imagePreview = Preview.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_16_9)
            }.build()



            videoCapture = VideoCapture.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_16_9)
            }.build()

            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imagePreview,
                // imageAnalysis,
                // imageCapture,
                videoCapture
            )
            binding.previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            imagePreview?.setSurfaceProvider(binding.previewView.surfaceProvider)
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            setZoomStateObserver()
        }, ContextCompat.getMainExecutor(this))
    }



    private fun setZoomStateObserver() {
        cameraInfo?.zoomState?.observe(this, { state ->
            // state.linearZoom
            // state.zoomRatio
            // state.maxZoomRatio
            // state.minZoomRatio
            Log.d(TAG, "${state.linearZoom}")
        })
    }





@SuppressLint("RestrictedApi")
    private fun recordVideo() {

        val file = createFile(
            outputDirectory,
            FILENAME,
            VIDEO_EXTENSION
        )
        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(file).build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        videoCapture?.startRecording(outputFileOptions, cameraExecutor, object : VideoCapture.OnVideoSavedCallback {
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                val msg = "Video capture succeeded: ${file.absolutePath}"

                Log.d(TAG, "Video saved in ${file.absolutePath}")
                binding.previewView.post {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                val msg = "Video capture failed: $message"
                binding.previewView.post {
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                 }
            }
        })
    }



    // Manage camera Zoom
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (linearZoom <= 0.9) {
                    linearZoom += 0.1f
                }
                cameraControl?.setLinearZoom(linearZoom)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (linearZoom >= 0.1) {
                    linearZoom -= 0.1f
                }
                cameraControl?.setLinearZoom(linearZoom)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun getOutputDirectory(): File {
        // TODO: 29/01/2021 Remove externalMediaDirs (deprecated)
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "file").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }




    fun playbackRecordedVideo() {
//        val mVideoView = findViewById<View>(R.id.video_view) as VideoView
//        mVideoView.setVideoURI(videoUri)
//        mVideoView.setMediaController(MediaController(this))
//        mVideoView.requestFocus()
//        mVideoView.start()
    }

    fun startRecordingVideo() {

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            val mediaFile = File(
                Environment.getExternalStorageDirectory().absolutePath + "/myvideo.mp4"
            )
            videoUri = Uri.fromFile(mediaFile)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            startActivityForResult(intent, VIDEO_CAPTURE)
        } else {
            Toast.makeText(this, "No camera on device", Toast.LENGTH_LONG).show()
        }

    }

    companion object {
        private const val TAG = "CameraX"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val VIDEO_EXTENSION2 = ".MPEG4"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)


        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )

        const val REQUEST_VIDEO_CAPTURE = 1
    }



}