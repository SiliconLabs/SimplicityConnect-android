package com.siliconlabs.bledemo.Application

import android.app.Application
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.BuildConfig
import com.siliconlabs.bledemo.thunderboard.injection.component.DaggerThunderBoardComponent
import com.siliconlabs.bledemo.thunderboard.injection.component.ThunderBoardComponent
import com.siliconlabs.bledemo.thunderboard.injection.module.ThunderBoardModule
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

@HiltAndroidApp
class SiliconLabsDemoApplication : Application() {
    companion object {
        lateinit var APP: SiliconLabsDemoApplication
    }

    private var component: ThunderBoardComponent? = null

    init {
        APP = this
    }

    override fun onCreate() {
        super.onCreate()
        Engine.init(this)

        component()?.inject(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    fun component(): ThunderBoardComponent? {
        if (component == null) {
            val module = ThunderBoardModule()
            component = DaggerThunderBoardComponent.builder().thunderBoardModule(module).build()
            module.setContext(this)
        }
        return component
    }

}