package com.siliconlabs.bledemo.thunderboard.base

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.R

abstract class BaseActivity : BaseActivity() {
    protected var mainSection: FrameLayout? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thunderboard_base)
        mainSection = findViewById<View>(R.id.main_section) as FrameLayout
    }

    override fun onDestroy() {
        retrieveDemoPresenter().clearViewListener()
        super.onDestroy()
    }

    fun onDisconnected() {
        retrieveDemoPresenter().clearViewListener()
    }

    protected abstract fun initControls()
    protected abstract fun retrieveDemoPresenter(): BasePresenter
}