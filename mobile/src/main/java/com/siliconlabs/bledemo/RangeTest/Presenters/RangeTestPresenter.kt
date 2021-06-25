package com.siliconlabs.bledemo.RangeTest.Presenters

import android.os.Looper
import androidx.annotation.UiThread
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

    private var view: RangeTestView? = null
    private var deviceName: String? = null
    private var modelNumber: String? = null
    private var txPower: TxPower? = null
    private var txPowerValues: List<TxPower>? = null
    private var payloadLength: Int? = null
    private var payloadLengthValues: List<Int>? = null
    private var maWindowSize: Int? = null
    private var maWindowSizeValues: List<Int>? = null
    private var channelNumber: Int? = null
    private var packetCountRepeat: Boolean? = null
    private var packetRequired: Int? = null
    private var packetReceived: Int? = null
    private var packetSent: Int? = null
    private var packetCount: Int? = null
    private var per: Float? = null
    private var ma: Float? = null
    private var remoteId: Int? = null
    private var selfId: Int? = null
    private var uartLogEnabled: Boolean? = null
    private var running: Boolean? = null
    private var phy: Int? = null
    private var phyMap: LinkedHashMap<Int, String>? = null
    private var lastPacketCount = -1
    private var lastPacketLoss = 0
    private val maBuffer = IntArray(MA_WINDOW_MAX)
    private var maBufferPtr = 0

    @UiThread
    fun setView(view: RangeTestView?) {
        this.view = view

        if (view == null) {
            return
        }

        if (deviceName != null) view.showDeviceName(deviceName)
        if (modelNumber != null) view.showModelNumber(modelNumber, running)
        if (txPower != null && txPowerValues != null) view.showTxPower(txPower, txPowerValues!!)
        if (payloadLength != null && payloadLengthValues != null) view.showPayloadLength(payloadLength!!, payloadLengthValues!!)
        if (maWindowSize != null && maWindowSizeValues != null) view.showMaWindowSize(maWindowSize!!, maWindowSizeValues!!)
        if (channelNumber != null) view.showChannelNumber(channelNumber!!)
        if (packetCountRepeat != null) view.showPacketCountRepeat(packetCountRepeat!!)
        if (packetRequired != null) view.showPacketRequired(packetRequired!!)
        if (packetSent != null) view.showPacketSent(packetSent!!)
        if (remoteId != null) view.showRemoteId(remoteId!!)
        if (selfId != null) view.showSelfId(selfId!!)
        if (uartLogEnabled != null) view.showUartLogEnabled(uartLogEnabled!!)
        if (running != null) view.showRunningState(running!!)
        if (phy != null && phyMap != null) view.showPhy(phy!!, phyMap!!)
        view.showMa(if (ma == null) 0f else ma!!)
        view.showPer(if (per == null) 0f else per!!)
        if (packetReceived == null || packetCount == null) {
            view.showTestRx(0, 0)
        } else {
            view.showTestRx(packetReceived!!, packetCount!!)
        }
    }

    // controller -> view
    fun onDeviceNameUpdated(deviceName: String?) {
        this.deviceName = deviceName
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showDeviceName(deviceName)
            }
        })
    }

    fun onModelNumberUpdated(number: String?) {
        modelNumber = number
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showModelNumber(number, running)
            }
        })
    }

    fun onTxPowerUpdated(power: Int) {
        txPower = TxPower(power)
        if (txPowerValues != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showTxPower(txPower, txPowerValues!!)
                }
            })
        }
    }

    fun onPayloadLengthUpdated(length: Int) {
        payloadLength = length
        if (payloadLengthValues != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPayloadLength(length, payloadLengthValues!!)
                }
            })
        }
    }

    fun onMaWindowSizeUpdated(size: Int) {
        maWindowSize = size
        if (maWindowSizeValues != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showMaWindowSize(size, maWindowSizeValues!!)
                }
            })
        }
    }

    fun onChannelNumberUpdated(number: Int) {
        channelNumber = number
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showChannelNumber(number)
            }
        })
    }

    fun onPacketCountRepeatUpdated(enabled: Boolean) {
        packetCountRepeat = enabled
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showPacketCountRepeat(enabled)
            }
        })
    }

    fun onPacketRequiredUpdated(required: Int) {
        packetRequired = required
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showPacketRequired(required)
            }
        })
    }

    fun onPacketReceivedUpdated(received: Int) {
        packetReceived = received
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                if (packetCount != null) {
                    view.showTestRx(received, packetCount!!)
                }
            }
        })
    }

    fun onPacketSentUpdated(sent: Int) {
        packetRequired?.let { packetRequired ->
            if (sent <= packetRequired) {
                packetSent = sent
                onView(object : ViewAction {
                    override fun run(view: RangeTestView) {
                        view.showPacketSent(sent)
                    }
                })
            }
        }
    }

    fun onPacketCountUpdated(packetCount: Int) {
        this.packetCount = packetCount
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                if (packetReceived != null) {
                    view.showTestRx(packetReceived!!, packetCount)
                }
            }
        })
    }

    fun onMaUpdated(ma: Float) {
        this.ma = ma
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showMa(ma)
            }
        })
    }

    fun onPerUpdated(per: Float) {
        this.per = per
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showPer(per)
            }
        })
    }

    fun onRemoteIdUpdated(id: Int) {
        remoteId = id
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showRemoteId(id)
            }
        })
    }

    fun onSelfIdUpdated(id: Int) {
        selfId = id
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showSelfId(id)
            }
        })
    }

    fun onUartLogEnabledUpdated(enabled: Boolean) {
        uartLogEnabled = enabled
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showUartLogEnabled(enabled)
            }
        })
    }

    fun onRunningStateUpdated(running: Boolean) {
        this.running = running
        onView(object : ViewAction {
            override fun run(view: RangeTestView) {
                view.showRunningState(running)
            }
        })
    }

    fun onPhyConfigUpdated(phy: Int) {
        this.phy = phy
        if (phyMap != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPhy(phy, phyMap!!)
                }
            })
        }
    }

    fun onPhyMapUpdated(phyMap: LinkedHashMap<Int, String>) {
        this.phyMap = phyMap
        if (phy != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPhy(phy!!, phyMap)
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
        txPowerValues = values
        if (txPower != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showTxPower(txPower, txPowerValues!!)
                }
            })
        }
    }

    fun onPayloadLengthRangeUpdated(from: Int, to: Int) {
        val values: MutableList<Int> = ArrayList()
        for (i in 0 until to - from + 1) {
            values.add(from + i)
        }
        payloadLengthValues = values
        if (payloadLength != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPayloadLength(payloadLength!!, payloadLengthValues!!)
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
        maWindowSizeValues = values
        if (maWindowSize != null) {
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showMaWindowSize(maWindowSize!!, maWindowSizeValues!!)
                }
            })
        }
    }

    fun onTestDataReceived(rssi: Int, packetCount: Int, packetReceived: Int) {
        if (packetCount < lastPacketCount) {
            clearMaBuffer()
            lastPacketLoss = 0
            onView(object : ViewAction {
                override fun run(view: RangeTestView) {
                    view.showPer(0f)
                    view.showMa(0f)
                    view.clearTestResults()
                }
            })
        }
        lastPacketCount = packetCount

//        this.packetRequired = packetCount;
        this.packetReceived = packetReceived
        val totalPacketLoss = packetCount - packetReceived
        val currentPacketLoss = totalPacketLoss - lastPacketLoss
        lastPacketLoss = totalPacketLoss
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
        maBuffer[maBufferPtr++] = packetsLost

        if (maBufferPtr >= maBuffer.size) {
            maBufferPtr = 0
        }

        val window = maWindow
        return sumMaBuffer(window) * 100f / window.toFloat()
    }

    private fun sumMaBuffer(window: Int): Int {
        var sum = 0
        for (i in 1..window) {
            var location = maBufferPtr - i
            if (location < 0) {
                location = maBuffer.size - i
            }
            val loss = maBuffer[location]
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
            val window = maWindowSize
            return if (window != null && window > 0 && window <= MA_WINDOW_MAX) window else MA_WINDOW_DEFAULT
        }

    private fun clearMaBuffer() {
        Arrays.fill(maBuffer, -1)
        maBufferPtr = 0
    }

    private interface ViewAction {
        fun run(view: RangeTestView)
    }

    companion object {
        private const val MA_WINDOW_MAX = 128
        private const val MA_WINDOW_DEFAULT = 32
    }
}