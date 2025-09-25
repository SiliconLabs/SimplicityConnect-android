package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.adapters.GdxAdapter

/**
 * LibGDX fragment host using AndroidFragmentApplication for LibGDX 1.13.5.
 * Avoids manual instantiation of AndroidApplication (which requires framework lifecycle).
 */
class MotionGdxFragment : AndroidFragmentApplication() {

    private var backgroundColor: Int = 0xFFFFFFFF.toInt()
    private var modelType: String? = null
    private var adapter: GdxAdapter? = null
    private var pendingOrientation: FloatArray? = null
    private var sceneListener: OnSceneLoadedListener? = null

    interface OnSceneLoadedListener { fun onSceneLoaded() }

    companion object {
        private const val ARG_COLOR = "arg_color"
        private const val ARG_MODEL = "arg_model"
        fun newInstance(color: Int, modelType: String?): MotionGdxFragment = MotionGdxFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_COLOR, color)
                putString(ARG_MODEL, modelType)
            }
        }
    }

    fun setOnSceneLoadedListener(l: OnSceneLoadedListener) { sceneListener = l }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            backgroundColor = it.getInt(ARG_COLOR)
            modelType = it.getString(ARG_MODEL)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            disableAudio = true
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useWakelock = false
        }
        adapter = GdxAdapter(backgroundColor, modelType).also { gdx ->
            gdx.setOnSceneLoadedListener(object : GdxAdapter.OnSceneLoadedListener {
                override fun onSceneLoaded() {
                    pendingOrientation?.let { (x,y,z) -> gdx.setOrientation(x,y,z) }
                    sceneListener?.onSceneLoaded()
                }
            })
        }
        return initializeForView(adapter, config)
    }

    fun setOrientation(x: Float, y: Float, z: Float) {
        val gdx = adapter
        if (gdx == null) {
            pendingOrientation = floatArrayOf(x,y,z)
        } else {
            gdx.setOrientation(x,y,z)
        }
    }
}

