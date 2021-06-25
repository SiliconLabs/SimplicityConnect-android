package com.siliconlabs.bledemo.Advertiser.Dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.Advertiser.Utils.AdvertiserStorage
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_info_ok_cancel.view.*

class RemoveServicesDialog(val callback: Callback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_info_ok_cancel, container, false).apply {

            tv_dialog_title.text = context.getString(R.string.advertiser_title_remove_service_list)
            tv_dialog_content.text = context.getString(R.string.advertiser_note_remove_services)

            btn_ok.setOnClickListener {
                if (cb_dont_show_again.isChecked) AdvertiserStorage(context).setShouldDisplayRemoveServicesDialog(false)
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