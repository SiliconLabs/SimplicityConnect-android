package com.siliconlabs.bledemo.advertiser.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.advertiser.models.DataTypeItem
import kotlinx.android.synthetic.main.spinner_data_type_item_layout.view.*

class DataTypeAdapter(context: Context,
                      private val values: List<DataTypeItem>,
                      private val callback: Callback) : ArrayAdapter<DataTypeItem>(context, R.layout.spinner_data_type_item_layout, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, parent)
    }

    private fun createViewFromResource(position: Int, parent: ViewGroup?): View {
        return (LayoutInflater.from(context).inflate(R.layout.spinner_data_type_item_layout, parent, false) as LinearLayout).apply {
            tv_identifier.text = values[position].identifier
            tv_name.text = values[position].name
            tv_identifier.isEnabled = isEnabled(position)
            tv_name.isEnabled = isEnabled(position)

            if (isEnabled(position)) setOnClickListener { callback.onItemClick(position) }
        }
    }

    override fun isEnabled(position: Int): Boolean {
        return values[position].enabled
    }

    fun setItemState(enabled: Boolean, position: Int) {
        values[position].enabled = enabled
    }

    interface Callback {
        fun onItemClick(position: Int)
    }
}