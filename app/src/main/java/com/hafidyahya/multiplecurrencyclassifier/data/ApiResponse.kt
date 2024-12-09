package com.hafidyahya.multiplecurrencyclassifier.data

import com.google.gson.annotations.SerializedName

data class ApiResponse(

	@field:SerializedName("hasilPrediksi")
	val hasilPrediksi: String? = null,

	@field:SerializedName("ConfidenceScore")
	val confidenceScore: Any? = null
)
