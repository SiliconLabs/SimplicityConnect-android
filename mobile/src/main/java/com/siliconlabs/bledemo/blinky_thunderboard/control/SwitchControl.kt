package com.siliconlabs.bledemo.blinky_thunderboard.control

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.R

class SwitchControl @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @BindView(R.id.switch_image)
    lateinit var switchImage: ImageView

    @BindView(R.id.switch_text)
    lateinit var switchText: TextView

    fun setChecked(checked: Boolean) {
        if (checked) {
            switchImage.setImageResource(R.drawable.switch_status_on)
        } else {
            switchImage.setImageResource(R.drawable.switch_status_off)
        }
        switchText.text = context.getString(if (checked) R.string.blinky_tb_on else R.string.blinky_tb_off)
    }

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.switch_control, null, false)
        addView(view)
        ButterKnife.bind(this, view)
    }
}