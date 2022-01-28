package com.siliconlabs.bledemo.blinky_thunderboard.presenters

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Handler
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.blinky_thunderboard.activities.BlinkyThunderboardActivity
import com.siliconlabs.bledemo.thunderboard.base.BasePresenter
import com.siliconlabs.bledemo.thunderboard.injection.scope.ActivityScope
import com.siliconlabs.bledemo.thunderboard.model.LedRGBState
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.sensor.SensorBlinky
import com.siliconlabs.bledemo.thunderboard.utils.BleUtils
import com.siliconlabs.bledemo.utils.UuidConsts
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

@ActivityScope
class BlinkyThunderboardPresenter : BasePresenter() {

    private var ledSent: Int? = null
    private var ledReceived: Int? = null
    private var configureIOSubscriber: Subscriber<ThunderBoardDevice>? = null
    private var ledRGBState: LedRGBState? = null
    private val handler: Handler = Handler()

    private var rgbLedMask = 0

    private val retryWriteLed: Runnable = object : Runnable {
        override fun run() {
            handler.removeCallbacks(this)
            setColorLEDs(ledRGBState)
        }
    }

    fun setRgbLedMask(maskRepresentation: Int) {
        rgbLedMask = maskRepresentation and 0xff
    }

    fun ledAction(action: Int) {
        ledReceived = action
        if (ledSent == null) {
            ledSent = ledReceived
            val submitted = BleUtils.writeCharacteristics(
                    bluetoothService?.connectedGatt!!,
                    GattService.AutomationIo.number,
                    GattCharacteristic.Digital.uuid,
                    ledSent!!, BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            )
            if (!submitted) {
                //TODO: replace context
                //Timber.i(context.getString(R.string.iodemo_alert_action_failed));
            }
            Timber.d("write led  %02x submitted: %s", ledSent, submitted)
        } else {
            // wait until cleared
        }
    }

    override fun subscribe() {
        super.subscribe()
        configureIO()
    }

    private fun configureIO() {
        if (bluetoothService?.connectedGatt != null) {
            val device = bluetoothService?.thunderboardDevice
            if (device != null) {
                val sensor = SensorBlinky()
                sensor.isNotificationEnabled = false
                device.sensorBlinky = sensor
                unsubscribeConfigureIOSubscriber()
                configureIOSubscriber = enableConfigureIO()
                bluetoothService?.selectedDeviceMonitor!!
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io()) // wait a second before subscribing to notification
                        .delay(1000, TimeUnit.MILLISECONDS)
                        .subscribe(configureIOSubscriber)
                configureIOSettings()
            }
        }
    }

    private fun configureIOSettings() {
        BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.AutomationIo.number,
                GattCharacteristic.Digital.uuid)
    }

    private fun enableConfigureIO(): Subscriber<ThunderBoardDevice> {
        return object : Subscriber<ThunderBoardDevice>() {
            override fun onCompleted() {}
            override fun onError(e: Throwable) {}
            override fun onNext(device: ThunderBoardDevice) {
                val submitted = BleUtils.setCharacteristicNotification(
                        bluetoothService?.connectedGatt,
                        GattService.AutomationIo.number,
                        GattCharacteristic.Digital.uuid,
                        UuidConsts.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        true)
                if (!submitted) {
                    device.sensorBlinky?.let {
                        it.isNotificationEnabled = false
                        //TODO: replace context
                        //Toast.makeText(context, R.string.iodemo_alert_configuration_failed,
                          //      Toast.LENGTH_SHORT).show();
                    }
                }
                unsubscribeConfigureIOSubscriber()
            }
        }
    }

    private fun unsubscribeConfigureIOSubscriber() {
        if (configureIOSubscriber != null && !configureIOSubscriber!!.isUnsubscribed) {
            configureIOSubscriber!!.unsubscribe()
        }
        configureIOSubscriber = null
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
                val sensor = device.sensorBlinky
                deviceAvailable = true
                if (sensor == null) {
                    return
                }
                this@BlinkyThunderboardPresenter.sensor = sensor
                if (sensor.isSensorDataChanged && viewListener != null) {
                    val sensorData = sensor.sensorData
                    (viewListener as BlinkyThunderboardListener).setButton0State(sensorData.sw0)
                    (viewListener as BlinkyThunderboardListener).setButton1State(sensorData.sw1)
                    if (ledSent != null && ledSent != ledReceived) {
                        Timber.d("1")
                        ledSent = ledReceived
                        ledAction(ledSent!!)
                    } else if (ledSent == null && ledReceived == null) {
                        (viewListener as BlinkyThunderboardListener).setLed0State(sensorData.ledb)
                        (viewListener as BlinkyThunderboardListener).setLed1State(sensorData.ledg)
                    } else {
                        Timber.d("3")
                        ledSent = null
                    }
                    sensor.isSensorDataChanged = false
                    if (boardType == ThunderBoardDevice.Type.THUNDERBOARD_SENSE ||
                            boardType == ThunderBoardDevice.Type.THUNDERBOARD_DEV_KIT) {
                        if (sensorData.colorLed != null) {
                            (viewListener as BlinkyThunderboardListener).setColorLEDsValue(sensorData.colorLed!!)
                        } else {
                            readColorLEDs()
                        }
                    }
                }
                if ( (boardType == ThunderBoardDevice.Type.THUNDERBOARD_SENSE ||
                                boardType == ThunderBoardDevice.Type.THUNDERBOARD_DEV_KIT)
                        && sensor != null && sensor.sensorData.colorLed == null) {
                    readColorLEDs()
                }
            }
        }
    }

    fun readColorLEDs(): Boolean {
        return BleUtils.readCharacteristic(bluetoothService?.connectedGatt,
                GattService.UserInterface.number,
                GattCharacteristic.RgbLeds.uuid)
    }

    fun setColorLEDs(ledRGBState: LedRGBState?) {
        this.ledRGBState = ledRGBState
        handler.removeCallbacks(retryWriteLed)
        val device = bluetoothService?.thunderboardDevice
        if (device != null && device.sensorBlinky != null && device.sensorBlinky?.sensorData != null) {
            device.sensorBlinky?.sensorData?.colorLed = null
        }
        val bytes = ByteArray(4)
        bytes[0] = (ledRGBState!!.blue and 0xff).toByte()
        bytes[1] = (ledRGBState.green and 0xff).toByte()
        bytes[2] = (ledRGBState.red and 0xff).toByte()
        bytes[3] = (if (ledRGBState.on) rgbLedMask else 0x00).toByte()
        val value = ByteBuffer.wrap(bytes).int
        val characteristicsWriteResult = BleUtils.writeCharacteristics(
                bluetoothService?.connectedGatt!!,
                GattService.UserInterface.number,
                GattCharacteristic.RgbLeds.uuid,
                value,
                BluetoothGattCharacteristic.FORMAT_UINT32, 0)
        if (!characteristicsWriteResult) {
            handler.postDelayed(retryWriteLed, 100)
        }
    }

    fun findRgbLedMaskDescriptor() : BluetoothGattDescriptor? {
        return bluetoothService?.connectedGatt?.getService(GattService.UserInterface.number)?.
        getCharacteristic(GattCharacteristic.RgbLeds.uuid)?.
        getDescriptor(BlinkyThunderboardActivity.LED_MASK_DESCRIPTOR)
    }

}