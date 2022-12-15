package com.siliconlabs.bledemo.features.configure.gatt_configurator.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters.EditGattServerAdapter.AddServiceListener
import kotlinx.android.synthetic.main.adapter_add_service.view.*

class AddServiceViewHolder(view: View, private val addServiceListener: AddServiceListener) : RecyclerView.ViewHolder(view) {
    private val btnAddService = view.btn_add_service

    fun bind() {
        handleClickEvents()
    }

    private fun handleClickEvents() {
        btnAddService.setOnClickListener {
            addServiceListener.onAddService()
        }
    }

    companion object {
        fun create(parent: ViewGroup, addServiceListener: AddServiceListener): AddServiceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_add_service, parent, false)
            return AddServiceViewHolder(view, addServiceListener)
        }
    }
}