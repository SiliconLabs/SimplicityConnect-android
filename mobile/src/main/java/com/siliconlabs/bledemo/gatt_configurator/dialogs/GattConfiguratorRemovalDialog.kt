package com.siliconlabs.bledemo.gatt_configurator.dialogs

import androidx.annotation.StringRes
import com.siliconlabs.bledemo.Utils.RemovalDialog
import com.siliconlabs.bledemo.gatt_configurator.utils.GattConfiguratorStorage

class GattConfiguratorRemovalDialog(
    @StringRes name: Int,
    onOkClicked: () -> Unit
) : RemovalDialog(name, onOkClicked) {

    override fun blockDisplayingRemovalDialog() {
        context?.let { GattConfiguratorStorage(it).setDisplayRemovalDialog(false) }
    }
}
