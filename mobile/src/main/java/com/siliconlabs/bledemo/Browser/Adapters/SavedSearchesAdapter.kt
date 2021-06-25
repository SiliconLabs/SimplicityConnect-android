package com.siliconlabs.bledemo.Browser.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Browser.Adapters.SavedSearchesAdapter.SavedSearchesViewHolder
import com.siliconlabs.bledemo.Browser.Models.SavedSearch
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.FilterDeviceParams
import com.siliconlabs.bledemo.Utils.SharedPrefUtils
import java.util.*

class SavedSearchesAdapter(private val savedSearchList: ArrayList<SavedSearch>, private val context: Context, private val savedSearchesCallback: SavedSearchesCallback) : RecyclerView.Adapter<SavedSearchesViewHolder>() {
    private val sharedPrefUtils = SharedPrefUtils(context)

    interface SavedSearchesCallback {
        fun onClick(name: String?)
    }

    inner class SavedSearchesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var tvSearchedText = itemView.findViewById(R.id.textview_saved_search) as TextView
        private var llSavedSearch = itemView.findViewById(R.id.saved_search) as LinearLayout
        private var btnRemove = itemView.findViewById(R.id.removeBtn) as ImageButton

        override fun onClick(v: View) {
            savedSearchesCallback.onClick(tvSearchedText.text.toString())
        }

        fun bind(savedSearch: SavedSearch) {
            tvSearchedText.text = savedSearch.searchText
            val lastFilterDeviceParams = sharedPrefUtils.lastFilter
            if (lastFilterDeviceParams != null && lastFilterDeviceParams.filterName.toLowerCase(Locale.getDefault()) == savedSearch.searchText.toLowerCase(Locale.getDefault())) {
                tvSearchedText.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue_selected))
            } else {
                tvSearchedText.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text))
            }

            llSavedSearch.setOnClickListener(this)
            btnRemove.setOnClickListener { removeAt(adapterPosition) }
        }
    }

    fun removeAt(position: Int) {
        if (position < 0) return
        val map: HashMap<String, FilterDeviceParams?> = sharedPrefUtils.mapFilter
        map.remove(savedSearchList[position].searchText)
        sharedPrefUtils.updateMapFilter(map)
        savedSearchList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, savedSearchList.size)
    }

    override fun onBindViewHolder(holder: SavedSearchesViewHolder, position: Int) {
        val savedSearch = savedSearchList[position]
        holder.bind(savedSearch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedSearchesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_saved_searches, parent, false)
        return SavedSearchesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return savedSearchList.size
    }

    fun addItem(savedSearch: SavedSearch) {
        savedSearchList.add(0, savedSearch)
        notifyDataSetChanged()
    }

}