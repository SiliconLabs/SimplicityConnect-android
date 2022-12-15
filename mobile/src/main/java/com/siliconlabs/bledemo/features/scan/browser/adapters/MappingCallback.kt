package com.siliconlabs.bledemo.features.scan.browser.adapters

import com.siliconlabs.bledemo.features.scan.browser.models.Mapping

interface MappingCallback {
    fun onNameChanged(mapping: Mapping)
}
