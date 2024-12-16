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
import com.siliconlabs.bledemo.databinding.DialogData128bitServiceBinding
//import kotlinx.android.synthetic.main.dialog_data_128bit_service.view.*
import java.util.*

class Service128BitDataDialog(val callback: Callback) : BaseDialogFragment() {
    private lateinit var binding: DialogData128bitServiceBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogData128bitServiceBinding.inflate(inflater, container, false).apply {
            btnCancel.setOnClickListener { dismiss() }
            btnSave.setOnClickListener {
                handleSave(et128bitService)
                dismiss()
            }
            btnClear.setOnClickListener { et128bitService.setText("") }

            handleUuidChanges(et128bitService, btnSave)
        }
        return binding.root
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