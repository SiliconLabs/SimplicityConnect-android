package com.siliconlabs.bledemo.features.demo.smartlock.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.SmartLockSelectionDialogBinding
import com.siliconlabs.bledemo.features.demo.smartlock.activities.SmartLockActivity


class SmartLockSelectionDialog(
    private val cancelCallback: CancelCallback,
    private val listener: SmartLockOptionSelectedListener
) : BaseDialogFragment(
    hasCustomWidth = true,
    isCanceledOnTouchOutside = false
) {
    private lateinit var _binding: SmartLockSelectionDialogBinding



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = SmartLockSelectionDialogBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
    }

    private fun setUpListeners() {
        _binding.apply {
            bleBtn.setOnClickListener {
                dismiss()
                (activity as SmartLockActivity).onSmartLockOptionSelected("BLE")
            }
            awsBtn.setOnClickListener {
                dismiss()
                (activity as SmartLockActivity).onSmartLockOptionSelected("AWS")
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        cancelCallback.onDismiss()
    }

    interface CancelCallback {
        fun onDismiss()
    }

    interface SmartLockOptionSelectedListener {
        fun onSmartLockOptionSelected(type: String)
    }
}