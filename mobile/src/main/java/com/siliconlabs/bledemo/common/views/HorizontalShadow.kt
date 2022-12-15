package com.siliconlabs.bledemo.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.siliconlabs.bledemo.R

class HorizontalShadow(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_shadow, this)
    }
}