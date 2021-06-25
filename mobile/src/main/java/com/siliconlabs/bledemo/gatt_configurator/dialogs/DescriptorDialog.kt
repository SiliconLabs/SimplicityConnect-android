package com.siliconlabs.bledemo.gatt_configurator.dialogs

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
import androidx.appcompat.widget.SwitchCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.BaseDialogFragment
import com.siliconlabs.bledemo.gatt_configurator.adapters.Descriptor16BitAdapter
import com.siliconlabs.bledemo.gatt_configurator.models.*
import com.siliconlabs.bledemo.gatt_configurator.utils.GattUtils
import com.siliconlabs.bledemo.gatt_configurator.utils.Validator
import kotlinx.android.synthetic.main.dialog_gatt_server_descriptor.*
import kotlinx.android.synthetic.main.dialog_add_descriptor_properties_content.*
import kotlinx.android.synthetic.main.dialog_gatt_configurator_initial_value.*

class DescriptorDialog(val listener: DescriptorChangeListener, val descriptor: Descriptor = Descriptor()) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_gatt_server_descriptor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initACTV(actv_descriptor_name, SearchMode.BY_NAME)
        initACTV(actv_descriptor_uuid, SearchMode.BY_UUID)
        initInitialValueSpinner()

        handleClickEvents()
        handleNameChanges()
        handleUuidChanges()
        handlePropertyStateChanges()
        handleInitialValueEditTextChanges()
        prepopulateFields()
    }

    private fun handleClickEvents() {
        btn_save.setOnClickListener {
            setDescriptorState()
            listener.onDescriptorChanged(descriptor)
            dismiss()
        }

        btn_clear.setOnClickListener {
            clearAllFields()
        }

        btn_cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun prepopulateFields() {
        actv_descriptor_name.setText(descriptor.name)
        actv_descriptor_uuid.setText(descriptor.uuid?.uuid)
        prepopulateProperties()
        prepopulatePropertyTypes()
        prepopulateInitialValue()
    }

    private fun prepopulateProperties() {
        descriptor.properties.apply {
            sw_read.isChecked = containsKey(Property.READ)
            sw_write.isChecked = containsKey(Property.WRITE)
        }
    }

    private fun prepopulatePropertyTypes() {
        descriptor.properties.apply {
            this[Property.READ]?.apply {
                cb_read_bonded.isChecked = contains(Property.Type.BONDED)
                cb_read_mitm.isChecked = contains(Property.Type.AUTHENTICATED)
            }

            this[Property.WRITE]?.apply {
                cb_write_bonded.isChecked = contains(Property.Type.BONDED)
                cb_write_mitm.isChecked = contains(Property.Type.AUTHENTICATED)
            }
        }
    }

    private fun prepopulateInitialValue() {
        when (descriptor.value?.type) {
            Value.Type.USER -> {
                sp_initial_value.setSelection(POSITION_INITIAL_VALUE_EMPTY)
            }
            Value.Type.UTF_8 -> {
                sp_initial_value.setSelection(POSITION_INITIAL_VALUE_TEXT)
                et_initial_value_text.setText(descriptor.value?.value)
            }
            Value.Type.HEX -> {
                sp_initial_value.setSelection(POSITION_INITIAL_VALUE_HEX)
                et_initial_value_hex.setText(descriptor.value?.value)
            }

        }
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

    private fun initACTV(actv: AutoCompleteTextView, searchMode: SearchMode) {
        val adapter = Descriptor16BitAdapter(requireContext(), GattUtils.get16BitDescriptors(), searchMode)
        actv.setAdapter(adapter)

        actv.setOnItemClickListener { _, _, position, _ ->
            val descriptor = adapter.getItem(position)
            actv_descriptor_name.setText(descriptor?.name)
            actv_descriptor_uuid.setText(descriptor?.getIdentifierAsString())
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
                et_initial_value_text.visibility = if (position == 1) View.VISIBLE else View.GONE
                ll_initial_value_hex.visibility = if (position == 2) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun handleNameChanges() {
        actv_descriptor_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_save.isEnabled = isInputValid()
            }
        })
    }

    private fun handleUuidChanges() {
        actv_descriptor_uuid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length
                if ((len == 8 || len == 13 || len == 18 || len == 23) && count > before) actv_descriptor_uuid.append("-")
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

    private fun handlePropertyStateChanges() {
        sw_read.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
            setPropertyParametersState(sw_read, cb_read_bonded, cb_read_mitm)
        }
        sw_write.setOnCheckedChangeListener { _, _ ->
            btn_save.isEnabled = isInputValid()
            setPropertyParametersState(sw_write, cb_write_bonded, cb_write_mitm)
        }
    }

    private fun setPropertyParametersState(switch: SwitchCompat, cbBonded: CheckBox, cbMitm: CheckBox) {
        cbBonded.isEnabled = switch.isChecked
        cbMitm.isEnabled = switch.isChecked
        if(!switch.isChecked) {
            cbBonded.isChecked = false
            cbMitm.isChecked = false
        }
    }

    private fun setDescriptorState() {
        descriptor.name = actv_descriptor_name.text.toString()
        descriptor.uuid = Uuid(actv_descriptor_uuid.text.toString())
        setPropertiesState()
        setInitialValue()
    }

    private fun setPropertiesState() {
        descriptor.properties.clear()
        if (sw_read.isChecked) descriptor.properties[Property.READ] = getSelectedReadTypes()
        if (sw_write.isChecked) descriptor.properties[Property.WRITE] = getSelectedWriteTypes()
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
                descriptor.value = Value(
                        value = "",
                        type = Value.Type.USER
                )
            }
            POSITION_INITIAL_VALUE_TEXT -> {
                descriptor.value = Value(
                        value = et_initial_value_text.text.toString(),
                        type = Value.Type.UTF_8,
                        length = et_initial_value_text.text.length
                )
            }
            POSITION_INITIAL_VALUE_HEX -> {
                descriptor.value = Value(
                        value = et_initial_value_hex.text.toString(),
                        type = Value.Type.HEX,
                        length = et_initial_value_text.length() / 2
                )
            }
        }
    }

    private fun isAnyPropertyChecked(): Boolean {
        return sw_read.isChecked ||
                sw_write.isChecked
    }

    private fun isUuidValid(uuid: String): Boolean {
        return Validator.is16BitUuidValid(uuid) || Validator.is128BitUuidValid(uuid)
    }

    private fun isInputValid(): Boolean {
        return isAnyPropertyChecked()
                && actv_descriptor_name.text.toString().isNotEmpty()
                && isUuidValid(actv_descriptor_uuid.text.toString())
                && isInitialValueValid()
    }

    private fun clearAllFields() {
        actv_descriptor_name.setText("")
        actv_descriptor_uuid.setText("")
        sw_read.isChecked = true
        sw_write.isChecked = false
        cb_read_bonded.isChecked = false
        cb_read_mitm.isChecked = false
        cb_write_bonded.isChecked = false
        cb_write_mitm.isChecked = false
        sp_initial_value.setSelection(0)
        et_initial_value_text.setText("")
        et_initial_value_hex.setText("")
    }

    interface DescriptorChangeListener {
        fun onDescriptorChanged(descriptor: Descriptor)
    }

    companion object {
        private const val POSITION_INITIAL_VALUE_EMPTY = 0
        private const val POSITION_INITIAL_VALUE_TEXT = 1
        private const val POSITION_INITIAL_VALUE_HEX = 2
    }
}