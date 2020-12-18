package com.siliconlabs.bledemo.Advertiser.Dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.Advertiser.Utils.AdvertiserStorage
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_info_ok_cancel.*

class LeaveAdvertiserConfigDialog(var callback: Callback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_info_ok_cancel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tv_dialog_title.text = context?.getString(R.string.title_unsaved_changes)
        tv_dialog_content.text = context?.getString(R.string.advertiser_note_leave_advertiser_config)

        btn_ok.text = context?.getString(R.string.button_yes)
        btn_cancel.text = context?.getString(R.string.button_no)

        btn_ok.setOnClickListener {
            if (cb_dont_show_again.isChecked) AdvertiserStorage(requireContext()).setShouldDisplayLeaveAdvertiserConfigDialog(false)
            callback.onYesClicked()
            dismiss()
        }

        btn_cancel.setOnClickListener {
            dismiss()
            callback.onNoClicked()
        }
    }

    interface Callback {
        fun onYesClicked()
        fun onNoClicked()
    }
}