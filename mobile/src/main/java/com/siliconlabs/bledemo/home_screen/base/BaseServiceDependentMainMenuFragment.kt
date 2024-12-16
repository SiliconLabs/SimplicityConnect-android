package com.siliconlabs.bledemo.home_screen.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.home_screen.viewmodels.MainActivityViewModel
import com.siliconlabs.bledemo.home_screen.views.BluetoothEnableBar
import com.siliconlabs.bledemo.home_screen.views.LocationEnableBar
import com.siliconlabs.bledemo.home_screen.views.LocationPermissionBar
import com.siliconlabs.bledemo.home_screen.views.BluetoothPermissionsBar

abstract class BaseServiceDependentMainMenuFragment : BaseMainMenuFragment() {

    protected open val bluetoothDependent: BluetoothDependent? = null
    protected open val locationDependent: LocationDependent? = null
    protected var activityViewModel: MainActivityViewModel? = null
    protected open val notificationDependent:NotificationDependent? = null

    protected fun toggleBluetoothBar(isOn: Boolean, bar: BluetoothEnableBar) {
        bar.visibility = if (isOn) View.GONE else View.VISIBLE
        if (!isOn) bar.resetState()
    }

    protected fun toggleBluetoothPermissionsBar(arePermissionsGranted: Boolean, bar: BluetoothPermissionsBar) {
        bar.visibility = if (arePermissionsGranted) View.GONE else View.VISIBLE
    }

    protected fun toggleLocationBar(isOn: Boolean, bar: LocationEnableBar) {
        bar.visibility = if (isOn) View.GONE else View.VISIBLE
    }

    protected fun toggleLocationPermissionBar(isPermissionGranted: Boolean, bar: LocationPermissionBar) {
        bar.visibility = if (isPermissionGranted) View.GONE else View.VISIBLE
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            activityViewModel = ViewModelProvider(it).get(MainActivityViewModel::class.java)
        }
        observeChanges()
        setupWarningBarsButtons()
    }

    override fun onResume() {
        super.onResume()
        bluetoothDependent?.refreshBluetoothDependentUi(isBluetoothOperationPossible())
    }

    private fun observeChanges() {
        activity?.let {
            activityViewModel?.isBluetoothOn?.observe(viewLifecycleOwner, Observer { isOn ->
                bluetoothDependent?.onBluetoothStateChanged(isOn)
            })
            activityViewModel?.areBluetoothPermissionGranted?.observe(viewLifecycleOwner, Observer { areGranted ->
                bluetoothDependent?.onBluetoothPermissionsStateChanged(areGranted)
            })
            activityViewModel?.isLocationOn?.observe(viewLifecycleOwner, Observer { isOn ->
                locationDependent?.onLocationStateChanged(isOn)
            })
            activityViewModel?.isLocationPermissionGranted?.observe(viewLifecycleOwner, Observer { isGranted ->
                locationDependent?.onLocationPermissionStateChanged(isGranted)
            })
            activityViewModel?.isNotificationOn?.observe(viewLifecycleOwner, Observer { isOn ->
                notificationDependent?.onNotificationStateChanged(isOn)
            })
            activityViewModel?.isNotificationPermissionGranted?.observe(viewLifecycleOwner, Observer { areGranted ->
               notificationDependent?.onNotificationPermissionsStateChanged(areGranted)
            })
        }
    }

    private fun setupWarningBarsButtons() {
        locationDependent?.let {
            it.setupLocationBarButtons()
            it.setupLocationPermissionBarButtons()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothDependent?.setupBluetoothPermissionsBarButtons()
        }
        notificationDependent?.let {
            it.setupNotificationBarButtons()
            it.setupNotificationPermissionBarButtons()
        }
    }

    fun showToastLengthShort(message: String) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show() }
    }

    fun showToastLengthLong(message: String) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show() }
    }

    protected fun isBluetoothOperationPossible() : Boolean {
        return activityViewModel?.let {
            it.getIsBluetoothOn() && it.getAreBluetoothPermissionsGranted()
        } ?: false
    }

}