package com.siliconlabs.bledemo.features.configure.advertiser.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.SpinnerDataTypeItemLayoutBinding
import com.siliconlabs.bledemo.features.configure.advertiser.models.DataTypeItem

//import kotlinx.android.synthetic.main.spinner_data_type_item_layout.view.*

class DataTypeAdapter(
    context: Context,
    private val values: List<DataTypeItem>,
    private val callback: Callback
) : ArrayAdapter<DataTypeItem>(context, R.layout.spinner_data_type_item_layout, values) {
    private lateinit var binding: SpinnerDataTypeItemLayoutBinding

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, parent)
    }


    private fun createViewFromResource(position: Int, parent: ViewGroup?): View {
        binding = SpinnerDataTypeItemLayoutBinding.inflate(LayoutInflater.from(context)).apply {
            tvIdentifier.text = values[position].identifier
            tvName.text = values[position].name
            tvIdentifier.isEnabled = isEnabled(position)
            tvName.isEnabled = isEnabled(position)

            tvName.setOnClickListener {
                callback.onItemClick(position)
            }
            tvIdentifier.setOnClickListener {
                callback.onItemClick(position)
            }
            //TODO
            //if (isEnabled(position)) setOnClickListener { callback.onItemClick(position) }

        }
        return binding.root
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