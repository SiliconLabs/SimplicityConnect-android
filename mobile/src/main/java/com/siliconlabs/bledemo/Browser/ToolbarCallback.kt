package com.siliconlabs.bledemo.browser

import com.siliconlabs.bledemo.utils.FilterDeviceParams

interface ToolbarCallback {
    fun close()
    fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean)
}
