package com.siliconlabs.bledemo.gatt_configurator.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.gatt_configurator.viewholders.GattServerViewHolder

class GattServerAdapter(private val list: List<GattServer>, private val listener: OnClickListener) : RecyclerView.Adapter<GattServerViewHolder>() {
    private var isExportMode: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GattServerViewHolder {
        return GattServerViewHolder.create(parent, list, listener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: GattServerViewHolder, position: Int) {
        holder.bind(list[position], isExportMode)
    }

    fun setExportMode(isExportMode: Boolean) {
        this.isExportMode = isExportMode
        notifyDataSetChanged()
    }

    interface OnClickListener {
        fun onCopyClick(gattServer: GattServer)
        fun onEditClick(position: Int, gattServer: GattServer)
        fun onRemoveClick(position: Int)
        fun switchItemOn(position: Int)
        fun switchItemOff(position: Int)
        fun onExportBoxClick()
    }
}