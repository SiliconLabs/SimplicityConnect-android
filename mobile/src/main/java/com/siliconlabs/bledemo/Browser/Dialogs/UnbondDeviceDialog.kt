package com.siliconlabs.bledemo.Browser.Dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.SharedPrefUtils
import kotlinx.android.synthetic.main.dialog_info_ok_cancel.view.*

class UnbondDeviceDialog(val callback: Callback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_info_ok_cancel, container, false).apply {

            tv_dialog_title.text = context.getString(R.string.device_services_title_unbond_device)
            tv_dialog_content.text = context.getString(R.string.device_services_note_unbond_device)

            btn_ok.setOnClickListener {
                if (cb_dont_show_again.isChecked) SharedPrefUtils(requireContext()).setShouldDisplayUnbondDeviceDialog(false)
                callback.onOkClicked()
                dismiss()
            }
            btn_cancel.setOnClickListener { dismiss() }
        }
    }

    interface Callback {
        fun onOkClicked()
    }
}
