package com.example.imageemojiconvertor

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice: CameraDevice
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var captRequest: CaptureRequest.Builder
    lateinit var imageReader: ImageReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getPermission()

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("CameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader?) {
                val image = reader?.acquireLatestImage()
                val buffer = image!!.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "img.jpeg")
                val outputStream = FileOutputStream(file)
                outputStream.write(bytes)
                outputStream.close()
                image.close()

                // Convert the captured image to a sticker
                val bitmap = BitmapFactory.decodeFile(file.path)
                val stickerBitmap: Bitmap = convertToSticker(bitmap)

                // Display the sticker image or do further processing as needed
                stickerBitmap.let {
                    saveImage(it)

                    val emojiBitmap = Emojifier.detectFacesandOverlayEmoji(this@MainActivity,it)

                    displayEmoji(emojiBitmap)
                   // shareImage(it)  to share the image
                }

                Toast.makeText(this@MainActivity, "Image captured and converted to sticker", Toast.LENGTH_SHORT).show()
            }
        }, handler)

        findViewById<Button>(R.id.captureButton).apply {
            setOnClickListener {
                captRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captRequest.addTarget(imageReader.surface)
                cameraCaptureSession.capture(captRequest.build(), null, null)
            }
        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
                // No implementation needed
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // No implementation needed
            }
        }
    }

    private fun displayEmoji(emojiBitmap: Bitmap) {

        val intent = Intent(this@MainActivity,DisplayImageActivity::class.java).apply {
            putExtra("emoji_bitmap", emojiBitmap)
        }

        startActivity(intent)


    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                this@MainActivity.cameraDevice = p0

                val captRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                val surface = Surface(textureView.surfaceTexture)
                captRequest.addTarget(surface)

                cameraDevice.createCaptureSession(
                    listOf(surface, imageReader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(p0: CameraCaptureSession) {
                            this@MainActivity.cameraCaptureSession = p0
                            cameraCaptureSession.setRepeatingRequest(captRequest.build(), null, null)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            // No implementation needed
                        }
                    },
                    handler
                )
            }

            override fun onDisconnected(camera: CameraDevice) {
                // No implementation needed
            }

            override fun onError(camera: CameraDevice, error: Int) {
                // No implementation needed
            }
        }, handler)
    }

    private fun getPermission() {
        val permissionList = mutableListOf<String>()

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.CAMERA
        )
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (permissionList.size > 0) requestPermissions(permissionList.toTypedArray(), 101)
    }

    private fun convertToSticker(bitmap: Bitmap): Bitmap {
        // Example of adding a border to the bitmap to create a sticker effect
        val stickerWidth = bitmap.width
        val stickerHeight = bitmap.height

        val stickerBitmap = Bitmap.createBitmap(stickerWidth, stickerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stickerBitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawRect(0f, 0f, stickerWidth.toFloat(), stickerHeight.toFloat(), borderPaint)

        return stickerBitmap
    }

    private fun saveImage(bitmap: Bitmap) {
        // Resize the bitmap to reduce its size
        val resizedBitmap = resizeBitmap(bitmap, 800) // Specify the desired width here (e.g., 800px)

        // Compress and save the resized bitmap
        val bytes = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
        val wallpaperDirectory = File("${Environment.getExternalStorageDirectory()}/Stickers/")
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }

        try {
            val f = File(wallpaperDirectory, "sticker.png")
            f.createNewFile()
            val fo: OutputStream = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            fo.close()
            Toast.makeText(this, "Sticker saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareImage(bitmap: Bitmap) {
        // Resize the bitmap to reduce its size
        val resizedBitmap = resizeBitmap(bitmap, 800) // Specify the desired width here (e.g., 800px)

        // Save the resized bitmap to external storage
        val bytes = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
        val wallpaperDirectory = File("${Environment.getExternalStorageDirectory()}/Stickers/")
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }

        try {
            val f = File(wallpaperDirectory, "sticker.png")
            f.createNewFile()
            val fo: OutputStream = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            fo.close()

            // Create a content URI for the saved image file
            val uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                f
            )

            // Create a share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Launch the share activity
            startActivity(Intent.createChooser(shareIntent, "Share Image"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = maxWidth.toFloat() / width
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
    }


}
