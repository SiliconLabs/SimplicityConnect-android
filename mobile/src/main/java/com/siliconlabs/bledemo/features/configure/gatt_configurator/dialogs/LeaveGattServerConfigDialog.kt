package com.siliconlabs.bledemo.features.configure.gatt_configurator.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.GattConfiguratorStorage

//import kotlinx.android.synthetic.main.dialog_info_ok_cancel.*

class LeaveGattServerConfigDialog(val callback: Callback) : BaseDialogFragment() {
    private lateinit var binding: DialogInfoOkCancelBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDialogTitle.text = context?.getString(R.string.title_unsaved_changes)
        binding.tvDialogContent.text =
            context?.getString(R.string.gatt_configurator_leave_config_dialog_content)

        binding.btnOk.text = context?.getString(R.string.button_yes)
        binding.btnCancel.text = context?.getString(R.string.button_no)

        binding.btnOk.setOnClickListener {
            if (binding.cbDontShowAgain.isChecked
            ) GattConfiguratorStorage(requireContext())
                .setShouldDisplayLeaveGattServerConfigDialog(false)
            callback.onYesClicked()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
            callback.onNoClicked()
        }

    }

    interface Callback {
        fun onYesClicked()
        fun onNoClicked()
    }

}