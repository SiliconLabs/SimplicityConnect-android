package com.siliconlabs.bledemo.Browser.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.Bluetooth.BLE.BlueToothService
import com.siliconlabs.bledemo.Browser.Activities.DeviceServicesActivity
import com.siliconlabs.bledemo.Browser.Adapters.ConnectionsAdapter
import com.siliconlabs.bledemo.Browser.ToolbarCallback
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_connections.*

class ConnectionsFragment : Fragment() {
    var adapter: ConnectionsAdapter? = null
    private var toolbarCallback: ToolbarCallback? = null
    private var bluetoothBinding: BlueToothService.Binding? = null

    fun setCallback(toolbarCallback: ToolbarCallback?): ConnectionsFragment {
        this.toolbarCallback = toolbarCallback
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connections, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iv_close.setOnClickListener { toolbarCallback?.close() }

        rv_connections.layoutManager = LinearLayoutManager(activity)
        val dividerItemDecoration = DividerItemDecoration(rv_connections.context, LinearLayoutManager(activity).orientation)
        rv_connections.addItemDecoration(dividerItemDecoration)
        rv_connections.adapter = adapter

        btn_disconnect_all.setOnClickListener {
            bluetoothBinding = object : BlueToothService.Binding(requireContext()) {
                override fun onBound(service: BlueToothService?) {
                    for (device in adapter?.connectionsList!!) {
                        val successDisconnected = service?.disconnectGatt(device.address)
                        if (!successDisconnected!!) {
                            Toast.makeText(context, R.string.device_not_from_EFR, Toast.LENGTH_LONG).show()
                        }
                    }

                    if (activity != null && activity is DeviceServicesActivity) {
                        activity?.finish()
                    } else {
                        toolbarCallback?.close()
                    }
                }
            }
            bluetoothBinding?.bind()
        }
    }
}