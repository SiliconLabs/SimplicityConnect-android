package com.siliconlabs.bledemo.features.configure.gatt_configurator.views

import android.content.Context
import android.text.Html
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.get
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ViewGattCharacteristicBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.activities.GattServerActivity
import com.siliconlabs.bledemo.features.configure.gatt_configurator.dialogs.DescriptorDialog
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Characteristic
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Descriptor
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.removeAsking

import com.siliconlabs.bledemo.features.configure.gatt_configurator.views.GattDescriptorView.DescriptorListener


class GattCharacteristicView(context: Context, attributeSet: AttributeSet? = null) :
    FrameLayout(context, attributeSet) {
    private var characteristic: Characteristic? = null
    lateinit var gattCharacterBinding: ViewGattCharacteristicBinding

    constructor(context: Context, characteristic: Characteristic) : this(context) {
        this.characteristic = characteristic
       gattCharacterBinding = ViewGattCharacteristicBinding.inflate(LayoutInflater.from(context),this,true)

        initView(characteristic)
        handleAddDescriptorClickEvent(characteristic.descriptors)
    }

    private fun initView(characteristic: Characteristic) {
        gattCharacterBinding.tvCharacteristicName.text = characteristic.name
        val uuidHtml = buildString {
            append("<b>")
            append(context.getString(R.string.UUID_colon_space))
            append("</b>")
            append(characteristic.uuid?.getAsFormattedText())
        }
        gattCharacterBinding.tvCharacteristicUuid.text = Html.fromHtml(uuidHtml, Html.FROM_HTML_MODE_LEGACY)
        showSelectedProperties(characteristic)
        showOrHideDescriptorsLabelWithDivider()
        initDescriptors()
    }

    fun refreshView() {
        characteristic?.let {
            initView(it)
        }
        gattCharacterBinding.root.invalidate()
    }

    private fun hideAllProperties() {

        gattCharacterBinding.tvPropertyRead.visibility = View.GONE
        gattCharacterBinding.tvPropertyWrite.visibility = View.GONE
        gattCharacterBinding.tvPropertyIndicate.visibility = View.GONE
        gattCharacterBinding.tvPropertyNotify.visibility = View.GONE
    }

    private fun showSelectedProperties(characteristic: Characteristic) {
        hideAllProperties()
        characteristic.properties.apply {
            if (containsKey(Property.READ)) gattCharacterBinding.tvPropertyRead.visibility = View.VISIBLE
            if (containsKey(Property.WRITE)) gattCharacterBinding.tvPropertyWrite.visibility = View.VISIBLE
            if (containsKey(Property.WRITE_WITHOUT_RESPONSE)) gattCharacterBinding.tvPropertyWrite.visibility =
                View.VISIBLE
            if (containsKey(Property.RELIABLE_WRITE)) gattCharacterBinding.tvPropertyWrite.visibility =
                View.VISIBLE
            if (containsKey(Property.INDICATE)) gattCharacterBinding.tvPropertyIndicate.visibility = View.VISIBLE
            if (containsKey(Property.NOTIFY)) gattCharacterBinding.tvPropertyNotify.visibility = View.VISIBLE
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_gatt_characteristic, this, true)
    }

    private fun initDescriptors() {
        gattCharacterBinding.llDescriptors.removeAllViews()
        for (descriptor in characteristic?.descriptors!!) {
            val view = GattDescriptorView(context, descriptor)
            handleDescriptorClickEvents(view, characteristic?.descriptors!!)
            gattCharacterBinding.llDescriptors.addView(view)
        }
    }

    private fun handleDescriptorClickEvents(
        view: GattDescriptorView,
        descriptors: ArrayList<Descriptor>
    ) {
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

        gattCharacterBinding.btnAddDescriptor.setOnClickListener {
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
        gattCharacterBinding.llDescriptors.addView(view)
        showOrHideDescriptorsLabelWithDivider()
    }

    private fun copyDescriptor(descriptor: Descriptor, descriptors: ArrayList<Descriptor>) {
        val copiedDescriptor = descriptor.deepCopy()
        val view = GattDescriptorView(context, copiedDescriptor)

        descriptors.add(copiedDescriptor)
        handleDescriptorClickEvents(view, descriptors)
        gattCharacterBinding.llDescriptors.addView(view)
    }

    private fun editDescriptor(
        descriptor: Descriptor,
        descriptors: ArrayList<Descriptor>) {
        DescriptorDialog(object : DescriptorDialog.DescriptorChangeListener {
            override fun onDescriptorChanged(descriptor: Descriptor) {
                val index = descriptors.indexOf(descriptor)
                val view  = gattCharacterBinding.llDescriptors.getChildAt(index)
                if(view is GattDescriptorView){
                    view.refreshView()
                }else{
                    Log.e("EditDescriptor", "View at index $index is not a GattDescriptorView.")
                }

            }
        }, descriptor).show(
            (context as GattServerActivity).supportFragmentManager,
            "dialog_descriptor"
        )
    }

    private fun removeDescriptor(descriptor: Descriptor, descriptors: ArrayList<Descriptor>) {
        val index = descriptors.indexOf(descriptor)
        gattCharacterBinding.llDescriptors.removeViewAt(index)
        descriptors.remove(descriptor)

        showOrHideDescriptorsLabelWithDivider()
    }

    private fun showOrHideDescriptorsLabelWithDivider() {
        characteristic?.descriptors?.apply {

            gattCharacterBinding.tvDescriptors.visibility = if (isEmpty()) View.GONE else View.VISIBLE
            gattCharacterBinding.cvDescriptors.visibility = if (isEmpty()) View.GONE else View.VISIBLE
        }
    }

    fun setCharacteristicListener(listener: CharacteristicListener) {
        gattCharacterBinding.ibCopy.setOnClickListener {
            characteristic?.let {
                listener.onCopyCharacteristic(it)
            }
        }
        gattCharacterBinding.ibEdit.setOnClickListener {
            characteristic?.let {
                listener.onEditCharacteristic(it)
            }
        }
        gattCharacterBinding.ibRemove.setOnClickListener {
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
