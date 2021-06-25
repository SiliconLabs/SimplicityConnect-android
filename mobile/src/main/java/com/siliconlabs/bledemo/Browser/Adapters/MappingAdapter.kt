package com.siliconlabs.bledemo.browser.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.browser.activities.MappingDictionaryActivity
import com.siliconlabs.bledemo.browser.adapters.MappingAdapter.NameMappingViewHolder
import com.siliconlabs.bledemo.browser.dialogs.MappingsEditDialog
import com.siliconlabs.bledemo.browser.MappingCallback
import com.siliconlabs.bledemo.browser.models.Mapping
import com.siliconlabs.bledemo.browser.models.MappingType
import com.siliconlabs.bledemo.R

class MappingAdapter(private val list: ArrayList<Mapping>, private val context: Context, private val type: MappingType) : RecyclerView.Adapter<NameMappingViewHolder>() {

    inner class NameMappingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var tvUuid = itemView.findViewById(R.id.text_view_uuid) as TextView
        private var tvName = itemView.findViewById(R.id.text_view_name) as TextView
        private var ibDelete = itemView.findViewById(R.id.image_button_delete) as ImageButton
        private var llCharacServiceTitle = itemView.findViewById(R.id.linear_layout_charac_service) as LinearLayout
        private var cvMapping = itemView.findViewById(R.id.mapping_card_view) as CardView

        override fun onClick(v: View) {
            val position = adapterPosition
            val mapping = list[position]
            val dialog: DialogFragment = MappingsEditDialog(mapping.name, mapping.uuid, object : MappingCallback {
                override fun onNameChanged(mapping: Mapping) {
                    list[position] = mapping
                    tvName.text = mapping.name
                }
            }, type)
            dialog.show((context as MappingDictionaryActivity).supportFragmentManager, "dialog_mappings_edit")
        }

        fun bind(mapping: Mapping) {
            val margin16Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
            val margin10Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics).toInt()
            val layoutParams = cvMapping.layoutParams as ViewGroup.MarginLayoutParams

            when (adapterPosition) {
                0 -> layoutParams.setMargins(margin16Dp, margin16Dp, margin16Dp, margin10Dp)
                itemCount - 1 -> layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin16Dp)
                else -> layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin10Dp)
            }

            cvMapping.requestLayout()
            tvUuid.text = mapping.uuid
            tvName.text = mapping.name
            ibDelete.setOnClickListener {
                list.removeAt(adapterPosition)
                notifyDataSetChanged()
            }

            llCharacServiceTitle.setOnClickListener(this)
        }
    }

    override fun onBindViewHolder(holder: NameMappingViewHolder, position: Int) {
        val mapping = list[position]
        holder.bind(mapping)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NameMappingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_mappings, parent, false)
        return NameMappingViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

}
