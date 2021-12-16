package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.siliconlabs.bledemo.R

abstract class BaseControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tileView = inflate(context, R.layout.environmentdemo_tile, this)
    protected val tileDescription: TextView = tileView.findViewById(R.id.env_description)
    protected val tileValue: TextView = tileView.findViewById(R.id.env_value)
    protected val tileIcon: ImageView = tileView.findViewById(R.id.env_icon)
}