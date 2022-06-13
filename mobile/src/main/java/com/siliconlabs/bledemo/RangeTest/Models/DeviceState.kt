package com.siliconlabs.bledemo.RangeTest.Models

import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import java.util.LinkedHashMap

class DeviceState {

    var deviceInfo: BluetoothDeviceInfo? = null
    var testRunning: Boolean = false
    var mode: RangeTestMode? = null

    var deviceName: String? = null
    var modelNumber: String? = null
    var txPower: TxPower? = null
    var txPowerValues: List<TxPower>? = null
    var payloadLength: Int? = null
    var payloadLengthValues: List<Int>? = null
    var maWindowSize: Int? = null
    var maWindowSizeValues: List<Int>? = null
    var channelNumber: Int? = null
    var packetCountRepeat: Boolean? = null
    var packetsRequired: Int? = null
    var packetsReceived: Int? = null
    var packetsSent: Int? = null
    var packetCount: Int? = null
    var per: Float? = null
    var ma: Float? = null
    var remoteId: Int? = null
    var selfId: Int? = null
    var uartLogEnabled: Boolean? = null
    var running: Boolean? = null
    var phy: Int? = null
    var phyMap: LinkedHashMap<Int, String>? = null
    var lastPacketCount = -1
    var lastPacketLoss = 0
    val maBuffer = IntArray(MA_WINDOW_MAX)
    var maBufferPtr = 0






    companion object {
        private const val MA_WINDOW_MAX = 128
        private const val MA_WINDOW_DEFAULT = 32
    }

}