package com.siliconlabs.bledemo.features.demo.connected_lighting.presenters

import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.features.demo.connected_lighting.models.TriggerSource
import java.util.*

class ConnectedLightingPresenter internal constructor(private val view: View, private val bluetoothController: BluetoothController) {

    interface View {
        fun showLightState(
            lightOn: Boolean,
            triggerSource: TriggerSource,
            gattService: GattService?
        )

        fun showLightStateWithSource(
            lightOn: Boolean,
            source: TriggerSource,
            gattService: GattService?
        ) // new method for protocol-specific UI

        fun showTriggerSourceDetails(source: TriggerSource?, gattService: GattService?)

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
        // Pass both light state and trigger source to the view for protocol-specific UI
        view.showLightStateWithSource(lightOn, triggerSource,gattService)
    }

    fun onSourceUpdated(value: TriggerSource) {
        println("ConnectedLightingScreen:onSourceUpdated: $value")
        triggerSource = value
        if (gattService === GattService.ProprietaryLightService && value == TriggerSource.ZIGBEE) {
            triggerSource = TriggerSource.PROPRIETARY
        } else if (gattService === GattService.ConnectLightService) {
            triggerSource = TriggerSource.CONNECT
        } else if (gattService === GattService.ThreadLightService) {
            triggerSource = TriggerSource.THREAD
        } /*else if(gattService === GattService.TheDMP){
            triggerSource = TriggerSource.AWS
        }*/
        view.showTriggerSourceDetails(triggerSource,gattService)
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
        view.showLightState(lightOn,triggerSource, gattService)
        view.showTriggerSourceDetails(triggerSource, gattService)
        view.showTriggerSourceAddress(sourceAddress, triggerSource)
        bluetoothController.setPresenter(this)
        periodicReadTimer = Timer()
    }
}