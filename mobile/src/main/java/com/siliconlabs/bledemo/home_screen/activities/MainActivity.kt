package com.siliconlabs.bledemo.home_screen.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.dialogs.PermissionsDialog
import com.siliconlabs.bledemo.home_screen.fragments.*
import com.siliconlabs.bledemo.home_screen.viewmodels.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
open class MainActivity : BaseActivity(),
        BluetoothService.ServicesStateListener
{

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var binding: BluetoothService.Binding
    var bluetoothService: BluetoothService? = null
        private set

    private val neededPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.show()

        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        handlePermissions()
        observeChanges()
        setupMainNavigationListener()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setIsLocationPermissionGranted(isPermissionGranted(neededPermissions[3]))
        viewModel.setAreBluetoothPermissionsGranted(areBluetoothPermissionsGranted())
    }

    private fun setupMainNavigationListener() {
        main_navigation.setOnNavigationItemSelectedListener {
            val fragmentToSwitch =
                if (Build.VERSION.SDK_INT > 30 &&
                        !viewModel.getAreBluetoothPermissionsGranted()) {
                    Toast.makeText(this, getString(R.string.bluetooth_permissions_needed),
                            Toast.LENGTH_SHORT).show()
                    null
                }
                else if (main_navigation.selectedItemId == it.itemId) null
                else {
                    when (it.itemId) {
                        R.id.main_navigation_scan -> ScanFragment()
                        R.id.main_navigation_configure -> ConfigureFragment()
                        R.id.main_navigation_test -> TestFragment()
                        R.id.main_navigation_demo -> DemoFragment()
                        R.id.main_navigation_settings -> SettingsFragment()
                        else -> null
                    }
                }

            fragmentToSwitch?.let { fragment ->
                switchToFragment(fragment)
                true
            } ?: false
        }
    }

    private fun observeChanges() {
        viewModel.areBluetoothPermissionGranted.observe(this, Observer { areGranted ->
            if (areGranted) {
                bluetooth_permissions_bar.visibility = View.GONE
                bindBluetoothService()
            }
        })
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
        setServicesInitialState()
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
        if (!areAllPermissionsGranted()) askForPermissions()
        else bindBluetoothService()
    }

    private fun setServicesInitialState() {
        viewModel.setIsLocationPermissionGranted(isPermissionGranted(neededPermissions[3]))
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

    private fun areBluetoothPermissionsGranted() : Boolean {
        return isPermissionGranted(neededPermissions[0]) &&
                isPermissionGranted(neededPermissions[1]) &&
                isPermissionGranted(neededPermissions[2])

    }

    private fun areAllPermissionsGranted() : Boolean {
        neededPermissions.forEach {
            if (!isPermissionGranted(it)) return false
        }
        return true
    }

    private fun shouldShowPermissionRationale() : Boolean {
        neededPermissions.forEach {
            if (shouldShowRequestPermissionRationale(it)) return true
        }
        return false
    }

    private fun askForPermissions() {
        if (shouldShowPermissionRationale()) {
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
            PERMISSIONS_REQUEST_CODE -> {
                viewModel.setAreBluetoothPermissionsGranted(areBluetoothPermissionsGranted())
                if (Build.VERSION.SDK_INT < 31) bindBluetoothService()
                else {
                    if (viewModel.getAreBluetoothPermissionsGranted()) bindBluetoothService()
                    else {
                        bluetooth_permissions_bar.visibility = View.VISIBLE
                    }
                }
            }
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