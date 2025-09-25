package com.siliconlabs.bledemo.application

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import com.siliconlabs.bledemo.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import com.siliconlabs.bledemo.utils.AppUtil

@HiltAndroidApp
class SiliconLabsDemoApplication : Application() {
    companion object {
        lateinit var APP: SiliconLabsDemoApplication
    }

    init {
        APP = this
    }

    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
        registerActivityLifecycle()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    private fun registerActivityLifecycle() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // For Android 14 and above,
                    // set edge-to-edge mode for all activities except SplashActivity and WifiThroughputActivity
                    if (activity::class.java.simpleName != "SplashActivity") {
                        AppUtil.setEdgeToEdge(activity.window,activity)
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}

        })
    }
}