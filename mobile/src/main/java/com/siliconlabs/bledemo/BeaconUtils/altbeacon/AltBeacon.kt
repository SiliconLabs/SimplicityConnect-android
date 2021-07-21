package com.siliconlabs.bledemo.BeaconUtils.altbeacon

import com.siliconlabs.bledemo.BeaconUtils.BleFormat
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo

class AltBeacon(deviceInfo: BluetoothDeviceInfo) {
    var manufacturerId: String
    var altBeaconId: String
    var altBeaconReferenceRssi: Byte
    var deviceAddress: String
    var rssi: Int

    init {
        deviceAddress = deviceInfo.address!!
        rssi = deviceInfo.rssi
        manufacturerId = parseManufacturerId(deviceInfo)
        altBeaconId = parseBeaconId(deviceInfo)
        altBeaconReferenceRssi = parseBeaconReferenceRssi(deviceInfo)
    }

    private fun parseManufacturerId(deviceInfo: BluetoothDeviceInfo): String {
        val bytes = deviceInfo.scanInfo?.scanRecord?.bytes
        val mfgIdBytes = bytes?.copyOfRange(2, 4)!!

        // reverse the order of the bytes, data received in little endian
        val lessSignificant = mfgIdBytes[0]
        mfgIdBytes[0] = mfgIdBytes[1]
        mfgIdBytes[1] = lessSignificant
        val mfgId = BleFormat.bytesToHex(mfgIdBytes)
        return "0x$mfgId"
    }

    private fun parseBeaconId(deviceInfo: BluetoothDeviceInfo): String {
        val bytes = deviceInfo.scanInfo?.scanRecord?.bytes
        val beaconIdBytes = bytes?.copyOfRange(6,26)!!
        val beaconId = BleFormat.bytesToHex(beaconIdBytes)
        return "0x$beaconId"
    }

    private fun parseBeaconReferenceRssi(deviceInfo: BluetoothDeviceInfo): Byte {
        val bytes = deviceInfo.scanInfo?.scanRecord?.bytes!!
        return bytes[26]
    }
}