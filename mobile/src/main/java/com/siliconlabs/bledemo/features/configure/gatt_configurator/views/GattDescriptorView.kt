package com.siliconlabs.bledemo.features.configure.gatt_configurator.views

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ViewGattDescriptorBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Descriptor
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property

//import kotlinx.android.synthetic.main.view_gatt_descriptor.view.*
//import kotlinx.android.synthetic.main.view_gatt_descriptor.view.ib_copy
//import kotlinx.android.synthetic.main.view_gatt_descriptor.view.ib_edit
//import kotlinx.android.synthetic.main.view_gatt_descriptor.view.ib_remove
//import kotlinx.android.synthetic.main.view_gatt_descriptor.view.tv_property_read
//import kotlinx.android.synthetic.main.view_gatt_descriptor.view.tv_property_write

class GattDescriptorView(context: Context, attributeSet: AttributeSet? = null) :
    FrameLayout(context, attributeSet) {
    private var descriptor: Descriptor? = null
    lateinit var gattViewDescriptorBinding: ViewGattDescriptorBinding

    constructor(context: Context, descriptor: Descriptor) : this(context) {
        this.descriptor = descriptor

        initView(descriptor)
        if (descriptor.isPredefined) hideButtons()
    }

    init {
        gattViewDescriptorBinding = ViewGattDescriptorBinding.inflate(LayoutInflater.from(context),this,true)
    }

    private fun initView(descriptor: Descriptor) {

        gattViewDescriptorBinding.tvDescriptorName.text = descriptor.name
        gattViewDescriptorBinding.tvDescriptorUuid.text = buildBoldHeaderTextLine(
            context.getString(R.string.UUID_colon_space),
            descriptor.uuid?.getAsFormattedText()
        )
        gattViewDescriptorBinding.tvDescriptorValue.text = buildBoldHeaderTextLine(
            context.getString(R.string.value_colon_space),
            descriptor.value?.getAsFormattedText()
        )
        showSelectedProperties(descriptor)
    }

    private fun buildBoldHeaderTextLine(header: String, content: String?): Spanned? {
        val htmlString = buildString {
            append("<b>")
            append(header)
            append("</b>")
            append(content)
        }
        return Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY)
    }

    fun refreshView() {
        descriptor?.let {
            initView(it)
        }
        gattViewDescriptorBinding.root.invalidate()
    }
    private fun hideButtons() {

        gattViewDescriptorBinding.ibCopy.visibility = View.GONE
        gattViewDescriptorBinding.ibRemove.visibility = View.GONE
        gattViewDescriptorBinding.ibEdit.visibility = View.GONE
    }

    private fun hideAllProperties() {
        gattViewDescriptorBinding.tvPropertyRead.visibility = View.GONE

         gattViewDescriptorBinding.tvPropertyWrite.visibility = View.GONE
    }

    private fun showSelectedProperties(descriptor: Descriptor) {
        hideAllProperties()
        descriptor.properties.apply {
            gattViewDescriptorBinding.tvPropertyRead
            if (containsKey(Property.READ)) gattViewDescriptorBinding.tvPropertyRead.visibility = View.VISIBLE
            if (containsKey(Property.WRITE))  gattViewDescriptorBinding.tvPropertyWrite.visibility = View.VISIBLE
        }
    }

    fun setDescriptorListener(listener: DescriptorListener) {
        gattViewDescriptorBinding.ibCopy.setOnClickListener {
            descriptor?.let {
                listener.onCopyDescriptor(it)
            }
        }
        gattViewDescriptorBinding.ibEdit.setOnClickListener {
            descriptor?.let {
                listener.onEditDescriptor(it)
            }
        }
        gattViewDescriptorBinding.ibRemove.setOnClickListener {
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