package com.siliconlabs.bledemo.features.demo.devkitsensor917.model

data class SensorsResponse(
    val led: LEDResponse,
    val light: AmbientLightResponse,
    val temperature: TempResponse,
    val accelerometer: AccelerometerGyroScopeResponse,
    val gyroscope: AccelerometerGyroScopeResponse,
    val humidity: HumidityResponse,
    val microphone: MicrophoneResponse
)
