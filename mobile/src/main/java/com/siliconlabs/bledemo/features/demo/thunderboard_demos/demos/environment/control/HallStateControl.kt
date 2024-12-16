package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.EnvironmentdemoTileBinding
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.HallState

//import kotlinx.android.synthetic.main.environmentdemo_tile.view.*

/**
 * Displays an icon and the Hall State value in a combo control.
 *
 *
 * The HallStateMeter and the TextView are added to the layout dynamically
 */
class HallStateControl(
    context: Context,
    description: String?,
    icon: Drawable?
) : EnvironmentControl(context, description, icon) {

    constructor(context: Context) : this(context, null, null)

    /*val tileViewHallState = EnvironmentdemoTileBinding.inflate(LayoutInflater.from(context))
    private val resetTamperTextView: TextView

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

        tileViewHallState.envValue.text = context.getString(hallStateTextResId)
        tileViewHallState.envValue.setTextAppearance(hallStateStyleResId)
        resetTamperTextView.visibility = resetTamperVisible
    }

    init {
        resetTamperTextView = TextView(context).apply {
            setText(R.string.environment_hall_state_reset_tamper)
            setTextAppearance(R.style.EnvironmentControlLabel_HallStateTampered)
            visibility = GONE
        }
        tileViewHallState.envLayout.addView(resetTamperTextView)
        tileViewHallState.cardviewEnvTile.isEnabled = false
    }*/
}