package com.siliconlabs.bledemo.features.configure.advertiser.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogDeviceNameBinding

class DeviceNameDialog(
        private val currentAdapterName: String,
        private val callback: DeviceNameCallback
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
) {

    private lateinit var _binding: DialogDeviceNameBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogDeviceNameBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDeviceNameEditText()
        setupInputVerification()
        setupUiListeners()
    }

    private fun setupUiListeners() {
        _binding.apply {
            btnCancel.setOnClickListener { dismiss() }
            btnSave.setOnClickListener {
                callback.onDeviceRenamed(_binding.etDeviceName.text.toString())
                dismiss()
            }
        }

    }

    private fun setupInputVerification() {
        _binding.apply {
            etDeviceName.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    btnSave.apply {
                        isEnabled = etDeviceName.text.isNotEmpty()
                    }
                }
            })
        }

    }

    private fun initDeviceNameEditText() {
        _binding.etDeviceName.apply {
            setText(currentAdapterName)
            requestFocus()
            setSelection(currentAdapterName.length)
        }
    }

    interface DeviceNameCallback {
        fun onDeviceRenamed(newName: String)
    }
}