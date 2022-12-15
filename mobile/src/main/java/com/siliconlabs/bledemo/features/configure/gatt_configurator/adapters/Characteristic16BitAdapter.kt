package com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Characteristic16Bit
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.SearchMode
import java.util.*
import kotlin.collections.ArrayList

class Characteristic16BitAdapter(context: Context, characteristics: List<Characteristic16Bit>, searchMode: SearchMode) : ArrayAdapter<Characteristic16Bit>(context, 0, characteristics) {
    private var characteristicListFull: List<Characteristic16Bit> = ArrayList<Characteristic16Bit>(characteristics)

    override fun getFilter(): Filter {
        return characteristicFilter
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView?
                ?: LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_item_layout, parent, false) as TextView
        val characteristicItem = getItem(position)
        view.text = characteristicItem?.getFullName()

        return view
    }

    private val characteristicFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions = ArrayList<Characteristic16Bit>()

            if (constraint == null || constraint.isEmpty()) {
                suggestions.addAll(characteristicListFull)
            } else {
                val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                for (item in characteristicListFull) {
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
            addAll(results?.values as List<Characteristic16Bit>)
            notifyDataSetChanged()
        }
    }
}

