package com.siliconlabs.bledemo.beacon_utils.ibeacon

import com.siliconlabs.bledemo.utils.Converters
import java.util.*


class IBeaconInfo(val uuid: String, val major: Int, val minor: Int, val power: Int) {

    companion object {
        fun getIBeaconInfo(scanRecord: ByteArray): IBeaconInfo? {
            var startByte = 2
            var patternFound = false
            while (startByte <= 5) {
                if (scanRecord[startByte + 2].toInt() and 0xff == 0x02 &&  //Identifies an iBeacon
                        scanRecord[startByte + 3].toInt() and 0xff == 0x15) { //Identifies correct data length
                    patternFound = true
                    break
                }
                startByte++
            }
            if (patternFound) {
                //Convert to hex String
                val uuidBytes = ByteArray(16)
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16)
                val hexString = Converters.bytesToHex(uuidBytes).toUpperCase(Locale.getDefault())

                // the iBeacon uuid
                val uuid = hexString.substring(0, 8) + "-" +
                        hexString.substring(8, 12) + "-" +
                        hexString.substring(12, 16) + "-" +
                        hexString.substring(16, 20) + "-" +
                        hexString.substring(20, 32)

                // the iBeacon major number
                val major: Int = (scanRecord[startByte + 20].toInt() and 0xff) * 0x100 + (scanRecord[startByte + 21].toInt() and 0xff)

                // the iBeacon minor number
                val minor: Int = (scanRecord[startByte + 22].toInt() and 0xff) * 0x100 + (scanRecord[startByte + 23].toInt() and 0xff)
                val power = scanRecord[startByte + 24].toInt()
                return IBeaconInfo(uuid, major, minor, power)
            }
            return null
        }
    }

}