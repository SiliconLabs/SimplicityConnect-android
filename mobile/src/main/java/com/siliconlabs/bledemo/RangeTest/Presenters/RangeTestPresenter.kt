package com.siliconlabs.bledemo.RangeTest.Presenters

import android.os.Looper
import androidx.annotation.UiThread
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.RangeTest.Models.DeviceState
import com.siliconlabs.bledemo.RangeTest.Models.RangeTestMode
import com.siliconlabs.bledemo.RangeTest.Models.TxPower
import java.util.*

/**
 * @author Comarch S.A.
 */
class RangeTestPresenter {

    interface Controller {
        fun setView(view: RangeTestView?)
        fun initTestMode(mode: RangeTestMode?)
        fun cancelTestMode()
        fun updateTxPower(power: Int)
        fun updatePayloadLength(length: Int)
        fun updateMaWindowSize(size: Int)
        fun updateChannel(channel: Int)
        fun updatePacketCount(count: Int)
        fun updateRemoteId(id: Int)
        fun updateSelfId(id: Int)
        fun updateUartLogEnabled(enabled: Boolean)
        fun toggleRunningState()
        fun updatePhyConfig(id: Int)
    }

    interface RangeTestView {
        fun runOnUiThread(runnable: Runnable?)

        @UiThread
        fun showDeviceName(name: String?)

        @UiThread
        fun showModelNumber(number: String?, running: Boolean?)

        @UiThread
        fun showTxPower(power: TxPower?, values: List<TxPower>)

        @UiThread
        fun showPayloadLength(length: Int, values: List<Int>)

        @UiThread
        fun showMaWindowSize(size: Int, values: List<Int>)

        @UiThread
        fun showChannelNumber(number: Int)

        @UiThread
        fun showPacketCountRepeat(enabled: Boolean)

        @UiThread
        fun showPacketRequired(required: Int)

        @UiThread
        fun showPacketSent(sent: Int)

        @UiThread
        fun showPer(per: Float)

        @UiThread
        fun showMa(ma: Float)

        @UiThread
        fun showRemoteId(id: Int)

        @UiThread
        fun showSelfId(id: Int)

        @UiThread
        fun showUartLogEnabled(enabled: Boolean)

        @UiThread
        fun showRunningState(running: Boolean)

        @UiThread
        fun showTestRssi(rssi: Int)

        @UiThread
        fun showTestRx(received: Int, required: Int)

        @UiThread
        fun showPhy(phy: Int, values: LinkedHashMap<Int, String>)

        @UiThread
        fun clearTestResults()
    }

    var deviceState1 = DeviceState()
    var deviceState2 = DeviceState()
    var currentDevice = deviceState1

    private var view: RangeTestView? = null

    @UiThread
    fun setView(view: RangeTestView?) {
        this.view = view

        if (view == null) {
            return
        }

        if (currentDevice.deviceName != null) view.showDeviceName(currentDevice.deviceName)
        if (currentDevice.modelNumber != null) view.showModelNumber(currentDevice.modelNumber, currentDevice.running)
        if (currentDevice.txPower != null && currentDevice.txPowerValues != null) view.showTxPower(currentDevice.txPower, currentDevice.txPowerValues!!)
        if (currentDevice.payloadLength != null && currentDevice.payloadLengthValues != null) view.showPayloadLength(currentDevice.payloadLength!!, currentDevice.payloadLengthValues!!)
        if (currentDevice.maWindowSize != null && currentDevice.maWindowSizeValues != null) view.showMaWindowSize(currentDevice.maWindowSize!!, currentDevice.maWindowSizeValues!!)
        if (currentDevice.channelNumber != null) view.showChannelNumber(currentDevice.channelNumber!!)
        if (currentDevice.packetCountRepeat != null) view.showPacketCountRepeat(currentDevice.packetCountRepeat!!)
        if (currentDevice.packetsRequired != null) view.showPacketRequired(currentDevice.packetsRequired!!)
        if (currentDevice.packetsSent != null) view.showPacketSent(currentDevice.packetsSent!!)
        if (currentDevice.remoteId != null) view.showRemoteId(currentDevice.remoteId!!)
        if (currentDevice.selfId != null) view.showSelfId(currentDevice.selfId!!)
        if (currentDevice.uartLogEnabled != null) view.showUartLogEnabled(currentDevice.uartLogEnabled!!)
        if (currentDevice.running != null) view.showRunningState(currentDevice.running!!)
        if (currentDevice.phy != null && currentDevice.phyMap != null) view.showPhy(currentDevice.phy!!, currentDevice.phyMap!!)
        view.showMa(if (currentDevice.ma == null) 0f else currentDevice.ma!!)
        view.showPer(if (currentDevice.per == null) 0f else currentDevice.per!!)
        if (currentDevice.packetsReceived == null || currentDevice.packetCount == null) {
            view.showTestRx(0, 0)
        } else {
            view.showTestRx(currentDevice.packetsReceived!!, currentDevice.packetCount!!)
        }
    }

    fun getDeviceByAddress(address: String) : DeviceState? {
        return when (address) {
            deviceState1.deviceInfo?.address -> deviceState1
            deviceState2.deviceInfo?.address -> deviceState2
            else -> null
        }
    }

    fun getDeviceInfo()  = currentDevice.deviceInfo
    fun getDeviceInfoAt(which: Int) : BluetoothDeviceInfo? {
        return if (which == 1) deviceState1.deviceInfo
               else deviceState2.deviceInfo
    }
    fun setDeviceInfo(info: BluetoothDeviceInfo?) {
        currentDevice.deviceInfo = info
    }
    fun setDeviceInfoAt(which: Int, info: BluetoothDeviceInfo?) {
        if (which == 1) deviceState1.deviceInfo = info
        else deviceState2.deviceInfo = info
    }

    fun getMode() = currentDevice.mode
    fun setMode(mode: RangeTestMode?) {
        currentDevice.mode = mode
    }
    fun setModeAt(which: Int, mode: RangeTestMode?) {
        if (which == 1) deviceState1.mode = mode
        else deviceState2.mode = mode
    }

    fun getTestRunning() = currentDevice.testRunning
    fun setTestRunning(address: String, running: Boolean) {
        getDeviceByAddress(address)?.testRunning = running
    }
    fun setTestRunningAt(which: Int, running: Boolean) {
        if (which == 1) deviceState1.testRunning = running
        else deviceState2.testRunning = running
    }

    fun resetDeviceAt(which: Int) {
        setDeviceInfoAt(which, null)
        setModeAt(which, null)
        setTestRunningAt(which, false)
    }

    fun switchCurrentDevice() {
        currentDevice =
                if (currentDevice === deviceState1) deviceState2
                else deviceState1
    }

    fun setCurrentDevice(which: Int) {
        currentDevice =
                if (which == 1) deviceState1
                else deviceState2
    }

    // controller -> view
    fun onDeviceNameUpdated(address: String, deviceName: String?) {
        getDeviceByAddress(address)?.let {
            it.deviceName = deviceName
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showDeviceName(deviceName)
                }
            })
        }

    }

    fun onModelNumberUpdated(address: String, number: String?) {
        getDeviceByAddress(address)?.let {
            it.modelNumber = number
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showModelNumber(number, it.running)
                }
            })
        }

    }

    fun onTxPowerUpdated(address: String, power: Int) {
        getDeviceByAddress(address)?.let {
            it.txPower = TxPower(power)
            if (it.txPowerValues != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showTxPower(it.txPower, it.txPowerValues!!)
                    }
                })
            }
        }
    }

    fun onPayloadLengthUpdated(address: String, length: Int) {
        getDeviceByAddress(address)?.let {
            it.payloadLength = length
            if (it.payloadLengthValues != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPayloadLength(length, it.payloadLengthValues!!)
                    }
                })
            }
        }

    }

    fun onMaWindowSizeUpdated(address: String, size: Int) {
        getDeviceByAddress(address)?.let {
            it.maWindowSize = size
            if (it.maWindowSizeValues != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showMaWindowSize(size, it.maWindowSizeValues!!)
                    }
                })
            }
        }
    }

    fun onChannelNumberUpdated(address: String, number: Int) {
        getDeviceByAddress(address)?.let {
            it.channelNumber = number
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showChannelNumber(number)
                }
            })
        }
    }

    fun onPacketCountRepeatUpdated(address: String, enabled: Boolean) {
        getDeviceByAddress(address)?.let {
            it.packetCountRepeat = enabled
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPacketCountRepeat(enabled)
                }
            })
        }
    }

    fun onPacketRequiredUpdated(address: String, required: Int) {
        getDeviceByAddress(address)?.let {
            it.packetsRequired = required
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPacketRequired(required)
                }
            })
        }
    }

    fun onPacketSentUpdated(address: String, sent: Int) {
        getDeviceByAddress(address)?.let {
            it.packetsRequired?.let { packetRequired ->
                if (sent <= packetRequired) {
                    it.packetsSent = sent
                    onView(it, object : ViewAction {
                        override fun run(view: RangeTestView) {
                            view.showPacketSent(sent)
                        }
                    })
                }
            }
        }
    }

    fun onRemoteIdUpdated(address: String, id: Int) {
        getDeviceByAddress(address)?.let {
            it.remoteId = id
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showRemoteId(id)
                }
            })
        }
    }

    fun onSelfIdUpdated(address: String, id: Int) {
        getDeviceByAddress(address)?.let {
            it.selfId = id
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showSelfId(id)
                }
            })
        }
    }

    fun onUartLogEnabledUpdated(address: String, enabled: Boolean) {
        getDeviceByAddress(address)?.let {
            it.uartLogEnabled = enabled
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showUartLogEnabled(enabled)
                }
            })
        }
    }

    fun onRunningStateUpdated(address: String, running: Boolean) {
        getDeviceByAddress(address)?.let {
            it.running = running
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showRunningState(running)
                }
            })
        }
    }

    fun onPhyConfigUpdated(address: String, phy: Int) {
        getDeviceByAddress(address)?.let {
            it.phy = phy
            if (it.phyMap != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPhy(phy, it.phyMap!!)
                    }
                })
            }
        }
    }

    fun onPhyMapUpdated(address: String, phyMap: LinkedHashMap<Int, String>) {
        getDeviceByAddress(address)?.let {
            it.phyMap = phyMap
            if (it.phy != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPhy(it.phy!!, phyMap)
                    }
                })
            }
        }
    }

    fun onTxPowerRangeUpdated(address: String, from: Int, to: Int) {
        val values: MutableList<TxPower> = ArrayList()
        var value = from
        while (value <= to) {
            values.add(TxPower(value))
            value += 5
        }

        getDeviceByAddress(address)?.let {
            it.txPowerValues = values
            if (it.txPower != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showTxPower(it.txPower, it.txPowerValues!!)
                    }
                })
            }
        }
    }

    fun onPayloadLengthRangeUpdated(address: String, from: Int, to: Int) {
        val values: MutableList<Int> = ArrayList()
        for (i in 0 until to - from + 1) {
            values.add(from + i)
        }

        getDeviceByAddress(address)?.let {
            it.payloadLengthValues = values
            if (it.payloadLength != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPayloadLength(it.payloadLength!!, it.payloadLengthValues!!)
                    }
                })
            }
        }
    }

    fun onMaWindowSizeRangeUpdated(address: String, from: Int, to: Int) {
        val values: MutableList<Int> = ArrayList()
        var i = from
        while (i <= to) {
            values.add(i)
            i *= 2
        }

        getDeviceByAddress(address)?.let {
            it.maWindowSizeValues = values
            if (it.maWindowSize != null) {
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showMaWindowSize(it.maWindowSize!!, it.maWindowSizeValues!!)
                    }
                })
            }
        }
    }

    fun onTestDataReceived(address: String, rssi: Int, packetCount: Int, packetReceived: Int) {
        getDeviceByAddress(address)?.let {
            if (packetCount < it.lastPacketCount) {
                clearMaBuffer(it)
                it.lastPacketLoss = 0
                onView(it, object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPer(0f)
                        view.showMa(0f)
                        view.clearTestResults()
                    }
                })
            }
            it.lastPacketCount = packetCount

            //        this.packetRequired = packetCount;
            it.packetsReceived = packetReceived
            val totalPacketLoss = packetCount - packetReceived
            val currentPacketLoss = totalPacketLoss - it.lastPacketLoss
            it.lastPacketLoss = totalPacketLoss
            val per = totalPacketLoss * 100f / packetCount.toFloat()
            val ma = updateMa(it, currentPacketLoss)
            onView(it, object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showTestRx(packetReceived, packetCount)
                    view.showPer(per)
                    view.showMa(ma)
                    view.showTestRssi(rssi)
                }
            })
        }
    }

    private fun onView(deviceState: DeviceState, action: ViewAction) {
        if (deviceState === currentDevice) {
            val view = view ?: return
            if (Thread.currentThread() === Looper.getMainLooper().thread) {
                action.run(view)
            } else {
                view.runOnUiThread(Runnable { action.run(view) })
            }
        }
    }

    private fun updateMa(deviceState: DeviceState, packetsLost: Int): Float {
        deviceState.maBuffer[deviceState.maBufferPtr++] = packetsLost

        if (deviceState.maBufferPtr >= deviceState.maBuffer.size) {
            deviceState.maBufferPtr = 0
        }

        val window = getMaWindow(deviceState)
        return sumMaBuffer(deviceState, window) * 100f / window.toFloat()
    }

    private fun sumMaBuffer(deviceState: DeviceState, window: Int): Int {
        var sum = 0
        for (i in 1..window) {
            var location = deviceState.maBufferPtr - i
            if (location < 0) {
                location = deviceState.maBuffer.size - i
            }
            val loss = deviceState.maBuffer[location]
            sum += if (loss >= 0) {
                loss
            } else {
                break
            }
        }
        return sum
    }

    private fun getMaWindow(deviceState: DeviceState) : Int {
        val window = deviceState.maWindowSize
        return if (window != null && window > 0 && window <= MA_WINDOW_MAX) window else MA_WINDOW_DEFAULT
    }

    private fun clearMaBuffer(deviceState: DeviceState) {
        Arrays.fill(deviceState.maBuffer, -1)
        deviceState.maBufferPtr = 0
    }

    private interface ViewAction {
        fun run(view: RangeTestView)
    }

    companion object {
        private const val MA_WINDOW_MAX = 128
        private const val MA_WINDOW_DEFAULT = 32
    }
}