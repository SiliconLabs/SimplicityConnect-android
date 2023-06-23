package com.siliconlabs.bledemo.common.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.siliconlabs.bledemo.R

class ExpandArrow(context: Context, attrs: AttributeSet? = null) : MaterialButton(context, attrs) {

    init {
        setIconResource(R.drawable.ic_arrow_down_on)
    }

    fun setState(shouldShowDetails: Boolean) {
        setIconResource(
            if (shouldShowDetails) R.drawable.ic_arrow_up_on
            else R.drawable.ic_arrow_down_on
        )
    }

}