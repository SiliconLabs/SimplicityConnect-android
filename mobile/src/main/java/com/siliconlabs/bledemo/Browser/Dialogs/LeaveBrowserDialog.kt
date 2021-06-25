package com.siliconlabs.bledemo.Browser.Dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.SharedPrefUtils
import kotlinx.android.synthetic.main.dialog_info_ok_cancel.*

class LeaveBrowserDialog(var callback: LeaveBrowserCallback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_info_ok_cancel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tv_dialog_title.text = context?.getString(R.string.Leave_Browser)
        tv_dialog_content.text = context?.getString(R.string.leave_browser_info)

        btn_ok.setOnClickListener {
            if (cb_dont_show_again.isChecked) SharedPrefUtils(context!!).setShouldDisplayLeaveBrowserDialog(false)
            callback.onOkClicked()
            dismiss()
        }

        btn_cancel.setOnClickListener { dismiss() }
    }

    interface LeaveBrowserCallback {
        fun onOkClicked()
    }

}