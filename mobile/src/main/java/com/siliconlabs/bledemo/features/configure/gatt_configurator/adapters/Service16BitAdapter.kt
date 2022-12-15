package com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.SearchMode
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Service16Bit
import java.util.*
import kotlin.collections.ArrayList

class Service16BitAdapter(context: Context, services: List<Service16Bit>, searchMode: SearchMode) : ArrayAdapter<Service16Bit>(context, 0, services) {
    private var serviceListFull: List<Service16Bit> = ArrayList<Service16Bit>(services)

    override fun getFilter(): Filter {
        return serviceFilter
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView?
                ?: LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_item_layout, parent, false) as TextView
        val serviceItem = getItem(position)
        view.text = serviceItem?.getFullName()

        return view
    }

    private val serviceFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions = ArrayList<Service16Bit>()

            if (constraint == null || constraint.isEmpty()) {
                suggestions.addAll(serviceListFull)
            } else {
                val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                for (item in serviceListFull) {
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
            addAll(results?.values as List<Service16Bit>)
            notifyDataSetChanged()
        }
    }
}