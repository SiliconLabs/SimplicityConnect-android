package com.siliconlabs.bledemo.home_screen.activities

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.dialogs.PermissionsDialog
import com.siliconlabs.bledemo.home_screen.fragments.*
import com.siliconlabs.bledemo.home_screen.viewmodels.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_light.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.adapter_scanned_device.*
import kotlinx.android.synthetic.main.fragment_scan.*
import java.util.concurrent.CountDownLatch

open class MainActivity : BaseActivity(),
        BluetoothService.ServicesStateListener
{

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var binding: BluetoothService.Binding
    var bluetoothService: BluetoothService? = null
        private set

    private val initialScanLatch = CountDownLatch(2)
    private val neededPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.show()

        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        bindBluetoothService()
        handlePermissions()
        setupMainNavigationListener()

        Thread {
            initialScanLatch.await()
            setServicesInitialState()
        }.start()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setIsLocationPermissionGranted(isPermissionGranted(neededPermissions[0]))
    }

    private fun setupMainNavigationListener() {
        main_navigation.setOnNavigationItemSelectedListener {
            (if (main_navigation.selectedItemId != it.itemId) {
                when (it.itemId) {
                    R.id.main_navigation_scan -> ScanFragment()
                    R.id.main_navigation_configure -> ConfigureFragment()
                    R.id.main_navigation_test -> TestFragment()
                    R.id.main_navigation_demo -> DemoFragment()
                    R.id.main_navigation_settings -> SettingsFragment()
                    else -> null
                }
            }
            else null)?.let { fragment ->
                switchToFragment(fragment)
                true
            } ?: false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onScanFragmentPrepared() {
        initialScanLatch.countDown()
    }

    fun toggleMainNavigation(isOn: Boolean) {
        main_navigation.visibility = if (isOn) View.VISIBLE else View.GONE
    }

    fun toggleHomeIcon(isOn: Boolean) {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(isOn)
            if (isOn) setHomeAsUpIndicator(R.drawable.redesign_ic_close)
        }
    }


    private fun switchToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.main_fragment, fragment)
            commit()
        }
    }

    private fun bindBluetoothService() {
        binding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                this@MainActivity.bluetoothService = service
                bluetoothService?.servicesStateListener = this@MainActivity
                main_navigation.selectedItemId = R.id.main_navigation_scan
            }
        }
        binding.bind()
    }

    private fun handlePermissions() {
        if (!isPermissionGranted(neededPermissions[0]) || !isPermissionGranted(neededPermissions[1])) {
            askForPermissions()
        } else {
            initialScanLatch.countDown()
        }
    }

    private fun setServicesInitialState() {
        viewModel.setIsLocationPermissionGranted(isPermissionGranted(neededPermissions[0]))
        bluetoothService?.let {
            viewModel.setIsBluetoothOn(it.isBluetoothOn())
            viewModel.setIsLocationOn(it.isLocationOn())
        }
    }

    override fun onBluetoothStateChanged(isOn: Boolean) {
        viewModel.setIsBluetoothOn(isOn)
    }

    override fun onLocationStateChanged(isOn: Boolean) {
        viewModel.setIsLocationOn(isOn)
    }

    private fun isPermissionGranted(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun askForPermissions() {
        if (shouldShowRequestPermissionRationale(neededPermissions[0]) ||
                shouldShowRequestPermissionRationale(neededPermissions[1])) {
            PermissionsDialog(object : PermissionsDialog.Callback {
                override fun onDismiss() {
                    requestPermissions(neededPermissions, PERMISSIONS_REQUEST_CODE)
                }
            }).show(supportFragmentManager, "permissions_dialog")
        } else {
            requestPermissions(neededPermissions, PERMISSIONS_REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> initialScanLatch.countDown()
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 400
        // private const val IMPORT_EXPORT_CODE_VERSION = 20
    }
//TODO: handle migration
/*
    private fun migrateGattDatabaseIfNeeded() {
        if (BuildConfig.VERSION_CODE <= IMPORT_EXPORT_CODE_VERSION - 1) {
            Migrator(this).migrate()
        }
    }
*/

}