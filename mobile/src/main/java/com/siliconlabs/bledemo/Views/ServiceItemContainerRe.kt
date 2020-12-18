package com.siliconlabs.bledemo.Views

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.list_item_debug_mode_service_re.view.*

class ServiceItemContainerRe : LinearLayout {

    init {
        LayoutInflater.from(context).inflate(R.layout.list_item_debug_mode_service_re, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, title: String?, text: String?) : super(context) {
        service_title.text = title
        sevice_uuid.text = text
    }
}