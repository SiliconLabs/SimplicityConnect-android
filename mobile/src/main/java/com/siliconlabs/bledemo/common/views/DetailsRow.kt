package com.siliconlabs.bledemo.common.views

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ListItemDetailsRowBinding

//import kotlinx.android.synthetic.main.list_item_details_row.view.*

class DetailsRow : LinearLayout {
    lateinit var binding: ListItemDetailsRowBinding

    init {
        // LayoutInflater.from(context).inflate(R.layout.list_item_details_row, this)
        binding = ListItemDetailsRowBinding.inflate(LayoutInflater.from(context))

    }

    constructor(context: Context) : super(context)
    constructor(context: Context, title: String?, text: String?) : super(context) {
//        tv_title.text = title
//        tv_details.text = text
        binding.tvDetails.text = text
        binding.tvTitle.text = title
    }
}