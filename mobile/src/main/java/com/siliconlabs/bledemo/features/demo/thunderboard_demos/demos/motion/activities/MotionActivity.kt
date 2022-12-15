package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.activities

import android.app.AlertDialog
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.siliconlabs.bledemo.home_screen.dialogs.SelectDeviceDialog
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.viewmodels.MotionViewModel
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.adapters.GdxAdapter
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.utils.SensorChecker
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.utils.SensorChecker.ThunderboardSensor
import kotlinx.android.synthetic.main.activity_motion.*
import kotlinx.android.synthetic.main.motiondemo_acceleration.*
import kotlinx.android.synthetic.main.motiondemo_orientation.*
import timber.log.Timber

class MotionActivity : GdxActivity() {

    private var calibratingDialog: AlertDialog? = null
    private var gdxAdapter: GdxAdapter? = null

    private lateinit var viewModel: MotionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(this)
                .inflate(R.layout.activity_motion, null, false)
        mainSection?.addView(view)
        viewModel = ViewModelProvider(this).get(MotionViewModel::class.java)

        setupClickListeners()
        setupDataListeners()
        setupDataInitialValues()

        val modelType = intent.getStringExtra(SelectDeviceDialog.MODEL_TYPE_EXTRA)
        gdxAdapter = GdxAdapter(getColor(R.color.silabs_white), modelType)
        initializeAnimation()

        findViewById<View>(R.id.divider)?.let {
            it.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val parent = it.parent as View
                    it.minimumHeight = parent.height
                    it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (!setup) queueMotionNotificationsSetup()
    }

    override fun onPause() {
        super.onPause()
        clearMotionNotifications()
    }

    private fun clearMotionNotifications() {
        gattQueue.let {
            it.queueCancelNotifications(getMotionCharacteristic(GattCharacteristic.Calibration))
            it.queueCancelNotifications(getMotionCharacteristic(GattCharacteristic.Acceleration))
            it.queueCancelNotifications(getMotionCharacteristic(GattCharacteristic.Orientation))
        }
    }

    private fun setAcceleration(x: Float, y: Float, z: Float) {
        val accelerationString = getString(R.string.motion_acceleration_g)
        acceleration_x.text = String.format(accelerationString, x)
        acceleration_y.text = String.format(accelerationString, y)
        acceleration_z.text = String.format(accelerationString, z)
    }

    // Angles are measured in degrees (-180 to 180)
    private fun setOrientation(x: Float, y: Float, z: Float) {
        val degreeString = getString(R.string.motion_orientation_degree)
        orientation_x.text = String.format(degreeString, x)
        orientation_y.text = String.format(degreeString, y)
        orientation_z.text = String.format(degreeString, z)
        gdxAdapter?.setOrientation(x, y, z)
    }

    private fun setupClickListeners() {
        calibrate.setOnClickListener {
            popupCalibratingDialog()
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.calibrate(service?.connectedGatt)
            }, 600)
        }
    }

    private fun setupDataListeners() {
        viewModel.acceleration.observe(this, Observer {
            setAcceleration(it[0], it[1], it[2])
        })
        viewModel.orientation.observe(this, Observer {
            setOrientation(it[0], it[1], it[2])
        })
    }

    private fun setupDataInitialValues() {
        viewModel.acceleration.postValue(floatArrayOf(0f, 0f, 0f))
        viewModel.orientation.postValue(floatArrayOf(0f, 0f, 0f))
    }

    private fun popupCalibratingDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.motion_calibrating)
            setCancelable(false)
            setMessage(R.string.motion_calibrating_message)
            calibratingDialog = this.create()
        }
        calibratingDialog?.show()
    }

    private fun closeCalibratingDialog() {
        calibratingDialog?.dismiss()
    }

    private fun setCalibrateVisible(enabled: Boolean) {
        calibrate.visibility =
            if (enabled)  View.VISIBLE
            else View.INVISIBLE
    }

    private fun initializeAnimation() {
        setCalibrateVisible(false)
        gdxAdapter!!.setOnSceneLoadedListener(object : GdxAdapter.OnSceneLoadedListener {
            override fun onSceneLoaded() {
                runOnUiThread { setCalibrateVisible(true) }
            }
        })

        val config = AndroidApplicationConfiguration().apply {
            disableAudio = true
            hideStatusBar = false
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useWakelock = false
        }
        val gdx3dView = initializeForView(gdxAdapter, config)
        car_animation.addView(gdx3dView)
    }

    private fun onDeviceDisconnect() {
        if (!this.isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }

    private fun showBrokenSensorsMessage(brokenSensors: Set<ThunderboardSensor>) {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.sensor_malfunction_dialog_title))
            setMessage(getString(R.string.critical_sensor_malfunction_dialog_message,
                    TextUtils.join(", ", brokenSensors)))
            setPositiveButton(getString(R.string.button_ok)) { _, _ ->
                service?.connectedGatt?.disconnect() ?: onDeviceDisconnect()
            }
            setCancelable(false)
            runOnUiThread { show() }
        }
    }

    private fun queueMotionNotificationsSetup() {
        gattQueue.let {
            it.queueNotify(getMotionCharacteristic(GattCharacteristic.Acceleration))
            it.queueNotify(getMotionCharacteristic(GattCharacteristic.Orientation))
            it.queueIndicate(getMotionCharacteristic(GattCharacteristic.Calibration)) /* Somehow needed for proper writing to this characteristic. */
        }
    }

    private fun getMotionCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        return service?.connectedGatt?.getService(GattService.Motion.number)?.getCharacteristic(characteristic.uuid)
    }

    override val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) return

            queueReadingDeviceCharacteristics()
            queueMotionNotificationsSetup()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            gattQueue.handleCommandProcessed()
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.DeviceName,
                GattCharacteristic.ModelNumberString,
                GattCharacteristic.BatteryLevel,
                GattCharacteristic.PowerSource,
                GattCharacteristic.FirmwareRevision -> statusFragment.handleBaseCharacteristic(characteristic)
                else -> { }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status != BluetoothGatt.GATT_SUCCESS) return

            if (characteristic.uuid == GattCharacteristic.Calibration.uuid) {
                when (characteristic.value[0]) {
                    0x01.toByte() -> viewModel.resetOrientation(gatt, characteristic)
                    0x02.toByte() -> closeCalibratingDialog()
                    else ->  { }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.BatteryLevel,
                GattCharacteristic.PowerSource -> statusFragment.handleBaseCharacteristic(characteristic)

                GattCharacteristic.Acceleration -> {
                    val accelerationX = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val accelerationY = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val accelerationZ = characteristic.getIntValue(gattCharacteristic.format, 4)
                    Timber.d("Acceleration; X = $accelerationX, Y = $accelerationY, Z = $accelerationZ")

                    if (setup) {
                        sensorChecker.checkIfMotionSensorBroken(ThunderboardSensor.Acceleration,
                                accelerationX, accelerationY, accelerationZ)
                    }

                    viewModel.acceleration.postValue(floatArrayOf(
                            accelerationX / 1000f, accelerationY / 1000f, accelerationZ / 1000f))
                }
                GattCharacteristic.Orientation -> {
                    val orientationX = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val orientationY = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val orientationZ = characteristic.getIntValue(gattCharacteristic.format, 4)
                    Timber.d("Orientation; X = $orientationX, Y = $orientationY, Z = $orientationZ")

                    if (setup) {
                        sensorChecker.checkIfMotionSensorBroken(ThunderboardSensor.Orientation,
                                orientationX, orientationY, orientationZ)
                        val brokenSensors = sensorChecker.motionSensors.filter {
                            it.value == SensorChecker.SensorState.BROKEN
                        }.keys
                        setup = false
                        dismissModalDialog()

                        if (brokenSensors.isNotEmpty()) {
                            clearMotionNotifications()
                            showBrokenSensorsMessage(brokenSensors)
                        }
                    }

                    viewModel.orientation.postValue(floatArrayOf(
                            orientationX / 100f, orientationY / 100f, orientationZ / 100f))
                }
                GattCharacteristic.Calibration -> {
                    when (characteristic.value[1]) {
                        0x01.toByte() -> viewModel.resetOrientation(gatt, characteristic) /* Somehow needed for properly resetting char's value */
                        else ->  { }
                    }
                }
                else -> { }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor,
                                       status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            gattQueue.handleCommandProcessed()
        }
    }
}