package com.siliconlabs.bledemo.features.scan.browser.viewmodels

class DeviceServicesViewModel {

    var deviceMode: DeviceMode = DeviceMode.NORMAL



    enum class DeviceMode {
        /* One must write 0x00 to OTA_CONTROL to get from NORMAL mode to DFU.
        *   This results in device disconnecting and changing its gatt database. When
        * reconnecting and rediscovering provides OTA_DATA characteristic and uploading file is
        * possible.
        */
        /* One must write 0x03 to OTA_CONTROL to get from DFU mode to NORMAL. */
        NORMAL,
        DFU
    }
}