package com.siliconlabs.bledemo.motion.activities

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Bluetooth.Services.ThunderboardActivityCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.motion.adapters.GdxAdapter
import com.siliconlabs.bledemo.motion.model.MotionEvent
import com.siliconlabs.bledemo.motion.presenters.MotionListener
import com.siliconlabs.bledemo.motion.presenters.MotionPresenter
import com.siliconlabs.bledemo.thunderboard.model.NotificationEvent
import com.siliconlabs.bledemo.thunderboard.model.StatusEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import timber.log.Timber
import java.util.*

class MotionActivity : GdxActivity(), MotionListener {
    @BindView(R.id.car_animation)
    lateinit var carAnimationHolder: FrameLayout

    @BindView(R.id.orientation_x)
    lateinit var orientationX: TextView

    @BindView(R.id.orientation_y)
    lateinit var orientationY: TextView

    @BindView(R.id.orientation_z)
    lateinit var orientationZ: TextView

    @BindView(R.id.acceleration_x)
    lateinit var accelerationX: TextView

    @BindView(R.id.acceleration_y)
    lateinit var accelerationY: TextView

    @BindView(R.id.acceleration_z)
    lateinit var accelerationZ: TextView

    @BindView(R.id.calibrate)
    lateinit var calibrate: TextView

    private var calibratingDialog: AlertDialog? = null
    private var gdxAdapter: GdxAdapter? = null
    private var sceneLoaded = false
    private var bluetoothBinding: BluetoothService.Binding? = null
    private var bluetoothService: BluetoothService? = null
    var presenter: MotionPresenter? = null
    private var wasAlreadyPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(this)
                .inflate(R.layout.activity_motion, null, false)
        mainSection?.addView(view)
        prepareToolbar()
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        bindBluetoothService()
        presenter = MotionPresenter()
        ButterKnife.bind(this)
        setOrientation(0f, 0f, 0f)
        setAcceleration(0f, 0f, 0f)
        val modelType = intent.getStringExtra(SelectDeviceDialog.MODEL_TYPE_EXTRA)
        gdxAdapter = GdxAdapter(context.resources.getColor(R.color.silabs_white), modelType)
        initControls()

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

    private fun prepareToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { view: View? -> onBackPressed() }
    }

    private val bluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (BluetoothAdapter.STATE_OFF == state) finish()
            }
        }
    }

    private fun bindBluetoothService() {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                service?.let {
                    if (!it.isGattConnected()) finish()
                    else {
                        bluetoothService = it
                        presenter?.bluetoothService = it
                        bluetoothService?.thunderboardDevice = ThunderBoardDevice(it.connectedGatt?.device!!)
                        bluetoothService?.thunderboardCallback = thunderboardActivityCallback
                        it.registerGattCallback(motionGattCallback)
                        it.discoverGattServices()
                        presenter?.loadStatusFragment(fragmentManager)
                        presenter?.showConnectionState()
                    }

                }
            }
        }
        bluetoothBinding?.bind()
    }

    var thunderboardActivityCallback = ThunderboardActivityCallback {
        presenter!!.prepareViewListener(this@MotionActivity)
        wasAlreadyPrepared = true
    }

    override fun onResume() {
        super.onResume()
        if (wasAlreadyPrepared) {
            thunderboardActivityCallback.onPrepared()
        }
    }

    override fun onPause() {
        super.onPause()
        presenter?.let {
            it.clearViewListener()
            it.clearMotionNotifications()
            it.resetDeviceSubscriptions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        bluetoothService?.clearConnectedGatt()
        bluetoothBinding?.unbind()
        presenter?.clearViewListener()
    }

    override fun retrieveDemoPresenter() = presenter!!

    override fun setAcceleration(x: Float, y: Float, z: Float) {
        val accelerationString = getString(R.string.motion_acceleration_g)
        accelerationX.text = String.format(accelerationString, x)
        accelerationY.text = String.format(accelerationString, y)
        accelerationZ.text = String.format(accelerationString, z)
    }

    // Angles are measured in degrees ( -180 to 180)
    override fun setOrientation(x: Float, y: Float, z: Float) {
        val degreeString = getString(R.string.motion_orientation_degree)
        orientationX.text = String.format(degreeString, x)
        orientationY.text = String.format(degreeString, y)
        orientationZ.text = String.format(degreeString, z)
        if (gdxAdapter != null) {
            gdxAdapter!!.setOrientation(x, y, z)
        }
    }

    @OnClick(R.id.calibrate)
    fun onCalibrate() {
        popupCalibratingDialog()
        Handler().postDelayed({ presenter!!.calibrate() }, 600)
    }

    override fun onCalibrateCompleted() {
        closeCalibratingDialog()
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

    public override fun initControls() {
        setCalibrateVisible(false)
        gdxAdapter!!.setOnSceneLoadedListener(object : GdxAdapter.OnSceneLoadedListener {
            override fun onSceneLoaded() {
                sceneLoaded = true
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
        carAnimationHolder.addView(gdx3dView)
    }

    private fun onDeviceDisconnect() {
        if (!this.isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }

    val motionGattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) return

            bluetoothService?.let {
                it.thunderboardDevice?.isServicesDiscovered = true
                it.readRequiredCharacteristics()
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.d("onCharacteristicRead; characteristic = ${characteristic.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val device = bluetoothService?.thunderboardDevice
            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.DeviceName -> device?.name = characteristic.getStringValue(0)
                GattCharacteristic.ModelNumberString -> device?.modelNumber = characteristic.getStringValue(0)
                GattCharacteristic.BatteryLevel -> {
                    device?.let {
                        it.batteryLevel = characteristic.getIntValue(gattCharacteristic.format, 0)
                        it.isBatteryConfigured = true
                    }
                }
                GattCharacteristic.PowerSource -> {
                    device?.let {
                        val powerSource = ThunderBoardDevice.PowerSource.fromInt(
                                characteristic.getIntValue(gattCharacteristic.format, 0))
                        it.powerSource = powerSource
                        it.isPowerSourceConfigured = true
                    }
                    bluetoothService?.let {
                        it.selectedDeviceStatusMonitor.onNext(StatusEvent(device))
                        it.selectedDeviceMonitor.onNext(device)
                    }
                }
                GattCharacteristic.FirmwareRevision -> {
                    device?.firmwareVersion = characteristic.getStringValue(0)
                    bluetoothService?.let {
                        it.selectedDeviceStatusMonitor.onNext(StatusEvent(device))
                        it.selectedDeviceMonitor.onNext(device)
                    }
                }
                else -> { }
            }

            bluetoothService?.readRequiredCharacteristics()
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite; characteristic = ${characteristic.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            if (characteristic.uuid == GattCharacteristic.Calibration.uuid) {
                val action = when (characteristic.value[0]) {
                    0x01.toByte() -> MotionEvent.ACTION_CALIBRATE
                    0x02.toByte() -> MotionEvent.ACTION_CLEAR_ORIENTATION
                    else -> 0
                }
                bluetoothService?.motionDetector?.onNext(MotionEvent(
                        bluetoothService?.thunderboardDevice,
                        characteristic.uuid,
                        action))
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("onCharacteristicChanged; characteristic = ${characteristic.uuid}")
            Timber.d("Raw value = ${Arrays.toString(characteristic.value)}")

            val device = bluetoothService?.thunderboardDevice
            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.BatteryLevel -> {
                    device?.let {
                        it.batteryLevel = characteristic.getIntValue(gattCharacteristic.format, 0)
                        it.isBatteryConfigured = true
                    }
                    bluetoothService?.selectedDeviceStatusMonitor?.onNext(StatusEvent(device))
                }
                GattCharacteristic.PowerSource -> {
                    device?.let {
                        val powerSource = ThunderBoardDevice.PowerSource.fromInt(
                                characteristic.getIntValue(gattCharacteristic.format, 0))
                        it.powerSource = powerSource
                        it.isPowerSourceConfigured = true
                    }
                    bluetoothService?.selectedDeviceStatusMonitor?.onNext(StatusEvent(device))
                }
                GattCharacteristic.Acceleration -> {
                    val accelerationX = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val accelerationY = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val accelerationZ = characteristic.getIntValue(gattCharacteristic.format, 4)
                    Timber.d("Acceleration; X = $accelerationX, Y = $accelerationY, Z = $accelerationZ")

                    device?.sensorMotion?.setAcceleration(
                            accelerationX / 1000f,
                            accelerationY / 1000f,
                            accelerationZ / 1000f
                    )
                    bluetoothService?.motionDetector?.onNext(MotionEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.Orientation -> {
                    val orientationX = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val orientationY = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val orientationZ = characteristic.getIntValue(gattCharacteristic.format, 4)
                    Timber.d("Orientation; X = $orientationX, Y = $orientationY, Z = $orientationZ")

                    device?.sensorMotion?.setOrientation(
                            orientationX / 100f,
                            orientationY / 100f,
                            orientationZ / 100f
                    )
                    bluetoothService?.motionDetector?.onNext(MotionEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.Calibration -> {
                    val action = when (characteristic.value[0]) {
                        0x01.toByte() -> MotionEvent.ACTION_CALIBRATE
                        0x02.toByte() -> MotionEvent.ACTION_CLEAR_ORIENTATION
                        else -> 0
                    }
                    bluetoothService?.motionDetector?.onNext(MotionEvent(
                            device, gattCharacteristic.uuid, action
                    ))
                }
                else -> { }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor,
                                       status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Timber.d("onDescriptorWrite; descriptor = ${descriptor.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(descriptor.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val device = bluetoothService?.thunderboardDevice
            val characteristicUuid = descriptor.characteristic.uuid
            Timber.d("descriptor for characteristic: $characteristicUuid")

            when (characteristicUuid) {
                GattCharacteristic.BatteryLevel.uuid -> {
                    device?.isBatteryNotificationEnabled = true
                    bluetoothService?.let {
                        it.readRequiredCharacteristics()
                        it.selectedDeviceMonitor.onNext(device)
                    }
                }
                GattCharacteristic.PowerSource.uuid -> {
                    device?.isPowerSourceNotificationEnabled = true
                    bluetoothService?.let {
                        it.readRequiredCharacteristics()
                        it.selectedDeviceMonitor.onNext(device)
                    }
                }
                GattCharacteristic.Acceleration.uuid -> {
                    val enabled = descriptor.value[0] == 0x01.toByte()
                    val notificationAction =
                            if (enabled) NotificationEvent.ACTION_NOTIFICATIONS_SET
                            else NotificationEvent.ACTION_NOTIFICATIONS_CLEAR
                    device?.let {
                        it.isAccelerationNotificationEnabled = enabled
                        it.sensorMotion?.setAccelerationNotificationEnabled(enabled)
                    }
                    bluetoothService?.notificationsMonitor?.onNext(NotificationEvent(
                            device!!,
                            characteristicUuid,
                            notificationAction))
                }
                GattCharacteristic.Orientation.uuid -> {
                    val enabled = descriptor.value[0] == 0x01.toByte()
                    val notificationAction =
                            if (enabled) NotificationEvent.ACTION_NOTIFICATIONS_SET
                            else NotificationEvent.ACTION_NOTIFICATIONS_CLEAR

                    device?.let {
                        it.isOrientationNotificationEnabled = enabled
                        it.sensorMotion?.setOrientationNotificationEnabled(enabled)
                    }
                    bluetoothService?.notificationsMonitor?.onNext(NotificationEvent(
                            device!!,
                            characteristicUuid,
                            notificationAction))
                }
                GattCharacteristic.Calibration.uuid -> {
                    val enabled = descriptor.value[0] == 0x01.toByte()
                    val notificationAction =
                            if (enabled) NotificationEvent.ACTION_NOTIFICATIONS_SET
                            else NotificationEvent.ACTION_NOTIFICATIONS_CLEAR
                    device?.isCalibrateNotificationEnabled = true
                    bluetoothService?.notificationsMonitor?.onNext(NotificationEvent(
                            device!!,
                            characteristicUuid,
                            notificationAction))
                }
                else -> { }
            }
        }
    }
}