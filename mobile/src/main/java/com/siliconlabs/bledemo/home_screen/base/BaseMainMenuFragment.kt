package com.siliconlabs.bledemo.home_screen.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.home_screen.views.BluetoothEnableBar
import com.siliconlabs.bledemo.home_screen.views.LocationEnableBar
import com.siliconlabs.bledemo.home_screen.views.LocationPermissionBar
import com.siliconlabs.bledemo.home_screen.viewmodels.MainActivityViewModel

abstract class BaseMainMenuFragment : Fragment() {

    protected var activityViewModel: MainActivityViewModel? = null

    protected fun toggleBluetoothBar(isOn: Boolean, bar: BluetoothEnableBar) {
        bar.visibility = if (isOn) View.GONE else View.VISIBLE
        if (!isOn) bar.resetState()
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
    }

    private fun observeChanges() {
        activity?.let {
            activityViewModel?.isBluetoothOn?.observe(viewLifecycleOwner, Observer { isOn ->
                onBluetoothStateChanged(isOn)
            })
            activityViewModel?.isLocationOn?.observe(viewLifecycleOwner, Observer { isOn ->
                onLocationStateChanged(isOn)
            })
            activityViewModel?.isLocationPermissionGranted?.observe(viewLifecycleOwner, Observer { isGranted ->
                onLocationPermissionStateChanged(isGranted)
            })
        }
    }

    fun showToastLengthShort(message: String) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show() }
    }

    fun showToastLengthLong(message: String) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show() }
    }


    abstract fun onBluetoothStateChanged(isOn: Boolean)
    abstract fun onLocationStateChanged(isOn: Boolean)
    abstract fun onLocationPermissionStateChanged(isGranted: Boolean)
}