package com.siliconlabs.bledemo.features.configure.gatt_configurator.views

import android.content.Context
import android.text.Html
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.get
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.gatt_configurator.activities.GattServerActivity
import com.siliconlabs.bledemo.features.configure.gatt_configurator.dialogs.DescriptorDialog
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Characteristic
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Descriptor
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.removeAsking
import kotlinx.android.synthetic.main.view_gatt_characteristic.view.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.views.GattDescriptorView.DescriptorListener

class GattCharacteristicView(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {
    private var characteristic: Characteristic? = null

    constructor(context: Context, characteristic: Characteristic) : this(context) {
        this.characteristic = characteristic

        initView(characteristic)
        handleAddDescriptorClickEvent(characteristic.descriptors)
    }

    private fun initView(characteristic: Characteristic) {
        tv_characteristic_name.text = characteristic.name
        val uuidHtml = buildString {
            append("<b>")
            append(context.getString(R.string.UUID_colon_space))
            append("</b>")
            append(characteristic.uuid?.getAsFormattedText())
        }
        tv_characteristic_uuid.text = Html.fromHtml(uuidHtml, Html.FROM_HTML_MODE_LEGACY)
        showSelectedProperties(characteristic)
        showOrHideDescriptorsLabelWithDivider()
        initDescriptors()
    }

    fun refreshView() {
        characteristic?.let {
            initView(it)
        }
    }

    private fun hideAllProperties() {
        tv_property_read.visibility = View.GONE
        tv_property_write.visibility = View.GONE
        tv_property_indicate.visibility = View.GONE
        tv_property_notify.visibility = View.GONE
    }

    private fun showSelectedProperties(characteristic: Characteristic) {
        hideAllProperties()
        characteristic.properties.apply {
            if (containsKey(Property.READ)) tv_property_read.visibility = View.VISIBLE
            if (containsKey(Property.WRITE)) tv_property_write.visibility = View.VISIBLE
            if (containsKey(Property.WRITE_WITHOUT_RESPONSE)) tv_property_write.visibility = View.VISIBLE
            if (containsKey(Property.RELIABLE_WRITE)) tv_property_write.visibility = View.VISIBLE
            if (containsKey(Property.INDICATE)) tv_property_indicate.visibility = View.VISIBLE
            if (containsKey(Property.NOTIFY)) tv_property_notify.visibility = View.VISIBLE
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_gatt_characteristic, this, true)
    }

    private fun initDescriptors() {
        ll_descriptors.removeAllViews()
        for (descriptor in characteristic?.descriptors!!) {
            val view = GattDescriptorView(context, descriptor)
            handleDescriptorClickEvents(view, characteristic?.descriptors!!)
            ll_descriptors.addView(view)
        }
    }

    private fun handleDescriptorClickEvents(view: GattDescriptorView, descriptors: ArrayList<Descriptor>) {
        view.setDescriptorListener(object : DescriptorListener {
            override fun onCopyDescriptor(descriptor: Descriptor) {
                copyDescriptor(descriptor, descriptors)
            }

            override fun onEditDescriptor(descriptor: Descriptor) {
                editDescriptor(descriptor, descriptors)
            }

            override fun onRemoveDescriptor(descriptor: Descriptor) {
                removeAsking(R.string.descriptor) {
                    removeDescriptor(descriptor, descriptors)
                }
            }
        })
    }

    private fun handleAddDescriptorClickEvent(descriptors: ArrayList<Descriptor>) {
        btn_add_descriptor.setOnClickListener {
            DescriptorDialog(object : DescriptorDialog.DescriptorChangeListener {
                override fun onDescriptorChanged(descriptor: Descriptor) {
                    addDescriptor(descriptor, descriptors)
                }
            }).show((context as GattServerActivity).supportFragmentManager, "dialog_descriptor")
        }
    }

    private fun addDescriptor(descriptor: Descriptor, descriptors: ArrayList<Descriptor>) {
        val view = GattDescriptorView(context, descriptor)
        handleDescriptorClickEvents(view, descriptors)

        descriptors.add(descriptor)
        ll_descriptors.addView(view)
        showOrHideDescriptorsLabelWithDivider()
    }

    private fun copyDescriptor(descriptor: Descriptor, descriptors: ArrayList<Descriptor>) {
        val copiedDescriptor = descriptor.deepCopy()
        val view = GattDescriptorView(context, copiedDescriptor)

        descriptors.add(copiedDescriptor)
        handleDescriptorClickEvents(view, descriptors)
        ll_descriptors.addView(view)
    }

    private fun editDescriptor(descriptor: Descriptor, descriptors: ArrayList<Descriptor>) {
        DescriptorDialog(object : DescriptorDialog.DescriptorChangeListener {
            override fun onDescriptorChanged(descriptor: Descriptor) {
                val index = descriptors.indexOf(descriptor)
                (ll_descriptors[index] as GattDescriptorView).refreshView()
            }
        }, descriptor).show((context as GattServerActivity).supportFragmentManager, "dialog_descriptor")
    }

    private fun removeDescriptor(descriptor: Descriptor, descriptors: ArrayList<Descriptor>) {
        val index = descriptors.indexOf(descriptor)
        ll_descriptors.removeViewAt(index)
        descriptors.remove(descriptor)

        showOrHideDescriptorsLabelWithDivider()
    }

    private fun showOrHideDescriptorsLabelWithDivider() {
        characteristic?.descriptors?.apply {
            tv_descriptors.visibility = if (isEmpty()) View.GONE else View.VISIBLE
            cv_descriptors.visibility = if (isEmpty()) View.GONE else View.VISIBLE
        }
    }

    fun setCharacteristicListener(listener: CharacteristicListener) {
        ib_copy.setOnClickListener {
            characteristic?.let {
                listener.onCopyCharacteristic(it)
            }
        }
        ib_edit.setOnClickListener {
            characteristic?.let {
                listener.onEditCharacteristic(it)
            }
        }
        ib_remove.setOnClickListener {
            characteristic?.let {
                listener.onRemoveCharacteristic(it)
            }
        }
    }

    interface CharacteristicListener {
        fun onCopyCharacteristic(characteristic: Characteristic)
        fun onEditCharacteristic(characteristic: Characteristic)
        fun onRemoveCharacteristic(characteristic: Characteristic)
    }

}
