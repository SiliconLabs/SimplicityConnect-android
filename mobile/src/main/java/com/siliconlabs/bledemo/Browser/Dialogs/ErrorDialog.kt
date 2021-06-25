package com.siliconlabs.bledemo.browser.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.BaseDialogFragment
import com.siliconlabs.bledemo.bluetooth.ble.ErrorCodes.getATTHTMLFormattedError
import com.siliconlabs.bledemo.bluetooth.ble.ErrorCodes.getOneOctetErrorCodeHexAsString
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_error.*

class ErrorDialog(private val errorCode: Int, private val otaErrorCallback: OtaErrorCallback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_ok.setOnClickListener {
            dismiss()
            otaErrorCallback.onDismiss()
        }

        error_title.text = getString(R.string.Error_colon) + " " + getOneOctetErrorCodeHexAsString(errorCode)
        error_description.text = Html.fromHtml(getATTHTMLFormattedError(errorCode))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        otaErrorCallback.onDismiss()
    }

    interface OtaErrorCallback {
        fun onDismiss()
    }

}
