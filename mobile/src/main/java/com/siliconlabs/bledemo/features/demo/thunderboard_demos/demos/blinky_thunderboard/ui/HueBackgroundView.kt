package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.blinky_thunderboard.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.siliconlabs.bledemo.R

class HueBackgroundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val brush: Paint
    private val hsvColor: FloatArray = FloatArray(3)
    private val spectrumColors: IntArray = IntArray(360)
    private val lineHeight: Float = context.resources.getDimension(R.dimen.color_hue_selection_line_height)

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (isEnabled) {
            val grad = LinearGradient(height / 2, 0.0f,
                    width - height / 2, 0.0f,
                    spectrumColors, null, Shader.TileMode.CLAMP)
            brush.shader = grad
            canvas.drawRect(
                    height / 2, (height - lineHeight) / 2,
                    width - height / 2, (height + lineHeight) / 2,
                    brush)
        }
    }

    private fun initSpectrumColors() {
        for (i in 0..359) {
            hsvColor[0] = i.toFloat()
            hsvColor[1] = 1.0f
            hsvColor[2] = 1.0f
            spectrumColors[i] = Color.HSVToColor(hsvColor)
        }
    }

    init {
        initSpectrumColors()
        brush = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
        }
    }
}