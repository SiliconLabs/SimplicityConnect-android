package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding
import com.siliconlabs.bledemo.utils.SharedPrefUtils


class UnbondDeviceDialog(val callback: Callback) : BaseDialogFragment() {

    private lateinit var binding: DialogInfoOkCancelBinding//dialog_info_ok_cancel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false).apply {

            tvDialogTitle.text =
                requireContext().getString(R.string.device_services_title_unbond_device)
            tvDialogContent.text =
                requireContext().getString(R.string.device_services_note_unbond_device)

            btnOk
                .setOnClickListener {
                    if (cbDontShowAgain.isChecked) SharedPrefUtils(requireContext()).setShouldDisplayUnbondDeviceDialog(
                        false
                    )
                    callback.onOkClicked()
                    dismiss()
                }
            btnCancel.setOnClickListener { dismiss() }
        }
        return binding.root
    }

    interface Callback {
        fun onOkClicked()
    }
}
