package com.siliconlabs.bledemo.features.demo.esl_demo.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.databinding.ActivityEslDemoBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.fragments.QrCodeScannerFragment
import com.siliconlabs.bledemo.features.demo.esl_demo.fragments.TagDataFragment
import com.siliconlabs.bledemo.features.demo.esl_demo.model.QrCodeData

class EslDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityEslDemoBinding
    private lateinit var tagDataFragment: TagDataFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEslDemoBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
                tagDataFragment.clearAdapterData()
                tagDataFragment.loadTags()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            with(supportFragmentManager) {
                if (backStackEntryCount > 0 && getBackStackEntryAt(
                        backStackEntryCount - 1).name == QR_CODE_SCANNER_FRAGMENT) {
                    popBackStack()
                    toggleFullscreen(toggleOn = false)
                }
                else {
                    finish()
                }
            }
        }
    }

    fun handleOnDeviceDisconnected() {
        onDeviceDisconnected()
    }

    fun handleScannedQrCode(qrCodeData: QrCodeData) {
        tagDataFragment.connectTag(qrCodeData)
    }

    private fun showQrCodeScanner() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showScannerFragment()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    fun toggleFullscreen(toggleOn: Boolean) {
        supportActionBar?.let {
            if (toggleOn) it.hide()
            else it.show()
        }

        val flag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        with(window) {
            if (toggleOn) addFlags(flag)
            else clearFlags(flag)
        }
    }

    private fun initDefaultFragment() {
        tagDataFragment = TagDataFragment()

        supportFragmentManager.beginTransaction().apply {
            add(binding.eslDemoFragmentContainer.id, tagDataFragment)
        }.commit()
    }

    private fun showScannerFragment() {
        toggleFullscreen(toggleOn = true)
        Handler(Looper.getMainLooper()).postDelayed({
            supportFragmentManager.beginTransaction().apply {
                hide(tagDataFragment)
                add(binding.eslDemoFragmentContainer.id, QrCodeScannerFragment())
                addToBackStack(QR_CODE_SCANNER_FRAGMENT)
            }.commit()
        }, SCANNER_FRAGMENT_LAUNCH_DELAY)
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
        private const val QR_CODE_SCANNER_FRAGMENT = "qr_code_scanner_fragment"
        private const val SCANNER_FRAGMENT_LAUNCH_DELAY = 200L // to smooth out the transition
    }
}