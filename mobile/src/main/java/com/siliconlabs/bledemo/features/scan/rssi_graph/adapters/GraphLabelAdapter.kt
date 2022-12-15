package com.siliconlabs.bledemo.features.scan.rssi_graph.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.AdapterGraphLabelBinding
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.utils.RecyclerViewUtils
import timber.log.Timber

class GraphLabelAdapter(
        private var deviceLabels: MutableList<ScanFragmentViewModel.LabelViewState>,
        private val legendClickHandler: LegendClickHandler
) : RecyclerView.Adapter<GraphLabelAdapter.DeviceLabelViewHolder>() {

    private var highlightedDevice: ScanFragmentViewModel.LabelViewState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceLabelViewHolder {
        val binding = AdapterGraphLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceLabelViewHolder(binding).apply {
            setupUiListeners(this)
        }
    }

    override fun onBindViewHolder(holder: DeviceLabelViewHolder, position: Int) {
        holder.bind(deviceLabels[position])
    }

    override fun getItemCount(): Int = deviceLabels.size


    fun updateLabels(newLabels: List<ScanFragmentViewModel.LabelViewState>) {
        val listDiff = DiffUtil.calculateDiff(DiffCallback(
                deviceLabels.toList(),
                newLabels
        ), true)

        deviceLabels = newLabels.toMutableList()
        listDiff.dispatchUpdatesTo(this)
    }

    fun resetAllLabels() {
        deviceLabels.clear()
        notifyDataSetChanged()
    }

    fun updateHighlightedDevice(highlightedDevice: ScanFragmentViewModel.LabelViewState?) {
        this.highlightedDevice = highlightedDevice
        notifyDataSetChanged()
    }

    fun addNewDeviceLabel(newLabel: ScanFragmentViewModel.LabelViewState) {
        deviceLabels.add(newLabel)
        notifyItemChanged(itemCount - 1)
    }

    private fun setupUiListeners(holder: DeviceLabelViewHolder) {
        holder._binding.root.setOnClickListener {
            RecyclerViewUtils.withProperAdapterPosition(holder) { pos ->
                legendClickHandler.onLegendItemClicked(deviceLabels[pos])
            }
        }
    }

    private class DiffCallback(
            private val oldList: List<ScanFragmentViewModel.LabelViewState>,
            private val newList: List<ScanFragmentViewModel.LabelViewState>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].address == newList[newItemPosition].address
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return true
        }
    }

    interface LegendClickHandler {
        fun onLegendItemClicked(device: ScanFragmentViewModel.LabelViewState?)
    }

    inner class DeviceLabelViewHolder(val _binding: AdapterGraphLabelBinding) : RecyclerView.ViewHolder(_binding.root) {

        fun bind(label: ScanFragmentViewModel.LabelViewState) {
            _binding.apply {
                rssiLabelColorDot.background.setTint(label.color)
                rssiLabelDeviceAddress.text = label.address
                rssiLabelDeviceName.text = label.name
                root.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context,
                        if (label.address == highlightedDevice?.address) android.R.color.white
                        else R.color.graph_label_inactive
                ))
            }
        }
    }
}