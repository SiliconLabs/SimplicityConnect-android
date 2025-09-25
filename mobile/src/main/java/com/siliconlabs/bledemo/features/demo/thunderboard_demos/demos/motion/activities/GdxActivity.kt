package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.activities

import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.activities.ThunderboardActivity

/**
 * Lightweight host Activity base that embeds a LibGDX View using an internal AndroidApplication.
 * This avoids relying on internal / package-private backend APIs that changed in LibGDX 1.13.5.
 */
abstract class GdxActivity : ThunderboardActivity() {
    // Custom subclass to expose protected lifecycle methods through public wrappers
    private class EmbeddedAndroidApplication : AndroidApplication() {
        fun hostResume() { super.onResume() }
        fun hostPause() { super.onPause() }
        fun hostLowMemory() { super.onLowMemory() }
        fun hostTrimMemory(level: Int) { super.onTrimMemory(level) }
    }

    private var gdxApp: EmbeddedAndroidApplication? = null
    private var gdxView: View? = null

    /** Initialize (or reuse) the LibGDX view and return it for insertion in layout. */
    protected fun initGdx(listener: ApplicationListener, config: AndroidApplicationConfiguration, hideStatusBar: Boolean = false): View {
        if (gdxApp == null) {
            gdxApp = EmbeddedAndroidApplication()
        }
        if (gdxView == null) {
            val shouldHide = hideStatusBar || readHideStatusBarFlag(config)
            if (shouldHide) {
                try { requestWindowFeature(Window.FEATURE_NO_TITLE) } catch (_: Exception) {}
            }
            gdxView = gdxApp!!.initializeForView(listener, config)
        }
        return gdxView!!
    }

    private fun readHideStatusBarFlag(config: AndroidApplicationConfiguration): Boolean = try {
        val f = AndroidApplicationConfiguration::class.java.getField("hideStatusBar")
        f.getBoolean(config)
    } catch (_: Throwable) { false }

    protected fun gdxFrameLayoutParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    override fun onResume() {
        super.onResume()
        gdxApp?.hostResume()
    }

    override fun onPause() {
        gdxApp?.hostPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        gdxApp?.hostLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        gdxApp?.hostTrimMemory(level)
    }

    // If orientation/config changes are needed, override in subclass and forward as desired.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}