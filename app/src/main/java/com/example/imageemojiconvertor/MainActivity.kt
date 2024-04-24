package com.example.imageemojiconvertor

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var emojiConverter: EmojiConverter
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest:CaptureRequest
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var captRequest:CaptureRequest.Builder
    lateinit var imageReader:ImageReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getPermission()

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("CameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)


        imageReader = ImageReader.newInstance(1080,1920,ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(p0: ImageReader?) {

                var image = p0?.acquireLatestImage()
                var buffer = image!!.planes[0].buffer
                var bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)


                var file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"img.jpeg")
                var opstream = FileOutputStream(file)

                opstream.write(bytes)

                opstream.close()
                image.close()


                Toast.makeText(this@MainActivity, "Image captured", Toast.LENGTH_SHORT).show()
            }


        },handler)

        findViewById<Button>(R.id.captureButton).apply {
            setOnClickListener {
                captRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captRequest.addTarget(imageReader.surface)
                cameraCaptureSession.capture(captRequest.build(),null,null)
            }
        }

        textureView.surfaceTextureListener = object :TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                TODO("Not yet implemented")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                //no action needed
            }

        }


        val captureButton : Button = findViewById(R.id.captureButton)


    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0],object :CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
               this@MainActivity.cameraDevice= p0

                var captRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                var surface = Surface(textureView.surfaceTexture)
                captRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface,imageReader.surface),object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                       this@MainActivity.cameraCaptureSession = p0
                        cameraCaptureSession.setRepeatingRequest(captRequest.build(),null,null)

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                },handler)
            }

            override fun onDisconnected(camera: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                TODO("Not yet implemented")
            }

        },handler,)
    }

    private fun getPermission() {
        var permissionList = mutableListOf<String>()

        if(checkSelfPermission(android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)permissionList.add(android.Manifest.permission.CAMERA)
        if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)permissionList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)

        if(permissionList.size>0) requestPermissions(permissionList.toTypedArray(),101)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it!=PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
                getPermission()
            }
        }
    }


}
