package com.siliconlabs.bledemo.iop_test.test_cases.ota

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Chronometer
import android.widget.ProgressBar
import android.widget.TextView
import com.siliconlabs.bledemo.R

class OtaProgressDialog(
        context: Context,
        deviceAddress: String
) : Dialog(context) {

    var progressBar: ProgressBar
    var dataRate: TextView
    var dataSize: TextView
    var filename: TextView
    var steps: TextView
    var chrono: Chronometer
    var btnOtaEnd: Button
    var sizename: TextView
    var uploadImage: ProgressBar

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.iop_ota_progress_dialog)

        findViewById<TextView>(R.id.device_address).text = deviceAddress
        progressBar = findViewById(R.id.otaprogress)
        dataRate = findViewById(R.id.datarate)
        dataSize = findViewById(R.id.datasize)
        filename = findViewById(R.id.filename)
        steps = findViewById(R.id.otasteps)
        chrono = findViewById(R.id.chrono)
        btnOtaEnd = findViewById(R.id.otabutton)
        sizename = findViewById(R.id.sizename)
        uploadImage = findViewById(R.id.connecting_spinner)
    }

    override fun show() {
        super.show()
        btnOtaEnd.isClickable = false
        btnOtaEnd.setBackgroundColor(context.getColor(R.color.silabs_inactive))
        setCanceledOnTouchOutside(false)
    }

    fun setProgressInfo(fileName: String?, fileSize: Int?) {
        filename.text = fileName
        steps.text = context.getString(R.string.iop_test_label_1_of_1)
        sizename.text = context.getString(R.string.iop_test_n_bytes, fileSize)
        uploadImage.visibility = View.VISIBLE
    }

}