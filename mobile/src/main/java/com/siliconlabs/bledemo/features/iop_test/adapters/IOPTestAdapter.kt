package com.siliconlabs.bledemo.features.iop_test.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.features.iop_test.models.ItemTestCaseInfo
import java.util.ArrayList

class IOPTestAdapter(var list: List<ItemTestCaseInfo>) : RecyclerView.Adapter<IOPTestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IOPTestViewHolder {
        return IOPTestViewHolder.create(parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: IOPTestViewHolder, position: Int) {
        val info = list[position]
        holder.bind(info)
    }

    fun refreshDataItem(list: ArrayList<ItemTestCaseInfo>) {
        this.list = list
        notifyDataSetChanged()
    }
}