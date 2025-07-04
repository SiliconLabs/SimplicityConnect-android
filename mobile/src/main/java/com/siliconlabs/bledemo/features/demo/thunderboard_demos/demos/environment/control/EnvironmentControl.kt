package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.control

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater

import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.cardview.widget.CardView
import androidx.gridlayout.widget.GridLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.EnvironmentdemoTileBinding
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.HallState
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.TemperatureScale

//import kotlinx.android.synthetic.main.environmentdemo_tile.view.*

open class EnvironmentControl(
    context: Context,
    description: String?,
    icon: Drawable?
) : LinearLayout(context, null, 0) {

    var tileView: EnvironmentdemoTileBinding? = null
    constructor(context: Context) : this(context, null, null)

    init {
     tileView = EnvironmentdemoTileBinding.inflate(LayoutInflater.from(context))
        val densityDpi = Resources.getSystem().displayMetrics.densityDpi
        val cardView: CardView = tileView!!.cardviewEnvTile


        if(densityDpi < 240){
            Log.e("DevKitSensorDisplay","1")
            //small
            // Desired width in dp
            val desiredWidthDp = 150

            // Convert dp to pixels
            val desiredWidthPx = (desiredWidthDp * Resources.getSystem().displayMetrics.density).toInt()

            // Get the current layout parameters
            val layoutParams = cardView.layoutParams as LinearLayout.LayoutParams

            // Set the new width
            layoutParams.width = desiredWidthPx

            // Apply the updated layout parameters
            cardView.layoutParams = layoutParams
        }
        else if(densityDpi in 240..319){
            Log.e("DevKitSensorDisplay","2")

            // Desired width in dp
            val desiredWidthDp = 125

            // Convert dp to pixels
            val desiredWidthPx = (desiredWidthDp * Resources.getSystem().displayMetrics.density).toInt()

            // Get the current layout parameters
            val layoutParams = cardView.layoutParams as LinearLayout.LayoutParams

            // Set the new width
            layoutParams.width = desiredWidthPx

            // Apply the updated layout parameters
            cardView.layoutParams = layoutParams

        }
        else if(densityDpi in 320 .. 479){
            // Desired width in dp
            Log.e("DevKitSensorDisplay","3")

            val desiredWidthDp = 115

            // Convert dp to pixels
            val desiredWidthPx = (desiredWidthDp * Resources.getSystem().displayMetrics.density).toInt()

            // Get the current layout parameters
            val layoutParams = cardView.layoutParams as LinearLayout.LayoutParams

            // Set the new width
            layoutParams.width = desiredWidthPx

            // Apply the updated layout parameters
            cardView.layoutParams = layoutParams
        }
        else{
            // Desired width in dp
            Log.e("DevKitSensorDisplay","4")

            val desiredWidthDp = 100

            // Convert dp to pixels
            val desiredWidthPx = (desiredWidthDp * Resources.getSystem().displayMetrics.density).toInt()

            // Get the current layout parameters
            val layoutParams = cardView.layoutParams as LinearLayout.LayoutParams

            // Set the new width
            layoutParams.width = desiredWidthPx

            // Apply the updated layout parameters
            cardView.layoutParams = layoutParams
        }
    }


    lateinit var resetTamperTextView: TextView

    fun setTemperature(temperature: Float, temperatureType: Int) {

        tileView?.envValue?.text = String.format(
            if (temperatureType == TemperatureScale.FAHRENHEIT) context.getString(R.string.environment_temp_f) else context.getString(
                R.string.environment_temp_c
            ),
            if (temperatureType == TemperatureScale.FAHRENHEIT) temperature * 1.8f + 32f else temperature
        )
    }

    fun setHumidity(humidity: Int) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_humidity_measure), humidity)
    }

    fun setUVIndex(uvIndex: Int) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_uv_unit), uvIndex)
    }

    fun setAmbientLight(ambientLight: Long) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_ambient_lx), ambientLight)
    }

    fun setSoundLevel(soundLevel: Int) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_sound_level_measure), soundLevel)
    }

    fun setPressure(pressure: Long) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_pressure_measure), pressure)
    }

    fun setCO2(co2Level: Int) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_co2_measure), co2Level)
    }

    fun setVOC(vocLevel: Int) {
        tileView?.envValue?.text =
            String.format(context.getString(R.string.environment_voc_measure), vocLevel)
    }

    fun setHallStrength(hallStrength: Int) {
        tileView?.envValue?.text =
            context.getString(R.string.environment_hall_strength_measure, hallStrength)
    }

    init {
        if(description!!.contains(context.getString(R.string.environment_hall_state))){
            resetTamperTextView = TextView(context).apply {
                setText(R.string.environment_hall_state_reset_tamper)
                setTextAppearance(R.style.EnvironmentControlLabel_HallStateTampered)
                visibility = GONE
            }
            tileView?.envLayout?.addView(resetTamperTextView)
            tileView?.cardviewEnvTile?.isEnabled = false
        }
        tileView?.apply {
            envDescription.text = description
            envValue.text = context.getString(R.string.environment_not_initialized)
            envIcon.setImageDrawable(icon)
        }
        layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
        }
    }

    fun setHallState(hallState: HallState) {
        var resetTamperVisible = GONE
        @StringRes var hallStateTextResId = R.string.environment_not_initialized
        @StyleRes var hallStateStyleResId = R.style.tb_robo_medium_18dp

        when (hallState) {
            HallState.TAMPERED -> {
                resetTamperVisible = VISIBLE
                hallStateTextResId = R.string.environment_hall_state_tampered
                hallStateStyleResId = R.style.EnvironmentControlLabel_HallStateTampered
            }

            HallState.CLOSED -> hallStateTextResId = R.string.environment_hall_state_closed
            HallState.OPENED -> hallStateTextResId = R.string.environment_hall_state_opened
        }

        tileView?.envValue?.text = context.getString(hallStateTextResId)
        tileView?.envValue?.setTextAppearance(hallStateStyleResId)
        resetTamperTextView.visibility = resetTamperVisible
    }
}