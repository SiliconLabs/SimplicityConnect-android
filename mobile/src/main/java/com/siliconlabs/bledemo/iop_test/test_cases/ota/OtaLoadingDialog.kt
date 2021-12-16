package com.siliconlabs.bledemo.iop_test.test_cases.ota

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.siliconlabs.bledemo.R

class OtaLoadingDialog(context: Context) : Dialog(context) {

    var loadingImage: ProgressBar
    var loadingLog: TextView

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.iop_ota_loading_dialog)

        loadingImage = findViewById(R.id.connecting_spinner)
        loadingLog = findViewById(R.id.loadingLog)
    }
}