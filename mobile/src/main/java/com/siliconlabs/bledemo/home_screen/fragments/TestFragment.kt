package com.siliconlabs.bledemo.home_screen.fragments

import android.content.*
import android.os.Bundle
import android.view.*
import com.siliconlabs.bledemo.home_screen.dialogs.SelectDeviceDialog
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentTestBinding
import com.siliconlabs.bledemo.features.iop_test.dialogs.AboutIopDialog
import com.siliconlabs.bledemo.home_screen.base.BaseMainMenuFragment

class TestFragment : BaseMainMenuFragment(), DialogInterface.OnDismissListener {

    private lateinit var viewBinding: FragmentTestBinding
    private var selectDeviceDialog: SelectDeviceDialog? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        viewBinding = FragmentTestBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = getString(R.string.main_navigation_test_title)

        initMainViewValues()
        setupUiListeners()
    }

    private fun initMainViewValues() {
        viewBinding.fragmentMainView.fullScreenInfo.apply {
            image.setImageResource(R.drawable.redesign_ic_main_view_iop)
            textPrimary.text = getString(R.string.iop_test_full_page_info)
            textSecondary.visibility = View.GONE
        }
        viewBinding.fragmentMainView.extendedFabMainView.text = getString(R.string.iop_test_select_device_btn)
    }

    private fun setupUiListeners() {
        viewBinding.fragmentMainView.extendedFabMainView.setOnClickListener {
            selectDeviceDialog = SelectDeviceDialog.newDialog(BluetoothService.GattConnectType.IOP_TEST)
            selectDeviceDialog?.show(childFragmentManager, "select_device_dialog")
        }
        viewBinding.locationBar.setLocationInfoFragmentManager(childFragmentManager)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_test_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.info_dialog -> {
                AboutIopDialog().show(childFragmentManager, "about_dialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBluetoothStateChanged(isOn: Boolean) {
        toggleBluetoothBar(isOn, viewBinding.bluetoothBar)
        viewBinding.fragmentMainView.extendedFabMainView.isEnabled = isOn
        if (!isOn) selectDeviceDialog?.dismiss()
    }

    override fun onLocationStateChanged(isOn: Boolean) {
        toggleLocationBar(isOn, viewBinding.locationBar)
    }

    override fun onLocationPermissionStateChanged(isGranted: Boolean) {
        toggleLocationPermissionBar(isGranted, viewBinding.locationPermissionBar)
        viewBinding.fragmentMainView.extendedFabMainView.isEnabled = isGranted
        if (!isGranted) selectDeviceDialog?.dismiss()
    }

    override fun onDismiss(dialogInterface: DialogInterface) {
        selectDeviceDialog = null
    }
}