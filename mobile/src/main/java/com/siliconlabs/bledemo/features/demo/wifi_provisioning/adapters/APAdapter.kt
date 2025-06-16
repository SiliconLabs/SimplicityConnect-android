package com.siliconlabs.bledemo.features.demo.wifi_provisioning.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.adapters.AccessPointsAdapter.OnItemClickListener
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.models.SecurityMode

import com.siliconlabs.bledemo.features.demo.wifi_provisioning.adapters.APAdapter.APViewHolder
import com.siliconlabs.bledemo.features.demo.wifi_provisioning.model.ScanResult


class APAdapter(private var scanResults: List<ScanResult>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<APViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): APViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_access_point, parent, false)
        return APViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return scanResults.size
    }

    fun getItem(position: Int): ScanResult {
        return scanResults[position]
    }

    override fun onBindViewHolder(holder: APViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun convertSecurityMode(mode: SecurityMode): Int {
        return when (mode) {
            SecurityMode.OPEN -> R.string.security_mode_open
            SecurityMode.WPA -> R.string.security_mode_wpa
            SecurityMode.WPA2 -> R.string.security_mode_wpa2
            SecurityMode.WPA3 -> R.string.security_mode_wpa3
            SecurityMode.WEP -> R.string.security_mode_wep
            SecurityMode.EAP_WPA -> R.string.security_mode_eap_wpa
            SecurityMode.EAP_WPA2 -> R.string.security_mode_eap_wpa2
            SecurityMode.WPA_WPA2 -> R.string.status_wpa_wpa2
            else -> R.string.security_mode_unknown
        }
    }

    interface OnItemClickListener {
        fun onItemClick(itemView: View?, position: Int)
    }

    fun updateData(newScanResults: List<ScanResult>) {
        scanResults = newScanResults
        println("SI Connect Size:${scanResults.size}")
        notifyDataSetChanged() // Notify the adapter about the data change
    }

    inner class APViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var name: TextView = itemView.findViewById(R.id.access_pt_name)
        private var securityMode: TextView = itemView.findViewById(R.id.access_pt_type)
        private var macAddress: TextView = itemView.findViewById(R.id.access_mac_address)
        private var ipAddress: TextView = itemView.findViewById(R.id.access_pt_ip_address)
        private var statusImage: ImageView =
            itemView.findViewById(R.id.access_pt_status)


        fun bind(position: Int) {
            val deviceName = scanResults[position].ssid
            name.text = deviceName
            securityMode.text = scanResults[position].security_type
            macAddress.text = scanResults[position].bssid.uppercase()
            ipAddress.text = "rssi :" + scanResults[position].rssi
            statusImage.visibility = View.GONE
            macAddress.visibility = View.VISIBLE
            ipAddress.visibility = View.VISIBLE
            itemView.setOnClickListener { view: View? -> listener.onItemClick(view, position) }
        }

    }
}