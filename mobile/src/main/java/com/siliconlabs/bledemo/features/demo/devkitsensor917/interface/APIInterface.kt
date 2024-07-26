package com.siliconlabs.bledemo.features.demo.devkitsensor917


import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.AccelerometerGyroScopeResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.AmbientLightResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.HumidityResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDStatusResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.MicrophoneResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.ProvisionResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.ScanResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.StatusResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.TempResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.Call

interface APIInterface {
    @GET("/led")
    suspend fun getLedStatus(): Response<LEDResponse>

    @GET("/temperature")
    suspend fun getTempStatus(): Response<TempResponse>

    @GET("/light")
    suspend fun getAmbitStatus(): Response<AmbientLightResponse>

    @GET("/humidity")
    suspend fun getHumidityStatus(): Response<HumidityResponse>

    @GET("/accelerometer")
    suspend fun getAccelerometerStatus(): Response<AccelerometerGyroScopeResponse>

    @GET("/gyroscope")
    suspend fun getGyroscopeStatus(): Response<AccelerometerGyroScopeResponse>

    @GET("/microphone")
    suspend fun getMicrophoneStatus(): Response<MicrophoneResponse>

    @GET("/provisioning")
    suspend fun getProvisioningStatus():Response<ProvisionResponse>

    @GET("/scan")
    suspend fun getScanStatus(): Response<ScanResponse>

    @GET("/Status")
    suspend fun getStatus():Response<StatusResponse>

    @POST("/led")
    fun setAllLEdsOnOff(@Body body: Map<String, String>): Call<LEDResponse>

    @POST("/led")
    fun setRedLEDOn(@Body body: Map<String, String>): Call<LEDResponse>

    @POST("/led")
    fun setGreenLEDOn(@Body body: Map<String, String>): Call<LEDResponse>

    @POST("/led")
    fun setBlueLEDOn(@Body body: Map<String, String>): Call<LEDResponse>

    @GET("/status_led")
    suspend fun getLedSwitchStatus(): Response<LEDStatusResponse>

    @POST("/status_led")
    fun setLEDStatusOff(@Body body: Map<String, String>): Call<LEDStatusResponse>


}