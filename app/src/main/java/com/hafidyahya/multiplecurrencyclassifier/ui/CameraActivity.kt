package com.hafidyahya.multiplecurrencyclassifier.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.hafidyahya.multiplecurrencyclassifier.R
import com.hafidyahya.multiplecurrencyclassifier.data.ApiResponse
import com.hafidyahya.multiplecurrencyclassifier.data.RetrofitInstance
import com.hafidyahya.multiplecurrencyclassifier.databinding.ActivityCameraBinding
import com.hafidyahya.multiplecurrencyclassifier.utils.convertBitmapToFile
import com.hafidyahya.multiplecurrencyclassifier.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false
    private var camera: Camera? = null
    private lateinit var tts: TextToSpeech // TTS instance
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        previewView = findViewById(R.id.cameraPreview)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupFlashSwitch()
        setupViewModelObservers()
        initializeTTS()
        checkAndPromptTtsSettings()
        startCamera()

        // Tombol kembali
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupFlashSwitch() {
        // Setel status awal Switch
        binding.flashSwitch.isChecked = viewModel.isFlashOn.value ?: false

        binding.flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFlashStatus(isChecked) // Perbarui status di ViewModel
        }
    }

    private fun setupViewModelObservers() {
        // Pantau status flash di ViewModel
        viewModel.isFlashOn.observe(this, Observer { isFlashOn ->
            updateFlashUI(isFlashOn)
        })
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = tts.setLanguage(Locale("id", "ID"))
                if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Bahasa Indonesia tidak didukung")
                } else {
                    Log.d("TTS", "TTS siap digunakan dengan Bahasa Indonesia")
                }
            } else {
                Log.e("TTS", "Inisialisasi TTS gagal")
            }
        }
    }

    private fun checkAndPromptTtsSettings() {
        val defaultEngine = android.provider.Settings.Secure.getString(
            contentResolver,
            "tts_default_synth"
        ) ?: "Unknown"

        if (defaultEngine != "com.google.android.tts") {
            Log.w("TTS", "Mesin TTS default bukan Google TTS: $defaultEngine")

            // Tampilkan dialog untuk mengarahkan pengguna ke pengaturan TTS
            runOnUiThread {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Gunakan Google TTS?")
                    .setMessage("Aplikasi ini lebih optimal dengan mesin Google Text-to-Speech. Jika tidak, mungkin beberapa bahasa tidak mengeluarkan suara. Dan mulai ulang aplikasi setelah pengaturan diubah. Apakah Anda ingin mengatur Google TTS sebagai default?")
                    .setPositiveButton("Ya") { _, _ -> openTtsSettings() }
                    .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun openTtsSettings() {
        val intent = Intent().apply {
            action = "com.android.settings.TTS_SETTINGS"
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("TTS", "Tidak dapat membuka pengaturan TTS", e)
            Toast.makeText(this, "Buka pengaturan TTS secara manual melalui Pengaturan > Bahasa & Masukan.", Toast.LENGTH_LONG).show()
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { bitmap ->
<<<<<<< HEAD
                        if (!isProcessing) {
                            isProcessing = true
                            processImage(bitmap)
                        }
=======
                        Log.d("CameraX", "Bitmap dihasilkan: ${bitmap.width}x${bitmap.height}")
>>>>>>> 65ca5b224f8d966a8d6302f7d8e47e344788a7ba
                    })
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )

                // Sinkronkan status flash saat kamera dimulai
                camera?.cameraControl?.enableTorch(viewModel.isFlashOn.value ?: false)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding gagal", exc)
                Toast.makeText(this, "Gagal memulai kamera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            uploadImage(bitmap)
            isProcessing = false
        }
    }

    private fun uploadImage(bitmap: Bitmap) {
        val file = convertBitmapToFile(bitmap, cacheDir, "temp_image.jpg")
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

        val apiService = RetrofitInstance.apiService
        apiService.predictCurrency(filePart).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val seribu = "Seribu"
                    val hasil = response.body()?.hasilPrediksi
                    val confidenceScoreFromApi = response.body()?.confidenceScore
                    val confidence = confidenceScoreFromApi as Double
                    if(confidence != null && confidence >= 80.0 && hasil != null){
                        binding.detectionResult.text = "Rp. ${hasil.toString()}"
                        if (hasil == "1ribu"){
                            speakResult(seribu)
                        }else{
                            hasil?.let {
                                speakResult(it)
                            } ?: Log.e("TTS", "Hasil prediksi kosong")
                        }
                    }

                } else {
                    Log.e("API Error", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("API Failure", "Gagal memanggil API: ${t.message}")
            }
        })
    }

    private fun speakResult(result: String) {
        if (::tts.isInitialized && result.isNotBlank()) {
            Log.d("TTS", "Memulai TTS dengan hasil: $result")
            tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TTS", "TTS tidak diinisialisasi atau hasil kosong")
        }
    }

    private fun updateFlashUI(isFlashOn: Boolean) {
        if (isFlashOn) {
            binding.flashIcon.setImageResource(R.drawable.baseline_flash_on_24)
        } else {
            binding.flashIcon.setImageResource(R.drawable.baseline_flash_off_24)
        }

        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraExecutor.shutdown()
            if (::tts.isInitialized) {
                tts.stop()
                tts.shutdown()
            }
        } catch (e: Exception) {
            Log.e("Cleanup", "Kesalahan saat membersihkan sumber daya", e)
        }
    }


    class ImageAnalyzer(private val onFrameAnalyzed: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val bitmap = imageProxy.toBitmap()
            onFrameAnalyzed(bitmap)
            imageProxy.close()
        }
    }
}
