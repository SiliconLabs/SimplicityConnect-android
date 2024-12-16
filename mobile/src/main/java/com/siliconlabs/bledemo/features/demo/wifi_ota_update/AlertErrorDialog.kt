package com.siliconlabs.bledemo.features.demo.wifi_ota_update

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogAlertErrorBinding


class AlertErrorDialog(
    private val otaErrorCallback: OtaErrorCallback
) : BaseDialogFragment(
    hasCustomWidth = true,
    isCanceledOnTouchOutside = false
) {
    private lateinit var binding: DialogAlertErrorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogAlertErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOk.setOnClickListener {
            dismiss()
            otaErrorCallback.onDismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        otaErrorCallback.onDismiss()
    }

    interface OtaErrorCallback {
        fun onDismiss()
    }
}