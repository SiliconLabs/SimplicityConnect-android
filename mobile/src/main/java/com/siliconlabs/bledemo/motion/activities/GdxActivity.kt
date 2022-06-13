package com.siliconlabs.bledemo.motion.activities

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Debug
import android.os.Handler
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import com.badlogic.gdx.*
import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.backends.android.*
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.siliconlabs.bledemo.thunderboard.base.ThunderboardActivity

abstract class GdxActivity : ThunderboardActivity(), AndroidApplicationBase {
    protected var graphics: AndroidGraphics? = null
    protected var _input: AndroidInput? = null
    protected var audio: AndroidAudio? = null
    protected var files: AndroidFiles? = null
    protected var net: AndroidNet? = null
    protected var clipboard: AndroidClipboard? = null
    protected var listener: ApplicationListener? = null
    var _handler: Handler? = null
    protected var firstResume = true
    protected val _runnables: Array<Runnable> = Array()
    protected val _executedRunnables: Array<Runnable> = Array()
    protected val _lifecycleListeners: SnapshotArray<LifecycleListener> = SnapshotArray(LifecycleListener::class.java)
    private val androidEventListeners: Array<AndroidEventListener> = Array()
    protected var _logLevel = 2
    protected var _applicationLogger: ApplicationLogger? = null
    protected var useImmersiveMode = false
    protected var hideStatusBar = false
    private var wasFocusChanged = -1
    private var isWaitingForAudio = false
    fun initialize(listener: ApplicationListener?) {
        val config = AndroidApplicationConfiguration()
        this.initialize(listener, config)
    }

    fun initialize(listener: ApplicationListener?, config: AndroidApplicationConfiguration) {
        init(listener, config, false)
    }

    fun initializeForView(listener: ApplicationListener?): View {
        val config = AndroidApplicationConfiguration()
        return this.initializeForView(listener, config)
    }

    fun initializeForView(listener: ApplicationListener?,
                          config: AndroidApplicationConfiguration): View {
        init(listener, config, true)
        return graphics!!.getView()
    }

    private fun init(listener: ApplicationListener?, config: AndroidApplicationConfiguration,
                     isForView: Boolean) {
        if (this.version < 8) {
            throw GdxRuntimeException("LibGDX requires Android API Level 8 or later.")
        } else {
            setApplicationLogger(AndroidApplicationLogger())
            graphics = AndroidGraphics(this, config,
                    (if (config.resolutionStrategy == null) FillResolutionStrategy() else config.resolutionStrategy) as ResolutionStrategy)
            _input = AndroidInputFactory.newAndroidInput(this, this, graphics!!.view, config)
            audio = AndroidAudio(this, config)
            this.filesDir
            files = AndroidFiles(this.assets, this.filesDir.absolutePath)
            net = AndroidNet(this)
            this.listener = listener
            _handler = Handler()
            useImmersiveMode = config.useImmersiveMode
            hideStatusBar = config.hideStatusBar
            clipboard = AndroidClipboard(this)
            addLifecycleListener(object : LifecycleListener {
                override fun resume() {}
                override fun pause() {
                    audio!!.pause()
                }

                override fun dispose() {
                    audio!!.dispose()
                }
            })
            Gdx.app = this
            Gdx.input = getInput()
            Gdx.audio = getAudio()
            Gdx.files = getFiles()
            Gdx.graphics = getGraphics()
            Gdx.net = getNet()
            if (!isForView) {
                try {
                    requestWindowFeature(1)
                } catch (var8: Exception) {
                    this.log("AndroidApplication",
                            "Content already displayed, cannot request FEATURE_NO_TITLE", var8)
                }
                this.window.setFlags(1024, 1024)
                this.window.clearFlags(2048)
                this.setContentView(graphics!!.getView(), createLayoutParams())
            }
            createWakeLock(config.useWakelock)
            hideStatusBar(hideStatusBar)
            useImmersiveMode(useImmersiveMode)
            if (useImmersiveMode && this.version >= 19) {
                try {
                    val vlistener = Class.forName(
                            "com.badlogic.gdx.backends.android.AndroidVisibilityListener")
                    val o = vlistener.newInstance()
                    val method = vlistener.getDeclaredMethod("createListener",
                            AndroidApplicationBase::class.java)
                    method.invoke(o, this)
                } catch (var7: Exception) {
                    this.log("AndroidApplication", "Failed to create AndroidVisibilityListener",
                            var7)
                }
            }
        }
    }

    protected fun createLayoutParams(): FrameLayout.LayoutParams {
        val layoutParams = FrameLayout.LayoutParams(-1, -1)
        layoutParams.gravity = 17
        return layoutParams
    }

    protected fun createWakeLock(use: Boolean) {
        if (use) {
            this.window.addFlags(128)
        }
    }

    protected fun hideStatusBar(hide: Boolean) {
        if (hide && this.version >= 11) {
            val rootView = this.window.decorView
            try {
                val m = View::class.java.getMethod("setSystemUiVisibility", Integer.TYPE)
                if (this.version <= 13) {
                    m.invoke(rootView, 0)
                }
                m.invoke(rootView, 1)
            } catch (var4: Exception) {
                this.log("AndroidApplication", "Can't hide status bar", var4)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        useImmersiveMode(useImmersiveMode)
        hideStatusBar(hideStatusBar)
        if (hasFocus) {
            wasFocusChanged = 1
            if (isWaitingForAudio) {
                audio!!.resume()
                isWaitingForAudio = false
            }
        } else {
            wasFocusChanged = 0
        }
    }

    @TargetApi(19)
    override fun useImmersiveMode(use: Boolean) {
        if (use && this.version >= 19) {
            val view = this.window.decorView
            try {
                val m = View::class.java.getMethod("setSystemUiVisibility", Integer.TYPE)
                val code = 5894
                m.invoke(view, Integer.valueOf(code))
            } catch (var5: Exception) {
                this.log("AndroidApplication", "Can't set immersive mode", var5)
            }
        }
    }

    override fun onPause() {
        val isContinuous = graphics!!.isContinuousRendering
        val isContinuousEnforced = AndroidGraphics.enforceContinuousRendering
        AndroidGraphics.enforceContinuousRendering = true
        graphics!!.isContinuousRendering = true
        graphics!!.pause()
        _input!!.onPause()
        if (this.isFinishing) {
            graphics!!.clearManagedCaches()
            graphics!!.destroy()
        }
        AndroidGraphics.enforceContinuousRendering = isContinuousEnforced
        graphics!!.isContinuousRendering = isContinuous
        graphics!!.onPauseGLSurfaceView()
        super.onPause()
    }

    override fun onResume() {
        Gdx.app = this
        Gdx.input = getInput()
        Gdx.audio = getAudio()
        Gdx.files = getFiles()
        Gdx.graphics = getGraphics()
        Gdx.net = getNet()
        _input!!.onResume()
        if (graphics != null) {
            graphics!!.onResumeGLSurfaceView()
        }
        if (!firstResume) {
            graphics!!.resume()
        } else {
            firstResume = false
        }
        isWaitingForAudio = true
        if (wasFocusChanged == 1 || wasFocusChanged == -1) {
            audio!!.resume()
            isWaitingForAudio = false
        }
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun getApplicationListener(): ApplicationListener {
        return listener!!
    }

    override fun getAudio(): Audio {
        return audio!!
    }

    override fun getFiles(): Files {
        return files!!
    }

    override fun getGraphics(): Graphics {
        return graphics!!
    }

    override fun getInput(): AndroidInput {
        return _input!!
    }

    override fun getNet(): Net {
        return net!!
    }

    override fun getType(): ApplicationType {
        return ApplicationType.Android
    }

    override fun getVersion(): Int {
        return VERSION.SDK_INT
    }

    override fun getJavaHeap(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    override fun getNativeHeap(): Long {
        return Debug.getNativeHeapAllocatedSize()
    }

    override fun getPreferences(name: String): Preferences {
        return AndroidPreferences(getSharedPreferences(name, 0))
    }

    override fun getClipboard(): Clipboard {
        return clipboard!!
    }

    override fun postRunnable(runnable: Runnable) {
        synchronized(_runnables) {
            _runnables.add(runnable)
            Gdx.graphics.requestRendering()
        }
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        var keyboardAvailable = false
        if (config.hardKeyboardHidden == 1) {
            keyboardAvailable = true
        }
        _input!!.keyboardAvailable = keyboardAvailable
    }

    override fun exit() {
        _handler!!.post { finish() }
    }

    override fun debug(tag: String, message: String) {
        if (_logLevel >= 3) {
            getApplicationLogger().debug(tag, message)
        }
    }

    override fun debug(tag: String, message: String, exception: Throwable) {
        if (_logLevel >= 3) {
            getApplicationLogger().debug(tag, message, exception)
        }
    }

    override fun log(tag: String, message: String) {
        if (_logLevel >= 2) {
            getApplicationLogger().log(tag, message)
        }
    }

    override fun log(tag: String, message: String, exception: Throwable) {
        if (_logLevel >= 2) {
            getApplicationLogger().log(tag, message, exception)
        }
    }

    override fun error(tag: String, message: String) {
        if (_logLevel >= 1) {
            getApplicationLogger().error(tag, message)
        }
    }

    override fun error(tag: String, message: String, exception: Throwable) {
        if (_logLevel >= 1) {
            getApplicationLogger().error(tag, message, exception)
        }
    }

    override fun setLogLevel(logLevel: Int) {
        this._logLevel = logLevel
    }

    override fun getLogLevel(): Int {
        return _logLevel
    }

    override fun setApplicationLogger(applicationLogger: ApplicationLogger) {
        this._applicationLogger = applicationLogger
    }

    override fun getApplicationLogger(): ApplicationLogger {
        return _applicationLogger!!
    }

    override fun addLifecycleListener(listener: LifecycleListener) {
        synchronized(_lifecycleListeners) { _lifecycleListeners.add(listener) }
    }

    override fun removeLifecycleListener(listener: LifecycleListener) {
        synchronized(_lifecycleListeners) { _lifecycleListeners.removeValue(listener, true) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        synchronized(androidEventListeners) {
            for (i in 0 until androidEventListeners.size) {
                androidEventListeners[i]!!.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    fun addAndroidEventListener(listener: AndroidEventListener?) {
        synchronized(androidEventListeners) { androidEventListeners.add(listener) }
    }

    fun removeAndroidEventListener(listener: AndroidEventListener?) {
        synchronized(androidEventListeners) { androidEventListeners.removeValue(listener, true) }
    }

    override fun getContext(): Context {
        return this
    }

    override fun getRunnables(): Array<Runnable> {
        return _runnables
    }

    override fun getExecutedRunnables(): Array<Runnable> {
        return _executedRunnables
    }

    override fun getLifecycleListeners(): SnapshotArray<LifecycleListener> {
        return _lifecycleListeners
    }

    override fun getApplicationWindow(): Window {
        return this.window
    }

    override fun getHandler(): Handler {
        return _handler!!
    }

    companion object {
        init {
            GdxNativesLoader.load()
        }
    }
}