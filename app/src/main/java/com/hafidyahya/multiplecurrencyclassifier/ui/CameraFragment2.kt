package com.hafidyahya.multiplecurrencyclassifier.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.hafidyahya.multiplecurrencyclassifier.R
import com.hafidyahya.multiplecurrencyclassifier.data.Api2Response
import com.hafidyahya.multiplecurrencyclassifier.data.Retrofit2Instance
import com.hafidyahya.multiplecurrencyclassifier.databinding.FragmentCamera2Binding
import com.hafidyahya.multiplecurrencyclassifier.utils.convertBitmapToFile
import com.hafidyahya.multiplecurrencyclassifier.viewmodel.CameraViewModel
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

class CameraFragment2 : Fragment(R.layout.fragment_camera2) {

    private var _binding: FragmentCamera2Binding? = null
    private val binding get() = _binding!!

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false
    private var camera: Camera? = null
    private lateinit var tts: TextToSpeech
    private val viewModel: CameraViewModel by viewModels()

    private var currentCall: Call<Api2Response>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCamera2Binding.bind(view)
        previewView = binding.cameraPreview
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupFlashSwitch()
        setupViewModelObservers()
        initializeTTS()
        checkAndPromptTtsSettings()

        previewView.post {
            startCamera()
        }

        binding.backButton.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }

    override fun onPause() {
        super.onPause()
        binding.detectionResult.text = getText(R.string.result_text)
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext())
        cameraProvider.get().unbindAll()
        tts.stop()
        currentCall?.cancel() // Batalkan permintaan API jika masih berjalan
    }

    private fun setupFlashSwitch() {
        binding.flashSwitch.isChecked = viewModel.isFlashOn.value ?: false

        binding.flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFlashStatus(isChecked)
        }
    }

    private fun setupViewModelObservers() {
        viewModel.isFlashOn.observe(viewLifecycleOwner, Observer { isFlashOn ->
            updateFlashUI(isFlashOn)
        })
    }

    private fun initializeTTS() {
        tts = TextToSpeech(requireContext()) { status ->
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
            requireContext().contentResolver,
            "tts_default_synth"
        ) ?: "Unknown"

        if (defaultEngine != "com.google.android.tts") {
            Log.w("TTS", "Mesin TTS default bukan Google TTS: $defaultEngine")

            requireActivity().runOnUiThread {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
            Toast.makeText(requireContext(), "Buka pengaturan TTS secara manual melalui Pengaturan > Bahasa & Masukan.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraX", "Error unbinding use cases: ${e.message}", e)
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { bitmap ->
                        if (!isProcessing) {
                            isProcessing = true
                            processImage(bitmap)
                        }
                        Log.d("CameraX", "Bitmap dihasilkan: ${bitmap.width}x${bitmap.height}")
                    })
                }

            try {
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )

                camera?.cameraControl?.enableTorch(viewModel.isFlashOn.value ?: false)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding gagal", exc)
                Toast.makeText(requireContext(), "Gagal memulai kamera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImage(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(15000)
            if (isResumed) {
                uploadImage(bitmap)
            }
            isProcessing = false
        }
    }

    private fun uploadImage(bitmap: Bitmap) {
        val file = convertBitmapToFile(bitmap, requireContext().cacheDir, "temp_image.jpg")
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

        currentCall = Retrofit2Instance.api2Service.detectCurrency(filePart)
        currentCall?.enqueue(object : Callback<Api2Response> {
            override fun onResponse(call: Call<Api2Response>, response: Response<Api2Response>) {
                if (!isResumed) return

                if (response.isSuccessful) {
                    val body = response.body()
                    val money1 = body?.detections?.getOrNull(0) ?: "Tidak diketahui"
                    val money2 = body?.detections?.getOrNull(1) ?: "Tidak diketahui"
                    val totalValue = body?.totalValue ?: "Tidak diketahui"

                    val readableMoney1 = if (money1 == "1ribu") "Seribu" else money1
                    val readableMoney2 = if (money2 == "1ribu") "Seribu" else money2
                    val readableTotal = if (totalValue == "1ribu") "Seribu" else totalValue

                    binding.detectionResult.text = "$readableMoney1 & $readableMoney2 Total: $readableTotal"

                    val ttsResult = "Uang pertama $readableMoney1, uang kedua $readableMoney2, total $readableTotal"
                    speakResult(ttsResult)
                } else {
                    Log.e("API Error", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Api2Response>, t: Throwable) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            cameraExecutor.shutdown()
            if (::tts.isInitialized) {
                tts.stop()
                tts.shutdown()
            }
        } catch (e: Exception) {
            Log.e("Cleanup", "Kesalahan saat membersihkan sumber daya", e)
        }
        currentCall?.cancel()
        _binding = null
    }

    class ImageAnalyzer(private val onFrameAnalyzed: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val bitmap = imageProxy.toBitmap()
            onFrameAnalyzed(bitmap)
            imageProxy.close()
        }
    }
}