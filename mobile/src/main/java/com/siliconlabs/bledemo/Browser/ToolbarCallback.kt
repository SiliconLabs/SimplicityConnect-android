package com.siliconlabs.bledemo.Browser

import com.siliconlabs.bledemo.Utils.FilterDeviceParams

interface ToolbarCallback {
    fun close()
    fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean)
}