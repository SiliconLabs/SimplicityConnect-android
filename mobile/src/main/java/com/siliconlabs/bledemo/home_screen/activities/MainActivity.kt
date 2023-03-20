package com.siliconlabs.bledemo.home_screen.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.dialogs.PermissionsDialog
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

    private val neededPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val android12Permissions = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MainAppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.show()

        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        handlePermissions()
        setupMainNavigationListener()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.getIsSetupFinished()) {
            viewModel.setIsLocationPermissionGranted(isPermissionGranted(neededPermissions[0]))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                viewModel.setAreBluetoothPermissionsGranted(areBluetoothPermissionsGranted())
            }
        }
    }

    private fun setupMainNavigationListener() {
        val navFragment = supportFragmentManager.findFragmentById(R.id.main_fragment) as NavHostFragment
        val navController = navFragment.navController
        NavigationUI.setupWithNavController(main_navigation, navController)
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

    fun toggleMainNavigation(isOn: Boolean) {
        main_navigation.visibility = if (isOn) View.VISIBLE else View.GONE
    }

    fun toggleHomeIcon(isOn: Boolean) {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(isOn)
            if (isOn) setHomeAsUpIndicator(R.drawable.redesign_ic_close)
        }
    }

    private fun bindBluetoothService() {
        binding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                this@MainActivity.bluetoothService = service
                bluetoothService?.servicesStateListener = this@MainActivity
                setServicesInitialState()
            }
        }
        binding.bind()
    }

    private fun setServicesInitialState() {
        viewModel.setIsLocationPermissionGranted(isPermissionGranted(neededPermissions[0]))
        viewModel.setAreBluetoothPermissionsGranted(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) areBluetoothPermissionsGranted()
            else true /* No runtime permissions needed for bluetooth operation in Android 11- */
        )
        bluetoothService?.let {
            viewModel.setIsBluetoothOn(it.isBluetoothOn())
            viewModel.setIsLocationOn(it.isLocationOn())
        }
        observeChanges()
        viewModel.setIsSetupFinished(isSetupFinished = true)
    }

    private fun observeChanges() {
        viewModel.areBluetoothPermissionGranted.observe(this) {
            bluetoothService?.setAreBluetoothPermissionsGranted(
                viewModel.getAreBluetoothPermissionsGranted())
        }
    }

    override fun onBluetoothStateChanged(isOn: Boolean) {
        viewModel.setIsBluetoothOn(isOn)
    }

    override fun onLocationStateChanged(isOn: Boolean) {
        viewModel.setIsLocationOn(isOn)
    }


    private fun handlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPermissions.addAll(android12Permissions)
        }

        if (neededPermissions.any { !isPermissionGranted(it) }) askForPermissions()
        else bindBluetoothService()
    }

    private fun askForPermissions() {
        val rationalesToShow = neededPermissions.filter { shouldShowRequestPermissionRationale(it) }
        val permissionsToRequest = neededPermissions.toTypedArray()

        if (rationalesToShow.isNotEmpty()) {
            PermissionsDialog(rationalesToShow, object : PermissionsDialog.Callback {
                override fun onDismiss() {
                    requestPermissions(permissionsToRequest, PERMISSIONS_REQUEST_CODE)
                }
            }).show(supportFragmentManager, "permissions_dialog")
        } else {
            requestPermissions(permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun isPermissionGranted(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun areBluetoothPermissionsGranted() : Boolean {
        return android12Permissions.all { isPermissionGranted(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> bindBluetoothService()
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