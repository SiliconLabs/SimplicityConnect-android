package com.siliconlabs.bledemo.features.demo.esl_demo.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.databinding.ActivityEslDemoBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.fragments.TagDataFragment
import com.siliconlabs.bledemo.features.demo.esl_demo.model.QrCodeData
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig

class EslDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityEslDemoBinding
    private lateinit var tagDataFragment: TagDataFragment

    private val scanCustomCode = registerForActivityResult(ScanCustomCode(), ::handleScanResult)
    private val scannerConfig = ScannerConfig.build {
        setBarcodeFormats(listOf(BarcodeFormat.FORMAT_QR_CODE))
        setOverlayStringRes(R.string.scan_qr_code)
        setShowCloseButton(true)
        setShowTorchToggle(true)
        setUseFrontCamera(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEslDemoBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        initDefaultFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_esl_demo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_scan_qr_code -> {
                showQrCodeScanner()
                true
            }
            R.id.menu_refresh_esl_tags -> {
                tagDataFragment.loadTags()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun handleOnDeviceDisconnected() {
        onDeviceDisconnected()
    }

    private fun handleScannedQrCode(qrCodeData: QrCodeData) {
        tagDataFragment.connectTag(qrCodeData)
    }

    private fun showQrCodeScanner() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showScannerFragment()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun initDefaultFragment() {
        tagDataFragment = TagDataFragment()

        supportFragmentManager.beginTransaction().apply {
            add(binding.eslDemoFragmentContainer.id, tagDataFragment)
        }.commit()
    }

    private fun showScannerFragment() {
        scanCustomCode.launch(scannerConfig)
    }

    private fun handleScanResult(result: QRResult) {
        when (result) {
            is QRResult.QRSuccess -> handleScanSuccess(result)
            is QRResult.QRError -> handleScanError(result)
            else -> Unit
        }
    }

    private fun handleScanSuccess(result: QRResult.QRSuccess) {
        val qrCodeData = QrCodeData.decode(result.content.rawValue)
        if (qrCodeData.isValid()) {
            handleScannedQrCode(qrCodeData)
        } else {
            showLongToast(getString(R.string.qr_code_unrecognized))
        }
    }

    private fun handleScanError(result: QRResult.QRError) {
        showLongToast(getString(R.string.qr_scan_error,result.exception.message ?: "unknown"))
    }

    private fun showLongToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showScannerFragment()
            }
        }
    }

    override fun onBluetoothServiceBound() {
        service?.let {
            tagDataFragment.prepareGatt(it, gatt)
        }
    }


    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 201
    }
}