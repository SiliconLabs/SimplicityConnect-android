package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogOtaProgressBinding

class OtaProgressDialog(
        private val callback: Callback,
        private val otaInfo: OtaInfo
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = false
) {

    private lateinit var _binding: DialogOtaProgressBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogOtaProgressBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewState()
        _binding.otabutton.setOnClickListener {
            dismiss()
            callback.onEndButtonClicked()
        }
    }

    private fun initViewState() {
        _binding.apply {
            fileName.text = otaInfo.fileName
            fileSize.text = getString(R.string.ota_file_size, otaInfo.fileSize)
            packetSize.text = otaInfo.packetSize.toString()
            otaNumberOfSteps.text = prepareStepsInfo()
            otabutton.isEnabled = false
            connectingSpinner.visibility = View.VISIBLE
            startChronometer()

            if (otaInfo.isDoubleStepUpload && otaInfo.isFirstFile) {
                otabutton.visibility = View.GONE
            }
        }
    }

    fun updateDataRate(dataRate: Float) {
        _binding.dataRate.text = getString(R.string.unit_value_kbit_per_second, dataRate)
    }

    fun updateDataProgress(progress: Int) {
        _binding.apply {
            otaProgressBar.progress = progress
            otaProgressPercent.text = getString(R.string.iop_test_n_percent, progress)
        }
    }

    fun toggleEndButton(isEnabled: Boolean) {
        _binding.otabutton.isEnabled = isEnabled
    }

    fun stopUploading() {
        _binding.apply {
            otaChronometer.stop()
            connectingSpinner.clearAnimation()
            connectingSpinner.visibility = View.INVISIBLE
        }
    }

    private fun startChronometer() {
        _binding.otaChronometer.apply {
            base = SystemClock.elapsedRealtime()
            start()
        }
    }

    private fun prepareStepsInfo() : String {
        return requireContext().getString(
                if (otaInfo.isDoubleStepUpload)
                    if (otaInfo.isFirstFile) R.string.ota_first_file_uploading
                    else R.string.ota_second_file_uploading
                else R.string.ota_one_file_uploading
        )
    }

    data class OtaInfo(
            val fileName: String,
            val fileSize: Int?,
            val packetSize: Int,
            val isDoubleStepUpload: Boolean,
            val isFirstFile: Boolean
    )

    interface Callback {
        fun onEndButtonClicked()
    }
}