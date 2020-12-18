package com.siliconlabs.bledemo.Browser.Adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.Browser.Adapters.ConnectionsAdapter.ConnectionViewHolder
import com.siliconlabs.bledemo.Browser.ServicesConnectionsCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.Constants
import java.util.*

class ConnectionsAdapter(var connectionsList: List<BluetoothDevice>, private val context: Context) : RecyclerView.Adapter<ConnectionViewHolder>() {
    private var servicesConnectionsCallback: ServicesConnectionsCallback? = null
    var selectedDevice: String? = null

    inner class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var tvName = itemView.findViewById(R.id.tv_device_name) as TextView
        private var btnDisconnect = itemView.findViewById(R.id.btn_disconnect) as Button
        private var progressBar = itemView.findViewById(R.id.progress_bar_connections) as ProgressBar
        var layout = itemView.findViewById(R.id.connected_device_item) as LinearLayout

        fun bind(device: BluetoothDevice) {
            progressBar.visibility = View.GONE
            btnDisconnect.visibility = View.VISIBLE

            var name = device.name
            val address = device.address
            if (name == null || name == "") {
                name = Constants.NA
            }

            if (selectedDevice != null) {
                if (address.toLowerCase(Locale.getDefault()) == selectedDevice?.toLowerCase(Locale.getDefault())) {
                    tvName.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue))
                } else {
                    tvName.setTextColor(ContextCompat.getColor(context, R.color.silabs_primary_text))
                }
            } else {
                tvName.setTextColor(ContextCompat.getColor(context, R.color.silabs_primary_text))
            }

            tvName.text = name
            btnDisconnect.setOnClickListener {
                val bluetoothDeviceInfo = BluetoothDeviceInfo()
                bluetoothDeviceInfo.device = device
                servicesConnectionsCallback?.onDisconnectClicked(bluetoothDeviceInfo)
            }

            layout.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                btnDisconnect.visibility = View.GONE
                val bluetoothDeviceInfo = BluetoothDeviceInfo()
                bluetoothDeviceInfo.device = device
                servicesConnectionsCallback?.onDeviceClicked(bluetoothDeviceInfo)
            }

        }
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        val device = connectionsList[position]
        holder.bind(device)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_connection, parent, false)
        return ConnectionViewHolder(view)
    }

    override fun getItemCount(): Int {
        return connectionsList.size
    }

    fun setServicesConnectionsCallback(servicesConnectionsCallback: ServicesConnectionsCallback?) {
        this.servicesConnectionsCallback = servicesConnectionsCallback
    }

}