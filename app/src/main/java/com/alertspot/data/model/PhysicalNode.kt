package com.alertspot.data.model

import com.google.gson.annotations.SerializedName

data class PhysicalNode(
    val id: String,
    @SerializedName("adc_raw")
    val adcRaw: Int,
    @SerializedName("temperature_c_estimated")
    val temperatureCEstimated: Double?,
    @SerializedName("temperature_calibrated")
    val calibrated: Boolean,
    val quality: String,
    @SerializedName("source_mode")
    val sourceMode: String,
    @SerializedName("location_is_demo")
    val locationIsDemo: Boolean,
    @SerializedName("updated_at")
    val updatedAt: String
)
