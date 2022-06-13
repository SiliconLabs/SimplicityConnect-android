package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.model.HallState
import kotlinx.android.synthetic.main.environmentdemo_tile.view.*

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

    private val tileView: View = inflate(context, R.layout.environmentdemo_tile, this)
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

        tileView.env_value.text = context.getString(hallStateTextResId)
        tileView.env_value.setTextAppearance(hallStateStyleResId)
        resetTamperTextView.visibility = resetTamperVisible
    }

    init {
        resetTamperTextView = TextView(context).apply {
            setText(R.string.environment_hall_state_reset_tamper)
            setTextAppearance(R.style.EnvironmentControlLabel_HallStateTampered)
            visibility = GONE
        }
        env_layout.addView(resetTamperTextView)
        cardview_env_tile.isEnabled = false
    }
}