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
import com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters.Characteristic16BitAdapter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.GattUtils
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.Validator
import kotlinx.android.synthetic.main.dialog_gatt_server_characteristic.*
import kotlinx.android.synthetic.main.gatt_configurator_initial_value.*
import kotlinx.android.synthetic.main.dialog_add_characteristic_properties_content.*

class CharacteristicDialog(val listener: CharacteristicChangeListener, val characteristic: Characteristic = Characteristic()) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_gatt_server_characteristic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initACTV(actv_characteristic_name, SearchMode.BY_NAME)
        initACTV(actv_characteristic_uuid, SearchMode.BY_UUID)
        initInitialValueSpinner()

        handleClickEvents()
        handleUuidChanges()
        handleNameChanges()
        handlePropertyStateChanges()
        handleInitialValueEditTextChanges()
        prepopulateFields()
    }

    private fun handleClickEvents() {
        btn_save.setOnClickListener {
            setCharacteristicState()
            listener.onCharacteristicChanged(characteristic)
            dismiss()
        }
        btn_cancel.setOnClickListener {
            dismiss()
        }
        btn_clear.setOnClickListener {
            clearAllFields()
        }
    }

    private fun prepopulateFields() {
        actv_characteristic_name.setText(characteristic.name)
        actv_characteristic_uuid.setText(characteristic.uuid?.uuid)
        prepopulateProperties()
        prepopulatePropertyTypes()
        prepopulateInitialValue()
    }

    private fun prepopulateProperties() {
        characteristic.properties.apply {
            sw_read.isChecked = containsKey(Property.READ)
            sw_write.isChecked = containsKey(Property.WRITE)
            sw_write_without_resp.isChecked = containsKey(Property.WRITE_WITHOUT_RESPONSE)
            sw_reliable_write.isChecked = containsKey(Property.RELIABLE_WRITE)
            sw_notify.isChecked = containsKey(Property.NOTIFY)
            sw_indicate.isChecked = containsKey(Property.INDICATE)
        }
    }

    private fun prepopulatePropertyTypes() {
        characteristic.properties.apply {
            this[Property.READ]?.apply {
                cb_read_bonded.isChecked = contains(Property.Type.BONDED)
                cb_read_mitm.isChecked = contains(Property.Type.AUTHENTICATED)
            }

            this[Property.WRITE]?.apply {
                cb_write_bonded.isChecked = contains(Property.Type.BONDED)
                cb_write_mitm.isChecked = contains(Property.Type.AUTHENTICATED)
                return
            }

            this[Property.WRITE_WITHOUT_RESPONSE]?.apply {
                cb_write_bonded.isChecked = contains(Property.Type.BONDED)
                cb_write_mitm.isChecked = contains(Property.Type.AUTHENTICATED)
                return
            }

            this[Property.RELIABLE_WRITE]?.apply {
                cb_write_bonded.isChecked = contains(Property.Type.BONDED)
                cb_write_mitm.isChecked = contains(Property.Type.AUTHENTICATED)
                return
            }
        }
    }

    private fun prepopulateInitialValue() {
        when (characteristic.value?.type) {
            Value.Type.USER -> {
                sp_initial_value.setSelection(POSITION_INITIAL_VALUE_EMPTY)
            }
            Value.Type.UTF_8 -> {
                sp_initial_value.setSelection(POSITION_INITIAL_VALUE_TEXT)
                et_initial_value_text.setText(characteristic.value?.value)
            }
            Value.Type.HEX -> {
                sp_initial_value.setSelection(POSITION_INITIAL_VALUE_HEX)
                et_initial_value_hex.setText(characteristic.value?.value)
            }
            else -> Unit
        }
    }

    private fun setCharacteristicState() {
        characteristic.name = actv_characteristic_name.text.toString()
        characteristic.uuid = Uuid(actv_characteristic_uuid.text.toString())
        setPropertiesState()
        setInitialValue()
    }

    private fun setPropertiesState() {
        characteristic.properties.clear()
        if (sw_read.isChecked) characteristic.properties[Property.READ] = getSelectedReadTypes()
        if (sw_write.isChecked) characteristic.properties[Property.WRITE] = getSelectedWriteTypes()
        if (sw_write_without_resp.isChecked) characteristic.properties[Property.WRITE_WITHOUT_RESPONSE] = getSelectedWriteTypes()
        if (sw_reliable_write.isChecked) characteristic.properties[Property.RELIABLE_WRITE] = getSelectedWriteTypes()
        if (sw_notify.isChecked) characteristic.properties[Property.NOTIFY] = hashSetOf()
        if (sw_indicate.isChecked) characteristic.properties[Property.INDICATE] = hashSetOf()
        handlePropertiesUsingDescriptors()
    }

    private fun handlePropertiesUsingDescriptors() {
        if (sw_reliable_write.isChecked) setReliableWritePropertyDescriptor()
        else removeReliableWritePropertyDescriptor()

        if (sw_indicate.isChecked || sw_notify.isChecked) setIndicateOrNotifyPropertyDescriptor()
        else removeIndicateOrNotifyPropertyDescriptor()
    }

    private fun setReliableWritePropertyDescriptor() {
        val result = characteristic.descriptors.filter { it.name == GattUtils.getReliableWriteDescriptor().name && it.isPredefined }
        if (result.isEmpty()) {
            characteristic.descriptors.add(GattUtils.getReliableWriteDescriptor())
        }
    }

    private fun removeReliableWritePropertyDescriptor() {
        val descriptor = characteristic.descriptors.find { it.name == GattUtils.getReliableWriteDescriptor().name && it.isPredefined }
        characteristic.descriptors.remove(descriptor)
    }

    private fun setIndicateOrNotifyPropertyDescriptor() {
        val result = characteristic.descriptors.filter { it.name == GattUtils.getIndicateOrNotifyDescriptor().name && it.isPredefined }
        if (result.isEmpty()) {
            characteristic.descriptors.add(GattUtils.getIndicateOrNotifyDescriptor())
        }
    }

    private fun removeIndicateOrNotifyPropertyDescriptor() {
        val descriptor = characteristic.descriptors.find { it.name == GattUtils.getIndicateOrNotifyDescriptor().name && it.isPredefined }
        characteristic.descriptors.remove(descriptor)
    }

    private fun getSelectedReadTypes(): HashSet<Property.Type> {
        return hashSetOf<Property.Type>().apply {
            if (cb_read_bonded.isChecked) add(Property.Type.BONDED)
            if (cb_read_mitm.isChecked) add(Property.Type.AUTHENTICATED)
        }
    }

    private fun getSelectedWriteTypes(): HashSet<Property.Type> {
        return hashSetOf<Property.Type>().apply {
            if (cb_write_bonded.isChecked) add(Property.Type.BONDED)
            if (cb_write_mitm.isChecked) add(Property.Type.AUTHENTICATED)
        }
    }

    private fun setInitialValue() {
        when (sp_initial_value.selectedItemPosition) {
            POSITION_INITIAL_VALUE_EMPTY -> {
                characteristic.value = Value(
                        value = "",
                        type = Value.Type.USER
                )
            }
            POSITION_INITIAL_VALUE_TEXT -> {
                characteristic.value = Value(
                        value = et_initial_value_text.text.toString(),
                        type = Value.Type.UTF_8,
                        length = et_initial_value_text.text.length
                )
            }
            POSITION_INITIAL_VALUE_HEX -> {
                characteristic.value = Value(
                        value = et_initial_value_hex.text.toString(),
                        type = Value.Type.HEX,
                        length = et_initial_value_hex.length() / 2
                )
            }
        }
    }

    private fun handleNameChanges() {
        actv_characteristic_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_save.isEnabled = isInputValid()
            }
        })
    }

    private fun handleUuidChanges() {
        actv_characteristic_uuid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length
                if ((len == 8 || len == 13 || len == 18 || len == 23) && count > before) actv_characteristic_uuid.append("-")
                btn_save.isEnabled = isInputValid()
            }
        })
    }

    private fun handleInitialValueEditTextChanges() {
        et_initial_value_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_save.isEnabled = isInputValid()
            }
        })

        et_initial_value_hex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_save.isEnabled = isInputValid()
            }
        })
    }

    private fun isAnyPropertyChecked(): Boolean {
        return sw_read.isChecked ||
                sw_write.isChecked ||
                sw_write_without_resp.isChecked ||
                sw_reliable_write.isChecked ||
                sw_notify.isChecked ||
                sw_indicate.isChecked
    }

    private fun handlePropertyStateChanges() {
        sw_read.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
            setPropertyParametersState(sw_read.isChecked, cb_read_bonded, cb_read_mitm)
        }
        sw_write.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
            setPropertyParametersState(sw_write.isChecked || sw_write_without_resp.isChecked || sw_reliable_write.isChecked, cb_write_bonded, cb_write_mitm)
        }
        sw_write_without_resp.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
            setPropertyParametersState(sw_write.isChecked || sw_write_without_resp.isChecked || sw_reliable_write.isChecked, cb_write_bonded, cb_write_mitm)
        }
        sw_reliable_write.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
            setPropertyParametersState(sw_write.isChecked || sw_write_without_resp.isChecked || sw_reliable_write.isChecked, cb_write_bonded, cb_write_mitm)
        }
        sw_notify.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
        }
        sw_indicate.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
        }
    }

    private fun setPropertyParametersState(switchState: Boolean, cbBonded: CheckBox, cbMitm: CheckBox) {
        cbBonded.isEnabled = switchState
        cbMitm.isEnabled = switchState
        if (!switchState) {
            cbBonded.isChecked = false
            cbMitm.isChecked = false
        }
    }

    private fun isInputValid(): Boolean {
        return isAnyPropertyChecked()
                && actv_characteristic_name.text.toString().isNotEmpty()
                && isUuidValid(actv_characteristic_uuid.text.toString())
                && isInitialValueValid()
    }

    private fun isInitialValueValid(): Boolean {
        when (sp_initial_value.selectedItemPosition) {
            POSITION_INITIAL_VALUE_EMPTY -> {
                return true
            }
            POSITION_INITIAL_VALUE_TEXT -> {
                return et_initial_value_text.text.toString().isNotEmpty()
            }
            POSITION_INITIAL_VALUE_HEX -> {
                return Validator.isHexValid(et_initial_value_hex.text.toString())
            }
        }
        return true
    }

    private fun isUuidValid(uuid: String): Boolean {
        return Validator.is16BitUuidValid(uuid) || Validator.is128BitUuidValid(uuid)
    }

    private fun initACTV(actv: AutoCompleteTextView, searchMode: SearchMode) {
        val adapter = Characteristic16BitAdapter(requireContext(), GattUtils.get16BitCharacteristics(), searchMode)
        actv.setAdapter(adapter)

        actv.setOnItemClickListener { _, _, position, _ ->
            val characteristic = adapter.getItem(position)
            actv_characteristic_name.setText(characteristic?.name)
            actv_characteristic_uuid.setText(characteristic?.getIdentifierAsString())
            actv.setSelection(actv.length())
            hideKeyboard()
        }
    }

    private fun initInitialValueSpinner() {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_layout, resources.getStringArray(R.array.gatt_configurator_initial_value))
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        sp_initial_value.adapter = adapter

        handleInitialValueSelection()
    }

    private fun handleInitialValueSelection() {
        sp_initial_value.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                btn_save.isEnabled = isInputValid()
                et_initial_value_text.visibility = if (position == POSITION_INITIAL_VALUE_TEXT) View.VISIBLE else View.GONE
                ll_initial_value_hex.visibility = if (position == POSITION_INITIAL_VALUE_HEX) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun clearAllFields() {
        actv_characteristic_name.setText("")
        actv_characteristic_uuid.setText("")
        sw_read.isChecked = true
        sw_write.isChecked = false
        sw_write_without_resp.isChecked = false
        sw_reliable_write.isChecked = false
        sw_notify.isChecked = false
        sw_indicate.isChecked = false
        cb_read_bonded.isChecked = false
        cb_read_mitm.isChecked = false
        cb_write_bonded.isChecked = false
        cb_write_mitm.isChecked = false
        sp_initial_value.setSelection(0)
        et_initial_value_text.setText("")
        et_initial_value_hex.setText("")
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