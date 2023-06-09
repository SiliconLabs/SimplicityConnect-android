package com.siliconlabs.bledemo.features.demo.esl_demo.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.zxing.BarcodeFormat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentEslQrScanBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.activities.EslDemoActivity
import com.siliconlabs.bledemo.features.demo.esl_demo.model.QrCodeData

class QrCodeScannerFragment : Fragment(R.layout.fragment_esl_qr_scan) {
    private val binding by viewBinding(FragmentEslQrScanBinding::bind)
    private lateinit var codeScanner: CodeScanner

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCodeScanner()
    }

    override fun onPause() {
        super.onPause()
        codeScanner.also {
            it.stopPreview()
            it.releaseResources()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    private fun initCodeScanner() {
        activity?.let { activity ->
            codeScanner = CodeScanner(activity, binding.scannerView).apply {
                camera = CodeScanner.CAMERA_BACK
                formats = listOf(BarcodeFormat.QR_CODE)
                autoFocusMode = AutoFocusMode.SAFE
                scanMode = ScanMode.SINGLE
                isAutoFocusEnabled = true
                isFlashEnabled = false

                decodeCallback = DecodeCallback {
                    val qrCodeData = QrCodeData.decode(it.text)
                    if (qrCodeData.isValid()) {
                        (activity as EslDemoActivity).handleScannedQrCode(qrCodeData)
                    } else {
                        showLongToast(getString(R.string.qr_code_unrecognized))
                    }

                    exitScannerFragment(activity)
                }
                errorCallback = ErrorCallback {
                    showLongToast(getString(R.string.qr_scan_error,it.message ?: "unknown"))
                    exitScannerFragment(activity)
                }
            }
        }
    }



    private fun exitScannerFragment(activity: FragmentActivity) {
        activity.supportFragmentManager.popBackStack()
        activity.runOnUiThread {
            (activity as EslDemoActivity).toggleFullscreen(toggleOn = false)
        }
    }

    private fun showLongToast(message: String) {
        context?.let {
            activity?.runOnUiThread {
                Toast.makeText(it, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}