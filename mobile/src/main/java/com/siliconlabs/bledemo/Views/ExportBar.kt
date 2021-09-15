package com.siliconlabs.bledemo.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.view_export_bar.view.*

class ExportBar(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_export_bar, this)
    }

    fun init(listener: Listener) {
        export_bar.setOnClickListener { listener.onExportClick() }
        cancel_bar.setOnClickListener { listener.onCancelClick() }
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    interface Listener {
        fun onExportClick()
        fun onCancelClick()
    }

}