package com.siliconlabs.bledemo.gatt_configurator.viewholders

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.gatt_configurator.adapters.GattServerAdapter.OnClickListener
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Views.DetailsRow
import kotlinx.android.synthetic.main.adapter_gatt_server.view.*

class GattServerViewHolder(view: View, private val list: List<GattServer>, private val listener: OnClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    private val llGattServerDetails = view.ll_gatt_server_details
    private val rlGattServer = view.rl_gatt_server
    private val cbExport = view.cb_export
    private val tvTotalServices = view.tv_total_services
    private val tvName = view.tv_gatt_server_name
    private val swGattServer = view.sw_gatt_server
    private val ibCopy = view.ib_copy
    private val ibEdit = view.ib_edit
    private val ibRemove = view.ib_remove

    fun bind(gattServer: GattServer, isExportMode: Boolean) {
        expandOrCollapseDetailsView(gattServer.isViewExpanded)

        itemView.setOnClickListener(this)
        tvName.text = gattServer.name
        tvTotalServices.text = itemView.context.resources.getString(R.string.gatt_configurator_n_services, gattServer.services.size)
        swGattServer.isChecked = gattServer.isSwitchedOn

        prepareView(isExportMode)
        prepareDetailsView(gattServer)
        handleClickActions(gattServer)
        handleSwitchActions()
    }

    private fun prepareView(isExportMode: Boolean) {
        cbExport.visibility = if (isExportMode) View.VISIBLE else View.GONE

        val margin8Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, itemView.context.resources.displayMetrics).toInt()

        val layoutParams = rlGattServer.layoutParams as ViewGroup.MarginLayoutParams
        if (isExportMode) {
            toggleImageButton(ibCopy, false)
            toggleImageButton(ibEdit, false)
            toggleImageButton(ibRemove, false)
            when (adapterPosition) {
                0 -> layoutParams.setMargins(0, margin8Dp, margin8Dp, 0)
                list.size - 1 -> layoutParams.setMargins(0, 0, margin8Dp, margin8Dp)
                else -> layoutParams.setMargins(0, 0, margin8Dp, 0)
            }
        } else {
            cbExport.isChecked = false
            toggleImageButton(ibCopy, true)
            toggleImageButton(ibEdit, true)
            toggleImageButton(ibRemove, true)
            when (adapterPosition) {
                0 -> layoutParams.setMargins(margin8Dp, margin8Dp, margin8Dp, 0)
                list.size - 1 -> layoutParams.setMargins(margin8Dp, 0, margin8Dp, margin8Dp)
                else -> layoutParams.setMargins(margin8Dp, 0, margin8Dp, 0)
            }
        }
    }

    private fun handleClickActions(gattServer: GattServer) {
        ibCopy.setOnClickListener {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onCopyClick(gattServer)
            }
        }

        ibEdit.setOnClickListener {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onEditClick(adapterPosition, gattServer)
            }
        }

        ibRemove.setOnClickListener {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onRemoveClick(adapterPosition)
            }
        }

        cbExport.setOnClickListener {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                list[adapterPosition].let {
                    it.isCheckedForExport = !it.isCheckedForExport
                    listener.onExportBoxClick()
                }
            }
        }
    }

    private fun handleSwitchActions() {
        swGattServer.setOnCheckedChangeListener { _, isChecked ->
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (isChecked) {
                    listener.switchItemOn(adapterPosition)
                } else {
                    listener.switchItemOff(adapterPosition)
                }
            }
        }
    }

    private fun prepareDetailsView(gattServer: GattServer) {
        llGattServerDetails.removeAllViews()
        for (service in gattServer.services) {
            llGattServerDetails.addView(DetailsRow(
                    itemView.context,
                    service.getUuidWithName(),
                    itemView.context.getString(R.string.gatt_configurator_n_characteristics, service.characteristics.size))
            )
        }
    }

    override fun onClick(v: View) {
        val gattServer = list[adapterPosition]
        gattServer.apply {
            isViewExpanded = !isViewExpanded
            expandOrCollapseDetailsView(isViewExpanded)
        }
    }

    private fun expandOrCollapseDetailsView(isExpanded: Boolean) {
        llGattServerDetails.visibility = if (isExpanded) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun toggleImageButton(ib: ImageButton, isEnabled: Boolean) {
        ib.let {
            it.isEnabled = isEnabled
            it.imageAlpha = if (isEnabled) 0xFF else 0x3F
        }
    }

    companion object {
        fun create(parent: ViewGroup, list: List<GattServer>, listener: OnClickListener): GattServerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_gatt_server, parent, false)
            return GattServerViewHolder(view, list, listener)
        }
    }
}