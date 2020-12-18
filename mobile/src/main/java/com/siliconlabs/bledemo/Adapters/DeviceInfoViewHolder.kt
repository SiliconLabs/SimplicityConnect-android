package com.siliconlabs.bledemo.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo

abstract class DeviceInfoViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!), View.OnClickListener {
    private var onClickListener: View.OnClickListener? = null

    abstract fun setData(info: BluetoothDeviceInfo, position: Int, size: Int)
    override fun onClick(v: View) {
        onClickListener?.onClick(v)
    }

    fun setOnClickListener(onClickListener: View.OnClickListener?) {
        this.onClickListener = onClickListener
    }

    abstract class Generator(private val layoutResId: Int) {
        fun generate(parent: ViewGroup): DeviceInfoViewHolder {
            val li = LayoutInflater.from(parent.context)
            val itemView = li.inflate(layoutResId, parent, false)
            return generate(itemView)
        }

        abstract fun generate(itemView: View): DeviceInfoViewHolder

    }
}