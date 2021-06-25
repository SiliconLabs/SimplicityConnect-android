package com.siliconlabs.bledemo.advertiser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.base.BaseDialogFragment
import kotlinx.android.synthetic.main.dialog_info_ok_cancel.view.*

class RemoveAdvertiserDialog(val callback: Callback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_info_ok_cancel, container, false).apply {

            tv_dialog_title.text = context.getString(R.string.advertiser_title_remove_advertiser)
            tv_dialog_content.text = context.getString(R.string.advertiser_note_remove_advertiser)

            btn_ok.setOnClickListener {
                if (cb_dont_show_again.isChecked) AdvertiserStorage(context).setShouldDisplayRemoveAdvertiserDialog(false)
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