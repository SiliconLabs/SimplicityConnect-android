package com.siliconlabs.bledemo.features.configure.gatt_configurator.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogGattServerCharacteristicBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters.Characteristic16BitAdapter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.GattUtils
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.Validator

//import kotlinx.android.synthetic.main.dialog_gatt_server_characteristic.*
//import kotlinx.android.synthetic.main.gatt_configurator_initial_value.*
//import kotlinx.android.synthetic.main.dialog_add_characteristic_properties_content.*

class CharacteristicDialog(
    val listener: CharacteristicChangeListener,
    val characteristic: Characteristic = Characteristic()
) : BaseDialogFragment() {
    private lateinit var binding: DialogGattServerCharacteristicBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogGattServerCharacteristicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initACTV(binding.actvCharacteristicName, SearchMode.BY_NAME)
        initACTV(binding.actvCharacteristicUuid, SearchMode.BY_UUID)
        initInitialValueSpinner()

        handleClickEvents()
        handleUuidChanges()
        handleNameChanges()
        handlePropertyStateChanges()
        handleInitialValueEditTextChanges()
        prepopulateFields()
    }

    private fun handleClickEvents() {
        binding.btnSave.setOnClickListener {
            setCharacteristicState()
            listener.onCharacteristicChanged(characteristic)
            dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.btnClear.setOnClickListener {
            clearAllFields()
        }
    }

    private fun prepopulateFields() {
        binding.actvCharacteristicName.setText(characteristic.name)
        binding.actvCharacteristicUuid.setText(characteristic.uuid?.uuid)
        prepopulateProperties()
        prepopulatePropertyTypes()
        prepopulateInitialValue()
    }

    private fun prepopulateProperties() {
        // binding.propertiesContent.apply {
        characteristic.properties.apply {
            binding.propertiesContent.swRead.isChecked = containsKey(Property.READ)
            binding.propertiesContent.swWrite.isChecked = containsKey(Property.WRITE)
            binding.propertiesContent.swWriteWithoutResp.isChecked =
                containsKey(Property.WRITE_WITHOUT_RESPONSE)
            binding.propertiesContent.swReliableWrite.isChecked =
                containsKey(Property.RELIABLE_WRITE)
            binding.propertiesContent.swNotify.isChecked = containsKey(Property.NOTIFY)
            binding.propertiesContent.swIndicate.isChecked = containsKey(Property.INDICATE)
        }

    }

    private fun prepopulatePropertyTypes() {
        characteristic.properties.apply {
            this[Property.READ]?.apply {
                binding.propertiesContent.cbReadBonded.isChecked = contains(Property.Type.BONDED)
                binding.propertiesContent.cbReadMitm.isChecked =
                    contains(Property.Type.AUTHENTICATED)
            }

            this[Property.WRITE]?.apply {
                binding.propertiesContent.cbWriteBonded.isChecked = contains(Property.Type.BONDED)
                binding.propertiesContent.cbWriteMitm.isChecked =
                    contains(Property.Type.AUTHENTICATED)
                return
            }

            this[Property.WRITE_WITHOUT_RESPONSE]?.apply {
                binding.propertiesContent.cbWriteBonded.isChecked = contains(Property.Type.BONDED)
                binding.propertiesContent.cbWriteMitm.isChecked =
                    contains(Property.Type.AUTHENTICATED)
                return
            }

            this[Property.RELIABLE_WRITE]?.apply {
                binding.propertiesContent.cbWriteBonded.isChecked = contains(Property.Type.BONDED)
                binding.propertiesContent.cbWriteMitm.isChecked =
                    contains(Property.Type.AUTHENTICATED)
                return
            }
        }
    }

    private fun prepopulateInitialValue() {
        when (characteristic.value?.type) {
            Value.Type.USER -> {
                binding.initialValue.spInitialValue.setSelection(POSITION_INITIAL_VALUE_EMPTY)
            }

            Value.Type.UTF_8 -> {
                binding.initialValue.spInitialValue.setSelection(POSITION_INITIAL_VALUE_TEXT)
                binding.initialValue.etInitialValueText.setText(characteristic.value?.value)
            }

            Value.Type.HEX -> {
                binding.initialValue.spInitialValue.setSelection(POSITION_INITIAL_VALUE_HEX)
                binding.initialValue.etInitialValueHex.setText(characteristic.value?.value)
            }

            else -> Unit
        }
    }

    private fun setCharacteristicState() {
        characteristic.name = binding.actvCharacteristicName.text.toString()
        characteristic.uuid = Uuid(binding.actvCharacteristicUuid.text.toString())
        setPropertiesState()
        setInitialValue()
    }

    private fun setPropertiesState() {
        characteristic.properties.clear()
        if (binding.propertiesContent.swRead.isChecked) characteristic.properties[Property.READ] =
            getSelectedReadTypes()
        if (binding.propertiesContent.swWrite.isChecked) characteristic.properties[Property.WRITE] =
            getSelectedWriteTypes()
        if (binding.propertiesContent.swWriteWithoutResp.isChecked) characteristic.properties[Property.WRITE_WITHOUT_RESPONSE] =
            getSelectedWriteTypes()
        if (binding.propertiesContent.swReliableWrite.isChecked) characteristic.properties[Property.RELIABLE_WRITE] =
            getSelectedWriteTypes()
        if (binding.propertiesContent.swNotify.isChecked) characteristic.properties[Property.NOTIFY] =
            hashSetOf()
        if (binding.propertiesContent.swIndicate.isChecked) characteristic.properties[Property.INDICATE] =
            hashSetOf()
        handlePropertiesUsingDescriptors()
    }

    private fun handlePropertiesUsingDescriptors() {
        if (binding.propertiesContent.swReliableWrite.isChecked) setReliableWritePropertyDescriptor()
        else removeReliableWritePropertyDescriptor()

        if (binding.propertiesContent.swIndicate.isChecked || binding.propertiesContent.swNotify.isChecked) setIndicateOrNotifyPropertyDescriptor()
        else removeIndicateOrNotifyPropertyDescriptor()
    }

    private fun setReliableWritePropertyDescriptor() {
        val result =
            characteristic.descriptors.filter { it.name == GattUtils.getReliableWriteDescriptor().name && it.isPredefined }
        if (result.isEmpty()) {
            characteristic.descriptors.add(GattUtils.getReliableWriteDescriptor())
        }
    }

    private fun removeReliableWritePropertyDescriptor() {
        val descriptor =
            characteristic.descriptors.find { it.name == GattUtils.getReliableWriteDescriptor().name && it.isPredefined }
        characteristic.descriptors.remove(descriptor)
    }

    private fun setIndicateOrNotifyPropertyDescriptor() {
        val result =
            characteristic.descriptors.filter { it.name == GattUtils.getIndicateOrNotifyDescriptor().name && it.isPredefined }
        if (result.isEmpty()) {
            characteristic.descriptors.add(GattUtils.getIndicateOrNotifyDescriptor())
        }
    }

    private fun removeIndicateOrNotifyPropertyDescriptor() {
        val descriptor =
            characteristic.descriptors.find { it.name == GattUtils.getIndicateOrNotifyDescriptor().name && it.isPredefined }
        characteristic.descriptors.remove(descriptor)
    }

    private fun getSelectedReadTypes(): HashSet<Property.Type> {
        return hashSetOf<Property.Type>().apply {
            if (binding.propertiesContent.cbReadBonded.isChecked) add(Property.Type.BONDED)
            if (binding.propertiesContent.cbReadMitm.isChecked) add(Property.Type.AUTHENTICATED)
        }
    }

    private fun getSelectedWriteTypes(): HashSet<Property.Type> {
        return hashSetOf<Property.Type>().apply {
            if (binding.propertiesContent.cbWriteBonded.isChecked) add(Property.Type.BONDED)
            if (binding.propertiesContent.cbWriteMitm.isChecked) add(Property.Type.AUTHENTICATED)
        }
    }

    private fun setInitialValue() {
        when (binding.initialValue.spInitialValue.selectedItemPosition) {
            POSITION_INITIAL_VALUE_EMPTY -> {
                characteristic.value = Value(
                    value = "",
                    type = Value.Type.USER
                )
            }

            POSITION_INITIAL_VALUE_TEXT -> {
                characteristic.value = Value(
                    value = binding.initialValue.etInitialValueText.text.toString(),
                    type = Value.Type.UTF_8,
                    length = binding.initialValue.etInitialValueText.text.length
                )
            }

            POSITION_INITIAL_VALUE_HEX -> {
                characteristic.value = Value(
                    value = binding.initialValue.etInitialValueHex.text.toString(),
                    type = Value.Type.HEX,
                    length = binding.initialValue.etInitialValueHex.length() / 2
                )
            }
        }
    }

    private fun handleNameChanges() {
        binding.actvCharacteristicName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSave.isEnabled = isInputValid()
            }
        })
    }

    private fun handleUuidChanges() {
        binding.actvCharacteristicUuid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length
                if ((len == 8 || len == 13 || len == 18 || len == 23) && count > before) binding.actvCharacteristicUuid.append(
                    "-"
                )
                binding.btnSave.isEnabled = isInputValid()
            }
        })
    }

    private fun handleInitialValueEditTextChanges() {
        binding.initialValue.etInitialValueText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSave.isEnabled = isInputValid()
            }
        })

        binding.initialValue.etInitialValueHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSave.isEnabled = isInputValid()
            }
        })
    }

    private fun isAnyPropertyChecked(): Boolean {
        return binding.propertiesContent.swRead.isChecked ||
                binding.propertiesContent.swWrite.isChecked ||
                binding.propertiesContent.swWriteWithoutResp.isChecked ||
                binding.propertiesContent.swReliableWrite.isChecked ||
                binding.propertiesContent.swNotify.isChecked ||
                binding.propertiesContent.swIndicate.isChecked
    }

    private fun handlePropertyStateChanges() {
        binding.propertiesContent.swRead.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
            setPropertyParametersState(
                binding.propertiesContent.swRead.isChecked,
                binding.propertiesContent.cbReadBonded,
                binding.propertiesContent.cbReadMitm
            )
        }
        binding.propertiesContent.swWrite.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
            setPropertyParametersState(
                binding.propertiesContent.swWrite.isChecked || binding.propertiesContent.swWriteWithoutResp.isChecked || binding.propertiesContent.swReliableWrite.isChecked,
                binding.propertiesContent.cbWriteBonded,
                binding.propertiesContent.cbWriteMitm
            )
        }
        binding.propertiesContent.swWriteWithoutResp.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
            setPropertyParametersState(
                binding.propertiesContent.swWrite.isChecked || binding.propertiesContent.swWriteWithoutResp.isChecked || binding.propertiesContent.swReliableWrite.isChecked,
                binding.propertiesContent.cbWriteBonded,
                binding.propertiesContent.cbWriteMitm
            )
        }
        binding.propertiesContent.swReliableWrite.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
            setPropertyParametersState(
                binding.propertiesContent.swWrite.isChecked || binding.propertiesContent.swWriteWithoutResp.isChecked || binding.propertiesContent.swReliableWrite.isChecked,
                binding.propertiesContent.cbWriteBonded,
                binding.propertiesContent.cbWriteMitm
            )
        }
        binding.propertiesContent.swNotify.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
        }
        binding.propertiesContent.swIndicate.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
        }
    }

    private fun setPropertyParametersState(
        switchState: Boolean,
        cbBonded: CheckBox,
        cbMitm: CheckBox
    ) {
        cbBonded.isEnabled = switchState
        cbMitm.isEnabled = switchState
        if (!switchState) {
            cbBonded.isChecked = false
            cbMitm.isChecked = false
        }
    }

    private fun isInputValid(): Boolean {
        return isAnyPropertyChecked()
                && binding.actvCharacteristicName.text.toString().isNotEmpty()
                && isUuidValid(binding.actvCharacteristicUuid.text.toString())
                && isInitialValueValid()
    }

    private fun isInitialValueValid(): Boolean {
        when (binding.initialValue.spInitialValue.selectedItemPosition) {
            POSITION_INITIAL_VALUE_EMPTY -> {
                return true
            }

            POSITION_INITIAL_VALUE_TEXT -> {
                return binding.initialValue.etInitialValueText.text.toString().isNotEmpty()
            }

            POSITION_INITIAL_VALUE_HEX -> {
                return Validator.isHexValid(binding.initialValue.etInitialValueHex.text.toString())
            }
        }
        return true
    }

    private fun isUuidValid(uuid: String): Boolean {
        return Validator.is16BitUuidValid(uuid) || Validator.is128BitUuidValid(uuid)
    }

    private fun initACTV(actv: AutoCompleteTextView, searchMode: SearchMode) {
        val adapter = Characteristic16BitAdapter(
            requireContext(),
            GattUtils.get16BitCharacteristics(),
            searchMode
        )
        actv.setAdapter(adapter)

        actv.setOnItemClickListener { _, _, position, _ ->
            val characteristic = adapter.getItem(position)
            binding.actvCharacteristicName.setText(characteristic?.name)
            binding.actvCharacteristicUuid.setText(characteristic?.getIdentifierAsString())
            actv.setSelection(actv.length())
            hideKeyboard()
        }
    }

    private fun initInitialValueSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_layout,
            resources.getStringArray(R.array.gatt_configurator_initial_value)
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        binding.initialValue.spInitialValue.adapter = adapter

        handleInitialValueSelection()
    }

    private fun handleInitialValueSelection() {
        binding.initialValue.spInitialValue.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    binding.btnSave.isEnabled = isInputValid()
                    binding.initialValue.etInitialValueText.visibility =
                        if (position == POSITION_INITIAL_VALUE_TEXT) View.VISIBLE else View.GONE
                    binding.initialValue.llInitialValueHex.visibility =
                        if (position == POSITION_INITIAL_VALUE_HEX) View.VISIBLE else View.GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
    }

    private fun clearAllFields() {
        binding.actvCharacteristicName.setText("")
        binding.actvCharacteristicUuid.setText("")
        binding.propertiesContent.swRead.isChecked = true
        binding.propertiesContent.swWrite.isChecked = false
        binding.propertiesContent.swWriteWithoutResp.isChecked = false
        binding.propertiesContent.swReliableWrite.isChecked = false
        binding.propertiesContent.swNotify.isChecked = false
        binding.propertiesContent.swIndicate.isChecked = false
        binding.propertiesContent.cbReadBonded.isChecked = false
        binding.propertiesContent.cbReadMitm.isChecked = false
        binding.propertiesContent.cbWriteBonded.isChecked = false
        binding.propertiesContent.cbWriteMitm.isChecked = false
        binding.initialValue.spInitialValue.setSelection(0)
        binding.initialValue.etInitialValueText.setText("")
        binding.initialValue.etInitialValueHex.setText("")
    }

    interface CharacteristicChangeListener {
        fun onCharacteristicChanged(characteristic: Characteristic)
    }

    companion object {
        private const val POSITION_INITIAL_VALUE_EMPTY = 0
        private const val POSITION_INITIAL_VALUE_TEXT = 1
        private const val POSITION_INITIAL_VALUE_HEX = 2
    }
}