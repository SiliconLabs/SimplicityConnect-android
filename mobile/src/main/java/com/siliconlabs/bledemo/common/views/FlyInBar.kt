package com.siliconlabs.bledemo.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FlyInBarBinding

class FlyInBar(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    private val _binding = FlyInBarBinding.inflate(LayoutInflater.from(context), this, true)
    private val gradientAnimation = AnimationUtils.loadAnimation(context, R.anim.connection_translate_right)
    private val flyInAnimation = AnimationUtils.loadAnimation(context, R.anim.scanning_bar_fly_in)
    private val flyOutAnimation = AnimationUtils.loadAnimation(context, R.anim.scanning_bar_fly_out)


    fun startFlyInAnimation(barLabel: String) {
        _binding.apply {
            connectingLabelTextview.text = barLabel
            translationAnimationContainer.startAnimation(gradientAnimation)
            root.startAnimation(flyInAnimation)
        }
    }

    fun startFlyOutAnimation(callback: Callback) {
        _binding.apply {
            translationAnimationContainer.clearAnimation()
            root.startAnimation(flyOutAnimation)
            flyOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {
                    callback.onFlyOutAnimationEnded()
                }
                override fun onAnimationRepeat(p0: Animation?) {}
            })
        }
    }

    fun clearBarAnimation() {
        _binding.apply {
            translationAnimationContainer.clearAnimation()
            root.clearAnimation()
        }
    }

    interface Callback {
        fun onFlyOutAnimationEnded()
    }
}