package com.siliconlabs.bledemo.gatt_configurator.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.gatt_configurator.models.Descriptor16Bit
import com.siliconlabs.bledemo.gatt_configurator.models.SearchMode
import java.util.*
import kotlin.collections.ArrayList

class Descriptor16BitAdapter(context: Context, descriptors: List<Descriptor16Bit>, searchMode: SearchMode) : ArrayAdapter<Descriptor16Bit>(context, 0, descriptors) {
    private var descriptorsListFull: List<Descriptor16Bit> = ArrayList<Descriptor16Bit>(descriptors)

    override fun getFilter(): Filter {
        return descriptorFilter
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView?
                ?: LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_item_layout, parent, false) as TextView
        val descriptorItem = getItem(position)
        view.text = descriptorItem?.getFullName()

        return view
    }

    private val descriptorFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions = ArrayList<Descriptor16Bit>()

            if (constraint == null || constraint.isEmpty()) {
                suggestions.addAll(descriptorsListFull)
            } else {
                val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                for (item in descriptorsListFull) {
                    if (searchMode == SearchMode.BY_NAME) {
                        if (item.name.toLowerCase(Locale.ROOT).contains(filterPattern)) {
                            suggestions.add(item)
                        }
                    } else {
                        if (item.identifier.toString(16).contains(filterPattern)) {
                            suggestions.add(item)
                        }
                    }
                }
            }

            results.values = suggestions
            results.count = suggestions.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            addAll(results?.values as List<Descriptor16Bit>)
            notifyDataSetChanged()
        }
    }
}

