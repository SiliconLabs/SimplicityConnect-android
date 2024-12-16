package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding
import com.siliconlabs.bledemo.utils.SharedPrefUtils


class LeaveApplicationDialog(var callback: Callback) : BaseDialogFragment() {

    private lateinit var binding: DialogInfoOkCancelBinding //dialog_info_ok_cancel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDialogTitle
            .text = context?.getString(R.string.Leave_Application)
        binding.tvDialogContent
            .text = context?.getString(R.string.leave_application_info)

        handleClickEvents()
    }

    private fun handleClickEvents() {
        binding.btnOk.setOnClickListener {
            if (binding.cbDontShowAgain.isChecked) SharedPrefUtils(requireContext()).setShouldLeaveApplicationDialog(
                false
            )
            callback.onOkClicked()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    interface Callback {
        fun onOkClicked()
    }

}
