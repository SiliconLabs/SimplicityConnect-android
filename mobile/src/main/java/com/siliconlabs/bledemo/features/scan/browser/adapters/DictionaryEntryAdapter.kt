package com.siliconlabs.bledemo.features.scan.browser.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.features.scan.browser.activities.UuidDictionaryActivity
import com.siliconlabs.bledemo.features.scan.browser.adapters.DictionaryEntryAdapter.NameMappingViewHolder
import com.siliconlabs.bledemo.features.scan.browser.dialogs.DictionaryEntryEditDialog
import com.siliconlabs.bledemo.features.scan.browser.models.Mapping
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.AdapterDictionaryEntryBinding
import com.siliconlabs.bledemo.utils.RecyclerViewUtils

class DictionaryEntryAdapter(
        private val list: ArrayList<Mapping>,
        private val context: Context,
        private val type: Mapping.Type
) : RecyclerView.Adapter<NameMappingViewHolder>() {

    inner class NameMappingViewHolder(
            private val _binding: AdapterDictionaryEntryBinding
    ) : RecyclerView.ViewHolder(_binding.root) {

        fun bind(mapping: Mapping) {
            _binding.apply {
                textViewName.text = mapping.name
                textViewUuid.text = itemView.context.getString(R.string.dictionary_uuid, mapping.uuid)
            }
        }
    }

    override fun onBindViewHolder(holder: NameMappingViewHolder, position: Int) {
        val mapping = list[position]
        holder.bind(mapping)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameMappingViewHolder {
        val _binding = AdapterDictionaryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NameMappingViewHolder(_binding).also {
            setupUiListeners(it, _binding)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun setupUiListeners(holder: NameMappingViewHolder, _binding: AdapterDictionaryEntryBinding) {
        _binding.apply {
            imageViewEditCharacService.setOnClickListener {
                RecyclerViewUtils.withProperAdapterPosition(holder) { position ->
                    showEditDialog(this, position)
                }
            }
            imageButtonDelete.setOnClickListener {
                RecyclerViewUtils.withProperAdapterPosition(holder) { position ->
                    list.removeAt(position)
                    notifyItemRemoved(position)
                }
            }
        }
    }

    private fun showEditDialog(_binding: AdapterDictionaryEntryBinding, position: Int) {
        val mapping = list[position]
        DictionaryEntryEditDialog(
                mapping.name,
                mapping.uuid,
                type,
                object : MappingCallback {
            override fun onNameChanged(mapping: Mapping) {
                list[position] = mapping
                _binding.textViewName.text = mapping.name
            }
        }).show((context as UuidDictionaryActivity).supportFragmentManager, "dialog_mappings_edit")
    }

}
