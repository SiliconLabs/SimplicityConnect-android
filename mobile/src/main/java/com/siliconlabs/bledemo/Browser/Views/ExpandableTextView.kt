package com.siliconlabs.bledemo.browser.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.siliconlabs.bledemo.R

class ExpandableTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0, defStyleRes: Int = 0) : LinearLayout(context, attrs, defStyle, defStyleRes) {
    private val tvText: TextView
    private var linesExpandLimit = 5

    init {
        LayoutInflater.from(context).inflate(R.layout.expandable_text_view, this, true)
        tvText = findViewById(R.id.tv_text)
        orientation = VERTICAL
    }

    fun setup(linesExpandLimit: Int) {
        this.linesExpandLimit = linesExpandLimit
    }

    fun setText(text: String) {
        tvText.text = text
        tvText.setOnClickListener {
            tvText.maxLines = if (tvText.maxLines == 1) linesExpandLimit else 1
        }
    }

    fun minimize() {
        tvText.maxLines = 1
    }

    fun hide() {
        visibility = View.GONE
    }

    fun show() {
        visibility = View.VISIBLE
        bringToFront()
    }

}
