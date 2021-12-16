package com.siliconlabs.bledemo.environment.control

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.model.HallState

/**
 * Displays an icon and the Hall State value in a combo control.
 *
 *
 * The HallStateMeter and the TextView are added to the layout dynamically
 */
class HallStateControl(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : BaseControl(context, attrs, defStyleAttr) {

    private val resetTamperTextView: TextView

    fun setHallState(@HallState hallState: Int? = null) {
        var resetTamperVisible = GONE
        @StringRes var hallStateTextResId = R.string.environment_not_initialized
        @StyleRes var hallStateStyleResId = R.style.tb_robo_medium_18dp

        if (isEnabled && hallState != null) {
            when (hallState) {
                HallState.TAMPERED -> {
                    resetTamperVisible = VISIBLE
                    hallStateTextResId = R.string.environment_hall_state_tampered
                    hallStateStyleResId = R.style.EnvironmentControlLabel_HallStateTampered
                }
                HallState.CLOSED -> hallStateTextResId = R.string.environment_hall_state_closed
                HallState.OPENED -> hallStateTextResId = R.string.environment_hall_state_opened
                else -> hallStateTextResId = R.string.environment_hall_state_opened
            }
        }

        tileValue.text = context.getString(hallStateTextResId)
        tileValue.setTextAppearance(hallStateStyleResId)
        resetTamperTextView.visibility = resetTamperVisible
    }

    init {
        tileDescription.setText(R.string.environment_hall_state)
        tileIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_doorstate, null))
        resetTamperTextView = TextView(context).apply {
            setText(R.string.environment_hall_state_reset_tamper)
            setTextAppearance(R.style.EnvironmentControlLabel_HallStateTampered)
            visibility = GONE
        }

        findViewById<LinearLayout>(R.id.env_layout).addView(resetTamperTextView)
        findViewById<CardView>(R.id.cardview_env_tile).isEnabled = false

        setHallState()
        isEnabled = false

    }
}