package com.tower.locator.network

import com.tower.locator.model.CellPayload
import com.tower.locator.model.LocateResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/cell")
    fun sendCellData(
        @Body payload: CellPayload
    ): Call<ResponseBody>

    @POST("/locate")
    fun locateCell(
        @Body payload: CellPayload
    ): Call<LocateResponse>
}
