package com.siliconlabs.bledemo.home_screen.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.siliconlabs.bledemo.home_screen.dialogs.SelectDeviceDialog
import com.siliconlabs.bledemo.home_screen.adapters.DemoAdapter
import com.siliconlabs.bledemo.home_screen.menu_items.*
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.databinding.FragmentDemoBinding
import com.siliconlabs.bledemo.features.demo.range_test.activities.RangeTestActivity
import com.siliconlabs.bledemo.home_screen.base.BaseMainMenuFragment
import kotlinx.android.synthetic.main.fragment_demo.*

class DemoFragment : BaseMainMenuFragment(), DemoAdapter.OnDemoItemClickListener, DialogInterface.OnDismissListener {

    private lateinit var viewBinding: FragmentDemoBinding
    private var demoAdapter: DemoAdapter? = null
    private val list: ArrayList<DemoMenuItem> = ArrayList()
    private var selectDeviceDialog: SelectDeviceDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list.apply {
            add(HealthThermometer(R.drawable.redesign_ic_demo_health_thermometer, getString(R.string.title_Health_Thermometer), getString(R.string.main_menu_description_thermometer)))
            add(ConnectedLighting(R.drawable.redesign_ic_demo_connected_lighting, getString(R.string.title_Connected_Lighting), getString(R.string.main_menu_description_connected_lighting)))
            add(RangeTest(R.drawable.redesign_ic_demo_range_test, getString(R.string.title_Range_Test), getString(R.string.main_menu_description_range_test)))
            add(Blinky(R.drawable.redesign_ic_demo_blinky, getString(R.string.title_Blinky), getString(R.string.main_menu_description_blinky)))
            add(Throughput(R.drawable.redesign_ic_demo_throughput, getString(R.string.title_Throughput), getString(R.string.main_menu_description_throughput)))
            add(Motion(R.drawable.redesign_ic_demo_motion, getString(R.string.motion_demo_title), getString(R.string.motion_demo_description)))
            add(Environment(R.drawable.redesign_ic_demo_environment, getString(R.string.environment_demo_title), getString(R.string.environment_demo_description)))
            add(WifiCommissioning(R.drawable.redesign_ic_demo_wifi_commissioning, getString(R.string.wifi_commissioning_label), getString(R.string.wifi_commissioning_description)))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        viewBinding = FragmentDemoBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.main_navigation_demo_title)
        viewBinding.locationBar.setLocationInfoFragmentManager(childFragmentManager)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        demoAdapter = DemoAdapter(list, this@DemoFragment)
        viewBinding.rvDemoMenu.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = demoAdapter
        }
        demoAdapter?.toggleItemsEnabled(activityViewModel?.isBluetoothOn?.value ?: false)
    }

    override fun onBluetoothStateChanged(isOn: Boolean) {
        toggleBluetoothBar(isOn, viewBinding.bluetoothBar)
        demoAdapter?.toggleItemsEnabled(isOn)
        if (!isOn) selectDeviceDialog?.dismiss()
    }
    override fun onLocationStateChanged(isOn: Boolean) {
        toggleLocationBar(isOn, viewBinding.locationBar)
    }

    override fun onLocationPermissionStateChanged(isGranted: Boolean) {
        toggleLocationPermissionBar(isGranted, viewBinding.locationPermissionBar)
        demoAdapter?.toggleItemsEnabled(isGranted)
        if (!isGranted) selectDeviceDialog?.dismiss()
    }

    override fun onDemoItemClicked(demoItem: DemoMenuItem) {
        if (demoItem.connectType == BluetoothService.GattConnectType.RANGE_TEST) {
            startActivity(Intent(requireContext(), RangeTestActivity::class.java))
        } else {
            selectDeviceDialog = SelectDeviceDialog.newDialog(demoItem.connectType)
            selectDeviceDialog?.show(childFragmentManager, "select_device_dialog")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        selectDeviceDialog = null
    }
}