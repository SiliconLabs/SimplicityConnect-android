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
import androidx.appcompat.widget.SwitchCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogGattServerDescriptorBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters.Descriptor16BitAdapter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.*
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.GattUtils
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.Validator

//import kotlinx.android.synthetic.main.dialog_gatt_server_descriptor.*
//import kotlinx.android.synthetic.main.dialog_add_descriptor_properties_content.*
//import kotlinx.android.synthetic.main.gatt_configurator_initial_value.*

class DescriptorDialog(
    val listener: DescriptorChangeListener,
    val descriptor: Descriptor = Descriptor()
) : BaseDialogFragment() {
    private lateinit var binding: DialogGattServerDescriptorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogGattServerDescriptorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initACTV(binding.actvDescriptorName, SearchMode.BY_NAME)
        initACTV(binding.actvDescriptorUuid, SearchMode.BY_UUID)
        initInitialValueSpinner()

        handleClickEvents()
        handleNameChanges()
        handleUuidChanges()
        handlePropertyStateChanges()
        handleInitialValueEditTextChanges()
        prepopulateFields()
    }

    private fun handleClickEvents() {
        binding.btnSave.setOnClickListener {
            setDescriptorState()
            listener.onDescriptorChanged(descriptor)
            dismiss()
        }

        binding.btnClear.setOnClickListener {
            clearAllFields()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun prepopulateFields() {
        binding.actvDescriptorName.setText(descriptor.name)
        binding.actvDescriptorUuid.setText(descriptor.uuid?.uuid)
        prepopulateProperties()
        prepopulatePropertyTypes()
        prepopulateInitialValue()
    }

    private fun prepopulateProperties() {
        descriptor.properties.apply {

            binding.propertiesContent.swRead.isChecked = containsKey(Property.READ)
            binding.propertiesContent.swWrite.isChecked = containsKey(Property.WRITE)
        }
    }

    private fun prepopulatePropertyTypes() {
        descriptor.properties.apply {
            this[Property.READ]?.apply {

                 binding.propertiesContent.cbReadBonded.isChecked = contains(Property.Type.BONDED)
                binding.propertiesContent.cbReadMitm.isChecked = contains(Property.Type.AUTHENTICATED)
            }

            this[Property.WRITE]?.apply {

                binding.propertiesContent.cbWriteBonded.isChecked = contains(Property.Type.BONDED)
                binding.propertiesContent.cbWriteMitm.isChecked = contains(Property.Type.AUTHENTICATED)
            }
        }
    }

    private fun prepopulateInitialValue() {
        when (descriptor.value?.type) {
            Value.Type.USER -> {

                binding.initialValue.spInitialValue.setSelection(POSITION_INITIAL_VALUE_EMPTY)
            }

            Value.Type.UTF_8 -> {

                binding.initialValue.spInitialValue.setSelection(POSITION_INITIAL_VALUE_TEXT)
                binding.initialValue.etInitialValueText.setText(descriptor.value?.value)
            }

            Value.Type.HEX -> {

                binding.initialValue.spInitialValue.setSelection(POSITION_INITIAL_VALUE_HEX)
                binding.initialValue.etInitialValueHex.setText(descriptor.value?.value)
            }

            else -> Unit
        }
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

    private fun initACTV(actv: AutoCompleteTextView, searchMode: SearchMode) {
        val adapter =
            Descriptor16BitAdapter(requireContext(), GattUtils.get16BitDescriptors(), searchMode)
        actv.setAdapter(adapter)

        actv.setOnItemClickListener { _, _, position, _ ->
            val descriptor = adapter.getItem(position)
            binding.actvDescriptorName.setText(descriptor?.name)
            binding.actvDescriptorUuid.setText(descriptor?.getIdentifierAsString())
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
        binding.initialValue.spInitialValue.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                binding.btnSave.isEnabled = isInputValid()
                binding.initialValue.etInitialValueText.visibility = if (position == 1) View.VISIBLE else View.GONE
                binding.initialValue.llInitialValueHex.visibility = if (position == 2) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun handleNameChanges() {
        binding.actvDescriptorName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnSave.isEnabled = isInputValid()
            }
        })
    }

    private fun handleUuidChanges() {
        binding.actvDescriptorUuid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length
                if ((len == 8 || len == 13 || len == 18 || len == 23) && count > before) binding.actvDescriptorUuid.append(
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

    private fun handlePropertyStateChanges() {
        binding.propertiesContent.swRead.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
            setPropertyParametersState(
                binding.propertiesContent.swRead,
                 binding.propertiesContent.cbReadBonded,
                binding.propertiesContent.cbReadMitm
            )
        }
        binding.propertiesContent.swWrite.setOnCheckedChangeListener { _, _ ->
            binding.btnSave.isEnabled = isInputValid()
            setPropertyParametersState(binding.propertiesContent.swWrite, binding.propertiesContent.cbWriteBonded, binding.propertiesContent.cbWriteMitm)
        }
    }

    private fun setPropertyParametersState(
        switch: SwitchCompat,
        cbBonded: CheckBox,
        cbMitm: CheckBox
    ) {
        cbBonded.isEnabled = switch.isChecked
        cbMitm.isEnabled = switch.isChecked
        if (!switch.isChecked) {
            cbBonded.isChecked = false
            cbMitm.isChecked = false
        }
    }

    private fun setDescriptorState() {
        descriptor.name = binding.actvDescriptorName.text.toString()
        descriptor.uuid = Uuid(binding.actvDescriptorUuid.text.toString())
        setPropertiesState()
        setInitialValue()
    }

    private fun setPropertiesState() {
        descriptor.properties.clear()
        if (binding.propertiesContent.swRead.isChecked) descriptor.properties[Property.READ] =
            getSelectedReadTypes()
        if (binding.propertiesContent.swWrite.isChecked) descriptor.properties[Property.WRITE] = getSelectedWriteTypes()
    }

    private fun getSelectedReadTypes(): HashSet<Property.Type> {
        return hashSetOf<Property.Type>().apply {
            if ( binding.propertiesContent.cbReadBonded.isChecked) add(Property.Type.BONDED)
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
                descriptor.value = Value(
                    value = "",
                    type = Value.Type.USER
                )
            }

            POSITION_INITIAL_VALUE_TEXT -> {
                descriptor.value = Value(
                    value = binding.initialValue.etInitialValueText.text.toString(),
                    type = Value.Type.UTF_8,
                    length = binding.initialValue.etInitialValueText.text.length
                )
            }

            POSITION_INITIAL_VALUE_HEX -> {
                descriptor.value = Value(
                    value = binding.initialValue.etInitialValueHex.text.toString(),
                    type = Value.Type.HEX,
                    length = binding.initialValue.etInitialValueText.length() / 2
                )
            }
        }
    }

    private fun isAnyPropertyChecked(): Boolean {
        return binding.propertiesContent.swRead.isChecked ||
                binding.propertiesContent.swWrite.isChecked
    }

    private fun isUuidValid(uuid: String): Boolean {
        return Validator.is16BitUuidValid(uuid) || Validator.is128BitUuidValid(uuid)
    }

    private fun isInputValid(): Boolean {
        return isAnyPropertyChecked()
                && binding.actvDescriptorName.text.toString().isNotEmpty()
                && isUuidValid(binding.actvDescriptorUuid.text.toString())
                && isInitialValueValid()
    }

    private fun clearAllFields() {
        binding.actvDescriptorName.setText("")
        binding.actvDescriptorUuid.setText("")
        binding.propertiesContent.swRead.isChecked = true
        binding.propertiesContent.swWrite.isChecked = false
         binding.propertiesContent.cbReadBonded.isChecked = false
        binding.propertiesContent.cbReadMitm.isChecked = false
        binding.propertiesContent.cbWriteBonded.isChecked = false
        binding.propertiesContent.cbWriteMitm.isChecked = false
        binding.initialValue.spInitialValue.setSelection(0)
        binding.initialValue.etInitialValueText.setText("")
        binding.initialValue.etInitialValueHex.setText("")
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