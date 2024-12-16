package com.siliconlabs.bledemo.features.configure.advertiser.dialogs

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.siliconlabs.bledemo.features.configure.advertiser.adapters.Service16BitAdapter
import com.siliconlabs.bledemo.features.configure.advertiser.models.Service16Bit
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Translator
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Validator
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogData16bitServiceBinding
//import kotlinx.android.synthetic.main.dialog_data_16bit_service.view.*
//import kotlinx.android.synthetic.main.dialog_data_manufacturer.view.btn_cancel
//import kotlinx.android.synthetic.main.dialog_data_manufacturer.view.btn_save

class Service16BitDataDialog(val callback: Callback) : BaseDialogFragment() {
    private lateinit var predefinedServices: List<Service16Bit>
    private lateinit var binding: DialogData16bitServiceBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogData16bitServiceBinding.inflate(inflater, container, false).apply {

            predefinedServices = Translator(requireContext()).get16BitServices()
            val adapter = Service16BitAdapter(
                requireContext(),
                Translator(requireContext()).get16BitServices()
            )
            actv16bitServices.setAdapter(adapter)

            btnCancel.setOnClickListener { dismiss() }
            btnSave.setOnClickListener { handleSave(actv16bitServices) }
            btnClear.setOnClickListener { actv16bitServices.setText("") }

            verifyDataCorrectness(actv16bitServices, btnSave)
            handleClickOnBluetoothGattServices(btnBluetoothGattServices)
        }
        return binding.root
    }

    private fun handleSave(actv: AutoCompleteTextView) {
        val text = actv.text.toString()

        if (Validator.isCompanyIdentifierValid(text)) {
            val hexId = actv.text.toString().toInt(16)
            val name = getServiceName(hexId, predefinedServices)
            callback.onSave(Service16Bit(hexId, name))
            dismiss()
        } else if (isPredefinedService(text, predefinedServices)) {
            callback.onSave(getPredefinedService(actv.text.toString(), predefinedServices)!!)
            dismiss()
        }
    }

    private fun verifyDataCorrectness(actv: AutoCompleteTextView, saveBtn: Button) {

        actv.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = actv.text.toString()
                saveBtn.isEnabled = Validator.isCompanyIdentifierValid(text) || isPredefinedService(
                    text,
                    predefinedServices
                )
            }
        })
    }

    private fun handleClickOnBluetoothGattServices(button: Button) {
        button.setOnClickListener {
            val uriUrl =
                Uri.parse("https://" + getString(R.string.advertiser_url_bluetooth_gatt_services))
            val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
            startActivity(launchBrowser)
        }
    }

    private fun isPredefinedService(text: String, services: List<Service16Bit>): Boolean {
        for (service in services) if (text == service.getFullName()) return true
        return false
    }

    private fun getPredefinedService(text: String, services: List<Service16Bit>): Service16Bit? {
        for (service in services) if (text == service.getFullName()) return service
        return null
    }

    private fun getServiceName(identifier: Int, services: List<Service16Bit>): String {
        for (service in services) if (identifier == service.identifier) return service.name
        return "Unknown Service"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    interface Callback {
        fun onSave(service: Service16Bit)
    }

}