package com.siliconlabs.bledemo.blinky_thunderboard.activities

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.Base.SelectDeviceDialog
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.Bluetooth.BLE.GattService
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.blinky_thunderboard.viewmodels.BlinkyThunderboardViewModel
import com.siliconlabs.bledemo.blinky_thunderboard.control.ColorLEDControl
import com.siliconlabs.bledemo.blinky_thunderboard.control.ColorLEDControl.ColorLEDControlListener
import com.siliconlabs.bledemo.thunderboard.base.ThunderboardActivity
import com.siliconlabs.bledemo.blinky_thunderboard.model.LedRGBState
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import kotlinx.android.synthetic.main.activity_blinky_thunderboard.*
import java.util.*

class BlinkyThunderboardActivity : ThunderboardActivity(), ColorLEDControlListener {

    private lateinit var ledsControl: CardView
    private lateinit var colorLEDControl: ColorLEDControl

    private lateinit var viewModel: BlinkyThunderboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(this).inflate(R.layout.activity_blinky_thunderboard, null, false)
        colorLEDControl = view.findViewById(R.id.color_led_control) // make the view gone (if necessary) before it can be showed
        ledsControl = view.findViewById(R.id.leds_control)
        val powerSourceIntent = intent.getIntExtra(SelectDeviceDialog.POWER_SOURCE_EXTRA, 0)
        val modelNumberIntent = intent.getStringExtra(SelectDeviceDialog.MODEL_TYPE_EXTRA)
        setControlsVisibility(ThunderBoardDevice.PowerSource.fromInt(powerSourceIntent), modelNumberIntent)

        mainSection?.addView(view)

        viewModel = ViewModelProvider(this).get(BlinkyThunderboardViewModel::class.java)
        bindBluetoothService(ioGattCallback)

        setupDataListeners(modelNumberIntent)
        setupUiListeners()
    }

    private fun setupUiListeners() {
        led_0.setOnCheckedChangeListener { _, isChecked ->
            var action = 0
            if (isChecked) action = BlinkyThunderboardViewModel.LED_0_ON
            if (led_1.isChecked) action = action or BlinkyThunderboardViewModel.LED_1_ON

            getDigitalWriteCharacteristic()?.apply {
                value = byteArrayOf(action.toByte())
                gattQueue.queueWrite(this)
            }
        }
        led_1.setOnCheckedChangeListener { _, isChecked ->
            var action = 0
            if (isChecked) action = BlinkyThunderboardViewModel.LED_1_ON
            if (led_0.isChecked) action = action or BlinkyThunderboardViewModel.LED_0_ON

            getDigitalWriteCharacteristic()?.apply {
                value = byteArrayOf(action.toByte())
                gattQueue.queueWrite(this)
            }
        }
        colorLEDControl.setColorLEDControlListener(this)
    }

    private fun setupDataListeners(modelNumber: String?) {
        viewModel.button0.observe(this, Observer { switch_0.setChecked(it) })
        viewModel.button1.observe(this, Observer { switch_1.setChecked(it) })
        viewModel.led0.observe(this, Observer {
            if (it != led_0.isChecked) led_0.isChecked = it })
        viewModel.led1.observe(this, Observer {
            if (it != led_1.isChecked) led_1.isChecked = it })

        when (modelNumber) {
            ThunderBoardDevice.THUNDERBOARD_MODEL_SENSE,
            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V1,
            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V2 -> {
                viewModel.colorLed.observe(this, Observer {
                    colorLEDControl.setColorLEDsUI(it)
                })
            }
        }
        viewModel.rgbLedMask.observe(this, Observer {
            gattQueue.queueRead(getRgbLedCharacteristic())
            gattQueue.queueNotify(getDigitalNotifyCharacteristic())
            dismissModalDialog()
        })
    }

    private fun sendColorLedCommand(rgbState: LedRGBState) {
        val data = byteArrayOf(
                (if (rgbState.on) viewModel.rgbLedMask.value!! else 0x00).toByte(),
                (rgbState.red and 0xff).toByte(),
                (rgbState.green and 0xff).toByte(),
                (rgbState.blue and 0xff).toByte()

        )
        getRgbLedCharacteristic()?.apply {
            value = data
            gattQueue.queueWrite(this)
        }
    }

    private fun setControlsVisibility(powerSource: ThunderBoardDevice.PowerSource, modelNumber: String?) {
        if (modelNumber == ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V1 ||
                modelNumber == ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V2) {
            ledsControl.visibility = View.GONE
        }

        if (modelNumber == ThunderBoardDevice.THUNDERBOARD_MODEL_SENSE &&
                powerSource == ThunderBoardDevice.PowerSource.COIN_CELL) {
            colorLEDControl.visibility = View.GONE
        }
    }

    private fun initControls() {
        if (statusFragment.viewModel.thunderboardDevice.value?.
                boardType != ThunderBoardDevice.Type.THUNDERBOARD_SENSE &&
            statusFragment.viewModel.thunderboardDevice.value?.
                boardType != ThunderBoardDevice.Type.THUNDERBOARD_DEV_KIT) {

            runOnUiThread { colorLEDControl.visibility = View.GONE }
        }

        getRgbLedMaskDescriptor()?.let {
            bluetoothService?.connectedGatt?.readDescriptor(it)
        } ?: viewModel.rgbLedMask.postValue(0x0f)
    }

    override fun updateColorLEDs(ledRGBState: LedRGBState) {
        sendColorLedCommand(ledRGBState)
    }

    override fun onLedUpdateStop() {
        gattQueue.clearAllButLast() // prevent from queueing more gatt commands
    }

    private fun getDigitalWriteCharacteristic() : BluetoothGattCharacteristic? {
        return bluetoothService?.connectedGatt?.getService(
                GattService.AutomationIo.number)?.characteristics
                ?.filter { it.uuid == GattCharacteristic.Digital.uuid } // there are two
                ?.first { it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 }
    }

    private fun getDigitalNotifyCharacteristic() : BluetoothGattCharacteristic? {
        return bluetoothService?.connectedGatt?.getService(
                GattService.AutomationIo.number)?.characteristics
                ?.filter { it.uuid == GattCharacteristic.Digital.uuid } // there are two
                ?.first { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 }
    }


    private fun getRgbLedCharacteristic() : BluetoothGattCharacteristic? {
        return bluetoothService?.connectedGatt?.getService(GattService.UserInterface.number)?.
        getCharacteristic(GattCharacteristic.RgbLeds.uuid)
    }

    private fun getRgbLedMaskDescriptor() : BluetoothGattDescriptor? {
        return bluetoothService?.connectedGatt?.getService(GattService.UserInterface.number)?.
        getCharacteristic(GattCharacteristic.RgbLeds.uuid)?.
        getDescriptor(LED_MASK_DESCRIPTOR)
    }

    private fun onDeviceDisconnect() {
        if (!this.isFinishing) {
            showMessage(R.string.device_has_disconnected)
            finish()
        }
    }

    private val ioGattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
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

                GattCharacteristic.RgbLeds -> {
                    val on = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val red = characteristic.getIntValue(gattCharacteristic.format, 1)
                    val green = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val blue = characteristic.getIntValue(gattCharacteristic.format, 3)
                    val ledState = LedRGBState(
                            on != null && on != 0,
                            red ?: 0,
                            green ?: 0,
                            blue ?: 0

                    )
                    viewModel.colorLed.postValue(ledState)
                }
                else -> { }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            gattQueue.handleCommandProcessed()
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val gattCharacteristic = GattCharacteristic.fromUuid(characteristic.uuid)

            when (gattCharacteristic) {
                GattCharacteristic.Digital -> {
                    val led0State =
                            (characteristic.value[0].toInt() and BlinkyThunderboardViewModel.LED_0_ON) != 0
                    val led1State =
                            (characteristic.value[0].toInt() and BlinkyThunderboardViewModel.LED_1_ON) != 0
                    viewModel.led0.postValue(led0State)
                    viewModel.led1.postValue(led1State)
                }
                GattCharacteristic.RgbLeds -> {
                    val on = characteristic.getIntValue(gattCharacteristic.format, 0)
                    val red = characteristic.getIntValue(gattCharacteristic.format, 1)
                    val green = characteristic.getIntValue(gattCharacteristic.format, 2)
                    val blue = characteristic.getIntValue(gattCharacteristic.format, 3)
                    val ledState = LedRGBState(
                            on != null && on != 0,
                            red ?: 0,
                            green ?: 0,
                            blue ?: 0
                    )
                    viewModel.colorLed.postValue(ledState)
                }
                else -> {
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

                GattCharacteristic.Digital -> {
                    val button0State =
                            (characteristic.value[0].toInt() and BlinkyThunderboardViewModel.BUTTON_0_ON) != 0
                    val button1State =
                            (characteristic.value[0].toInt() and BlinkyThunderboardViewModel.BUTTON_1_ON) != 0
                    viewModel.button0.postValue(button0State)
                    viewModel.button1.postValue(button1State)
                }
                else -> { }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?,
                                      status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)

            if (descriptor?.uuid == LED_MASK_DESCRIPTOR) {
                viewModel.rgbLedMask.postValue(descriptor?.value?.get(0)?.toInt() ?: 0)
            }

        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor,
                                       status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            gattQueue.handleCommandProcessed()
            if (status != BluetoothGatt.GATT_SUCCESS) return

            when (descriptor.characteristic.uuid) {
                GattCharacteristic.BatteryLevel.uuid -> initControls()
                else -> { }
            }
        }
    }

    companion object {
        private val LED_MASK_DESCRIPTOR: UUID? = UUID.fromString("1c694489-8825-45cc-8720-28b54b1fbf00")
    }
}