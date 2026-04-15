package com.bestplus.mobileinspector.data.remote

import com.bestplus.mobileinspector.data.remote.dto.RouteSheetDto
import com.bestplus.mobileinspector.data.remote.dto.SendInfoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit-интерфейс к REST API 1С
 *
 * Original C#: MSystem.cs → IMasterSystem
 * Endpoint: {baseUrl}/hs/api/WorkTasks
 * Auth: Basic (base64 login:password)
 * Header: UUID (device identifier)
 */
interface OneCApi {

    @GET
    suspend fun getRoutSheets(
        @Url url: String,
        @Header("UUID") uuid: String,
    ): Response<List<RouteSheetDto>>

    @POST
    suspend fun sendCompletedTasks(
        @Url url: String,
        @Header("UUID") uuid: String,
        @Body data: List<SendInfoDto>,
    ): Response<Unit>
}
