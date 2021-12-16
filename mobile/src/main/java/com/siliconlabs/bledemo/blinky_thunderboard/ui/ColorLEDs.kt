package com.siliconlabs.bledemo.blinky_thunderboard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.R

class ColorLEDs @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val brush: Paint
    private var ledWidth = 0f
    private var ledSeparation = 0f
    private val ledHeight: Float
    private val greyColor: Int
    private var color = 0
    private var alpha = 0xff


    override fun onWindowFocusChanged(focus: Boolean) {
        super.onWindowFocusChanged(focus)
        val viewWidth = width
        ledWidth = (viewWidth / 6).toFloat()
        ledSeparation = (viewWidth - ledWidth * 5) / 4
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawColor = if (isEnabled) color else greyColor

        for (i in 0..4) {
            ContextCompat.getDrawable(context, R.drawable.led_oval)?.let {
                it.setColorFilter(drawColor, PorterDuff.Mode.MULTIPLY)
                it.setBounds(
                        (i * (ledWidth + ledSeparation)).toInt(), 0,
                        (ledWidth + i * (ledWidth + ledSeparation)).toInt(), ledHeight.toInt())
                it.draw(canvas)
            }

        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        brush.color =
                if (enabled) color
                else greyColor
        invalidate()
    }

    fun setColor(@ColorInt c: Int) {
        color = c
        brush.color = color
        applyAlpha()
    }

    fun setAlpha(alpha: Int) {
        this.alpha = alpha
        applyAlpha()
    }

    private fun applyAlpha() {
        color = color and 0x00ffffff or (alpha and 0xff shl 24)
        if (isEnabled) {
            brush.color = color
        } else {
            brush.color = greyColor
        }
        invalidate()
    }

    init {
        val res = context.resources
        ledHeight = res.getDimension(R.dimen.color_led_height)
        greyColor = res.getColor(R.color.sl_light_grey)
        brush = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
            color = greyColor // default is grey
        }
    }
}