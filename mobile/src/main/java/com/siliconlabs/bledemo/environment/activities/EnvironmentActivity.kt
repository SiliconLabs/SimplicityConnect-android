package com.siliconlabs.bledemo.environment.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.gridlayout.widget.GridLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Bluetooth.Services.ThunderboardActivityCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.environment.control.*
import com.siliconlabs.bledemo.environment.model.EnvironmentEvent
import com.siliconlabs.bledemo.environment.model.HallState
import com.siliconlabs.bledemo.environment.presenters.EnvironmentListener
import com.siliconlabs.bledemo.environment.presenters.EnvironmentPresenter
import com.siliconlabs.bledemo.thunderboard.base.BaseActivity
import com.siliconlabs.bledemo.thunderboard.model.NotificationEvent
import com.siliconlabs.bledemo.thunderboard.model.StatusEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.utils.BleUtils
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class EnvironmentActivity : BaseActivity(), EnvironmentListener {
    private var temperatureControl: TemperatureControl? = null
    private var humidityControl: HumidityControl? = null
    private var ambientLightControl: AmbientLightControl? = null
    private var uvIndexControl: UVControl? = null
    private var pressureControl: PressureControl? = null
    private var soundLevelControl: SoundLevelControl? = null
    private var co2Control: CO2Control? = null
    private var vocControl: VOCControl? = null
    private var hallStrengthControl: HallStrengthControl? = null
    private var hallStateControl: HallStateControl? = null



    @BindView(R.id.env_grid)
    lateinit var envGrid: GridLayout
    private var powerSource = ThunderBoardDevice.PowerSource.UNKNOWN

    @Inject
    lateinit var presenter: EnvironmentPresenter

    private var bluetoothBinding: BluetoothService.Binding? = null
    private var bluetoothService: BluetoothService? = null
    private var isAlreadyPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater
                .from(this).inflate(R.layout.activity_environment, null, false)
        mainSection?.addView(view)
        prepareToolbar()
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        bindBluetoothService()
        ButterKnife.bind(this)
        getDaggerComponent().inject(this)
        setupEnvList()
        initControls()
    }

    private fun bindBluetoothService() {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                if (service != null) {
                    if (!service.isGattConnected()) finish() else {
                        bluetoothService = service
                        presenter.bluetoothService = service
                        bluetoothService?.thunderboardDevice = ThunderBoardDevice(service.connectedGatt?.device!!)
                        bluetoothService?.thunderboardCallback = thunderboardActivityCallback
                        service.registerGattCallback(environmentGattCallback)
                        service.discoverGattServices()
                        presenter.loadStatusFragment(fragmentManager)
                        presenter.showConnectionState()
                    }
                }
            }
        }
        bluetoothBinding?.bind()
    }

    var thunderboardActivityCallback = ThunderboardActivityCallback {
        if (!isAlreadyPrepared) {
            runOnUiThread { presenter.prepareViewListener(this@EnvironmentActivity) }
            isAlreadyPrepared = true
        }
    }

    public override fun onResume() {
        super.onResume()
        presenter.checkSettings()
        if (isAlreadyPrepared) {
            isAlreadyPrepared = false
            presenter.notificationsHaveBeenSet = false
            thunderboardActivityCallback.onPrepared()
        }
    }

    override fun onPause() {
        super.onPause()
        presenter.clearViewListener()
        presenter.clearEnvironmentNotifications()
        presenter.clearHallStateNotifications()
        presenter.resetDeviceSubscriptions()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        bluetoothService?.clearConnectedGatt()
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

    private fun setupEnvList() {
        temperatureControl = TemperatureControl(this)
        humidityControl = HumidityControl(this)
        ambientLightControl = AmbientLightControl(this)
        uvIndexControl = UVControl(this)
        pressureControl = PressureControl(this)
        soundLevelControl = SoundLevelControl(this)
        co2Control = CO2Control(this)
        vocControl = VOCControl(this)
        hallStrengthControl = HallStrengthControl(this)
        hallStateControl = HallStateControl(this)

        temperatureControl?.layoutParams = layoutParams
        soundLevelControl?.layoutParams = layoutParams
        ambientLightControl?.layoutParams = layoutParams
        uvIndexControl?.layoutParams = layoutParams
        humidityControl?.layoutParams = layoutParams
        pressureControl?.layoutParams = layoutParams
        co2Control?.layoutParams = layoutParams
        vocControl?.layoutParams = layoutParams
        hallStrengthControl?.layoutParams = layoutParams
        hallStateControl?.layoutParams = layoutParams
        hallStateControl?.setOnClickListener { presenter.onHallStateClick() }
    }

    private val layoutParams: GridLayout.LayoutParams
        get() {
            val layoutParams: GridLayout.LayoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f))
            layoutParams.width = 0
            return layoutParams
        }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        bluetoothBinding?.unbind()
        presenter.clearViewListener()
    }

    private fun prepareToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_demo, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun retrieveDemoPresenter() = presenter

    override fun setTemperature(temperature: Float, temperatureType: Int) {
        if (temperatureControl?.isEnabled == true) {
            temperatureControl?.setTemperature(temperature, temperatureType)
        }
    }

    override fun setHumidity(humidity: Int) {
        if (humidityControl?.isEnabled == true) {
            humidityControl?.setHumidity(humidity)
        }
    }

    override fun setUvIndex(uvIndex: Int) {
        if (uvIndexControl?.isEnabled == true) {
            uvIndexControl?.setUVIndex(uvIndex)
        }
    }

    override fun setAmbientLight(ambientLight: Long) {
        if (ambientLightControl?.isEnabled == true) {
            ambientLightControl?.setAmbientLight(ambientLight)
        }
    }

    override fun setSoundLevel(soundLevel: Float) {
        if (soundLevelControl?.isEnabled == true) {
            soundLevelControl?.setSoundLevel(soundLevel.toInt())
        }
    }

    override fun setPressure(pressure: Float) {
        if (pressureControl?.isEnabled == true) {
            pressureControl?.setPressure(pressure.toInt())
        }
    }

    override fun setCO2Level(co2Level: Int) {
        if (co2Control?.isEnabled == true) {
            co2Control?.setCO2(co2Level)
        }
    }

    override fun setTVOCLevel(tvocLevel: Int) {
        if (vocControl?.isEnabled == true) {
            vocControl?.setVOC(tvocLevel)
        }
    }

    override fun setHallStrength(hallStrength: Float) {
        if (hallStrengthControl?.isEnabled == true) {
            hallStrengthControl?.setHallStrength(hallStrength)
        }
    }

    override fun setHallState(@HallState hallState: Int) {
        if (hallStateControl?.isEnabled == true) {
            hallStateControl?.setHallState(hallState)
        }
    }

    override fun setTemperatureEnabled(enabled: Boolean) {
        temperatureControl?.isEnabled = enabled
    }

    override fun setHumidityEnabled(enabled: Boolean) {
        humidityControl?.isEnabled = enabled
    }

    override fun setUvIndexEnabled(enabled: Boolean) {
        uvIndexControl?.isEnabled = enabled
    }

    override fun setAmbientLightEnabled(enabled: Boolean) {
        ambientLightControl?.isEnabled = enabled
    }

    override fun setSoundLevelEnabled(enabled: Boolean) {
        soundLevelControl?.isEnabled = enabled
    }

    override fun setPressureEnabled(enabled: Boolean) {
        pressureControl?.isEnabled = enabled
    }

    override fun setCO2LevelEnabled(enabled: Boolean) {
        co2Control?.isEnabled = enabled
    }

    override fun setTVOCLevelEnabled(enabled: Boolean) {
        vocControl?.isEnabled = enabled
    }

    override fun setHallStrengthEnabled(enabled: Boolean) {
        hallStrengthControl?.isEnabled = enabled
    }

    override fun setHallStateEnabled(enabled: Boolean) {
        hallStateControl?.isEnabled = enabled
    }

    private fun isPowerSufficient(): Boolean {
        when (powerSource) {
            ThunderBoardDevice.PowerSource.UNKNOWN,
            ThunderBoardDevice.PowerSource.COIN_CELL -> return false
        }
        return true
    }

    override fun setPowerSource(powerSource: ThunderBoardDevice.PowerSource) {
        this.powerSource = powerSource
    }

    override fun initGrid() {
        envGrid.apply {
            addView(temperatureControl)
            if (presenter.characteristicHumidityAvailable) addView(humidityControl)
            if (presenter.characteristicAmbientLightReactAvailable ||
                    presenter.characteristicAmbientLightSenseAvailable) {
                addView(ambientLightControl)
            }
            if (presenter.characteristicUvIndexAvailable) addView(uvIndexControl)
            if (presenter.characteristicPressureAvailable) addView(pressureControl)
            if (presenter.characteristicSoundLevelAvailable) addView(soundLevelControl)
            if (presenter.characteristicCo2ReadingAvailable && isPowerSufficient()) addView(co2Control)
            if (presenter.characteristicTvocReadingAvailable && isPowerSufficient()) addView(vocControl)
            if (presenter.characteristicHallFieldStrengthAvailable) addView(hallStrengthControl)
            if (presenter.characteristicHallStateAvailable) addView(hallStateControl)
        }
    }

    public override fun initControls() {
        // disable everything at first...
        setTemperatureEnabled(false)
        setHumidityEnabled(false)
        setAmbientLightEnabled(false)
        setUvIndexEnabled(false)
        setPressureEnabled(false)
        setSoundLevelEnabled(false)
        setCO2LevelEnabled(false)
        setTVOCLevelEnabled(false)
        setHallStrengthEnabled(false)
        setHallStateEnabled(false)
    }

    private fun onDeviceDisconnect() {
        if (!this.isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }


    val environmentGattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status ==  BluetoothGatt.GATT_SUCCESS) {
                bluetoothService?.let {
                    it.thunderboardDevice?.isServicesDiscovered = true
                    it.readRequiredCharacteristics()
                }
                presenter.checkAvailableCharacteristics()
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

                GattCharacteristic.EnvironmentTemperature -> {
                    val temperature = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setTemperature(temperature)
                    bluetoothService?.let {
                        it.selectedDeviceMonitor.onNext(device)
                        it.environmentReadMonitor.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                    }
                }

                GattCharacteristic.Humidity -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setHumidity(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.UvIndex -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setUvIndex(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.SoundLevel -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setSoundLevel(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.Pressure -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0).toLong()
                    device?.sensorEnvironment?.setPressure(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.CO2Reading -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setCO2Level(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.TVOCReading -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setTVOCLevel(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.HallFieldStrength -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0).toLong().toFloat()
                    device?.sensorEnvironment?.setHallStrength(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                GattCharacteristic.HallState -> {
                    val environmentValue = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setHallState(environmentValue)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }

                GattCharacteristic.AmbientLightReact,
                GattCharacteristic.AmbientLightSense -> {
                    val ambientLight = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val ambientLightLong =
                            if (ambientLight < 0) abs(ambientLight).toLong() + Int.MAX_VALUE.toLong()
                            else ambientLight.toLong()
                    device?.sensorEnvironment?.setAmbientLight(ambientLightLong)
                    bluetoothService?.environmentReadMonitor?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                else -> { }
            }

            bluetoothService?.readRequiredCharacteristics() // another call for the rest if any
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite; characteristic = ${characteristic.uuid}, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            if (characteristic.uuid == GattCharacteristic.HallControlPoint.uuid) {
                BleUtils.readCharacteristic(gatt,
                        GattService.HallEffect.number,
                        GattCharacteristic.HallState.uuid)
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
                GattCharacteristic.HallState -> {
                    val hallState = characteristic.getIntValue(gattCharacteristic.format, 0)
                    device?.sensorEnvironment?.setHallState(hallState)
                    bluetoothService?.environmentDetector?.onNext(EnvironmentEvent(device, gattCharacteristic.uuid))
                }
                else -> { }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
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
                GattCharacteristic.HallState.uuid -> {
                    val enabled = descriptor.value[0] == 0x01.toByte()
                    val notificationAction =
                            if (enabled) NotificationEvent.ACTION_NOTIFICATIONS_SET
                            else NotificationEvent.ACTION_NOTIFICATIONS_CLEAR

                    device?.let {
                        it.isHallStateNotificationEnabled = enabled
                        it.sensorEnvironment?.setHallStateNotificationEnabled()
                    }
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