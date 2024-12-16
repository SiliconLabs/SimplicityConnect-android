package com.siliconlabs.bledemo.features.configure.advertiser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.features.configure.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding

//mport kotlinx.android.synthetic.main.dialog_info_ok_cancel.*

class LeaveAdvertiserConfigDialog(var callback: Callback) : BaseDialogFragment() {
    private lateinit var binding:DialogInfoOkCancelBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
       // return inflater.inflate(R.layout.dialog_info_ok_cancel, container, false)
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDialogTitle.text = context?.getString(R.string.title_unsaved_changes)
        binding.tvDialogContent.text = context?.getString(R.string.advertiser_note_leave_advertiser_config)

        binding.btnOk.text = context?.getString(R.string.button_yes)
        binding.btnCancel.text = context?.getString(R.string.button_no)

        binding.btnOk.setOnClickListener {
            if (binding.cbDontShowAgain.isChecked) AdvertiserStorage(requireContext()).setShouldDisplayLeaveAdvertiserConfigDialog(false)
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