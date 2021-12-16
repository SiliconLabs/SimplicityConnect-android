package com.siliconlabs.bledemo.iop_test.test_cases.ota

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.siliconlabs.bledemo.R

class OtaFileSelectionDialog(context: Context, listener: FileSelectionListener, deviceAddress: String) : Dialog(context) {

    var selectFileButton: Button
    var otaButton: Button
    var cancelButton: Button

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.iop_ota_file_selection_dialog)

        findViewById<TextView>(R.id.device_address).text = deviceAddress
        selectFileButton = findViewById(R.id.iop_select_app_file_btn)
        otaButton = findViewById(R.id.ota_proceed)
        cancelButton = findViewById(R.id.ota_cancel)

        selectFileButton.setOnClickListener {
            listener.onSelectFileButtonClicked()
        }
        cancelButton.setOnClickListener {
            listener.onCancelButtonClicked()
        }
        otaButton.setOnClickListener {
            listener.onOtaButtonClicked()
        }
    }

    interface FileSelectionListener {
        fun onSelectFileButtonClicked()
        fun onOtaButtonClicked()
        fun onCancelButtonClicked()
    }
}