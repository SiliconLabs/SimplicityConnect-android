package com.siliconlabs.bledemo.features.demo.connected_lighting.presenters

import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.features.demo.connected_lighting.models.TriggerSource
import java.util.*

class ConnectedLightingPresenter internal constructor(private val view: View, private val bluetoothController: BluetoothController) {

    interface View {
        fun showLightState(lightOn: Boolean)

        fun showTriggerSourceDetails(source: TriggerSource?)

        fun showTriggerSourceAddress(sourceAddress: String, source: TriggerSource?)

        fun showDeviceDisconnectedDialog()
    }

    interface BluetoothController {
        fun setLightValue(lightOn: Boolean): Boolean

        fun setPresenter(presenter: ConnectedLightingPresenter?)

        fun getLightValue(): Boolean

        fun leaveDemo()
    }

    private var lightOn = false
    private var triggerSource = TriggerSource.UNKNOWN
    private var sourceAddress = ""
    private var periodicReadTimer: Timer
    var gattService: GattService? = null

    private fun updateLight() {
        bluetoothController.getLightValue()
    }

    fun onLightClicked() {
        bluetoothController.setLightValue(!lightOn)
    }

    fun onLightUpdated(isLightOn: Boolean) {
        lightOn = isLightOn
        view.showLightState(lightOn)
    }

    fun onSourceUpdated(value: TriggerSource) {
        triggerSource = value
        if (gattService === GattService.ProprietaryLightService && value == TriggerSource.ZIGBEE) {
            triggerSource = TriggerSource.PROPRIETARY
        } else if (gattService === GattService.ConnectLightService) {
            triggerSource = TriggerSource.CONNECT
        } else if (gattService === GattService.ThreadLightService) {
            triggerSource = TriggerSource.THREAD
        }
        view.showTriggerSourceDetails(triggerSource)
    }

    fun onSourceAddressUpdated(sourceAddress: String) {
        this.sourceAddress = sourceAddress
        view.showTriggerSourceAddress(sourceAddress, triggerSource)
    }

    fun cancelPeriodicReads() {
        periodicReadTimer.cancel()
    }

    val lightValueDelayed: Unit
        get() {
            periodicReadTimer.cancel()
            periodicReadTimer.purge()
            periodicReadTimer = Timer()
            periodicReadTimer.schedule(object : TimerTask() {
                override fun run() {
                    updateLight()
                }
            }, FAST_TOGGLING_READ_DELAY.toLong())
        }

    fun showDeviceDisconnectedDialog() {
        view.showDeviceDisconnectedDialog()
    }

    fun leaveDemo() {
        bluetoothController.leaveDemo()
    }

    companion object {
        private const val FAST_TOGGLING_READ_DELAY = 800
    }

    init {
        //light_on will be set according to what we read from bluetooth, just hardcoding to start with off for now
        view.showLightState(lightOn)
        view.showTriggerSourceDetails(triggerSource)
        view.showTriggerSourceAddress(sourceAddress, triggerSource)
        bluetoothController.setPresenter(this)
        periodicReadTimer = Timer()
    }
}