package com.siliconlabs.bledemo.features.scan.browser.views

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.bluetooth.parsing.Common
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import com.siliconlabs.bledemo.features.scan.browser.models.Mapping
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.CharacteristicContainerBinding
import com.siliconlabs.bledemo.databinding.PropertyContainerBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property
import com.siliconlabs.bledemo.utils.UuidUtils

class CharacteristicItemContainer(
        context: Context,
        private val callback: Callback,
        val characteristic: BluetoothGattCharacteristic,
        private val isMandatorySystemService: Boolean,
        private val characteristicDictionaryUuids: Map<String, Mapping>
) : LinearLayout(context) {

    private var nameType: ServiceItemContainer.NameType = ServiceItemContainer.NameType.UNKNOWN

    private val _binding: CharacteristicContainerBinding =
            CharacteristicContainerBinding.inflate(LayoutInflater.from(context), this, true)
    private val propertyBindings = mutableMapOf<Property, PropertyContainerBinding>()

    val characteristicExpansion: LinearLayout

    init {
        initViews()
        addPropertyContainers()
        setupUiListeners()

        characteristicExpansion = _binding.characteristicExpansion
        characteristicExpansion.id = View.generateViewId()
    }

    private fun initViews() {
        _binding.apply {
            characteristicTitle.text = getCharacteristicName()
            tvRenameChar.visibility = when (nameType) {
                ServiceItemContainer.NameType.ENGINE, ServiceItemContainer.NameType.CUSTOM -> View.GONE
                ServiceItemContainer.NameType.USER, ServiceItemContainer.NameType.UNKNOWN -> View.VISIBLE
            }
            characteristicUuid.text = UuidUtils.getUuidText(characteristic.uuid)
        }
    }

    private fun addPropertyContainers() {
        Common.getPropertiesList(characteristic.properties).also { properties ->
            if (isCharacteristicWritable(properties)) { // one "Write" icon for all types
                properties.remove(Property.RELIABLE_WRITE)
                properties.remove(Property.WRITE_WITHOUT_RESPONSE)
            }
        }.forEach { property ->
            PropertyContainerBinding.inflate(LayoutInflater.from(context)).apply {
                propertyText.text = context.getString(getIconText(property))
                propertyIcon.setImageDrawable(ContextCompat.getDrawable(context, getIconDrawable(property)))

                if (isMandatorySystemService) {
                    root.isEnabled = false
                    root.alpha = 0.3f // show as disabled
                } else setupPropertyUiListener(this, property)
            }.also {
                _binding.characteristicPropsContainer.addView(it.root)
                propertyBindings[property] = it
            }
        }
    }

    private fun setupPropertyUiListener(_binding: PropertyContainerBinding, property: Property) {
        _binding.root.setOnClickListener {
            _binding.propertyContainer.startAnimation(AnimationUtils.loadAnimation(
                    context, R.anim.property_image_click
            ))
            callback.onPropertyClicked(
                    property,
                    this@CharacteristicItemContainer
            )
        }
    }

    private fun isCharacteristicWritable(properties: List<Property>) : Boolean {
        return properties.contains(Property.WRITE_WITHOUT_RESPONSE) ||
                properties.contains(Property.WRITE) ||
                properties.contains(Property.RELIABLE_WRITE)
    }

    private fun setupUiListeners() {
        _binding.apply {
            tvRenameChar.setOnClickListener { callback.onRenameClicked(this@CharacteristicItemContainer) }
        }
    }

    fun hideDescriptorsContainer() {
        _binding.descriptorsView.visibility = View.GONE
    }

    fun addDescriptorContainer(container: DescriptorContainer) {
        _binding.linearLayoutDescriptor.addView(container)
    }

    fun getCharacteristicName() : String {
        return Engine.getCharacteristic(characteristic.uuid)?.let { char ->
            nameType = ServiceItemContainer.NameType.ENGINE
            char.name
        } ?: run {
            Common.getCustomCharacteristicName(characteristic.uuid, context)?.let { name ->
                nameType = ServiceItemContainer.NameType.CUSTOM
                name
            }
        } ?: run {
            characteristicDictionaryUuids[UuidUtils.getUuidText(characteristic.uuid)]?.let { mapping ->
                nameType = ServiceItemContainer.NameType.USER
                mapping.name
            }
        } ?: run {
            nameType = ServiceItemContainer.NameType.UNKNOWN
            context.getString(R.string.unknown_characteristic_label)
        }
    }

    fun setCharacteristicName(newName: String) {
        nameType = ServiceItemContainer.NameType.USER
        _binding.characteristicTitle.text = newName
    }

    fun getPropertyBinding(property: Property) : PropertyContainerBinding? {
        return propertyBindings[property]
    }

    @DrawableRes
    private fun getIconDrawable(property: Property) : Int {
        return when (property) {
            Property.BROADCAST -> R.drawable.ic_debug_prop_broadcast
            Property.READ -> R.drawable.redesign_ic_property_read
            Property.WRITE -> R.drawable.redesign_ic_property_write
            Property.NOTIFY -> R.drawable.redesign_ic_property_notify
            Property.INDICATE -> R.drawable.redesign_ic_property_indicate
            Property.RELIABLE_WRITE -> R.drawable.ic_debug_prop_signed_write
            Property.EXTENDED_PROPS -> R.drawable.ic_debug_prop_ext
            else -> R.drawable.ic_debug_prop_ext
        }
    }

    @StringRes
    private fun getIconText(property: Property) : Int {
        return when (property) {
            Property.BROADCAST -> R.string.property_broadcast
            Property.READ -> R.string.property_read
            Property.WRITE -> R.string.property_write
            Property.WRITE_WITHOUT_RESPONSE -> R.string.property_write_no_response
            Property.RELIABLE_WRITE -> R.string.property_signed_write
            Property.NOTIFY -> R.string.property_notify
            Property.INDICATE -> R.string.property_indicate
            Property.EXTENDED_PROPS -> R.string.property_extended_props
        }
    }

    interface Callback {
        fun onRenameClicked(container: CharacteristicItemContainer)
        fun onPropertyClicked(
                property: Property,
                characteristicContainer: CharacteristicItemContainer
        )
    }

}