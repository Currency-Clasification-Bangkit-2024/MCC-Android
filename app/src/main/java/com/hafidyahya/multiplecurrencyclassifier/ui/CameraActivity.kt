package com.hafidyahya.multiplecurrencyclassifier.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import com.hafidyahya.multiplecurrencyclassifier.R
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Inisialisasi PreviewView
        previewView = findViewById(R.id.cameraPreview)

        // Executor untuk operasi background
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Mulai kamera
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Dapatkan CameraProvider
            val cameraProvider = cameraProviderFuture.get()

            // Konfigurasi Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Konfigurasi ImageAnalysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { bitmap ->
                        // Di sini Anda bisa mengolah Bitmap, misalnya mengirim ke API
                        Log.d("CameraX", "Bitmap dihasilkan: ${bitmap.width}x${bitmap.height}")
                    })
                }

            try {
                // Bind use case ke lifecycle
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA, // Kamera belakang
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding gagal", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Pastikan executor dihentikan
    }

    // Analyzer untuk mengolah frame kamera
    class ImageAnalyzer(private val onFrameAnalyzed: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            // Ubah ImageProxy menjadi Bitmap
            val bitmap = imageProxy.toBitmap()

            // Kirimkan bitmap ke callback
            onFrameAnalyzed(bitmap)

            // Tutup frame setelah selesai diproses
            imageProxy.close()
        }

        // Ekstensi untuk mengonversi ImageProxy ke Bitmap
        private fun ImageProxy.toBitmap(): Bitmap {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val yuvImage = YuvImage(bytes, ImageFormat.NV21, width, height, null)

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
            val byteArray = out.toByteArray()

            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }
}
