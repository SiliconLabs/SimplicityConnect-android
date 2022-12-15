package com.siliconlabs.bledemo.common.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.siliconlabs.bledemo.R

class MainActionButton(context: Context, attrs: AttributeSet) : ExtendedFloatingActionButton(context, attrs) {

    companion object {
        private val STATE_IS_ACTION_ON = intArrayOf(R.attr.is_action_on)
    }
    private var isActionOn = false

    fun setIsActionOn(isActionOn: Boolean) {
        this.isActionOn = isActionOn
        refreshDrawableState()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isActionOn) {
            mergeDrawableStates(drawableState, STATE_IS_ACTION_ON)
        }
        return drawableState
    }
}