package com.siliconlabs.bledemo.Advertiser.Dialogs

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_device_name.view.*

class DeviceNameDialog(val callback: DeviceNameCallback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_device_name, container, false).apply {
            initDeviceNameEditText(et_device_name)
            verifyInputCorectness(et_device_name, btn_save)
            btn_cancel.setOnClickListener { dismiss() }
            btn_clear.setOnClickListener { et_device_name.setText("") }
            btn_save.setOnClickListener {
                BluetoothAdapter.getDefaultAdapter().name = et_device_name.text.toString()
                callback.onDismiss()
                dismiss()
            }
        }
    }

    private fun verifyInputCorectness(deviceNameEt: EditText, saveBtn: Button) {
        deviceNameEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                saveBtn.isEnabled = deviceNameEt.text.isNotEmpty()
            }
        })
    }

    private fun initDeviceNameEditText(deviceNameET: EditText) {
        val name = BluetoothAdapter.getDefaultAdapter().name
        deviceNameET.setText(name)
        deviceNameET.requestFocus()
        deviceNameET.setSelection(name.length)
    }

    interface DeviceNameCallback {
        fun onDismiss()
    }

}