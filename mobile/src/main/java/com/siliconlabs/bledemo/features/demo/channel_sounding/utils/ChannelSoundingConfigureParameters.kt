package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.ranging.ble.cs.BleCsRangingCapabilities
import android.ranging.ble.cs.BleCsRangingParams
import android.ranging.oob.OobInitiatorRangingConfig
import android.ranging.uwb.UwbAddress
import android.ranging.uwb.UwbRangingParams
import androidx.annotation.RequiresApi
import androidx.core.content.edit

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ChannelSoundingConfigureParameters private constructor(
    var global: Global,
    var uwb: Uwb,
    var bleCs: BleCs,
    var bleRssi: BleRssi,
    var wifiNanRtt: WifiNanRtt,
    var oob: Oob
) {
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private constructor(isResponder: Boolean) : this(
        Global(),
        Uwb(isResponder),
        BleCs(),
        BleRssi(),
        WifiNanRtt(),
        Oob()
    )

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun saveInstance(context: Context) {
        val pref = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        pref.edit {
            global.toPref(this)
            uwb.toPref(this)
            bleCs.toPref(this)
            bleRssi.toPref(this)
            wifiNanRtt.toPref(this)
            oob.toPref(this)
        }
    }

    companion object {
        private const val PREF_CONFIG = "PrefConfig"

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun resetInstance(context: Context, isResponder: Boolean): ChannelSoundingConfigureParameters {
            val pref = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
            pref.edit {
                clear()
            }
            return ChannelSoundingConfigureParameters(isResponder)
        }


        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun restoreInstance(context: Context, isResponder: Boolean): ChannelSoundingConfigureParameters {
            val pref = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
            return ChannelSoundingConfigureParameters(
                Global.fromPref(pref, isResponder),
                Uwb.fromPref(pref, isResponder),
                BleCs.fromPref(pref, isResponder),
                BleRssi.fromPref(pref, isResponder),
                WifiNanRtt.fromPref(pref, isResponder),
                Oob.fromPref(pref, isResponder)
            )
        }
    }

    open class BaseTechConfig(var technology: ChannelSoundingRangingParameters.Technology)

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    class Global : BaseTechConfig(ChannelSoundingRangingParameters.Technology.OOB) {
        var sensorFusionEnabled: Boolean = true

        fun toPref(editor: SharedPreferences.Editor) {
            editor.putBoolean("sensorFusionEnabled", sensorFusionEnabled)
        }

        companion object {
            @JvmStatic
            fun fromPref(pref: SharedPreferences, isResponder: Boolean): Global {
                val g = Global()
                g.sensorFusionEnabled = pref.getBoolean("sensorFusionEnabled", g.sensorFusionEnabled)
                return g
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    class Uwb private constructor(
        var isResponder: Boolean,
        var deviceAddress: UwbAddress,
        var peerDeviceAddress: UwbAddress
    ) : BaseTechConfig(ChannelSoundingRangingParameters.Technology.UWB) {
        var channel: Int = DEFAULT_CHANNEL
        var preamble: Int = DEFAULT_PREAMBLE
        var configId: Int = UwbRangingParams.CONFIG_PROVISIONED_MULTICAST_DS_TWR
        var sessionId: Int = 5
        var sessionKey: ByteArray = byteArrayOf(
            0x1, 0x2, 0x3, 0x4,
            0x5, 0x6, 0x7, 0x8,
            0x8, 0x7, 0x6, 0x5,
            0x4, 0x3, 0x2, 0x1
        )

        constructor(isResponder: Boolean) : this(
            isResponder,
            if (isResponder) UWB_ADDRESSES[0] else UWB_ADDRESSES[1],
            if (isResponder) UWB_ADDRESSES[1] else UWB_ADDRESSES[0]
        )

        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun toPref(editor: SharedPreferences.Editor) {
            editor.putBoolean("isResponder", isResponder)
            editor.putInt("channel", channel)
            editor.putInt("preamble", preamble)
            editor.putInt("configId", configId)
            editor.putInt("sessionId", sessionId)
            editor.putString("deviceAddress", String(deviceAddress.addressBytes))
            editor.putString("peerDeviceAddress", String(peerDeviceAddress.addressBytes))
        }

        companion object {
            // Mirror static imports from Java (keeping numeric values inline to avoid import dependency)
            private const val DEFAULT_CHANNEL: Int = 9 // UWB_CHANNEL_9
            private const val DEFAULT_PREAMBLE: Int = 11 // UWB_PREAMBLE_CODE_INDEX_11

            private val UWB_ADDRESSES = arrayOf(
                UwbAddress.fromBytes(byteArrayOf(0x5, 0x6)),
                UwbAddress.fromBytes(byteArrayOf(0x6, 0x5))
            )

            @JvmStatic
            fun fromPref(pref: SharedPreferences, isResponder: Boolean): Uwb {
                val hasKey = pref.contains("isResponder")
                val constructed = if (!hasKey || pref.getBoolean("isResponder", false) != isResponder) {
                    Uwb(isResponder)
                } else {
                    val devStr = pref.getString("deviceAddress", String(UWB_ADDRESSES[0].addressBytes))
                    val peerStr = pref.getString("peerDeviceAddress", String(UWB_ADDRESSES[1].addressBytes))
                    Uwb(
                        isResponder,
                        UwbAddress.fromBytes(devStr!!.toByteArray()),
                        UwbAddress.fromBytes(peerStr!!.toByteArray())
                    )
                }
                constructed.channel = pref.getInt("channel", constructed.channel)
                constructed.preamble = pref.getInt("preamble", constructed.preamble)
                constructed.configId = pref.getInt("configId", constructed.configId)
                constructed.sessionId = pref.getInt("sessionId", constructed.sessionId)
                return constructed
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    class BleCs : BaseTechConfig(ChannelSoundingRangingParameters.Technology.BLE_CS) {
        var securityLevel: Int = BleCsRangingCapabilities.CS_SECURITY_LEVEL_ONE
        var locationType: Int = BleCsRangingParams.LOCATION_TYPE_UNKNOWN
        var sightType:Int = BleCsRangingParams.SIGHT_TYPE_UNKNOWN

        fun toPref(editor: SharedPreferences.Editor) {
            editor.putInt("securityLevel", securityLevel)
            editor.putInt("locationType", locationType)
            editor.putInt("sightType", sightType)
        }

        companion object {
            @JvmStatic
            fun fromPref(pref: SharedPreferences, isResponder: Boolean): BleCs {
                val b = BleCs()
                b.securityLevel = pref.getInt("securityLevel", b.securityLevel)
                b.locationType = pref.getInt("locationType", b.locationType)
                b.sightType = pref.getInt("sightType", b.sightType)
                return b
            }
        }
    }

    class BleRssi : BaseTechConfig( // Preserve original (possibly intentional) mapping to BLE_CS
        ChannelSoundingRangingParameters.Technology.BLE_CS
    ) {
        fun toPref(editor: SharedPreferences.Editor) { /* no-op */ }

        companion object {
            @JvmStatic
            fun fromPref(pref: SharedPreferences, isResponder: Boolean): BleRssi = BleRssi()
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    class WifiNanRtt : BaseTechConfig(ChannelSoundingRangingParameters.Technology.WIFI_NAN_RTT) {
        var serviceName: String = "ranging_service"
        var isPeriodicRangingEnabled: Boolean = false

        fun toPref(editor: SharedPreferences.Editor) {
            editor.putString("serviceName", serviceName)
            editor.putBoolean("isPeriodicRangingEnabled", isPeriodicRangingEnabled)
        }

        companion object {
            @JvmStatic
            fun fromPref(pref: SharedPreferences, isResponder: Boolean): WifiNanRtt {
                val w = WifiNanRtt()
                w.serviceName = pref.getString("serviceName", w.serviceName) ?: w.serviceName
                w.isPeriodicRangingEnabled = pref.getBoolean("isPeriodicRangingEnabled", w.isPeriodicRangingEnabled)
                return w
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    class Oob : BaseTechConfig(ChannelSoundingRangingParameters.Technology.OOB) {
        var securityLevel: Int = OobInitiatorRangingConfig.SECURITY_LEVEL_BASIC
        var mode: Int = OobInitiatorRangingConfig.RANGING_MODE_AUTO

        fun toPref(editor: SharedPreferences.Editor) {
            // Write both legacy and corrected key to remain backward compatible
            editor.putInt("securitylevel", securityLevel) // legacy key (notice lowercase 'l')

            editor.putInt("mode", mode)
        }

        companion object {
            @JvmStatic
            fun fromPref(pref: SharedPreferences, isResponder: Boolean): Oob {
                val o = Oob()
                // Attempt to read corrected key first, fall back to legacy
                o.securityLevel = pref.getInt(
                    "securityLevel",
                    pref.getInt("securitylevel", o.securityLevel)
                )
                o.mode = pref.getInt("mode", o.mode)
                return o
            }
        }
    }
}
