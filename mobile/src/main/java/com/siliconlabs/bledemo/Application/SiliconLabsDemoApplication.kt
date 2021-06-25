package com.siliconlabs.bledemo.application

import android.app.Application
import com.siliconlabs.bledemo.BuildConfig
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree


@HiltAndroidApp
class SiliconLabsDemoApplication : Application() {
    companion object {
        lateinit var APP: SiliconLabsDemoApplication
    }

    override fun onCreate() {
        super.onCreate()
        Engine.init(this)

        // This will initialise Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    init {
        APP = this
    }
}
