package com.siliconlabs.bledemo.features.iop_test.test_cases.ota

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.*
import com.siliconlabs.bledemo.R

class OtaProgressDialog(context: Context) : Dialog(context) {

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
        setContentView(R.layout.dialog_ota_progress_iop)
        window?.apply {
            setLayout(
                    (context.resources.displayMetrics.widthPixels * 0.8f).toInt(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        progressBar = findViewById(R.id.ota_progress_bar)
        dataRate = findViewById(R.id.data_rate)
        dataSize = findViewById(R.id.ota_progress_percent)
        filename = findViewById(R.id.file_name)
        steps = findViewById(R.id.ota_number_of_steps)
        chrono = findViewById(R.id.ota_chronometer)
        btnOtaEnd = findViewById(R.id.otabutton)
        sizename = findViewById(R.id.file_size)
        uploadImage = findViewById(R.id.connecting_spinner)
    }

    override fun show() {
        super.show()
        setCanceledOnTouchOutside(false)
    }

    fun setProgressInfo(fileName: String?, fileSize: Int?) {
        filename.text = fileName
        steps.text = context.getString(R.string.iop_test_label_1_of_1)
        sizename.text = context.getString(R.string.iop_test_n_bytes, fileSize)
        uploadImage.visibility = View.VISIBLE
    }

}