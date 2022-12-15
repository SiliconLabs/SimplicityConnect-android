package com.siliconlabs.bledemo.features.scan.active_connections.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.features.scan.active_connections.adapters.ConnectionsAdapter.ConnectionViewHolder
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.AdapterConnectedDeviceBinding
import com.siliconlabs.bledemo.utils.RecyclerViewUtils

class ConnectionsAdapter(
        private val connectionsAdapterCallback: ConnectionsAdapterCallback
) : RecyclerView.Adapter<ConnectionViewHolder>() {

    private var connectionsList: List<ScanFragmentViewModel.ConnectionViewState> = listOf()
    private lateinit var _binding: AdapterConnectedDeviceBinding

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads)
        else holder.showChanges(payloads)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(connectionsList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        _binding = AdapterConnectedDeviceBinding.inflate(LayoutInflater.from(parent.context))
        return ConnectionViewHolder(_binding.root).apply {
            setUiListeners(this)
        }
    }

    override fun getItemCount() = connectionsList.size

    fun resetProgressBar(holder: ConnectionViewHolder) {
        holder.toggleProgressBar(shouldDisplayBar = false)
    }


    private fun setUiListeners(holder: ConnectionViewHolder) {
        _binding.apply {
            disconnectBtn.setOnClickListener {
                RecyclerViewUtils.withProperAdapterPosition(holder) { pos ->
                    connectionsAdapterCallback.onDisconnectClicked(connectionsList[pos].device.address)
                }
            }
            deviceContainer.setOnClickListener {
                RecyclerViewUtils.withProperAdapterPosition(holder) { pos ->
                    holder.toggleProgressBar(shouldDisplayBar = true)
                    connectionsAdapterCallback.onDeviceClicked(connectionsList[pos].device)
                }
            }
        }
    }

    fun updateList(newList: List<ScanFragmentViewModel.ConnectionViewState>) {
        val listDiff = DiffUtil.calculateDiff(DiffCallback(
                connectionsList,
                newList
        ), false)

        connectionsList = newList
        listDiff.dispatchUpdatesTo(this)
    }

    private class DiffCallback(
            private val oldList: List<ScanFragmentViewModel.ConnectionViewState>,
            private val newList: List<ScanFragmentViewModel.ConnectionViewState>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].device.address == newList[newItemPosition].device.address
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.rssi == newItem.rssi
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
            return PayloadChange(oldList[oldItemPosition], newList[newItemPosition])
        }
    }

    private class PayloadChange(
            val oldState: ScanFragmentViewModel.ConnectionViewState,
            val newState: ScanFragmentViewModel.ConnectionViewState
    )

    inner class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(viewState: ScanFragmentViewModel.ConnectionViewState) {
            _binding.apply {
                deviceName.text = viewState.device.name
                tvDeviceAddress.text = viewState.device.address
                deviceType.text = itemView.context.getString(viewState.beaconType.nameResId)
                rssi.text = itemView.context.getString(R.string.unit_value_dbm, viewState.rssi)
                tvInterval.text = itemView.context.getString(R.string.unit_value_ms, viewState.intervalNanos / 1_000_000)
            }
            toggleProgressBar(shouldDisplayBar = false)
        }

        fun showChanges(payloads: MutableList<Any>) {
            val oldState = (payloads.first() as PayloadChange).oldState
            val newState = (payloads.last() as PayloadChange).newState

            if (oldState.rssi != newState.rssi) {
                _binding.rssi.text = itemView.context.getString(R.string.unit_value_dbm, newState.rssi)
            }
        }

        fun toggleProgressBar(shouldDisplayBar: Boolean) {
            _binding.apply {
                progressBarConnections.visibility = if (shouldDisplayBar) View.VISIBLE else View.GONE
                disconnectBtn.visibility = if (shouldDisplayBar) View.INVISIBLE else View.VISIBLE
            }
        }
    }

}
