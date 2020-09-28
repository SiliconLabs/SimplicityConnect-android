package com.siliconlabs.bledemo.Advertiser.Dialogs

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
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.Advertiser.Adapters.Service16BitAdapter
import com.siliconlabs.bledemo.Advertiser.Models.Service16Bit
import com.siliconlabs.bledemo.Advertiser.Utils.Translator
import com.siliconlabs.bledemo.Advertiser.Utils.Validator
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_data_16bit_service.view.*
import kotlinx.android.synthetic.main.dialog_data_manufacturer.view.btn_cancel
import kotlinx.android.synthetic.main.dialog_data_manufacturer.view.btn_save

class Service16BitDataDialog(val callback: Callback) : DialogFragment() {
    private lateinit var predefinedServices: List<Service16Bit>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_data_16bit_service, container, false).apply {

            predefinedServices = Translator(context).get16BitServices()
            val adapter = Service16BitAdapter(context, Translator(context).get16BitServices())
            actv_16bit_services.setAdapter(adapter)

            btn_cancel.setOnClickListener { dismiss() }
            btn_save.setOnClickListener { handleSave(actv_16bit_services) }
            btn_clear.setOnClickListener { actv_16bit_services.setText("") }

            verifyDataCorrectness(actv_16bit_services, btn_save)
            handleClickOnBluetoothGattServices(btn_bluetooth_gatt_services)
        }
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
                saveBtn.isEnabled = Validator.isCompanyIdentifierValid(text) || isPredefinedService(text, predefinedServices)
            }
        })
    }

    private fun handleClickOnBluetoothGattServices(button: Button) {
        button.setOnClickListener {
            val uriUrl = Uri.parse("https://" + getString(R.string.advertiser_url_bluetooth_gatt_services))
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