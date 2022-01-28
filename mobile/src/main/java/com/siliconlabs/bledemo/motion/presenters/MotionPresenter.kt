package com.siliconlabs.bledemo.motion.presenters

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.motion.model.MotionEvent
import com.siliconlabs.bledemo.thunderboard.base.BasePresenter
import com.siliconlabs.bledemo.thunderboard.injection.scope.ActivityScope
import com.siliconlabs.bledemo.thunderboard.model.NotificationEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.sensor.SensorMotion
import com.siliconlabs.bledemo.thunderboard.utils.BleUtils
import com.siliconlabs.bledemo.utils.UuidConsts
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@ActivityScope
class MotionPresenter : BasePresenter() {
    private var motionDetector: PublishSubject<MotionEvent>? = null
    private var motionSubscriber: Subscriber<MotionEvent>? = null
    private var notificationsMonitor: BehaviorSubject<NotificationEvent>? = null
    private var notificationsSubscriber: Subscriber<NotificationEvent>? = null
    private var configureMotionSubscriber: Subscriber<NotificationEvent>? = null
    private var isCalibrating = false

    override fun subscribe() {
        super.subscribe()
        motionSubscriber = onMotion()
        motionDetector = bluetoothService?.motionDetector
        motionDetector!!
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(motionSubscriber)
        notificationsSubscriber = onNotification()
        notificationsMonitor = bluetoothService?.notificationsMonitor
        notificationsMonitor!!
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(notificationsSubscriber)
        configureMotion()
    }

    override fun unsubscribe() {
        super.unsubscribe()
        if (motionSubscriber != null && !motionSubscriber!!.isUnsubscribed) {
            motionSubscriber!!.unsubscribe()
        }
        motionSubscriber = null
        if (notificationsSubscriber != null && !notificationsSubscriber!!.isUnsubscribed) {
            notificationsSubscriber!!.unsubscribe()
        }
        notificationsSubscriber = null
        Timber.d("cancel startCalibration")
        isCalibrating = false
    }

    fun resetDeviceSubscriptions() {
        bluetoothService?.thunderboardDevice?.apply {
            isCalibrateNotificationEnabled = null
            isAccelerationNotificationEnabled = null
            isOrientationNotificationEnabled = null
        }
    }

    fun calibrate() {
        if (!isCalibrating) {
            val calibrationSubmitted = startCalibration()
            if (!calibrationSubmitted) {
                Handler().postDelayed({ calibrate() }, 500)
            } else {
                isCalibrating = true
            }
        }
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
                if (boardType != ThunderBoardDevice.Type.THUNDERBOARD_SENSE &&
                        boardType != ThunderBoardDevice.Type.THUNDERBOARD_BLUE &&
                        boardType != ThunderBoardDevice.Type.THUNDERBOARD_DEV_KIT) {
                    unsubscribe()
                }
            }
        }
    }

    protected fun onMotion(): Subscriber<MotionEvent> {
        return object : Subscriber<MotionEvent>() {
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

            override fun onNext(event: MotionEvent) {
                Timber.d("motion for device: %s", event.device!!.name)
                if (event.action != null) {
                    if (isCalibrating) {
                        if (event.action == MotionEvent.ACTION_CALIBRATE) {
                            resetOrientation()
                            return
                        }
                        if (event.action == MotionEvent.ACTION_CLEAR_ORIENTATION) {
                            finishCalibration()
                            return
                        }
                    }
                }
                val sensor = event.device.sensorMotion
                this@MotionPresenter.sensor = sensor
                if (sensor?.isSensorDataChanged == true && viewListener != null) {
                    val sensorData = sensor.sensorData
                    (viewListener as MotionListener).setOrientation(sensorData.ox,
                            sensorData.oy, sensorData.oz)
                    (viewListener as MotionListener).setAcceleration(sensorData.ax,
                            sensorData.ay, sensorData.az)
                    sensor.isSensorDataChanged = false
                }
            }
        }
    }

    private fun finishCalibration() {
        isCalibrating = false
        if (viewListener != null) {
            (viewListener as MotionListener).onCalibrateCompleted()
        }
    }

    protected fun onNotification(): Subscriber<NotificationEvent> {
        return object : Subscriber<NotificationEvent>() {
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

            override fun onNext(event: NotificationEvent) {
                if (NotificationEvent.ACTION_NOTIFICATIONS_SET != event.action) {
                    return
                }
                val device = event.device
                Timber.d("notification for device: %s", device.name)
                val sensor = event.device.sensorMotion
                this@MotionPresenter.sensor = sensor
                if (sensor != null) {
                    val characteristicsStatus = sensor.characteristicsStatus
                    when (characteristicsStatus and 0x055) {
                        0x05 -> enableAcceleration(true)
                        0x15 -> enableOrientation(true)
                        else -> {
                        }
                    }
                }
            }
        }
    }

    private fun configureMotionCalibrate(enabled: Boolean) {
        val submitted = BleUtils.setCharacteristicIndications(
                bluetoothService?.connectedGatt!!,
                GattService.Motion.number,
                GattCharacteristic.Calibration.uuid,
                UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                enabled)
        Timber.d("%s acceleration indication submitted: %s", enabled, submitted)
    }

    private fun enableConfigureMotion(device: ThunderBoardDevice): Subscriber<NotificationEvent> {
        return object : Subscriber<NotificationEvent>() {
            override fun onCompleted() {}
            override fun onError(e: Throwable) {}
            override fun onNext(notificationEvent: NotificationEvent) {
                if (device.isCalibrateNotificationEnabled == null || !device.isCalibrateNotificationEnabled!!) {
                    configureMotionCalibrate(true)
                    return
                }
                if (device.isAccelerationNotificationEnabled == null || !device.isAccelerationNotificationEnabled!!) {
                    val submitted = enableAcceleration(true)
                    Timber.d("enable acceleration indication submitted: %s", submitted)
                    return
                }
                if (device.isOrientationNotificationEnabled == null || !device.isOrientationNotificationEnabled!!) {
                    val submitted = enableOrientation(true)
                    Timber.d("enable orientation indication submitted: %s", submitted)
                    return
                }
                unsubscribeConfigureMotionSubscriber()
            }
        }
    }

    fun configureMotion() {
        if (bluetoothService?.connectedGatt != null) {
            val device = bluetoothService?.thunderboardDevice
            if (device != null) {
                val sensor = SensorMotion()
                sensor.isNotificationEnabled = false
                device.sensorMotion = sensor
                unsubscribeConfigureMotionSubscriber()
                configureMotionSubscriber = enableConfigureMotion(device)
                notificationsMonitor!!
                        .delay(100, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(configureMotionSubscriber)
                configureMotionCalibrate(true)
            }
        }
    }

    private fun unsubscribeConfigureMotionSubscriber() {
        configureMotionSubscriber?.let {
            if (!it.isUnsubscribed) it.unsubscribe()
        }
        configureMotionSubscriber = null
    }

    private fun clearCalibrateNotification() {
        if (bluetoothService?.connectedGatt != null) {
            val submittedCalibrate = BleUtils.unsetCharacteristicNotification(
                    bluetoothService?.connectedGatt,
                    GattService.Motion.number,
                    GattCharacteristic.Calibration.uuid,
                    UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, false)
            Timber.d("disable calibration indication submitted: %s", submittedCalibrate)
        }
    }

    private fun clearAccelerationNotification() {
        if (bluetoothService?.connectedGatt != null) {
            val submittedAcceleration = BleUtils.unsetCharacteristicNotification(
                    bluetoothService?.connectedGatt,
                    GattService.Motion.number,
                    GattCharacteristic.Acceleration.uuid,
                    UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, false)
            Timber.d("disable acceleration indication submitted: %s", submittedAcceleration)
        }
    }

    private fun clearOrientationNotification() {
        if (bluetoothService?.connectedGatt != null) {
            val submittedOrientation = BleUtils.unsetCharacteristicNotification(
                    bluetoothService?.connectedGatt,
                    GattService.Motion.number,
                    GattCharacteristic.Orientation.uuid,
                    UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, false)
            Timber.d("disable orientation indication submitted: %s", submittedOrientation)
        }
    }

    // resets subscription to BLE chars, might be needed
    fun clearMotionNotifications() {
        if (bluetoothService?.connectedGatt != null) {
            clearCalibrateNotification()
            clearAccelerationNotification()
            clearOrientationNotification()
        }
    }

    fun enableOrientation(enabled: Boolean): Boolean {
        return BleUtils.setCharacteristicNotification(
                bluetoothService?.connectedGatt,
                GattService.Motion.number,
                GattCharacteristic.Orientation.uuid,
                UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, enabled)
    }

    fun enableAcceleration(enabled: Boolean): Boolean {
        return BleUtils.setCharacteristicNotification(
                bluetoothService?.connectedGatt,
                GattService.Motion.number,
                GattCharacteristic.Acceleration.uuid,
                UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR, enabled)
    }

    fun startCalibration(): Boolean {
        val submitted = BleUtils.writeCharacteristics(
                bluetoothService?.connectedGatt!!,
                GattService.Motion.number,
                GattCharacteristic.Calibration.uuid, 0x01,
                BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        Timber.d("submitted: %s", submitted)
        return submitted
    }

    fun resetOrientation(): Boolean {
        val submitted = BleUtils.writeCharacteristics(
                bluetoothService?.connectedGatt!!,
                GattService.Motion.number,
                GattCharacteristic.Calibration.uuid, 0x02,
                BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        Timber.d("submitted: %s", submitted)
        return submitted
    }
}