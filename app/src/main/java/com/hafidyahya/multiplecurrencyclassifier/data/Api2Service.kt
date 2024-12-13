package com.hafidyahya.multiplecurrencyclassifier.data


import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface Api2Service {
    @Multipart
    @POST("detect")
    fun detectCurrency(
        @Part file: MultipartBody.Part
    ): Call<Api2Response>
}
