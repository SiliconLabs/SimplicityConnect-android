package com.siliconlabs.bledemo.gatt_configurator.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.gatt_configurator.models.Descriptor
import com.siliconlabs.bledemo.gatt_configurator.models.Property
import kotlinx.android.synthetic.main.view_gatt_descriptor.view.*

class GattDescriptorView(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {
    private var descriptor: Descriptor? = null

    constructor(context: Context, descriptor: Descriptor) : this(context) {
        this.descriptor = descriptor

        initView(descriptor)
        if(descriptor.isPredefined) hideButtons()
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_gatt_descriptor, this, true)
    }

    private fun initView(descriptor: Descriptor) {
        tv_descriptor_name.text = descriptor.name
        tv_descriptor_uuid.text = context.getString(R.string.UUID_colon).plus(" ").plus(descriptor.uuid?.getAsFormattedText())
        tv_descriptor_value.text = context.getString(R.string.Value_colon).plus(" ").plus(descriptor.value?.getAsFormattedText())
        showSelectedProperties(descriptor)
    }

    fun refreshView() {
        descriptor?.let {
            initView(it)
        }
    }

    private fun hideButtons() {
        ib_copy.visibility = View.GONE
        ib_remove.visibility = View.GONE
        ib_edit.visibility = View.GONE
    }

    private fun hideAllProperties() {
        tv_property_read.visibility = View.GONE
        tv_property_write.visibility = View.GONE
    }

    private fun showSelectedProperties(descriptor: Descriptor) {
        hideAllProperties()
        descriptor.properties.apply {
            if (containsKey(Property.READ)) tv_property_read.visibility = View.VISIBLE
            if (containsKey(Property.WRITE)) tv_property_write.visibility = View.VISIBLE
        }
    }

    fun setDescriptorListener(listener: DescriptorListener) {
        ib_copy.setOnClickListener {
            descriptor?.let {
                listener.onCopyDescriptor(it)
            }
        }
        ib_edit.setOnClickListener {
            descriptor?.let {
                listener.onEditDescriptor(it)
            }
        }
        ib_remove.setOnClickListener {
            descriptor?.let {
                listener.onRemoveDescriptor(it)
            }
        }
    }

    interface DescriptorListener {
        fun onCopyDescriptor(descriptor: Descriptor)
        fun onEditDescriptor(descriptor: Descriptor)
        fun onRemoveDescriptor(descriptor: Descriptor)
    }
}