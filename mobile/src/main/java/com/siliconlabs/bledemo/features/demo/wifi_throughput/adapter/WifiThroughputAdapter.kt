package com.siliconlabs.bledemo.features.demo.wifi_throughput.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.wifi_throughput.model.WifiThroughputData

class WifiThroughputAdapter(private val list: MutableList<String>) : RecyclerView.Adapter<WifiThroughputAdapter.ViewHolder>() {
    val myData = mutableListOf<String>()

    fun submitList(newData: MutableList<String>) {
        myData.clear()
        myData.addAll(newData)
        notifyDataSetChanged()
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val interval_tv: TextView
        val transfer_rate_tv: TextView
        val bandwidth_rate_tv: TextView

        init {
            // Define click listener for the ViewHolder's View
            interval_tv = view.findViewById(R.id.interval)
            transfer_rate_tv = view.findViewById(R.id.transfer_rate)
            bandwidth_rate_tv = view.findViewById(R.id.bandwidth)

        }
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.wifi_throughput_log_row, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val throughPutValues = myData.get(position).split(",").toTypedArray()

        holder.interval_tv.text = throughPutValues.get(0)
        holder.transfer_rate_tv.text = throughPutValues.get(1)
        holder.bandwidth_rate_tv.text = "${throughPutValues.get(2)} Mbits/sec"
       /* holder.transfer_rate_tv.visibility = View.GONE
        holder.bandwidth_rate_tv.visibility = View.GONE*/

    }

    override fun getItemCount(): Int {
        return  myData.size
    }
}