package com.siliconlabs.bledemo.Advertiser.Adapters

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Advertiser.Models.Advertiser
import com.siliconlabs.bledemo.Advertiser.Utils.Translator
import com.siliconlabs.bledemo.Advertiser.Views.AdvertiserDetails
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.adapter_advertiser.view.*

class AdvertiserAdapter(val items: ArrayList<Advertiser>, val context: Context, private val itemClickListener: OnItemClickListener) : RecyclerView.Adapter<AdvertiserAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_advertiser, parent, false), context)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: Advertiser = items[position]
        holder.bind(item, itemClickListener)

        val margin16Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
        val margin10Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics).toInt()

        val layoutParams = holder.cvAdvertiser.layoutParams as ViewGroup.MarginLayoutParams
        when (position) {
            0 -> layoutParams.setMargins(margin16Dp, margin16Dp, margin16Dp, margin10Dp)
            itemCount - 1 -> layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin16Dp)
            else -> layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin10Dp)
        }

        holder.itemView.setOnClickListener {
            item.displayDetailsView = !item.displayDetailsView
            holder.collapseOrExpand(holder.llDetails, item.displayDetailsView)
        }
    }

    class ViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val translator = Translator(context)

        private val tvDeviceName = itemView.tv_device_name as TextView
        private val tvTxPower = itemView.tv_tx_power as TextView
        private val tvInterval = itemView.tv_interval as TextView
        private val ibCopy = itemView.ib_copy as ImageButton
        private val ibEdit = itemView.ib_edit as ImageButton
        private val ibRemove = itemView.ib_remove as ImageButton
        private val swAdvertiser = itemView.sw_advertiser as SwitchCompat
        val cvAdvertiser = itemView.cv_advertiser as CardView
        val llDetails = itemView.ll_advertisement_details as LinearLayout

        fun bind(item: Advertiser, clickListener: OnItemClickListener) {
            llDetails.removeAllViews()
            llDetails.addView(AdvertiserDetails(context).getAdvertiserDetailsView(item, translator))

            collapseOrExpand(llDetails, item.displayDetailsView)

            val data = item.data
            tvDeviceName.text = data.name
            val txPower = data.txPower
            tvTxPower.text = context.getString(R.string.unit_value_dbm, txPower)
            tvInterval.text = context.getString(R.string.unit_value_ms, data.advertisingIntervalMs)

            if (swAdvertiser.isChecked != item.isRunning) swAdvertiser.isChecked = item.isRunning

            ibCopy.setOnClickListener { clickListener.onCopyClick(item) }
            ibEdit.setOnClickListener { if (adapterPosition != RecyclerView.NO_POSITION) clickListener.onEditClick(adapterPosition) }
            ibRemove.setOnClickListener { if (adapterPosition != RecyclerView.NO_POSITION) clickListener.onRemoveClick(adapterPosition) }

            swAdvertiser.setOnCheckedChangeListener { _, isChecked ->
                if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
                    if (isChecked && adapterPosition != RecyclerView.NO_POSITION) clickListener.switchItemOn(adapterPosition)
                    else if (adapterPosition != RecyclerView.NO_POSITION) clickListener.switchItemOff(adapterPosition)
                } else {
                    Toast.makeText(context, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show()
                    if (isChecked) swAdvertiser.isChecked = false
                }
            }
        }

        fun collapseOrExpand(view: View, displayDetails: Boolean) {
            view.visibility = if (displayDetails) View.VISIBLE else View.GONE
        }
    }

    interface OnItemClickListener {
        fun onCopyClick(item: Advertiser)
        fun onEditClick(position: Int)
        fun onRemoveClick(position: Int)
        fun switchItemOn(position: Int)
        fun switchItemOff(position: Int)
    }
}