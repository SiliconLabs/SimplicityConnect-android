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
        if (currentDevice.packetRequired != null) view.showPacketRequired(currentDevice.packetRequired!!)
        if (currentDevice.packetSent != null) view.showPacketSent(currentDevice.packetSent!!)
        if (currentDevice.remoteId != null) view.showRemoteId(currentDevice.remoteId!!)
        if (currentDevice.selfId != null) view.showSelfId(currentDevice.selfId!!)
        if (currentDevice.uartLogEnabled != null) view.showUartLogEnabled(currentDevice.uartLogEnabled!!)
        if (currentDevice.running != null) view.showRunningState(currentDevice.running!!)
        if (currentDevice.phy != null && currentDevice.phyMap != null) view.showPhy(currentDevice.phy!!, currentDevice.phyMap!!)
        view.showMa(if (currentDevice.ma == null) 0f else currentDevice.ma!!)
        view.showPer(if (currentDevice.per == null) 0f else currentDevice.per!!)
        if (currentDevice.packetReceived == null || currentDevice.packetCount == null) {
            view.showTestRx(0, 0)
        } else {
            view.showTestRx(currentDevice.packetReceived!!, currentDevice.packetCount!!)
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
    fun setTestRunning(running: Boolean) {
        currentDevice.testRunning = running
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
    fun onDeviceNameUpdated(deviceName: String?) {
        currentDevice.deviceName = deviceName
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showDeviceName(deviceName)
            }
        })
    }

    fun onModelNumberUpdated(number: String?) {
        currentDevice.modelNumber = number
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showModelNumber(number, currentDevice.running)
            }
        })
    }

    fun onTxPowerUpdated(power: Int) {
        currentDevice.txPower = TxPower(power)
        if (currentDevice.txPowerValues != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showTxPower(currentDevice.txPower, currentDevice.txPowerValues!!)
                }
            })
        }
    }

    fun onPayloadLengthUpdated(length: Int) {
        currentDevice.payloadLength = length
        if (currentDevice.payloadLengthValues != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPayloadLength(length, currentDevice.payloadLengthValues!!)
                }
            })
        }
    }

    fun onMaWindowSizeUpdated(size: Int) {
        currentDevice.maWindowSize = size
        if (currentDevice.maWindowSizeValues != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showMaWindowSize(size, currentDevice.maWindowSizeValues!!)
                }
            })
        }
    }

    fun onChannelNumberUpdated(number: Int) {
        currentDevice.channelNumber = number
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showChannelNumber(number)
            }
        })
    }

    fun onPacketCountRepeatUpdated(enabled: Boolean) {
        currentDevice.packetCountRepeat = enabled
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showPacketCountRepeat(enabled)
            }
        })
    }

    fun onPacketRequiredUpdated(required: Int) {
        currentDevice.packetRequired = required
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showPacketRequired(required)
            }
        })
    }

    fun onPacketReceivedUpdated(received: Int) {
        currentDevice.packetReceived = received
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                if (currentDevice.packetCount != null) {
                    view.showTestRx(received, currentDevice.packetCount!!)
                }
            }
        })
    }

    fun onPacketSentUpdated(sent: Int) {
        currentDevice.packetRequired?.let { packetRequired ->
            if (sent <= packetRequired) {
                currentDevice.packetSent = sent
                onView(object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPacketSent(sent)
                    }
                })
            }
        }
    }

    fun onPacketCountUpdated(packetCount: Int) {
        currentDevice.packetCount = packetCount
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                if (currentDevice.packetReceived != null) {
                    view.showTestRx(currentDevice.packetReceived!!, packetCount)
                }
            }
        })
    }

    fun onMaUpdated(ma: Float) {
        currentDevice.ma = ma
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showMa(ma)
            }
        })
    }

    fun onPerUpdated(per: Float) {
        currentDevice.per = per
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showPer(per)
            }
        })
    }

    fun onRemoteIdUpdated(id: Int) {
        currentDevice.remoteId = id
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showRemoteId(id)
            }
        })
    }

    fun onSelfIdUpdated(id: Int) {
        currentDevice.selfId = id
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showSelfId(id)
            }
        })
    }

    fun onUartLogEnabledUpdated(enabled: Boolean) {
        currentDevice.uartLogEnabled = enabled
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showUartLogEnabled(enabled)
            }
        })
    }

    fun onRunningStateUpdated(running: Boolean) {
        currentDevice.running = running
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showRunningState(running)
            }
        })
    }

    fun onPhyConfigUpdated(phy: Int) {
        currentDevice.phy = phy
        if (currentDevice.phyMap != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPhy(phy, currentDevice.phyMap!!)
                }
            })
        }
    }

    fun onPhyMapUpdated(phyMap: LinkedHashMap<Int, String>) {
        currentDevice.phyMap = phyMap
        if (currentDevice.phy != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPhy(currentDevice.phy!!, phyMap)
                }
            })
        }
    }

    fun onTxPowerRangeUpdated(from: Int, to: Int) {
        val values: MutableList<TxPower> = ArrayList()
        var value = from
        while (value <= to) {
            values.add(TxPower(value))
            value += 5
        }
        currentDevice.txPowerValues = values
        if (currentDevice.txPower != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showTxPower(currentDevice.txPower, currentDevice.txPowerValues!!)
                }
            })
        }
    }

    fun onPayloadLengthRangeUpdated(from: Int, to: Int) {
        val values: MutableList<Int> = ArrayList()
        for (i in 0 until to - from + 1) {
            values.add(from + i)
        }
        currentDevice.payloadLengthValues = values
        if (currentDevice.payloadLength != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPayloadLength(currentDevice.payloadLength!!, currentDevice.payloadLengthValues!!)
                }
            })
        }
    }

    fun onMaWindowSizeRangeUpdated(from: Int, to: Int) {
        val values: MutableList<Int> = ArrayList()
        var i = from
        while (i <= to) {
            values.add(i)
            i *= 2
        }
        currentDevice.maWindowSizeValues = values
        if (currentDevice.maWindowSize != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showMaWindowSize(currentDevice.maWindowSize!!, currentDevice.maWindowSizeValues!!)
                }
            })
        }
    }

    fun onTestDataReceived(rssi: Int, packetCount: Int, packetReceived: Int) {
        if (packetCount < currentDevice.lastPacketCount) {
            clearMaBuffer()
            currentDevice.lastPacketLoss = 0
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPer(0f)
                    view.showMa(0f)
                    view.clearTestResults()
                }
            })
        }
        currentDevice.lastPacketCount = packetCount

//        this.packetRequired = packetCount;
        currentDevice.packetReceived = packetReceived
        val totalPacketLoss = packetCount - packetReceived
        val currentPacketLoss = totalPacketLoss - currentDevice.lastPacketLoss
        currentDevice.lastPacketLoss = totalPacketLoss
        val per = totalPacketLoss * 100f / packetCount.toFloat()
        val ma = updateMa(currentPacketLoss)
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showTestRx(packetReceived, packetCount)
                view.showPer(per)
                view.showMa(ma)
                view.showTestRssi(rssi)
            }
        })
    }

    private fun onView(action: ViewAction) {
        val view = view ?: return
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            action.run(view)
        } else {
            view.runOnUiThread(Runnable { action.run(view) })
        }
    }

    private fun updateMa(packetsLost: Int): Float {
        currentDevice.maBuffer[currentDevice.maBufferPtr++] = packetsLost

        if (currentDevice.maBufferPtr >= currentDevice.maBuffer.size) {
            currentDevice.maBufferPtr = 0
        }

        val window = maWindow
        return sumMaBuffer(window) * 100f / window.toFloat()
    }

    private fun sumMaBuffer(window: Int): Int {
        var sum = 0
        for (i in 1..window) {
            var location = currentDevice.maBufferPtr - i
            if (location < 0) {
                location = currentDevice.maBuffer.size - i
            }
            val loss = currentDevice.maBuffer[location]
            sum += if (loss >= 0) {
                loss
            } else {
                break
            }
        }
        return sum
    }

    private val maWindow: Int
        get() {
            val window = currentDevice.maWindowSize
            return if (window != null && window > 0 && window <= MA_WINDOW_MAX) window else MA_WINDOW_DEFAULT
        }

    private fun clearMaBuffer() {
        Arrays.fill(currentDevice.maBuffer, -1)
        currentDevice.maBufferPtr = 0
    }

    private interface ViewAction {
        fun run(view: RangeTestView)
    }

    companion object {
        private const val MA_WINDOW_MAX = 128
        private const val MA_WINDOW_DEFAULT = 32
    }
}