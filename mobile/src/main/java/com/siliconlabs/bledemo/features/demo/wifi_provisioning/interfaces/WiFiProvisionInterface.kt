package com.siliconlabs.bledemo.features.demo.wifi_provisioning.interfaces

import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDStatusResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.ProvisionResponse
import com.siliconlabs.bledemo.features.demo.wifi_provisioning.model.ScanResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WiFiProvisionInterface {

    @GET("/scan")
    suspend fun getWiFiProvisionScanner(): Response<ScanResponse>

    @POST("/provisioning")
    suspend fun setWiFiProvisionProvisioning(): Response<ScanResponse>

    @POST("/connect")
    fun setWiFiProvisionConnect(@Body body: Map<String, String>): Call<ProvisionResponse>

    @GET("/all_sensors")
    suspend fun getLedStatus(): Response<LEDStatusResponse>

    @POST("/status_led")
    fun setLEDStatusON(@Body body: Map<String, String>): Call<LEDStatusResponse>
}