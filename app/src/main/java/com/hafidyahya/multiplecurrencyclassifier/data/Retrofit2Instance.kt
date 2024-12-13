package com.hafidyahya.multiplecurrencyclassifier.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Retrofit2Instance {
    private const val BASE_URL = "https://predict-yolo-863244423296.asia-southeast2.run.app/"

    val api2Service: Api2Service by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api2Service::class.java)
    }
}
