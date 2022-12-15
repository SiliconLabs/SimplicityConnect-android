package com.siliconlabs.bledemo.features.scan.browser.views

import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DescriptorContainerBinding
import com.siliconlabs.bledemo.utils.UuidUtils

class DescriptorContainer(
        context: Context,
        private val callback: Callback,
        val descriptor: BluetoothGattDescriptor
) : LinearLayout(context) {

    private val _binding: DescriptorContainerBinding =
            DescriptorContainerBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        initViews()
        setupUiListeners()
    }

    private fun initViews() {
        _binding.apply {
            tvName.text = Engine.getDescriptorByUUID(descriptor.uuid)?.name
                    ?: context.getString(R.string.unknown_descriptor_label)
            tvUuid.text = UuidUtils.getUuidText(descriptor.uuid)
        }
    }

    private fun setupUiListeners() {
        _binding.tvRead.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(
                    context,
                    R.anim.property_image_click
            ))
            callback.onReadPropertyClicked(descriptor)
        }
    }

    interface Callback {
        fun onReadPropertyClicked(descriptor: BluetoothGattDescriptor)
    }
}