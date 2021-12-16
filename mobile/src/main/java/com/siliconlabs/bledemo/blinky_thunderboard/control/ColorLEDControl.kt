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
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.blinky_thunderboard.ui.ColorLEDs
import com.siliconlabs.bledemo.blinky_thunderboard.ui.HueBackgroundView
import com.siliconlabs.bledemo.thunderboard.model.LedRGBState

class ColorLEDControl @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @BindView(R.id.iodemo_color_leds)
    lateinit var colorLEDs: ColorLEDs

    @BindView(R.id.iodemo_hue_select)
    lateinit var hueSelect: SeekBar

    @BindView(R.id.iodemo_brightness_select)
    lateinit var brightnessSelect: SeekBar

    @BindView(R.id.iodemo_color_switch)
    lateinit var colorSwitch: SwitchCompat

    @BindView(R.id.iodemo_hue_background)
    lateinit var hueBackgroundView: HueBackgroundView

    private var hue: Float = 0f // from 0 to 360
    private var brightness: Float = 1f // from 0 to 1

    private var colorLEDControlListener: ColorLEDControlListener? = null

    var selectBrightnessListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            brightness = findBrightness(progress)
            setColorLEDs(colorSwitch.isChecked, hue, brightness)
            colorLEDs.setAlpha(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
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
        fun updateColorLEDs(ledRGBState: LedRGBState?)
    }

    init {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.iodemo_color_leds, this, false) as LinearLayout
        addView(layout)
        ButterKnife.bind(this, this)

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
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
        initHueSelectBackground()
        brightnessSelect.apply {
            max = 0xff
            progress = 0xff
            setOnSeekBarChangeListener(selectBrightnessListener)
        }
        colorSwitch.apply {
            setOnCheckedChangeListener { buttonView, isChecked ->
                enableControls(isChecked)
                setColorLEDs(isChecked, hue, brightness)
            }
            isChecked = false
        }
        enableControls(false)
    }
}