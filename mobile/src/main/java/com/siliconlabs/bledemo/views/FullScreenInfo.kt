package com.siliconlabs.bledemo.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.full_screen_info.view.*

class FullScreenInfo @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0, defStyleRes: Int = 0) : LinearLayout(context, attrs, defStyle, defStyleRes) {
    init {
        LayoutInflater.from(context).inflate(R.layout.full_screen_info, this, true)
        orientation = VERTICAL
    }

    fun initialize(imgRes: Int, info: String) {
        iv_image?.setImageResource(imgRes)
        tv_info?.text = info
    }

    fun show() {
        ll_full_screen_info?.visibility = View.VISIBLE
    }

    fun hide() {
        ll_full_screen_info.visibility = View.INVISIBLE
    }

}

