package com.siliconlabs.bledemo.features.scan.active_connections.fragments

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.scan.browser.activities.DeviceServicesActivity
import com.siliconlabs.bledemo.features.scan.active_connections.adapters.ConnectionsAdapter
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.features.scan.active_connections.adapters.ConnectionsAdapterCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentConnectionsBinding
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.common.other.CardViewListDecoration
import com.siliconlabs.bledemo.home_screen.base.ViewPagerFragment
import com.siliconlabs.bledemo.home_screen.base.BaseServiceDependentMainMenuFragment
import com.siliconlabs.bledemo.home_screen.base.BluetoothDependent
import com.siliconlabs.bledemo.home_screen.base.NotificationDependent

class ConnectionsFragment : BaseServiceDependentMainMenuFragment() {

    private lateinit var _binding: FragmentConnectionsBinding
    private lateinit var service: BluetoothService
    private lateinit var viewModel: ScanFragmentViewModel
    var adapter: ConnectionsAdapter? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(null != (activity as MainActivity).bluetoothService){
            service = (activity as MainActivity).bluetoothService!!
        }
        viewModel = (parentFragment as ViewPagerFragment).getScanFragment().viewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectionsBinding.inflate(inflater)
        hidableActionButton = _binding.fragmentMainView.extendedFabMainView
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMainViewValues()
        initConnectionsAdapter()
        setUiListeners()
        setDataObservers()
    }

    override val bluetoothDependent = object : BluetoothDependent {

        override fun onBluetoothStateChanged(isBluetoothOn: Boolean) {
            toggleBluetoothBar(isBluetoothOn, _binding.bluetoothEnable)
            viewModel.updateActiveConnections(service.getActiveConnections())
        }
        override fun onBluetoothPermissionsStateChanged(arePermissionsGranted: Boolean) {
            toggleBluetoothPermissionsBar(arePermissionsGranted, _binding.bluetoothPermissionsBar)
        }

        override fun refreshBluetoothDependentUi(isBluetoothOperationPossible: Boolean) {
            /* Not needed */
        }
        override fun setupBluetoothPermissionsBarButtons() {
            _binding.bluetoothPermissionsBar.setFragmentManager(childFragmentManager)
        }
    }


    override fun onResume() {
        super.onResume()
        service.registerGattCallback(gattCallback)
        refreshConnectionsList()
    }

    override fun onStop() {
        super.onStop()
        resetProgressBars()
    }

    private fun initMainViewValues() {
        _binding.fragmentMainView.fullScreenInfo.apply {
            image.setImageResource(R.drawable.redesign_ic_main_view_active_connections)
            textPrimary.text = getString(R.string.connections_info_primary_text)
            textSecondary.text = getString(R.string.connections_info_secondary_text)
        }
        _binding.fragmentMainView.extendedFabMainView.text = getString(R.string.connections_button_disconnect_all)
    }

    private fun initConnectionsAdapter() {
        adapter = ConnectionsAdapter(connectionAdapterCallback)
        _binding.fragmentMainView.rvMainView.apply {
            layoutManager = getLayoutManagerWithHidingUIElements(context)
            addItemDecoration(CardViewListDecoration())
            adapter = this@ConnectionsFragment.adapter
        }
    }

    private fun setUiListeners() {
        _binding.fragmentMainView.extendedFabMainView.setOnClickListener { service.disconnectAllGatts() }
    }

    private fun setDataObservers() {
        viewModel.activeConnections.observe(viewLifecycleOwner, Observer {
            refreshConnectionsList()
        })
    }

    private fun refreshConnectionsList() {
        val list = viewModel.getConnectionViewStates()
        adapter?.updateList(list)
        toggleMainView(list.isNotEmpty())
    }

    private fun toggleMainView(isAnyDeviceConnected: Boolean) {
        _binding.fragmentMainView.apply {
            if (isAnyDeviceConnected) {
                fullScreenInfo.root.visibility = View.GONE
                rvMainView.visibility = View.VISIBLE
                if(hidableActionButton == null){
                    hidableActionButton = extendedFabMainView
                    extendedFabMainView.show()
                }
            } else {
                fullScreenInfo.root.visibility = View.VISIBLE
                rvMainView.visibility = View.GONE
                hidableActionButton = null
                extendedFabMainView.hide()
            }
        }
    }

    private fun resetProgressBars() {
        adapter?.let { adapter ->
            for (pos in 0.. adapter.itemCount) {
                val viewHolder = _binding.fragmentMainView.rvMainView
                        .findViewHolderForAdapterPosition(pos) as? ConnectionsAdapter.ConnectionViewHolder
                viewHolder?.let { adapter.resetProgressBar(it) }
            }
        }
    }

    private fun showToastMessage(deviceName: String) {
        Toast.makeText(requireContext(),
                getString(R.string.device_x_disconnected, deviceName), Toast.LENGTH_SHORT).also {
            if (service.getActiveConnections().isEmpty()) {
                it.setText(R.string.all_devices_disconnected)
            }
            it.show()
        }

    }

    private val connectionAdapterCallback = object : ConnectionsAdapterCallback {
        override fun onDisconnectClicked(deviceAddress: String) {
            service.disconnectGatt(deviceAddress)
        }

        override fun onDeviceClicked(deviceToConnect: BluetoothDevice) {
            DeviceServicesActivity.startActivity(requireContext(), deviceToConnect)
        }
    }

    private val gattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            activity?.runOnUiThread {
                viewModel.updateActiveConnections(service.getActiveConnections())
            }

            if (newState == BluetoothGatt.STATE_DISCONNECTED &&
                    (status == BluetoothGatt.GATT_SUCCESS || status == 19)) {
                activity?.runOnUiThread {
                    val deviceName = viewModel.findConnectionByAddress(gatt.device.address)
                            ?.bluetoothInfo?.name ?: "Unknown"
                    showToastMessage(deviceName)
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                activity?.runOnUiThread {
                    viewModel.updateActiveConnections(service.getActiveConnections())
                }
            }
        }
    }
}
