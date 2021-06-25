package com.siliconlabs.bledemo.Advertiser.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.siliconlabs.bledemo.Advertiser.Models.Service16Bit
import com.siliconlabs.bledemo.R
import java.util.*
import kotlin.collections.ArrayList

class Service16BitAdapter(context: Context, services: List<Service16Bit>) : ArrayAdapter<Service16Bit>(context, 0, services) {
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
                val filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim()
                for (item in serviceListFull) {
                    if (item.name.toLowerCase(Locale.getDefault()).contains(filterPattern) || item.identifier.toString(16).startsWith(filterPattern)) {
                        suggestions.add(item)
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

        override fun convertResultToString(resultValue: Any?): CharSequence {
            val result = resultValue as Service16Bit
            return result.getFullName()
        }
    }
}