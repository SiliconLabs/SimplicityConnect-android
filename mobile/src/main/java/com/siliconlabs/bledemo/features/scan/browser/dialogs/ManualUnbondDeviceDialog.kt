package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding
import com.siliconlabs.bledemo.utils.SharedPrefUtils


class ManualUnbondDeviceDialog(val callback: Callback) : BaseDialogFragment() {
    private lateinit var binding: DialogInfoOkCancelBinding//dialog_info_ok_cancel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false).apply {

            binding.tvDialogTitle.text =
                requireContext().getString(R.string.device_services_title_unbond_device_manual)
            binding.tvDialogContent.text =
                requireContext().getString(R.string.device_services_note_unbond_device_manual)

             binding.btnOk.text = requireContext().getString(R.string.button_proceed)

             binding.btnOk.setOnClickListener {
                if (binding.cbDontShowAgain
                    .isChecked) SharedPrefUtils(requireContext()).setShouldDisplayManualUnbondDeviceDialog(
                    false
                )
                callback.onOkClicked()
                dismiss()
            }
            binding.btnCancel.setOnClickListener { dismiss() }
        }

        return binding.root
    }

    interface Callback {
        fun onOkClicked()
    }
}
