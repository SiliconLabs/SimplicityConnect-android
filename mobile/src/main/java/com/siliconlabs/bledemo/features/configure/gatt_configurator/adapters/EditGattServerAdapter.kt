package com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Service
import com.siliconlabs.bledemo.features.configure.gatt_configurator.viewholders.AddServiceViewHolder
import com.siliconlabs.bledemo.features.configure.gatt_configurator.viewholders.EditGattServerViewHolder

class EditGattServerAdapter(private val list: ArrayList<Service>, private val serviceListener: ServiceListener, private val addServiceListener: AddServiceListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == SERVICE_TYPE) EditGattServerViewHolder.create(parent, list, serviceListener) else AddServiceViewHolder.create(parent, addServiceListener)
    }

    override fun getItemCount(): Int {
        return list.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < itemCount - 1) SERVICE_TYPE else ADD_SERVICE_BUTTON_TYPE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == itemCount - 1) {
            (holder as AddServiceViewHolder).bind()
        } else {
            (holder as EditGattServerViewHolder).bind(list[position])
        }
    }

    interface ServiceListener {
        fun onCopyService(service: Service)
        fun onRemoveService(position: Int)
    }

    interface AddServiceListener {
        fun onAddService()
    }

    companion object {
        private const val SERVICE_TYPE = 1
        private const val ADD_SERVICE_BUTTON_TYPE = 2
    }
}