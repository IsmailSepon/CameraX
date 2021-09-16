package com.sepon.videoresumetest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File

class Video_record : AppCompatActivity() {

    private val REQUEST_ID_READ_WRITE_PERMISSION = 99
    private val REQUEST_ID_IMAGE_CAPTURE = 100
    private val REQUEST_ID_VIDEO_CAPTURE = 101
    lateinit var   videoView : VideoView
    lateinit var   button : Button
    lateinit var   imageView : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_capture)


        button = findViewById(R.id.button_video)
        imageView = findViewById(R.id.imageView)
        videoView = findViewById(R.id.videoView)


        button.setOnClickListener(View.OnClickListener {

            askPermissionAndCaptureVideo()
        })


    }


    private fun askPermissionAndCaptureVideo() {

        // With Android Level >= 23, you have to ask the user
        // for permission to read/write data on the device.
        if (Build.VERSION.SDK_INT >= 23) {

            // Check if we have read/write permission
            val readPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED
            ) {
                // If don't have permission so prompt the user.
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), REQUEST_ID_READ_WRITE_PERMISSION
                )
                return
            }
        }
        captureVideo()
    }

    private fun captureVideo() {
        try {
            // Create an implicit intent, for video capture.
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

            // The external storage directory.
            val dir = Environment.getExternalStorageDirectory()
            if (!dir.exists()) {
                dir.mkdirs()
            }
            // file:///storage/emulated/0/myvideo.mp4
            val savePath = dir.absolutePath + "/myvideo.mp4"
            val videoFile = File(savePath)
            val videoUri = Uri.fromFile(videoFile)

            // Specify where to save video files.
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val builder = VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            this.startActivityForResult(
                intent,
                REQUEST_ID_VIDEO_CAPTURE
            ) // (**)
        } catch (e: Exception) {
            Toast.makeText(this, "Error capture video: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_ID_READ_WRITE_PERMISSION -> {

                if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show()
                    captureVideo()
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ID_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                val bp = data!!.extras!!["data"] as Bitmap?
                imageView.setImageBitmap(bp)
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Action canceled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Action Failed", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == REQUEST_ID_VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK) {
                val videoUri = data!!.data
                Log.i("MyLog", "Video saved to: $videoUri")
                Toast.makeText(
                    this, """
     Video saved to:
     $videoUri
     """.trimIndent(), Toast.LENGTH_LONG
                ).show()
                videoView.setVideoURI(videoUri)
                videoView.start()
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(
                    this, "Action Cancelled.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this, "Action Failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}