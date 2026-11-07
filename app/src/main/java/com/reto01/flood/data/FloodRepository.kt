package com.reto01.flood.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

class FloodRepository(
    var baseUrl: String,
    var mockMode: Boolean = false,
) {
    private var api: ApiService = createApiService(baseUrl)

    fun updateBaseUrl(url: String) {
        baseUrl = url
        api = createApiService(url)
    }

    private fun createApiService(url: String): ApiService {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Ok(body)
                } else {
                    ApiResult.HttpError(response.code(), "Respuesta vacía del servidor")
                }
            } else {
                val errBody = response.errorBody()?.string() ?: response.message()
                ApiResult.HttpError(response.code(), errBody)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Error de red")
        }
    }

    suspend fun health(): ApiResult<HealthResponse> {
        if (mockMode) return ApiResult.Ok(HealthResponse("ok", "MOCK"))
        return safeCall { api.getHealth() }
    }

    suspend fun station(stationId: String = "RIO-01"): ApiResult<FloodStationDto> {
        if (mockMode) {
            val now = Instant.now().toString()
            return ApiResult.Ok(
                FloodStationDto(
                    stationId = stationId,
                    name = "Estación RIO-01 (MOCK)",
                    lat = -14.88,
                    lon = -66.75,
                    observedAt = now,
                    sourceMode = "MOCK",
                    levelCm = 175.0,
                    riseCmPerMin = 1.2,
                    forecastLevel30mCm = 211.0,
                    rainForecast3hMm = 15.0,
                    rainSource = "OPEN_METEO",
                    satellite = StationSatelliteDto(
                        status = "AVAILABLE",
                        reason = null,
                        source = "NASA_FIRMS",
                        hotspotsCount = 2,
                        observedAt = now,
                        maxFrpMw = 12.4,
                        floodExtentKm2 = null
                    ),
                    riskScore = 55,
                    riskLevel = "WARNING",
                    modelType = "HEURISTIC_TREND_V1",
                    dataStatus = "OK",
                    alertId = 1,
                    alertStatus = "PENDING",
                    mountHeightCm = 250.0,
                    receivedAt = now,
                    thresholds = ThresholdsDto(levelWarningCm = 140.0, levelCriticalCm = 190.0)
                )
            )
        }
        return safeCall { api.getStation(stationId) }
    }

    suspend fun history(limit: Int = 30, stationId: String = "RIO-01"): ApiResult<HistoryResponse> {
        if (mockMode) {
            val now = Instant.now()
            val readings = (0 until limit).map { i ->
                ReadingDto(
                    id = i + 1,
                    stationId = stationId,
                    timestamp = now.minusSeconds((limit - i) * 60L).toString(),
                    distanceCm = 100.0 - i * 0.5,
                    mountHeightCm = 250.0,
                    levelCm = 150.0 + i * 0.5,
                    sourceMode = "MOCK",
                    transport = "HTTP",
                    quality = "OK",
                    receivedAt = now.toString(),
                    riskScore = 40 + i,
                    riskLevel = if (40 + i >= 70) "CRITICAL" else if (40 + i >= 40) "WARNING" else "NORMAL"
                )
            }
            return ApiResult.Ok(HistoryResponse(readings, readings.size))
        }
        return safeCall { api.getHistory(stationId, limit) }
    }

    suspend fun alerts(status: String = "ALL"): ApiResult<AlertsResponse> {
        if (mockMode) {
            val now = Instant.now().toString()
            return ApiResult.Ok(
                AlertsResponse(
                    listOf(
                        AlertDto(
                            id = 1,
                            stationId = "RIO-01",
                            readingId = 15,
                            riskLevel = "WARNING",
                            riskScore = 55,
                            levelCm = 175.0,
                            status = "PENDING",
                            createdAt = now,
                            acknowledgedAt = null,
                            acknowledgedBy = null,
                            resolvedAt = null
                        )
                    )
                )
            )
        }
        return safeCall { api.getAlerts(status) }
    }

    suspend fun ack(alertId: Int, operator: String): ApiResult<AckResponse> {
        if (mockMode) {
            return ApiResult.Ok(
                AckResponse(
                    id = alertId,
                    status = "ACKNOWLEDGED",
                    acknowledgedAt = Instant.now().toString(),
                    acknowledgedBy = operator
                )
            )
        }
        return safeCall { api.acknowledgeAlert(alertId, AckRequest(operator)) }
    }

    suspend fun firms(stationId: String = "RIO-01", days: Int = 2): ApiResult<FirmsDto> {
        if (mockMode) {
            val now = Instant.now().toString()
            return ApiResult.Ok(
                FirmsDto(
                    provider = "NASA_FIRMS",
                    status = "AVAILABLE",
                    reason = null,
                    bbox = listOf(-67.25, -15.38, -66.25, -14.38),
                    dayRange = days,
                    sources = listOf("VIIRS_NRT", "MODIS_NRT"),
                    fetchedAt = now,
                    count = 2,
                    items = listOf(
                        HotspotDto(
                            lat = -14.85,
                            lon = -66.72,
                            observedAt = now,
                            brightnessK = 315.5,
                            frpMw = 12.4,
                            confidence = "nominal",
                            satellite = "N20",
                            instrument = "VIIRS",
                            daynight = "D",
                            source = "VIIRS_NRT"
                        ),
                        HotspotDto(
                            lat = -14.90,
                            lon = -66.78,
                            observedAt = now,
                            brightnessK = 308.2,
                            frpMw = 4.8,
                            confidence = "low",
                            satellite = "Aqua",
                            instrument = "MODIS",
                            daynight = "D",
                            source = "MODIS_NRT"
                        )
                    ),
                    stationId = stationId,
                    note = "NASA FIRMS modo mock"
                )
            )
        }
        return safeCall { api.getFirms(stationId, days) }
    }
}
