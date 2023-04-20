package com.siliconlabs.bledemo.home_screen.views

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.siliconlabs.bledemo.R

class HidableBottomNavigationView: BottomNavigationView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private var hiding = false
    private val flyInAnimation = AnimationUtils.loadAnimation(context, R.anim.scanning_bar_fly_in)
    private val flyOutAnimation = AnimationUtils.loadAnimation(context, R.anim.scanning_bar_fly_out)

    fun show(instant: Boolean = false) {
        flyInAnimation.duration = 150
        if(visibility != VISIBLE){
            visibility = VISIBLE
            if(!instant) {
                startAnimation(flyInAnimation)
            }
        } else if (hiding) {
            flyOutAnimation.setAnimationListener(null)
            clearAnimation()
            hiding = false
        }
    }

    fun hide(instant: Boolean = false) {
        flyOutAnimation.duration = 150
        if (visibility == VISIBLE && !hiding){
            flyOutAnimation.setAnimationListener(null)
            clearAnimation()
            if(instant){
                visibility = GONE
                hiding = false
            } else {
                hiding = true
                startAnimation(flyOutAnimation)
                flyOutAnimation.setAnimationListener(flyOutListener)
            }
        }
    }

    private val flyOutListener = object : Animation.AnimationListener {
        override fun onAnimationStart(p0: Animation?) {}
        override fun onAnimationEnd(p0: Animation?) {
            visibility = GONE
            hiding = false
        }
        override fun onAnimationRepeat(p0: Animation?) {}
    }
}