package com.hafidyahya.multiplecurrencyclassifier.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.File

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    val yuvImage = YuvImage(bytes, ImageFormat.NV21, width, height, null)

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val byteArray = out.toByteArray()

    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}

fun convertBitmapToFile(bitmap: Bitmap, cacheDir: File, fileName: String): File {
    val file = File(cacheDir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    return file
}
