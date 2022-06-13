package com.siliconlabs.bledemo.rssi_graph.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.rssi_graph.dialog_fragments.SortDialogFragment
import com.siliconlabs.bledemo.rssi_graph.model.ScannedDevice

class DeviceLabelAdapter(
        private val context: Context,
        private val legendClickHandler: LegendClickHandler
) : RecyclerView.Adapter<DeviceLabelAdapter.DeviceLabelViewHolder>() {

    private var highlightedDevice: ScannedDevice? = null
    private var activeSortMode = SortDialogFragment.SortMode.NONE

    private val sortedListCallback: SortedList.Callback<ScannedDevice> = object : SortedList.Callback<ScannedDevice>() {
        override fun compare(o1: ScannedDevice, o2: ScannedDevice): Int {
            return when (activeSortMode) {
                SortDialogFragment.SortMode.ASCENDING -> o1.graphData.last().rssi - o2.graphData.last().rssi
                SortDialogFragment.SortMode.DESCENDING -> o2.graphData.last().rssi - o1.graphData.last().rssi
                SortDialogFragment.SortMode.A_TO_Z -> o1.name.compareTo(o2.name)
                SortDialogFragment.SortMode.Z_TO_A -> o2.name.compareTo(o1.name)
                else -> -1
            }
        }

        override fun onInserted(position: Int, count: Int) {
            notifyDataSetChanged()
        }

        override fun onRemoved(position: Int, count: Int) { }
        override fun onMoved(fromPosition: Int, toPosition: Int) { }
        override fun onChanged(position: Int, count: Int) { }
        override fun areContentsTheSame(oldItem: ScannedDevice, newItem: ScannedDevice): Boolean {
            return oldItem.address == newItem.address }
        override fun areItemsTheSame(item1: ScannedDevice, item2: ScannedDevice): Boolean {
            return item1.address == item2 .address}

    }
    private var deviceLabels = SortedList(ScannedDevice::class.java, sortedListCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceLabelViewHolder {
        val view =  LayoutInflater.from(context).inflate(R.layout.rssi_device_label, parent, false)
        return DeviceLabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceLabelViewHolder, position: Int) {
        val device = deviceLabels[position]

        holder.apply {
            dotView.background.setTint(device.dataColor)
            deviceAddress.text = device.address
            deviceName.text = device.name

            itemView.alpha =
                    if (device.address == highlightedDevice?.address) 1.0f
                    else 0.8f
        }
    }

    override fun getItemCount(): Int = deviceLabels.size()

    fun updateAllLabels(scannedDevices: List<ScannedDevice>) {
        deviceLabels.clear()
        deviceLabels.addAll(scannedDevices)
        notifyDataSetChanged()
    }

    fun resetAllLabels() {
        deviceLabels.clear()
        notifyDataSetChanged()
    }

    fun updateHighlightedDevice(highlightedDevice: ScannedDevice?) {
        this.highlightedDevice = highlightedDevice
        notifyDataSetChanged()
    }

    fun addNewDeviceLabel(newDevice: ScannedDevice) {
        deviceLabels.add(newDevice)
    }

    fun updateSortMode(sortMode: SortDialogFragment.SortMode) {
        activeSortMode = sortMode
    }

    interface LegendClickHandler {
        fun onLegendItemClicked(device: ScannedDevice?)
    }

    inner class DeviceLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dotView: View = itemView.findViewById<View>(R.id.rssi_label_color_dot)
        val deviceAddress: TextView = itemView.findViewById(R.id.rssi_label_device_address)
        val deviceName: TextView = itemView.findViewById(R.id.rssi_label_device_name)

        init {
            itemView.setOnClickListener {
                if (this.adapterPosition != RecyclerView.NO_POSITION) {
                    legendClickHandler.onLegendItemClicked(deviceLabels[this.adapterPosition])
                }
            }
        }
    }
}