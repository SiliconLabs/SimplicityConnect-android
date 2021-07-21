package com.siliconlabs.bledemo.Utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import kotlinx.android.synthetic.main.dialog_info_ok_cancel.view.*

abstract class RemovalDialog(
    @StringRes private val nameRes: Int,
    private val onOkClicked: () -> Unit
) : BaseDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater
        .inflate(R.layout.dialog_info_ok_cancel, container, false)
        .apply {
            val name = context.getString(nameRes)
            tv_dialog_title.text = context.getString(R.string.dialog_title_remove, name)
            tv_dialog_content.text = context.getString(R.string.dialog_description_remove, name)

            btn_ok.setOnClickListener {
                if (cb_dont_show_again.isChecked) {
                    blockDisplayingRemovalDialog()
                }
                onOkClicked()
                dismiss()
            }
            btn_cancel.setOnClickListener { dismiss() }
        }

    abstract fun blockDisplayingRemovalDialog()
}
