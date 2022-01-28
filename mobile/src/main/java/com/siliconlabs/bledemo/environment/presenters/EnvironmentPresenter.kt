package com.siliconlabs.bledemo.environment.presenters

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.environment.model.EnvironmentEvent
import com.siliconlabs.bledemo.environment.model.HallState
import com.siliconlabs.bledemo.thunderboard.base.BasePresenter
import com.siliconlabs.bledemo.thunderboard.injection.scope.ActivityScope
import com.siliconlabs.bledemo.thunderboard.model.NotificationEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.sensor.SensorEnvironment
import com.siliconlabs.bledemo.thunderboard.utils.BleUtils
import com.siliconlabs.bledemo.thunderboard.utils.PreferenceManager
import com.siliconlabs.bledemo.utils.UuidConsts
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityScope
class EnvironmentPresenter @Inject constructor(
        private val preferenceManager: PreferenceManager) : BasePresenter() {

    private var environmentSubscriber: Subscriber<EnvironmentEvent>? = null
    private var readSubscriber: Subscriber<EnvironmentEvent>? = null
    private var notificationSubscriber: Subscriber<NotificationEvent>? = null
    private var configureEnvironmentSubscriber: Subscriber<NotificationEvent>? = null
    private var handler: Handler? = null
    private var readStatus = 0
    var notificationsHaveBeenSet = false
    private var notificationRetries = 0

    var characteristicHallStateAvailable = false
    var characteristicHallFieldStrengthAvailable = false
    var characteristicCo2ReadingAvailable = false
    var characteristicTvocReadingAvailable = false
    var characteristicPressureAvailable = false
    var characteristicSoundLevelAvailable = false
    var characteristicHumidityAvailable = false
    var characteristicUvIndexAvailable = false
    var characteristicAmbientLightReactAvailable = false
    var characteristicAmbientLightSenseAvailable = false

    override fun subscribe() {
        super.subscribe()
        notificationRetries = 0
        notificationsHaveBeenSet = false
        notificationSubscriber = onNotification()
        environmentSubscriber = onEnvironment()
        readSubscriber = onRead()
        bluetoothService?.notificationsMonitor!!
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(notificationSubscriber)
        bluetoothService?.environmentDetector!!
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(environmentSubscriber)
        bluetoothService?.environmentReadMonitor!!
                .delay(BLE_PERIODIC_READ_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(readSubscriber)
        configureEnvironment()
        notificationsHaveBeenSet = false
        handler = Handler()
    }

    override fun unsubscribe() {
        clearEnvironmentNotifications()
        notificationSubscriber?.unsubscribe()
        environmentSubscriber?.unsubscribe()
        readSubscriber?.unsubscribe()
        handler?.removeCallbacks(startPeriodicReads)
        super.unsubscribe()
    }

    fun resetDeviceSubscriptions() {
        bluetoothService?.thunderboardDevice?.isHallStateNotificationEnabled = null
    }

    override fun onDevice(): Subscriber<ThunderBoardDevice> {
        return object : Subscriber<ThunderBoardDevice>() {
            override fun onCompleted() {
                Timber.d("completed")
                if (!isUnsubscribed) {
                    unsubscribe()
                }
            }

            override fun onError(e: Throwable) {
                Timber.d("error: %s", e.message)
                if (!isUnsubscribed) {
                    unsubscribe()
                }
            }

            override fun onNext(device: ThunderBoardDevice) {
                Timber.d("device: %s", device.name)
                deviceAvailable = true
                val sensor = device.sensorEnvironment
                this@EnvironmentPresenter.sensor = sensor
                if (viewListener != null) {
                    if (device.isPowerSourceConfigured != null && device.isPowerSourceConfigured!!) {
                        (viewListener as EnvironmentListener).setPowerSource(device.powerSource)
                    }
                    (viewListener as EnvironmentListener).initGrid()
                }
                if (sensor != null && sensor.isSensorDataChanged && viewListener != null) {
                    val sensorData = sensor.sensorData
                    (viewListener as EnvironmentListener).setHallState(sensorData.hallState)
                    sensor.isSensorDataChanged = false
                    readStatus = sensor.readStatus
                    if (!isUnsubscribed) {
                        unsubscribe()
                    }
                }
                startEnvironmentNotifications()
            }
        }
    }

    private fun onNotification(): Subscriber<NotificationEvent> {
        return object : Subscriber<NotificationEvent>() {
            override fun onCompleted() {}
            override fun onError(e: Throwable) {}
            override fun onNext(event: NotificationEvent) {
                if (GattCharacteristic.HallState.uuid == event.characteristicUuid) {
                    if (NotificationEvent.ACTION_NOTIFICATIONS_SET == event.action) {
                        notificationsHaveBeenSet = true
                    }
                    readStatus = 0
                    readHallState()
                }
            }
        }
    }

    private val startPeriodicReads: Runnable = object : Runnable {
        override fun run() {
            Timber.d("Start periodic reads")
            handler?.removeCallbacks(this)
            readStatus = 0x01
            val submitted = readTemperature()
            Timber.d("Temperature submitted? %s", submitted)
            (viewListener as EnvironmentListener).setTemperatureEnabled(submitted)
            if (!submitted) {
                handler?.postDelayed(this, BLE_RETRY_DELAY.toLong())
            }
        }
    }

    private fun onRead(): Subscriber<EnvironmentEvent> {
        return object : Subscriber<EnvironmentEvent>() {
            override fun onCompleted() {
                if (!isUnsubscribed) {
                    unsubscribe()
                }
            }

            override fun onError(e: Throwable) {
                if (!isUnsubscribed) {
                    unsubscribe()
                }
            }

            override fun onNext(environmentEvent: EnvironmentEvent) {
                val sensor = environmentEvent.device?.sensorEnvironment ?: return
                val sensorData = sensor.sensorData
                if (GattCharacteristic.HallState.uuid == environmentEvent.characteristicUuid) {
                    (viewListener as EnvironmentListener).setHallState(sensorData.hallState)
                    handler?.post(startPeriodicReads)
                    return
                }
                var submitted = false
                when (readStatus and 0x55555) {
                    0x01 -> {
                        (viewListener as EnvironmentListener).setTemperature(
                                sensorData.temperature,
                                sensor.temperatureType)
                        readStatus = readStatus or 0x04
                        if (characteristicHumidityAvailable) {
                            submitted = readHumidity()
                            (viewListener as EnvironmentListener).setHumidityEnabled(submitted)
                            Timber.d("readStatus: %02x, submitted: %s", readStatus, submitted)
                        }
                    }
                    0x05 -> {
                        if (characteristicHumidityAvailable) {
                            (viewListener as EnvironmentListener).setHumidity(
                                    sensorData.humidity)
                        }
                        readStatus = readStatus or 0x10
                        if (characteristicUvIndexAvailable) {
                            submitted = readUvIndex()
                            (viewListener as EnvironmentListener).setUvIndexEnabled(submitted)
                            Timber.d("readStatus: %02x, submitted: %s", readStatus, submitted)
                        } else { /* Hotfix for 2601b board. Skip UV index sensor. */
                            readStatus = readStatus or 0x30
                            readStatus = readStatus or 0x40
                            if (characteristicAmbientLightReactAvailable || characteristicAmbientLightSenseAvailable) {
                                submitted = readAmbientLightReact() || readAmbientLightSense()
                                (viewListener as EnvironmentListener).setAmbientLightEnabled(submitted)
                                Timber.d("readStatus: %02x, submitted: %s", readStatus, submitted)
                            }
                        }
                    }
                    0x15 -> {
                        if (characteristicUvIndexAvailable) {
                            (viewListener as EnvironmentListener).setUvIndex(sensorData.uvIndex)
                        }
                        readStatus = readStatus or 0x40
                        if (characteristicAmbientLightReactAvailable || characteristicAmbientLightSenseAvailable) {
                            submitted = readAmbientLightReact() || readAmbientLightSense()
                            (viewListener as EnvironmentListener).setAmbientLightEnabled(submitted)
                            Timber.d("readStatus: %02x, submitted: %s", readStatus, submitted)
                        }
                    }
                    0x55 -> {
                        if (characteristicAmbientLightReactAvailable || characteristicAmbientLightSenseAvailable) {
                            (viewListener as EnvironmentListener).setAmbientLight(sensorData.ambientLight)
                        }
                        readStatus = readStatus or 0x100
                        if (characteristicSoundLevelAvailable) {
                            submitted = readSoundLevel()
                            (viewListener as EnvironmentListener).setSoundLevelEnabled(submitted)
                            Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                        }
                    }
                    0x155 -> {
                        if (characteristicSoundLevelAvailable) {
                            (viewListener as EnvironmentListener).setSoundLevel(sensorData.sound)
                        }
                        readStatus = readStatus or 0x400
                        if (characteristicPressureAvailable) {
                            submitted = readPressure()
                            (viewListener as EnvironmentListener).setPressureEnabled(submitted)
                            Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                        } else { /* Hotfix for 4184b board. Skip air quality and pressure sensors. */
                            readStatus = readStatus or 0x0c00
                            readStatus = readStatus or 0x1000
                            readStatus = readStatus or 0x3000
                            readStatus = readStatus or 0x4000
                            readStatus = readStatus or 0xc000
                            readStatus = readStatus or 0x10000
                            if (characteristicHallFieldStrengthAvailable) {
                                submitted = readHallStrength()
                                (viewListener as EnvironmentListener).setHallStrengthEnabled(submitted)
                                Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                            }

                        }
                    }
                    0x555 -> {
                        if (characteristicPressureAvailable) {
                            (viewListener as EnvironmentListener).setPressure(sensorData.pressure)
                        }
                        readStatus = readStatus or 0x1000
                        if (characteristicCo2ReadingAvailable) {
                            submitted = readCO2Level()
                            (viewListener as EnvironmentListener).setCO2LevelEnabled(submitted)
                            Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                        } else { /* Hotfix for 2601b board. Skip air quality sensors. */
                            readStatus = readStatus or 0x3000
                            readStatus = readStatus or 0x4000
                            readStatus = readStatus or 0xc000
                            readStatus = readStatus or 0x10000
                            if (characteristicHallFieldStrengthAvailable) {
                                submitted = readHallStrength()
                                (viewListener as EnvironmentListener).setHallStrengthEnabled(submitted)
                                Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                            }
                        }
                    }
                    0x1555 -> {
                        if (characteristicCo2ReadingAvailable) {
                            (viewListener as EnvironmentListener).setCO2Level(sensorData.cO2Level)
                        }
                        readStatus = readStatus or 0x4000
                        if (characteristicTvocReadingAvailable) {
                            submitted = readTVOCLevel()
                            (viewListener as EnvironmentListener).setTVOCLevelEnabled(submitted)
                            Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                        }
                    }
                    0x5555 -> {
                        if (characteristicTvocReadingAvailable) {
                            (viewListener as EnvironmentListener).setTVOCLevel(sensorData.tVOCLevel)
                        }
                        readStatus = readStatus or 0x10000
                        if (characteristicHallFieldStrengthAvailable) {
                            submitted = readHallStrength()
                            (viewListener as EnvironmentListener).setHallStrengthEnabled(submitted)
                            Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                        }
                    }
                    0x15555 -> {
                        if (characteristicHallFieldStrengthAvailable) {
                            (viewListener as EnvironmentListener).setHallStrength(sensorData.hallStrength)
                        }
                        readStatus = 0
                        submitted = readTemperature()
                        (viewListener as EnvironmentListener).setTemperatureEnabled(submitted)
                        Timber.d("readStatus: %04x, submitted: %s", readStatus, submitted)
                    }
                    else -> submitted = false
                }
                if (!submitted) {
                    handler?.postDelayed(startPeriodicReads, 200)
                }
            }
        }
    }

    private fun onEnvironment(): Subscriber<EnvironmentEvent> {
        return object : Subscriber<EnvironmentEvent>() {
            override fun onCompleted() {
                if (!isUnsubscribed) {
                    unsubscribe()
                }
            }

            override fun onError(e: Throwable) {
                if (!isUnsubscribed) {
                    unsubscribe()
                }
            }

            override fun onNext(event: EnvironmentEvent) {
                val sensor = event.device?.sensorEnvironment ?: return
                val sensorData = sensor.sensorData
                if (GattCharacteristic.HallState.uuid == event.characteristicUuid) {
                    (viewListener as EnvironmentListener).setHallState(sensorData.hallState)
                }
            }
        }
    }

    fun onHallStateClick() {
        val sensorData = if (sensor != null) (sensor as SensorEnvironment).sensorData else null
        if (sensorData != null && sensorData.hallState == HallState.TAMPERED) {
            resetHallEffectTamper()
        } else {
            Timber.d("onHallStateClick had no effect: current state is not tamper.")
        }
    }

    fun startEnvironmentNotifications() {
        Timber.d("NotificationsHaveBeenSet %s", notificationsHaveBeenSet)
        if (notificationsHaveBeenSet) {
            return
        }
        val submitted = enableHallStateMeasurement(true)
        (viewListener as EnvironmentListener).setHallStateEnabled(submitted)
        Timber.d("start environment notifications returned %s", submitted)
        if (!submitted) {
            if (notificationRetries > NOTIFICATION_MAX_RETRIES) {
                Timber.e("Could not start environment notifications, retry limit reached")
                handler?.post(startPeriodicReads)
                return
            }
            notificationRetries++
            handler?.postDelayed({ startEnvironmentNotifications() }, BLE_RETRY_DELAY.toLong())
        }
    }

    fun clearEnvironmentNotifications() {
        Timber.d("stop environment notifications")
        clearHallStateNotifications()
    }

    fun checkSettings() {
        if (sensor != null) {
            (sensor as SensorEnvironment).temperatureType =
                    preferenceManager.preferences?.temperatureType!!
        }
    }

    private fun configureEnvironment() {
        if (bluetoothService?.connectedGatt != null) {
            val device = bluetoothService?.thunderboardDevice
            if (device != null) {
                val sensor = SensorEnvironment(
                        preferenceManager.preferences?.temperatureType!!)
                sensor.isNotificationEnabled = false
                device.sensorEnvironment = sensor
                unsubscribeConfigureEnvironmentSubscriber()
                configureEnvironmentSubscriber = enableConfigureEnvironment(device)
                bluetoothService?.notificationsMonitor!!
                        .delay(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(configureEnvironmentSubscriber)
            }
        }
    }

    private fun enableConfigureEnvironment(
            device: ThunderBoardDevice): Subscriber<NotificationEvent> {
        return object : Subscriber<NotificationEvent>() {
            override fun onCompleted() {}
            override fun onError(e: Throwable) {
                Timber.d(e)
            }

            override fun onNext(notificationEvent: NotificationEvent) {
                if (device.isHallStateNotificationEnabled == null || !device.isHallStateNotificationEnabled!!) {
                    val submitted = enableHallStateMeasurement(true)
                    Timber.d("enable hall state notification submitted: %s", submitted)
                    return
                }
                unsubscribeConfigureEnvironmentSubscriber()
            }
        }
    }

    private fun unsubscribeConfigureEnvironmentSubscriber() {
        if (configureEnvironmentSubscriber != null && !configureEnvironmentSubscriber?.isUnsubscribed!!) {
            configureEnvironmentSubscriber?.unsubscribe()
        }
        configureEnvironmentSubscriber = null
    }

    fun readTemperature(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.EnvironmentTemperature.uuid)
    }

    fun readHumidity(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.Humidity.uuid)
    }

    fun readUvIndex(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.UvIndex.uuid)
    }

    fun readAmbientLightReact(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.AmbientLightReact.uuid)
    }

    fun readAmbientLightSense(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.AmbientLightSense.uuid)
    }

    fun readSoundLevel(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.SoundLevel.uuid)
    }

    fun readPressure(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.EnvironmentalSensing.number,
                GattCharacteristic.Pressure.uuid)
    }

    fun readCO2Level(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.IndoorAirQuality.number,
                GattCharacteristic.CO2Reading.uuid)
    }

    fun readTVOCLevel(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.IndoorAirQuality.number,
                GattCharacteristic.TVOCReading.uuid)
    }

    fun readHallStrength(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.HallEffect.number,
                GattCharacteristic.HallFieldStrength.uuid)
    }

    fun readHallState(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.HallEffect.number,
                GattCharacteristic.HallState.uuid)
    }

    private fun resetHallEffectTamper(): Boolean {
        val submitted = BleUtils.writeCharacteristics(bluetoothService?.connectedGatt!!,
                GattService.HallEffect.number,
                GattCharacteristic.HallControlPoint.uuid,
                HallState.OPENED,
                BluetoothGattCharacteristic.FORMAT_UINT16,
                0)
        Timber.d("submitted: %s", submitted)
        return submitted
    }

    fun enableHallStateMeasurement(enabled: Boolean): Boolean {
        return BleUtils.setCharacteristicNotification(
                bluetoothService?.connectedGatt,
                GattService.HallEffect.number,
                GattCharacteristic.HallState.uuid,
                UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                enabled)
    }

    fun clearHallStateNotifications() {
        if (bluetoothService?.connectedGatt != null) {
            val submitted = BleUtils.unsetCharacteristicNotification(
                    bluetoothService?.connectedGatt,
                    GattService.HallEffect.number,
                    GattCharacteristic.HallState.uuid,
                    UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                    false)
            Timber.d("disable hall state submitted: %s", submitted)
        }
    }

    fun checkAvailableCharacteristics() {
        characteristicHallStateAvailable = false
        characteristicHallFieldStrengthAvailable = false
        characteristicCo2ReadingAvailable = false
        characteristicTvocReadingAvailable = false
        characteristicPressureAvailable = false
        characteristicSoundLevelAvailable = false
        characteristicHumidityAvailable = false
        characteristicUvIndexAvailable = false
        characteristicAmbientLightReactAvailable = false
        characteristicAmbientLightSenseAvailable = false
        if (bluetoothService?.connectedGatt == null) {
            return
        }
        for (service in bluetoothService?.connectedGatt?.services!!) {
            if (service.uuid == GattService.EnvironmentalSensing.number) {
                for (characteristic in service.characteristics) {
                    if (characteristic.uuid == GattCharacteristic.Humidity.uuid) {
                        characteristicHumidityAvailable = true
                    } else if (characteristic.uuid == GattCharacteristic.UvIndex.uuid) {
                        characteristicUvIndexAvailable = true
                    } else if (characteristic.uuid == GattCharacteristic.AmbientLightReact.uuid) {
                        characteristicAmbientLightReactAvailable = true
                    } else if (characteristic.uuid == GattCharacteristic.Pressure.uuid) {
                        characteristicPressureAvailable = true
                    } else if (characteristic.uuid == GattCharacteristic.SoundLevel.uuid) {
                        characteristicSoundLevelAvailable = true
                    }
                }
            } else if (service.uuid == GattService.AmbientLight.number) {
                for (characteristic in service.characteristics) {
                    if (characteristic.uuid == GattCharacteristic.AmbientLightReact.uuid) {
                        characteristicAmbientLightReactAvailable = true
                    }
                }
            } else if (service.uuid == GattService.HallEffect.number) {
                for (characteristic in service.characteristics) {
                    if (characteristic.uuid == GattCharacteristic.HallState.uuid) {
                        characteristicHallStateAvailable = true
                    } else if (characteristic.uuid == GattCharacteristic.HallFieldStrength.uuid) {
                        characteristicHallFieldStrengthAvailable = true
                    }
                }
            } else if (service.uuid == GattService.IndoorAirQuality.number) {
                for (characteristic in service.characteristics) {
                    if (characteristic.uuid == GattCharacteristic.CO2Reading.uuid) {
                        characteristicCo2ReadingAvailable = true
                    } else if (characteristic.uuid == GattCharacteristic.TVOCReading.uuid) {
                        characteristicTvocReadingAvailable = true
                    }
                }
            }
        }
    }

    companion object {
        private const val BLE_RETRY_DELAY = 100
        private const val BLE_PERIODIC_READ_DELAY = 100
        private const val NOTIFICATION_MAX_RETRIES = 3
    }
}