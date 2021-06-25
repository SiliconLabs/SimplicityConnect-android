package com.siliconlabs.bledemo.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.view_bottom_bar_with_button.view.*

class BottomBarWithButton(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_bottom_bar_with_button, this)
    }

    fun init(text: String, listener: Listener) {
        btn_bar.text = text
        btn_bar.setOnClickListener {
            listener.onClick()
        }
    }

    fun show() {
        rl_bottom_bar.visibility = View.VISIBLE
    }

    fun hide() {
        rl_bottom_bar.visibility = View.GONE
    }

    interface Listener {
        fun onClick()
    }

}