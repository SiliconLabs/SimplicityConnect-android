package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.glutils.ShaderProgram

/**
 * Created by james.ayvaz on 5/17/16.
 */
class EmissiveShader : DefaultShader {
    constructor(renderable: Renderable?) : super(renderable) {}
    constructor(renderable: Renderable?, config: Config?) : super(renderable, config) {}
    constructor(renderable: Renderable?, config: Config?, prefix: String?) : super(renderable,
            config, prefix) {
    }

    constructor(renderable: Renderable?, config: Config?, prefix: String?, vertexShader: String?,
                fragmentShader: String?) : super(renderable, config, prefix, vertexShader,
            fragmentShader) {
    }

    constructor(renderable: Renderable?, config: Config?, shaderProgram: ShaderProgram?) : super(
            renderable, config, shaderProgram) {
    }

    companion object {
        var defaultVertexShader: String? = null
            get() {
                if (field == null) {
                    field = Gdx.files.internal("data/default.vertex.glsl").readString()
                }
                return field
            }
            private set
        var defaultFragmentShader: String? = null
            get() {
                if (field == null) {
                    field = Gdx.files.internal("data/default.fragment.glsl").readString()
                }
                return field
            }
            private set
    }
}