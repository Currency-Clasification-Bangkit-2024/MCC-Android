package com.hafidyahya.multiplecurrencyclassifier.data

import com.google.gson.annotations.SerializedName

data class Api2Response(

	@field:SerializedName("total_value")
	val totalValue: String? = null,

	@field:SerializedName("detection_info")
	val detectionInfo: List<String?>? = null,

	@field:SerializedName("detections")
	val detections: List<String?>? = null
)
