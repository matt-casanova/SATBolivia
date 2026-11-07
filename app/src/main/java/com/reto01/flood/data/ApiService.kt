package com.reto01.flood.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("api/v1/stations/{id}")
    suspend fun getStation(@Path("id") stationId: String): Response<FloodStationDto>

    @GET("api/v1/stations/{id}/history")
    suspend fun getHistory(
        @Path("id") stationId: String,
        @Query("limit") limit: Int = 30
    ): Response<HistoryResponse>

    @GET("api/v1/alerts")
    suspend fun getAlerts(@Query("status") status: String = "ALL"): Response<AlertsResponse>

    @POST("api/v1/alerts/{id}/ack")
    suspend fun acknowledgeAlert(
        @Path("id") alertId: Int,
        @Body request: AckRequest
    ): Response<AckResponse>

    @GET("api/v1/satellite/firms")
    suspend fun getFirms(
        @Query("station_id") stationId: String = "RIO-01",
        @Query("days") days: Int = 2
    ): Response<FirmsDto>
}
