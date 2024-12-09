package com.hafidyahya.multiplecurrencyclassifier.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    // LiveData untuk status flash
    private val _isFlashOn = MutableLiveData(true)
    val isFlashOn: LiveData<Boolean> get() = _isFlashOn

    // Fungsi untuk mengubah status flash
    fun toggleFlash() {
        _isFlashOn.value = _isFlashOn.value != true // Toggle status
    }

    // Fungsi untuk menyetel status flash secara langsung
    fun setFlashStatus(status: Boolean) {
        _isFlashOn.value = status
    }
}
