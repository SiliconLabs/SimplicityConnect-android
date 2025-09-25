package com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.databinding.ActivityEnvironmentBinding
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.activities.ThunderboardActivity
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.utils.SensorChecker
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.utils.SensorChecker.ThunderboardSensor
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.control.EnvironmentControl
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.control.HallStateControl
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.dialogs.SettingsDialog
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.HallState
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.TemperatureScale
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.viewmodels.EnvironmentViewModel
import com.siliconlabs.bledemo.utils.AppUtil
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.Converters
import timber.log.Timber

class EnvironmentActivity : ThunderboardActivity() {

    private val controls = mutableMapOf<ThunderboardSensor, EnvironmentControl>()
    private var hallStateControl: HallStateControl? = null

    private lateinit var viewModel: EnvironmentViewModel
    private lateinit var binding: ActivityEnvironmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnvironmentBinding.inflate(
            layoutInflater
        )
        mainSection?.addView(binding.root)
        viewModel = ViewModelProvider(
            this,
            EnvironmentViewModel.Factory(this)
        ).get(EnvironmentViewModel::class.java)
        setupDataListeners()
        prepareToolBar()
    }

    private fun prepareToolBar() {
        AppUtil.setEdgeToEdge(window, this)
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.matter_back)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.title = this.getString(R.string.environment_demo_title)
        }
    }


    public override fun onResume() {
        super.onResume()
        viewModel.checkTemperatureScale()
        if (!setup) {
            gattQueue.queueNotify(getHallEffectCharacteristic(GattCharacteristic.HallState))
            queueReadingEnvironmentalData()
        }
    }

    override fun onPause() {
        super.onPause()
        gattQueue.clear()
        gattQueue.queueCancelNotifications(getHallEffectCharacteristic(GattCharacteristic.HallState))
    }

    private fun setupDataListeners() {
        viewModel.controlsRead.observe(this, Observer {
            if (it != 0 && it == viewModel.activeControls) {
                viewModel.resetControlsRead()
                queueReadingEnvironmentalData()
            }
        })
        viewModel.temperature.observe(this, Observer {
            controls[ThunderboardSensor.Temperature]?.setTemperature(it, viewModel.temperatureScale)
        })
        viewModel.humidity.observe(this, Observer {
            controls[ThunderboardSensor.Humidity]?.setHumidity(it)
        })
        viewModel.uvIndex.observe(this, Observer {
            controls[ThunderboardSensor.UvIndex]?.setUVIndex(it)
        })
        viewModel.ambientLight.observe(this, Observer {
            controls[ThunderboardSensor.AmbientLight]?.setAmbientLight(it)
        })
        viewModel.soundLevel.observe(this, Observer {
            controls[ThunderboardSensor.SoundLevel]?.setSoundLevel(it)
        })
        viewModel.pressure.observe(this, Observer {
            controls[ThunderboardSensor.Pressure]?.setPressure(it)
        })
        viewModel.co2Level.observe(this, Observer {
            controls[ThunderboardSensor.CO2]?.setCO2(it)
        })
        viewModel.tvocLevel.observe(this, Observer {
            controls[ThunderboardSensor.TVOC]?.setVOC(it)
        })
        viewModel.hallStrength.observe(this, Observer {
            controls[ThunderboardSensor.MagneticField]?.setHallStrength(it)
        })
        viewModel.hallState.observe(this, Observer {
          //  hallStateControl?.setHallState(it)
            controls[ThunderboardSensor.DoorState]?.setHallState(it)
        })
    }

    private fun showBrokenSensorsMessage(brokenSensors: Set<ThunderboardSensor>) {
        val isAnySensorWorking = sensorChecker.environmentSensors.filter {
            it.value == SensorChecker.SensorState.WORKING
        }.any()
        val dialogMessage =
            if (isAnySensorWorking) getString(
                R.string.sensor_malfunction_dialog_message,
                TextUtils.join(", ", brokenSensors)
            )
            else getString(
                R.string.critical_sensor_malfunction_dialog_message,
                TextUtils.join(", ", brokenSensors)
            )

        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.sensor_malfunction_dialog_title))
            setMessage(dialogMessage)
            setPositiveButton(getString(R.string.button_ok)) { _, _ ->
                if (isAnySensorWorking) startReadings()
                else finish()
            }
            setCancelable(false)
            runOnUiThread { show() }
        }
    }

    private fun onHallStateClick() {
        if (viewModel.hallState.value == HallState.TAMPERED) {
            getHallEffectCharacteristic(GattCharacteristic.HallControlPoint)?.let {
                gattQueue.clear() // show click effect immediately
                it.value = byteArrayOf(HallState.OPENED.value.toByte(), 0)
                gattQueue.queueWrite(it)
            }
        } else {
            Timber.d("onHallStateClick had no effect: current state is not tamper.")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_environment, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("MissingPermission")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                gatt?.disconnect()
                onBackPressed()
                true
            }

            R.id.action_settings -> {
                showSettings()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettings() {
        SettingsDialog(this, object : SettingsDialog.SettingsHandler {
            override fun onSettingsSaved(scale: TemperatureScale) {
                viewModel.temperatureScale = scale.scale
                gattQueue.clear() // show temperature scale change immediately (it's the first message to queue)
                queueReadingEnvironmentalData()
            }
        }).also {
            it.show(supportFragmentManager, SETTINGS_DIALOG_FRAGMENT)
        }
    }

    private fun startReadings() {
        initGrid()
        queueReadingEnvironmentalData()
    }

    private fun isPowerSufficient(): Boolean {
        return when (statusFragment.viewModel.thunderboardDevice.value?.powerSource) {
            ThunderBoardDevice.PowerSource.USB -> true
            else -> false
        }
    }

    private fun initGrid() {
        binding.envGrid.apply {
            sensorChecker.environmentSensors.filter {
                it.value == SensorChecker.SensorState.WORKING
            }.forEach {
                if (it.key == ThunderboardSensor.CO2 && !isPowerSufficient()) return@forEach
                if (it.key == ThunderboardSensor.TVOC && !isPowerSufficient()) return@forEach

                if (it.key != ThunderboardSensor.DoorState) {
                    controls[it.key] = EnvironmentControl(
                        this@EnvironmentActivity,
                        getString(getTileDescription(it.key)),
                        ContextCompat.getDrawable(this@EnvironmentActivity, getTileIcon(it.key))
                    ).also {
                        runOnUiThread { addView(it.tileView?.root) }
                    }
                } else {

                    controls[it.key] = EnvironmentControl(
                        this@EnvironmentActivity,
                        getString(getTileDescription(it.key)),
                        ContextCompat.getDrawable(this@EnvironmentActivity, getTileIcon(it.key))
                    ).also {
                        runOnUiThread { addView(it.tileView?.root) }
                    }
                }
            }
        }

    }

    private fun setupSensorCharacteristics() {
        sensorChecker.let {
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.Temperature,
                getEnvironmentalSensingCharacteristic(GattCharacteristic.EnvironmentTemperature)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.Humidity,
                getEnvironmentalSensingCharacteristic(GattCharacteristic.Humidity)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.UvIndex,
                getEnvironmentalSensingCharacteristic(GattCharacteristic.UvIndex)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.Pressure,
                getEnvironmentalSensingCharacteristic(GattCharacteristic.Pressure)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.SoundLevel,
                getEnvironmentalSensingCharacteristic(GattCharacteristic.SoundLevel)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.AmbientLight,
                getAmbientLightCharacteristic()
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.CO2,
                getAirQualityCharacteristic(GattCharacteristic.CO2Reading)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.TVOC,
                getAirQualityCharacteristic(GattCharacteristic.TVOCReading)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.MagneticField,
                getHallEffectCharacteristic(GattCharacteristic.HallFieldStrength)
            )
            it.setupEnvSensorCharacteristic(
                ThunderboardSensor.DoorState,
                getHallEffectCharacteristic(GattCharacteristic.HallState)
            )
        }
    }

    private fun queueReadingEnvironmentalData() {
        gattQueue.let {
            sensorChecker.environmentSensors.filter { entry ->
                entry.value == SensorChecker.SensorState.WORKING
            }.forEach { entry ->
                if (setup && entry.key == ThunderboardSensor.DoorState) {
                    // DoorState setup depends on MagneticField sensor anyway
                    return@forEach
                }

                it.queueRead(entry.key.characteristic)
            }
        }
    }

    private fun getEnvironmentalSensingCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.EnvironmentalSensing.number)
            ?.getCharacteristic(characteristic.uuid)
    }

    private fun getAirQualityCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.IndoorAirQuality.number)
            ?.getCharacteristic(characteristic.uuid)
    }

    private fun getHallEffectCharacteristic(characteristic: GattCharacteristic): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.HallEffect.number)
            ?.getCharacteristic(characteristic.uuid)
    }

    private fun getAmbientLightCharacteristic(): BluetoothGattCharacteristic? {
        val lightReact = getEnvironmentalSensingCharacteristic(GattCharacteristic.AmbientLightReact)
        if (lightReact != null) return lightReact

        val lightReact2 = BLEUtils.getCharacteristic(
            gatt, GattService.AmbientLight,
            GattCharacteristic.AmbientLightReact
        )
        if (lightReact2 != null) return lightReact2

        val lightSense = getEnvironmentalSensingCharacteristic(GattCharacteristic.AmbientLightSense)
        if (lightSense != null) return lightSense

        return null
    }

    @StringRes
    private fun getTileDescription(sensor: ThunderboardSensor): Int {
        return when (sensor) {
            ThunderboardSensor.Temperature -> R.string.environment_temp
            ThunderboardSensor.Humidity -> R.string.environment_humidity
            ThunderboardSensor.AmbientLight -> R.string.environment_ambient
            ThunderboardSensor.UvIndex -> R.string.environment_uv
            ThunderboardSensor.Pressure -> R.string.environment_pressure
            ThunderboardSensor.SoundLevel -> R.string.environment_sound_level
            ThunderboardSensor.CO2 -> R.string.environment_co2
            ThunderboardSensor.TVOC -> R.string.environment_vocs
            ThunderboardSensor.MagneticField -> R.string.environment_hall_strength
            ThunderboardSensor.DoorState -> R.string.environment_hall_state
            else -> 0
        }
    }

    @DrawableRes
    private fun getTileIcon(sensor: ThunderboardSensor): Int {
        return when (sensor) {
            ThunderboardSensor.Temperature -> R.drawable.icon_temp
            ThunderboardSensor.Humidity -> R.drawable.icon_environment
            ThunderboardSensor.AmbientLight -> R.drawable.icon_light
            ThunderboardSensor.UvIndex -> R.drawable.icon_uv
            ThunderboardSensor.Pressure -> R.drawable.icon_airpressure
            ThunderboardSensor.SoundLevel -> R.drawable.icon_sound
            ThunderboardSensor.CO2 -> R.drawable.icon_co2
            ThunderboardSensor.TVOC -> R.drawable.icon_vocs
            ThunderboardSensor.MagneticField -> R.drawable.icon_magneticfield
            ThunderboardSensor.DoorState -> R.drawable.icon_doorstate
            else -> 0
        }
    }

    override val gattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnected()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) return

            queueReadingDeviceCharacteristics()
            setupSensorCharacteristics()
            gattQueue.queueNotify(getHallEffectCharacteristic(GattCharacteristic.HallState))
            queueReadingEnvironmentalData()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            gattQueue.handleCommandProcessed()
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.DeviceName,
                GattCharacteristic.ModelNumberString,
                GattCharacteristic.BatteryLevel,
                GattCharacteristic.PowerSource,
                GattCharacteristic.FirmwareRevision -> statusFragment.handleBaseCharacteristic(
                    characteristic
                )

                GattCharacteristic.EnvironmentTemperature -> {
                    val temperature = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.Temperature,
                            temperature.toLong()
                        )
                    } else viewModel.incrementControlsRead()
                    viewModel.temperature.postValue(temperature / 100.0f)
                }

                GattCharacteristic.Humidity -> {
                    val humidity = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.Humidity,
                            humidity.toLong()
                        )
                    } else viewModel.incrementControlsRead()
                    viewModel.humidity.postValue(humidity / 100)
                }

                GattCharacteristic.UvIndex -> {
                    val uvIndex = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.UvIndex,
                            uvIndex.toLong()
                        )
                    } else viewModel.incrementControlsRead()
                    viewModel.uvIndex.postValue(uvIndex)
                }

                GattCharacteristic.SoundLevel -> {
                    val soundLevel = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.SoundLevel,
                            soundLevel.toLong()
                        )
                    } else viewModel.incrementControlsRead()
                    viewModel.soundLevel.postValue(soundLevel / 100)
                }

                GattCharacteristic.Pressure -> {
                    val pressure = Converters.calculateLongValue(characteristic.value, false)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(ThunderboardSensor.Pressure, pressure)
                    } else viewModel.incrementControlsRead()
                    viewModel.pressure.postValue(pressure / 1000)
                }

                GattCharacteristic.CO2Reading -> {
                    val co2Level = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.CO2,
                            co2Level.toLong()
                        )
                    } else viewModel.incrementControlsRead()
                    viewModel.co2Level.postValue(co2Level)
                }

                GattCharacteristic.TVOCReading -> {
                    val tvocLevel = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.TVOC,
                            tvocLevel.toLong()
                        )
                    } else viewModel.incrementControlsRead()
                    viewModel.tvocLevel.postValue(tvocLevel)
                }

                GattCharacteristic.HallFieldStrength -> {
                    val hallStrength = characteristic.getIntValue(gattCharacteristic.format, 0)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.MagneticField,
                            hallStrength.toLong()
                        )
                        val brokenSensors = sensorChecker.environmentSensors.filter {
                            it.value == SensorChecker.SensorState.BROKEN
                        }.keys
                        setup = false // last sensor to check
                        viewModel.activeControls = sensorChecker.environmentSensors.filter {
                            it.value == SensorChecker.SensorState.WORKING
                        }.size
                        dismissModalDialog()

                        if (brokenSensors.isNotEmpty()) showBrokenSensorsMessage(brokenSensors)
                        else startReadings()
                    } else viewModel.incrementControlsRead()
                    viewModel.hallStrength.postValue(hallStrength)

                }

                GattCharacteristic.HallState -> {
                    val hallState = characteristic.getIntValue(gattCharacteristic.format, 0)
                    viewModel.hallState.postValue(HallState.fromValue(hallState))
                    viewModel.incrementControlsRead()

                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            queueReadingEnvironmentalData()
                        }, 50) //last characteristic, so read all again
                    }
                }

                GattCharacteristic.AmbientLightReact,
                GattCharacteristic.AmbientLightSense -> {
                    var ambientLight = Converters.calculateLongValue(characteristic.value, false)
                    if (setup) {
                        sensorChecker.checkIfEnvSensorBroken(
                            ThunderboardSensor.AmbientLight,
                            ambientLight
                        )
                    } else viewModel.incrementControlsRead()
                    ambientLight /= 100
                    viewModel.ambientLight.postValue(
                        if (ambientLight > MAX_AMBIENT_LIGHT) MAX_AMBIENT_LIGHT.toLong()
                        else ambientLight
                    )
                }

                else -> {}
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            gattQueue.handleCommandProcessed()
            if (status != BluetoothGatt.GATT_SUCCESS) return

            if (characteristic.uuid == GattCharacteristic.HallControlPoint.uuid) {
                gattQueue.queueRead(getHallEffectCharacteristic(GattCharacteristic.HallState))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.BatteryLevel,
                GattCharacteristic.PowerSource -> statusFragment.handleBaseCharacteristic(
                    characteristic
                )

                GattCharacteristic.HallState -> {
                    val hallState = characteristic.getIntValue(gattCharacteristic.format, 0)
                    viewModel.hallState.postValue(HallState.fromValue(hallState))
                }

                else -> {}
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            gattQueue.handleCommandProcessed()
        }
    }

    companion object {
        private const val MAX_AMBIENT_LIGHT = 99999
        private const val SETTINGS_DIALOG_FRAGMENT = "settings_dialog_fragment"
    }
}