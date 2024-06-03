package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.adapters

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Matrix4
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.ui.EmissiveShader
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice
import java.util.*

class GdxAdapter(private val backgroundColor: Int, modelType: String?) :
        ApplicationAdapter() {
    private val instances: ArrayList<ModelInstance>
    private var modelType: ModelType? = null
    private var modelBatch: ModelBatch? = null
    private var environment: Environment? = null
    private var cam: Camera? = null
    private var camController: CameraInputController? = null
    private val swipeCamera = false
    var model: Model? = null
    private var x = 0f
    private var y = 0f
    private var z = 0f
    private val assets: AssetManager
    private var loading = false
    private var backgroundColorR = 0f
    private var backgroundColorG = 0f
    private var backgroundColorB = 0f
    private val initMatrix: Matrix4
    private var instance: ModelInstance? = null
    private var onSceneLoadedListener: OnSceneLoadedListener? = null
    private var ledColor = Color.CLEAR
    private val materialIds: List<String> = listOf(
            "thunderboardsense_lowpoly_007:lambert28sg",
            "thunderboardsense_lowpoly_007:lambert32sg",
            "lambert25sg",
            "lambert26sg"
    )

    private enum class ModelType {
        SENSE, BLUE
    }

    interface OnSceneLoadedListener {
        fun onSceneLoaded()
    }

    /**
     * create
     *
     *
     * Creates the resources for the 3D display: camera, background color and object
     */
    override fun create() {

        /* TODO The pinewood model is not lit correctly using the EmissiveShader - need to
            troubleshoot so we can use the same shader for all models */
        val config = DefaultShader.Config(
                EmissiveShader.defaultVertexShader,
                EmissiveShader.defaultFragmentShader)
        modelBatch = ModelBatch(EmissiveShaderProvider(config))
        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 1.0f, 1.0f, 1.0f, 1f))
        environment!!.add(DirectionalLight().set(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 10.0f))
        val fieldOfView = 1.75f
        cam = PerspectiveCamera(
                fieldOfView, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position[0f, 0f] = 175f
            lookAt(0f, 0f, 0f)
            near = 10f
            far = 300f
            update()
        }

        backgroundColorR = android.graphics.Color.red(backgroundColor) / 255f
        backgroundColorG = android.graphics.Color.green(backgroundColor) / 255f
        backgroundColorB = android.graphics.Color.blue(backgroundColor) / 255f

        // uncomment to enable touch control of 3dview
        //        swipeCamera = true;
        if (swipeCamera) {
            camController = CameraInputController(cam)
            Gdx.input.inputProcessor = camController
        } else {
            Gdx.input.inputProcessor = NullInputProcessor()
        }
        initModel()
    }

    private val modelFilename: String
        private get() = when (modelType) {
            ModelType.SENSE -> "data/TBSense_Rev_Lowpoly.g3dj"
            ModelType.BLUE -> "data/BRD4184A_LowPoly.g3dj"
            else -> "data/BRD4184A_LowPoly.g3dj"
        }

    private fun initOrientation() {
        initMatrix.setToRotation(1f, 0f, 0f, 90f)
        initMatrix.scale(0.4f, 0.4f, 0.4f)
    }

    fun initModel() {
        // initMatrix is our starting position, it has to compensate for any transforms
        // in the model file we load
        assets.load(modelFilename, Model::class.java)
        initOrientation()
        loading = true
    }

    var lightMaterials: MutableList<Material>? = null
    private fun doneLoading() {
        lightMaterials = ArrayList()
        model = assets.get(modelFilename, Model::class.java)
        instance = ModelInstance(model)
        // Example of adding parts to which will get toggled when the light comes on
        for (node in instance!!.nodes) {
            for (child in node.children) {
                for (part in child.parts) {
                    if (part.material.id != null && materialIds.contains(part.material.id
                                    .toLowerCase())) {
                        val material = part.material
                        lightMaterials?.add(material)
                    }
                }
            }
        }
        instances.add(instance!!)
        loading = false
        if (onSceneLoadedListener != null) {
            onSceneLoadedListener!!.onSceneLoaded()
        }
    }

    fun setOnSceneLoadedListener(onSceneLoadedListener: OnSceneLoadedListener?) {
        this.onSceneLoadedListener = onSceneLoadedListener
    }

    override fun render() {
        if (loading && assets.update()) {
            doneLoading()
        }
        if (camController != null) {
            camController!!.update()
        }
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(backgroundColorR, backgroundColorG, backgroundColorB, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        modelBatch!!.begin(cam)
        if (instance != null) {
            instance!!.transform.set(initMatrix)
            //            instance.transform.translate(-100, -2, 2);
            instance!!.transform.rotate(0f, 1f, 0f, z)
            instance!!.transform.rotate(0f, 0f, 1f, y)
            instance!!.transform.rotate(1f, 0f, 0f, -x)
        }
        modelBatch!!.render(instances, environment)
        modelBatch!!.end()
    }

    override fun dispose() {
        modelBatch!!.dispose()
        if (model != null) {
            model!!.dispose()
        }
        instances.clear()
        instance = null
    }

    /**
     * setOrientation
     *
     *
     * Sets the 3D object's orientation around the x, y, and z axes.
     *
     *
     * Parameters are in degrees.
     *
     * @param x
     * @param y
     * @param z
     */
    fun setOrientation(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }


    // An input processor that doesn't do anything
    internal inner class NullInputProcessor : InputProcessor {
        override fun keyDown(i: Int): Boolean {
            return false
        }

        override fun keyUp(i: Int): Boolean {
            return false
        }

        override fun keyTyped(c: Char): Boolean {
            return false
        }

        override fun touchDown(i: Int, i1: Int, i2: Int, i3: Int): Boolean {
            return false
        }

        override fun touchUp(i: Int, i1: Int, i2: Int, i3: Int): Boolean {
            return false
        }

        override fun touchDragged(i: Int, i1: Int, i2: Int): Boolean {
            return false
        }

        override fun mouseMoved(i: Int, i1: Int): Boolean {
            return false
        }

        override fun scrolled(i: Int): Boolean {
            return false
        }
    }

    internal inner class EmissiveShaderProvider @JvmOverloads constructor(
            config: DefaultShader.Config? = null as DefaultShader.Config?) : BaseShaderProvider() {
        val config: DefaultShader.Config

        constructor(vertexShader: String?, fragmentShader: String?) : this(
                DefaultShader.Config(vertexShader, fragmentShader)) {
        }

        constructor(vertexShader: FileHandle, fragmentShader: FileHandle) : this(
                vertexShader.readString(), fragmentShader.readString()) {
        }

        override fun createShader(renderable: Renderable): Shader {
            return EmissiveShader(renderable, config)
        }

        init {
            this.config = config ?: DefaultShader.Config()
        }
    }

    init {
        initMatrix = Matrix4()
        assets = AssetManager()
        instances = ArrayList()
        when (modelType) {
            ThunderBoardDevice.THUNDERBOARD_MODEL_BLUE_V1,
            ThunderBoardDevice.THUNDERBOARD_MODEL_BLUE_V2 -> this.modelType = ModelType.BLUE
            ThunderBoardDevice.THUNDERBOARD_MODEL_SENSE,
            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V1,
            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V2,
            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V3 -> this.modelType = ModelType.SENSE
            else -> this.modelType = ModelType.SENSE
        }
    }
}