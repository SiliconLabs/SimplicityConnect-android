package com.siliconlabs.bledemo.home_screen.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.bluetooth.ble.ScanResultCompat
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.scan.browser.fragments.FilterFragment
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.home_screen.viewmodels.MainActivityViewModel
import com.siliconlabs.bledemo.home_screen.base.ViewPagerFragment

class ScanFragment : Fragment(), BluetoothService.ScanListener {

    private lateinit var btService: BluetoothService
    lateinit var viewModel: ScanFragmentViewModel
        private set
    private var activityViewModel: MainActivityViewModel? = null

    private var scanFragmentListener: ScanFragmentListener? = null
    private val viewPagerFragment = ViewPagerFragment()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, ScanFragmentViewModel.Factory(context))
                .get(ScanFragmentViewModel::class.java)
        activity?.let {
            activityViewModel = ViewModelProvider(it).get(MainActivityViewModel::class.java)
        }
        btService = (activity as MainActivity).bluetoothService!!
        setupBackStackCallbacks()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_scan, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.fragment_scan_label)

        observeChanges()

        childFragmentManager.beginTransaction().apply {
            add(R.id.child_fragment_container, viewPagerFragment)
            commit()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateActiveConnections(btService.getActiveConnections())
    }

    override fun onPause() {
        super.onPause()
        viewModel.setIsScanningOn(false)
    }

    fun setScanFragmentListener(listener: ScanFragmentListener) {
        scanFragmentListener = listener
    }

    private fun setupBackStackCallbacks() {
        activity?.onBackPressedDispatcher?.addCallback(this, backPressedCallback)
    }

    private fun observeChanges() {
        viewModel.isFilterViewOn.observe(viewLifecycleOwner, Observer {
            toggleFilterFragment(it)
        })
        viewModel.isScanningOn.observe(viewLifecycleOwner, Observer {
            if (activityViewModel?.isLocationPermissionGranted?.value == true) {
                toggleScannerState(it)
                if (it) viewModel.setTimestamps()
                scanFragmentListener?.onScanningStateChanged(it)
            }
        })
    }

    fun toggleScannerState(isOn: Boolean) {
        if (isOn) startDiscovery()
        else stopDiscovery()
    }

    private fun startDiscovery() {
        (activity as MainActivity).bluetoothService?.let {
            it.removeListener(this)
            it.addListener(this)
            it.startDiscovery(emptyList())
        }
    }

    private fun stopDiscovery() {
        (activity as MainActivity).bluetoothService?.let {
            it.removeListener(this)
            it.stopDiscovery()
        }
    }

    private fun toggleFilterFragment(isFilterViewOn: Boolean) {
        childFragmentManager.apply {
            if (isFilterViewOn) {
                beginTransaction().apply {
                    hide(viewPagerFragment)
                    add(R.id.child_fragment_container, FilterFragment())
                    addToBackStack(null)
                    commit()
                }
            } else {
                popBackStack()
                activity?.title = getString(R.string.fragment_scan_label)
            }
        }
        (activity as MainActivity).apply {
            toggleMainNavigation(!isFilterViewOn)
            toggleHomeIcon(isFilterViewOn)
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.getIsFilterViewOn()) {
                viewModel.setIsFilterViewOn(false)
            }
            else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    override fun handleScanResult(scanResult: ScanResultCompat) {
        viewModel.handleScanResult(scanResult)
    }

    override fun onDiscoveryFailed() {
        viewModel.setIsScanningOn(false)
    }

    interface ScanFragmentListener {
        fun onScanningStateChanged(isOn: Boolean)
    }

}