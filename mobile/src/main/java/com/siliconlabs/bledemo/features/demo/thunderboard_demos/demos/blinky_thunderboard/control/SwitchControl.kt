package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.blinky_thunderboard.control

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.SwitchControlBinding

//import kotlinx.android.synthetic.main.switch_control.view.*

class SwitchControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: SwitchControlBinding

    fun setChecked(checked: Boolean) {
        binding.switchImage.setImageResource(
            if (checked) R.drawable.switch_status_on
            else R.drawable.switch_status_off
        )
        binding.switchText.text =
            context.getString(if (checked) R.string.blinky_tb_on else R.string.blinky_tb_off)
    }

    init {
        val inflater = LayoutInflater.from(context)
        binding = SwitchControlBinding.inflate(inflater, null, false)
        addView(binding.root)
    }
}