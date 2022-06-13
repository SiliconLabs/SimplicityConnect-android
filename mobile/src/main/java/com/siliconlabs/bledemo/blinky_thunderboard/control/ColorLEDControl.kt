package com.siliconlabs.bledemo.blinky_thunderboard.control

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.SwitchCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.blinky_thunderboard.model.LedRGBState
import com.siliconlabs.bledemo.blinky_thunderboard.ui.ColorLEDs
import com.siliconlabs.bledemo.blinky_thunderboard.ui.HueBackgroundView
import kotlinx.android.synthetic.main.iodemo_color_leds.view.*

class ColorLEDControl @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var colorLEDs: ColorLEDs
    private lateinit var hueSelect: SeekBar
    private lateinit var brightnessSelect: SeekBar
    private lateinit var colorSwitch: SwitchCompat
    private lateinit var hueBackgroundView: HueBackgroundView

    private var hue: Float = 0f // from 0 to 360
    private var brightness: Float = 1f // from 0 to 1

    private var colorLEDControlListener: ColorLEDControlListener? = null

    private fun setupViews(rootView: LinearLayout) {
        colorLEDs = rootView.iodemo_color_leds
        hueSelect = rootView.iodemo_hue_select
        brightnessSelect = rootView.iodemo_brightness_select
        colorSwitch = rootView.iodemo_color_switch
        hueBackgroundView = rootView.iodemo_hue_background
    }

    private var selectBrightnessListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            brightness = findBrightness(progress)
            setColorLEDs(colorSwitch.isChecked, hue, brightness)
            colorLEDs.setAlpha(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            colorLEDControlListener?.onLedUpdateStop()
        }
    }

    fun setColorLEDControlListener(listener: ColorLEDControlListener?) {
        colorLEDControlListener = listener
    }

    fun setColorLEDsUI(colorLEDsValue: LedRGBState) {
        val isOn = colorLEDsValue.on
        colorSwitch.isChecked = isOn
        enableControls(isOn)
        val hsv = FloatArray(3)
        Color.RGBToHSV(
                colorLEDsValue.red,
                colorLEDsValue.green,
                colorLEDsValue.blue,
                hsv)
        hue = hsv[0]
        brightness = hsv[2]
        hueSelect.progress = hue.toInt()
        brightnessSelect.progress = findAlpha(brightness)
        colorLEDs.isEnabled = isOn
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        enableControls(enabled)
        colorSwitch.isEnabled = enabled
        hueBackgroundView.isEnabled = enabled
    }

    private fun enableControls(enable: Boolean) {
        hueSelect.isEnabled = enable
        brightnessSelect.isEnabled = enable
        colorLEDs.isEnabled = enable
    }

    private fun initHueSelectBackground() {
        val shape = ShapeDrawable(RectShape())
        shape.paint.color = 0x00000000 // make shape transparent
        hueSelect.progressDrawable = shape
    }

    private fun setColorLEDs(switchState: Boolean, hue: Float, brightness: Float) {
        val color = hsvToRGB(hue, brightness)
        colorLEDControlListener?.updateColorLEDs(LedRGBState(
                switchState,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        ))
    }

    private fun hsvToRGB(hue: Float, brightness: Float): Int {
        val hsv = FloatArray(3)
        hsv[0] = hue
        hsv[1] = 1.0f
        hsv[2] = brightness
        return Color.HSVToColor(hsv)
    }

    private fun findBrightness(alpha: Int): Float {
        return alpha.toFloat() / 255.0f
    }

    private fun findAlpha(brightness: Float): Int {
        return (brightness * 255f).toInt()
    }

    interface ColorLEDControlListener {
        fun updateColorLEDs(ledRGBState: LedRGBState)
        fun onLedUpdateStop()
    }

    init {
        val layout = LayoutInflater.from(context).inflate(
                R.layout.iodemo_color_leds, this, false) as LinearLayout
        setupViews(layout)
        addView(layout)

        val color = hsvToRGB(hue, brightness)

        colorLEDs.apply {
            setColor(color)
            setAlpha(0xff)
        }
        hueSelect.apply {
            max = 359
            progress = 0
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    hue = progress.toFloat()
                    setColorLEDs(colorSwitch.isChecked, hue, brightness)
                    colorLEDs.setColor(hsvToRGB(hue, 1f))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    colorLEDControlListener?.onLedUpdateStop()
                }
            })
        }
        initHueSelectBackground()
        brightnessSelect.apply {
            max = 0xff
            progress = 0xff
            setOnSeekBarChangeListener(selectBrightnessListener)
        }
        colorSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                enableControls(isChecked)
                setColorLEDs(isChecked, hue, brightness)
            }
            isChecked = false
        }
        enableControls(false)
    }
}