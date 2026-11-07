package com.reto01.flood.data

import com.google.gson.annotations.SerializedName

sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
}

data class ThresholdsDto(
    @SerializedName("level_warning_cm") val levelWarningCm: Double? = null,
    @SerializedName("level_critical_cm") val levelCriticalCm: Double? = null,
)

data class StationSatelliteDto(
    val status: String? = null,
    val reason: String? = null,
    val source: String? = null,
    @SerializedName("hotspots_count") val hotspotsCount: Int? = null,
    @SerializedName("observed_at") val observedAt: String? = null,
    @SerializedName("max_frp_mw") val maxFrpMw: Double? = null,
    @SerializedName("flood_extent_km2") val floodExtentKm2: Double? = null,
)

data class FloodStationDto(
    @SerializedName("station_id") val stationId: String? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerializedName("observed_at") val observedAt: String? = null,
    @SerializedName("source_mode") val sourceMode: String? = null,
    @SerializedName("level_cm") val levelCm: Double? = null,
    @SerializedName("rise_cm_per_min") val riseCmPerMin: Double? = null,
    @SerializedName("forecast_level_30m_cm") val forecastLevel30mCm: Double? = null,
    @SerializedName("rain_forecast_3h_mm") val rainForecast3hMm: Double? = null,
    @SerializedName("rain_source") val rainSource: String? = null,
    val satellite: StationSatelliteDto? = null,
    @SerializedName("risk_score") val riskScore: Int? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
    @SerializedName("model_type") val modelType: String? = null,
    @SerializedName("data_status") val dataStatus: String? = null,
    @SerializedName("alert_id") val alertId: Int? = null,
    @SerializedName("alert_status") val alertStatus: String? = null,
    @SerializedName("mount_height_cm") val mountHeightCm: Double? = null,
    @SerializedName("received_at") val receivedAt: String? = null,
    val thresholds: ThresholdsDto? = null,
)

data class ReadingDto(
    val id: Int? = null,
    @SerializedName("station_id") val stationId: String? = null,
    val timestamp: String? = null,
    @SerializedName("distance_cm") val distanceCm: Double? = null,
    @SerializedName("mount_height_cm") val mountHeightCm: Double? = null,
    @SerializedName("level_cm") val levelCm: Double? = null,
    @SerializedName("source_mode") val sourceMode: String? = null,
    val transport: String? = null,
    val quality: String? = null,
    @SerializedName("received_at") val receivedAt: String? = null,
    @SerializedName("risk_score") val riskScore: Int? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
)

data class HistoryResponse(
    val items: List<ReadingDto>? = null,
    val count: Int? = null,
)

data class AlertDto(
    val id: Int? = null,
    @SerializedName("station_id") val stationId: String? = null,
    @SerializedName("reading_id") val readingId: Int? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
    @SerializedName("risk_score") val riskScore: Int? = null,
    @SerializedName("level_cm") val levelCm: Double? = null,
    val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerializedName("acknowledged_by") val acknowledgedBy: String? = null,
    @SerializedName("resolved_at") val resolvedAt: String? = null,
)

data class AlertsResponse(
    val items: List<AlertDto>? = null,
)

data class AckRequest(
    val operator: String? = null,
)

data class AckResponse(
    val id: Int? = null,
    val status: String? = null,
    @SerializedName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerializedName("acknowledged_by") val acknowledgedBy: String? = null,
)

data class HotspotDto(
    val lat: Double? = null,
    val lon: Double? = null,
    @SerializedName("observed_at") val observedAt: String? = null,
    @SerializedName("brightness_k") val brightnessK: Double? = null,
    @SerializedName("frp_mw") val frpMw: Double? = null,
    val confidence: String? = null,
    val satellite: String? = null,
    val instrument: String? = null,
    val daynight: String? = null,
    val source: String? = null,
)

data class FirmsDto(
    val provider: String? = null,
    val status: String? = null,
    val reason: String? = null,
    val bbox: List<Double>? = null,
    @SerializedName("day_range") val dayRange: Int? = null,
    val sources: List<String>? = null,
    @SerializedName("fetched_at") val fetchedAt: String? = null,
    val count: Int? = null,
    val items: List<HotspotDto>? = null,
    @SerializedName("station_id") val stationId: String? = null,
    val note: String? = null,
)

data class HealthResponse(
    val status: String? = null,
    val mode: String? = null,
)
