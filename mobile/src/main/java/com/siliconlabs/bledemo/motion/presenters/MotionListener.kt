package com.siliconlabs.bledemo.motion.presenters

import com.siliconlabs.bledemo.thunderboard.base.BaseViewListener

interface MotionListener : BaseViewListener {
    fun setOrientation(x: Float, y: Float, z: Float)
    fun setAcceleration(x: Float, y: Float, z: Float)
    fun onCalibrateCompleted()
}