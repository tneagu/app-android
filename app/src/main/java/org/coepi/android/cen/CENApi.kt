package org.coepi.android.cen

import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CENApi {
    // post CENReport, which contains info needed for matching
    @POST("/cenreport")
    fun postCENReport(@Body report : CenReport): Single<Unit>

    // get CENReport report based on timestamp
    @GET("/cenreport/{timestamp}")
    fun getCENReport(@Path("timestamp") timestamp: Int): Call<Array<CenReport>>
}
