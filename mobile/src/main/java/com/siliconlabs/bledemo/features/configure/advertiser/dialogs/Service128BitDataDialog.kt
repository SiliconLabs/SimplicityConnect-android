package com.siliconlabs.bledemo.features.configure.advertiser.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.siliconlabs.bledemo.features.configure.advertiser.models.Service128Bit
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Validator
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_data_128bit_service.view.*
import java.util.*

class Service128BitDataDialog(val callback: Callback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_data_128bit_service, container, false).apply {
            btn_cancel.setOnClickListener { dismiss() }
            btn_save.setOnClickListener {
                handleSave(et_128bit_service)
                dismiss()
            }
            btn_clear.setOnClickListener { et_128bit_service.setText("") }

            handleUuidChanges(et_128bit_service, btn_save)
        }
    }

    private fun handleSave(et: EditText) {
        val service = Service128Bit(UUID.fromString(et.text.toString()))
        callback.onSave(service)
    }

    private fun handleUuidChanges(et: EditText, saveBtn: Button) {
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length
                if ((len == 8 || len == 13 || len == 18 || len == 23) && count > before) et.append("-")
                val uuid = s.toString()
                saveBtn.isEnabled = Validator.validateUUID(uuid)
            }
        })
    }

    interface Callback {
        fun onSave(service: Service128Bit)
    }

}