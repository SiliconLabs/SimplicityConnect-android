package com.siliconlabs.bledemo.gatt_configurator.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.Utils.UuidUtils
import com.siliconlabs.bledemo.gatt_configurator.adapters.Service16BitAdapter
import com.siliconlabs.bledemo.gatt_configurator.models.*
import com.siliconlabs.bledemo.gatt_configurator.utils.GattUtils
import com.siliconlabs.bledemo.gatt_configurator.utils.Validator
import kotlinx.android.synthetic.main.dialog_gatt_server_service.*
import java.util.*

class ServiceDialog(val listener: ServiceChangeListener, var service: Service = Service()) : BaseDialogFragment() {
    private val predefinedServices: List<Service16Bit> = GattUtils.get16BitServices()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_gatt_server_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initACTV(actv_service_name, SearchMode.BY_NAME)
        initACTV(actv_service_uuid, SearchMode.BY_UUID)
        initServiceTypeSpinner()

        handleClickEvents()
        handleNameChanges()
        handleUuidChanges()
    }

    private fun initACTV(actv: AutoCompleteTextView, searchMode: SearchMode) {
        val adapter = Service16BitAdapter(requireContext(), GattUtils.get16BitServices(), searchMode)
        actv.setAdapter(adapter)

        actv.setOnItemClickListener { _, _, position, _ ->
            val service = adapter.getItem(position)
            actv_service_name.setText(service?.name)
            actv_service_uuid.setText(service?.getIdentifierAsString())
            actv.setSelection(actv.length())
            hideKeyboard()
        }
    }

    private fun initServiceTypeSpinner() {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_layout, resources.getStringArray(R.array.gatt_configurator_service_type))
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        sp_service_type.adapter = adapter
    }

    private fun handleClickEvents() {
        btn_bluetooth_gatt_services.setOnClickListener {
            launchBluetoothGattServicesWebPage()
        }

        btn_clear.setOnClickListener {
            resetInput()
        }

        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_save.setOnClickListener {
            setServiceState()
            listener.onServiceChanged(service)
            dismiss()
        }
    }

    private fun setServiceState() {
        if (cb_mandatory_requirements.isChecked) handleMandatoryServiceRequirements()
        service.apply {
            name = actv_service_name.text.toString()
            uuid = Uuid(actv_service_uuid.text.toString())
            type = getServiceTypeFromSelection()
        }
    }

    private fun handleMandatoryServiceRequirements() {
        val uuid = UuidUtils.convert16to128UUID(actv_service_uuid.text.toString())
        service = getMandatoryServiceRequirements(UUID.fromString(uuid))
    }

    private fun getMandatoryServiceRequirements(serviceUUID: UUID): Service {
        val serviceRes = Engine.getService(serviceUUID)
        val service = Service()

        serviceRes?.let {
            for (characteristicRes in it.characteristics!!) {
                val characteristicType =
                    Engine.getCharacteristicByType(characteristicRes.type!!)
                val characteristic = Characteristic().apply {
                    name = characteristicType?.name!!
                    uuid = Uuid(UuidUtils.convert128to16UUID(characteristicType.uuid!!.toString()))
                    value = Value()

                    if (characteristicRes.isReadPropertyMandatory()) properties[Property.READ] =
                        hashSetOf()
                    if (characteristicRes.isWritePropertyMandatory()) properties[Property.WRITE] =
                        hashSetOf()
                    if (characteristicRes.isWriteWithoutResponsePropertyMandatory()) properties[Property.WRITE_WITHOUT_RESPONSE] =
                        hashSetOf()
                    if (characteristicRes.isReliableWritePropertyMandatory()) properties[Property.RELIABLE_WRITE] =
                        hashSetOf()
                    if (characteristicRes.isNotifyPropertyMandatory()) properties[Property.NOTIFY] =
                        hashSetOf()
                    if (characteristicRes.isIndicatePropertyMandatory()) properties[Property.INDICATE] =
                        hashSetOf()
                }

                for (descriptorRes in characteristicRes.descriptors!!) {
                    val descriptorType = Engine.getDescriptorByType(descriptorRes.type!!)
                    val descriptor = Descriptor().apply {
                        name = descriptorType?.name!!
                        uuid = Uuid(UuidUtils.convert128to16UUID(descriptorType.uuid!!.toString()))
                        value = Value()

                        if (descriptorRes.isReadPropertyMandatory()) properties[Property.READ] =
                            hashSetOf()
                        if (descriptorRes.isWritePropertyMandatory()) properties[Property.WRITE] =
                            hashSetOf()
                    }
                    characteristic.descriptors.add(descriptor)
                }
                service.characteristics.add(characteristic)
            }
        }

        return service
    }

    private fun getServiceTypeFromSelection(): Service.Type {
        return when (sp_service_type.selectedItemPosition) {
            0 -> Service.Type.PRIMARY
            else -> Service.Type.SECONDARY
        }
    }

    private fun handleNameChanges() {
        actv_service_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_save.isEnabled = isInputValid()
                setMandatoryRequirementsCheckBoxState()
                setServiceTypeSpinnerState()
            }
        })
    }

    private fun handleUuidChanges() {
        actv_service_uuid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length
                if ((len == 8 || len == 13 || len == 18 || len == 23) && count > before) actv_service_uuid.append("-")

                btn_save.isEnabled = isInputValid()
                setMandatoryRequirementsCheckBoxState()
                setServiceTypeSpinnerState()
            }
        })
    }

    private fun setMandatoryRequirementsCheckBoxState() {
        if (isInput16BitService()) {
            cb_mandatory_requirements.isEnabled = true
        } else {
            cb_mandatory_requirements.isEnabled = false
            cb_mandatory_requirements.isChecked = false
        }
    }

    private fun setServiceTypeSpinnerState() {
        if (isInput16BitService()) {
            sp_service_type.setSelection(0)
            sp_service_type.isEnabled = false
        } else {
            sp_service_type.isEnabled = true
        }
    }

    private fun isInput16BitService(): Boolean {
        val name = actv_service_name.text.toString()
        val uuid = actv_service_uuid.text.toString()

        val result = predefinedServices.filter { it.name == name && it.getIdentifierAsString() == uuid }
        return result.isNotEmpty()
    }

    private fun isInputValid(): Boolean {
        return actv_service_name.text.toString().isNotEmpty() && isUuidValid(actv_service_uuid.text.toString())
    }

    private fun isUuidValid(uuid: String): Boolean {
        return Validator.is16BitUuidValid(uuid) || Validator.is128BitUuidValid(uuid)
    }

    private fun launchBluetoothGattServicesWebPage() {
        val uriUrl = Uri.parse("https://" + getString(R.string.advertiser_url_bluetooth_gatt_services))
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }

    private fun resetInput() {
        actv_service_name.setText("")
        actv_service_uuid.setText("")
        cb_mandatory_requirements.isChecked = false
        sp_service_type.setSelection(0)
    }

    interface ServiceChangeListener {
        fun onServiceChanged(service: Service)
    }
}
