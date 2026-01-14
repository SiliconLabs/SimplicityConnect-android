package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.ranging.RangingDevice
import android.ranging.RangingManager
import android.ranging.RangingPreference
import android.ranging.SensorFusionParams
import android.ranging.SessionConfig
import android.ranging.ble.cs.BleCsRangingParams
import android.ranging.ble.rssi.BleRssiRangingParams
import android.ranging.oob.DeviceHandle
import android.ranging.oob.OobInitiatorRangingConfig
import android.ranging.oob.OobResponderRangingConfig
import android.ranging.raw.RawInitiatorRangingConfig
import android.ranging.raw.RawRangingDevice
import android.ranging.raw.RawResponderRangingConfig
import android.ranging.uwb.UwbComplexChannel
import android.ranging.uwb.UwbRangingParams
import android.ranging.wifi.rtt.RttRangingParams
import androidx.annotation.RequiresApi
import com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces.BleConnection
import java.time.Duration
import java.util.UUID

class ChannelSoundingRangingParameters private constructor() {

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    enum class Freq(val freq: Int) {
        HIGH(RawRangingDevice.UPDATE_RATE_FREQUENT),
        MEDIUM(RawRangingDevice.UPDATE_RATE_NORMAL),
        LOW(RawRangingDevice.UPDATE_RATE_INFREQUENT);


        fun getSlowestIntervalDuration(): Duration = when (this) {
            HIGH -> Duration.ofMillis(1000)
            MEDIUM -> Duration.ofMillis(5000)
            LOW -> Duration.ofMillis(10000)
        }

        fun getFastestIntervalDuration(): Duration = when (this) {
            HIGH -> Duration.ofMillis(100)
            MEDIUM -> Duration.ofMillis(1000)
            LOW -> Duration.ofMillis(5000)
        }

        override fun toString(): String = name

        companion object {
            @JvmStatic
            fun fromName(name: String): Freq = try {
                valueOf(name)
            } catch (_: IllegalArgumentException) {
                MEDIUM
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    enum class Technology(val tech: Int) {
        UWB(RangingManager.UWB),
        BLE_RSSI(RangingManager.BLE_RSSI),
        BLE_CS(RangingManager.BLE_CS),
        WIFI_NAN_RTT(RangingManager.WIFI_NAN_RTT),
        OOB(1000);


        override fun toString(): String = name

        companion object {
            @JvmStatic
            fun fromName(name: String): Technology = try {
                valueOf(name)
            } catch (_: IllegalArgumentException) {
                BLE_RSSI
            }
        }
    }


    companion object {
        private val TAG = ChannelSoundingRangingParameters::class.java.simpleName.toString()


        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun createInitiatorRangingPreference(
            context: Context,
            bleConnection: BleConnection,
            rangingTechnologyName: String,
            freqName: String,
            configParams: ChannelSoundingConfigureParameters,
            duration: Int,
            targetBtDevice: BluetoothDevice
        ): RangingPreference? {
            val initiatorRangingConfig = when (Technology.fromName(rangingTechnologyName)) {
                Technology.OOB -> createOobInitiatorConfig(
                    context, bleConnection, freqName, configParams, targetBtDevice
                )

                else -> createRawInitiatorConfig(
                    rangingTechnologyName, freqName, configParams, targetBtDevice
                )
            }
            if (initiatorRangingConfig == null) return null
            val sessionConfig = SessionConfig.Builder()
                .setSensorFusionParams(
                    SensorFusionParams.Builder()
                        .setSensorFusionEnabled(configParams.global.sensorFusionEnabled)
                        .build()
                ).setRangingMeasurementsLimit(duration)
                .build()
            return RangingPreference.Builder(
                RangingPreference.DEVICE_ROLE_INITIATOR,
                initiatorRangingConfig
            ).setSessionConfig(sessionConfig)
                .build()
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        private fun createOobInitiatorConfig(
            context: Context,
            bleConnection: BleConnection,
            freqName: String,
            configParams: ChannelSoundingConfigureParameters,
            targetBtDevice: BluetoothDevice
        ): OobInitiatorRangingConfig? {
            val oobBleClient = OobBleClient(context, bleConnection, targetBtDevice)
            if (!oobBleClient.waitForSocketCreation()) {
                oobBleClient.close()
                return null
            }
            return OobInitiatorRangingConfig.Builder()
                .addDeviceHandle(
                    DeviceHandle.Builder(
                        RangingDevice.Builder()
                            .setUuid(UUID.nameUUIDFromBytes(targetBtDevice.address.toByteArray()))
                            .build(), oobBleClient
                    ).build()
                )
                .setSecurityLevel(configParams.oob.securityLevel)
                .setRangingMode(configParams.oob.mode)
                .setSlowestRangingInterval(Freq.fromName(freqName).getSlowestIntervalDuration())
                .setFastestRangingInterval(Freq.fromName(freqName).getFastestIntervalDuration())
                .build()
        }


        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        private fun createRawInitiatorConfig(
            rangingTechnologyName: String,
            freqName: String,
            configParams: ChannelSoundingConfigureParameters,
            targetBtDevice: BluetoothDevice
        ): RawInitiatorRangingConfig {
            val rawRangingDeviceBuilder = RawRangingDevice.Builder()
                .setRangingDevice(
                    RangingDevice.Builder()
                        .setUuid(UUID.nameUUIDFromBytes(targetBtDevice.address.toByteArray()))
                        .build()
                )


            when (Technology.fromName(rangingTechnologyName)) {
                Technology.UWB -> rawRangingDeviceBuilder.setUwbRangingParams(
                    UwbRangingParams.Builder(
                        configParams.uwb.sessionId,
                        configParams.uwb.configId,
                        configParams.uwb.deviceAddress,
                        configParams.uwb.peerDeviceAddress
                    ).setComplexChannel(
                        UwbComplexChannel.Builder()
                            .setChannel(configParams.uwb.channel)
                            .setPreambleIndex(configParams.uwb.preamble)
                            .build()
                    ).setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .setSessionKeyInfo(configParams.uwb.sessionKey)
                        .build()


                )


                Technology.BLE_CS -> rawRangingDeviceBuilder.setCsRangingParams(
                    BleCsRangingParams.Builder(targetBtDevice.address)
                        .setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .setSecurityLevel(configParams.bleCs.securityLevel)
                        .setSightType(configParams.bleCs.sightType)
                        .setLocationType(configParams.bleCs.locationType)
                        .build()
                )


                Technology.BLE_RSSI -> rawRangingDeviceBuilder.setBleRssiRangingParams(
                    BleRssiRangingParams.Builder(targetBtDevice.address)
                        .setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .build()
                )

                Technology.WIFI_NAN_RTT -> rawRangingDeviceBuilder.setRttRangingParams(
                    RttRangingParams.Builder(configParams.wifiNanRtt.serviceName)
                        .setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .setPeriodicRangingHwFeatureEnabled(configParams.wifiNanRtt.isPeriodicRangingEnabled)
                        .build()
                )

                Technology.OOB -> { /* not part of raw config */
                }

            }
            return RawInitiatorRangingConfig.Builder()
                .addRawRangingDevice(rawRangingDeviceBuilder.build())
                .build()
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        private fun createRawResponderConfig(
            rangingTechnologyName: String,
            freqName: String,
            configParams: ChannelSoundingConfigureParameters,
            targetBtDevice: BluetoothDevice
        ): RawResponderRangingConfig {
            val rawRangingDeviceBuilder = RawRangingDevice.Builder()
                .setRangingDevice(
                    RangingDevice.Builder()
                        .setUuid(UUID.nameUUIDFromBytes(targetBtDevice.address.toByteArray()))
                        .build()
                )
            when (Technology.fromName(rangingTechnologyName)) {
                Technology.UWB -> rawRangingDeviceBuilder.setUwbRangingParams(
                    UwbRangingParams.Builder(
                        configParams.uwb.sessionId,
                        configParams.uwb.configId,
                        configParams.uwb.deviceAddress,
                        configParams.uwb.peerDeviceAddress
                    ).setComplexChannel(
                        UwbComplexChannel.Builder()
                            .setChannel(configParams.uwb.channel)
                            .setPreambleIndex(configParams.uwb.preamble)
                            .build()
                    ).setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .setSessionKeyInfo(configParams.uwb.sessionKey).build()
                )

                Technology.BLE_CS -> rawRangingDeviceBuilder.setCsRangingParams(
                    BleCsRangingParams.Builder(targetBtDevice.address)
                        .setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .setSecurityLevel(configParams.bleCs.securityLevel)
                        .build()
                )

                Technology.BLE_RSSI -> rawRangingDeviceBuilder.setBleRssiRangingParams(
                    BleRssiRangingParams.Builder(targetBtDevice.address)
                        .setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .build()
                )

                Technology.WIFI_NAN_RTT -> rawRangingDeviceBuilder.setRttRangingParams(
                    RttRangingParams.Builder(configParams.wifiNanRtt.serviceName)
                        .setRangingUpdateRate(Freq.fromName(freqName).freq)
                        .setPeriodicRangingHwFeatureEnabled(
                            configParams.wifiNanRtt.isPeriodicRangingEnabled
                        ).build()
                )

                Technology.OOB -> { /* not part of raw config */
                }
            }
            return RawResponderRangingConfig.Builder()
                .setRawRangingDevice(rawRangingDeviceBuilder.build())
                .build()
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun createResponderRangingPreference(
            context: Context,
            bleConnection: BleConnection,
            rangingTechnologyName: String,
            freqName: String,
            configParams: ChannelSoundingConfigureParameters,
            duration: Int,
            targetBtDevice: BluetoothDevice
        ): RangingPreference? {
            val responderRangingConfig = when (Technology.fromName(rangingTechnologyName)) {
                Technology.OOB -> createOobResponderConfig(
                    context,
                    bleConnection,
                    freqName,
                    configParams,
                    targetBtDevice
                )

                else -> createRawResponderConfig(
                    rangingTechnologyName, freqName, configParams, targetBtDevice
                )
            }
            if (responderRangingConfig == null) return null
            val sessionConfig = SessionConfig.Builder()
                .setSensorFusionParams(
                    SensorFusionParams.Builder()
                        .setSensorFusionEnabled(configParams.global.sensorFusionEnabled)
                        .build()
                )
                .setRangingMeasurementsLimit(duration)
                .build()
            return RangingPreference.Builder(
                RangingPreference.DEVICE_ROLE_RESPONDER,
                responderRangingConfig
            )
                .setSessionConfig(sessionConfig)
                .build()
        }


        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        private fun createOobResponderConfig(
            context: Context,
            bleConnection: BleConnection,
            freqName: String,
            configParams: ChannelSoundingConfigureParameters,
            targetBtDevice: BluetoothDevice
        ): OobResponderRangingConfig? {
            val oobBleServer = OobBleServer(context, bleConnection, targetBtDevice)
            if (!oobBleServer.waitForSocketCreation()) {
                oobBleServer.close()
                return null
            }
            return OobResponderRangingConfig.Builder(
                DeviceHandle.Builder(
                    RangingDevice.Builder()
                        .setUuid(UUID.nameUUIDFromBytes(targetBtDevice.address.toByteArray()))
                        .build(),
                    oobBleServer
                ).build()
            ).build()
        }
    }
}