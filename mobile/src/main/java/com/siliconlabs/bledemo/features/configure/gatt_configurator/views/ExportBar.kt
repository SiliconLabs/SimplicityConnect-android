package com.siliconlabs.bledemo.features.configure.gatt_configurator.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.siliconlabs.bledemo.databinding.ViewExportBarBinding

class ExportBar(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {

    private val viewBinding: ViewExportBarBinding = ViewExportBarBinding.inflate(
            LayoutInflater.from(context), this, true)

    fun init(listener: Listener) {
        viewBinding.exportBar.setOnClickListener { listener.onExportClick() }
        viewBinding.cancelBar.setOnClickListener { listener.onCancelClick() }
    }

    fun setExportBtnEnabled(isEnabled: Boolean) {
        viewBinding.exportBar.isEnabled = isEnabled
    }

    interface Listener {
        fun onExportClick()
        fun onCancelClick()
    }

}