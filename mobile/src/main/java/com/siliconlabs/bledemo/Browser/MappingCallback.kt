package com.siliconlabs.bledemo.browser

import com.siliconlabs.bledemo.browser.models.Mapping

interface MappingCallback {
    fun onNameChanged(mapping: Mapping)
}
