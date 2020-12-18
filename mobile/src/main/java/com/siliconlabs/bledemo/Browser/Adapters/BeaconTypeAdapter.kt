package com.siliconlabs.bledemo.Browser.Adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.BeaconUtils.BleFormat
import com.siliconlabs.bledemo.Browser.Adapters.BeaconTypeAdapter.BeaconTypeViewHolder
import com.siliconlabs.bledemo.Browser.Models.BeaconType
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.name_with_checkmark.view.*

class BeaconTypeAdapter(val beaconTypeList: List<BeaconType>, private val context: Context) : RecyclerView.Adapter<BeaconTypeViewHolder>() {

    inner class BeaconTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var tvBeaconType = itemView.tv_name as TextView
        private var ivBeaconIsChecked = itemView.iv_checkmark as ImageView

        fun bind(beaconType: BeaconType) {
            tvBeaconType.text = beaconType.beaconTypeName

            if (beaconType.isChecked) {
                setBeaconSelected()
            } else {
                setBeaconDeselected()
            }

            itemView.setOnClickListener(this)
        }

        private fun setBeaconSelected() {
            ivBeaconIsChecked.visibility = View.VISIBLE
            tvBeaconType.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue))
            tvBeaconType.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        private fun setBeaconDeselected() {
            ivBeaconIsChecked.visibility = View.GONE
            tvBeaconType.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text))
            tvBeaconType.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            val beaconType = beaconTypeList[position]
            if (beaconType.isChecked) {
                setBeaconDeselected()
            } else {
                setBeaconSelected()
            }
            beaconType.isChecked = !beaconType.isChecked
            notifyDataSetChanged()
        }

    }

    override fun onBindViewHolder(holder: BeaconTypeViewHolder, position: Int) {
        val beaconType = beaconTypeList[position]
        holder.bind(beaconType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconTypeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.name_with_checkmark, parent, false)
        return BeaconTypeViewHolder(view)
    }

    override fun getItemCount(): Int {
        return beaconTypeList.size
    }

    fun selectBeacons(bleFormats: List<BleFormat>?) {
        for (b in beaconTypeList) {
            var isChecked = false
            if (bleFormats != null) {
                for (bf in bleFormats) {
                    if (b.bleFormat == bf) {
                        isChecked = true
                        break
                    }
                }
            }
            b.isChecked = isChecked
        }
        notifyDataSetChanged()
    }
}