package com.siliconlabs.bledemo.throughput.viewmodels

import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.throughput.models.PhyStatus
import com.siliconlabs.bledemo.throughput.utils.Converter
import timber.log.Timber
import java.util.*

class ThroughputViewModel : ViewModel() {
    private val _phyStatus: MutableLiveData<PhyStatus> = MutableLiveData()
    val phyStatus = _phyStatus
    private val _mtuSize: MutableLiveData<Int> = MutableLiveData()
    val mtuSize: LiveData<Int> = _mtuSize
    private val _pduSize: MutableLiveData<Int> = MutableLiveData()
    val pduSize: LiveData<Int> = _pduSize
    private val _connectionInterval: MutableLiveData<Double> = MutableLiveData()
    val connectionInterval = _connectionInterval
    private val _slaveLatency: MutableLiveData<Double> = MutableLiveData()
    val slaveLatency = _slaveLatency
    private val _supervisionTimeout: MutableLiveData<Int> = MutableLiveData()
    val supervisionTimeout: LiveData<Int> = _supervisionTimeout
    private val _throughputSpeed: MutableLiveData<Int> = MutableLiveData()
    val throughputSpeed: LiveData<Int> = _throughputSpeed

    private var bitsCounted: Int = 0
    private var timerTask: TimerTask? = null
    private var timer: Timer? = null

    private val _isDownloadActive: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloadActive: LiveData<Boolean> = _isDownloadActive

    var isUploadActive = false
    var isUploadingNotifications = false

    fun updateDownload(characteristic: BluetoothGattCharacteristic, gattCharacteristic: GattCharacteristic) {
        when (gattCharacteristic) {
            GattCharacteristic.ThroughputPhyStatus -> updatePhyStatus(characteristic)
            GattCharacteristic.ThroughputConnectionInterval -> updateConnectionInterval(characteristic)
            GattCharacteristic.ThroughputSlaveLatency -> updateSlaveLatency(characteristic)
            GattCharacteristic.ThroughputSupervisionTimeout -> updateSupervisionTimeout(characteristic)
            GattCharacteristic.ThroughputMtuSize -> updateMtuSize(characteristic)
            GattCharacteristic.ThroughputPduSize -> updatePduSize(characteristic)

            GattCharacteristic.ThroughputTransmissionOn -> switchClock(characteristic, false)
            GattCharacteristic.ThroughputIndications -> addBitsToCount(characteristic)
            GattCharacteristic.ThroughputNotifications -> addBitsToCount(characteristic)
            else -> { }
        }
    }

    fun updateUpload(characteristic: BluetoothGattCharacteristic, gattCharacteristic: GattCharacteristic) {
        when (gattCharacteristic) {
            GattCharacteristic.ThroughputTransmissionOn -> switchClock(characteristic, true)
            GattCharacteristic.ThroughputNotifications -> addBitsToCount(characteristic)
            GattCharacteristic.ThroughputIndications -> addBitsToCount(characteristic)
            else -> { }
        }
    }

    private fun addBitsToCount(characteristic: BluetoothGattCharacteristic) {
        bitsCounted += characteristic.value.size * 8
    }

    private fun switchClock(characteristic: BluetoothGattCharacteristic, isUpload: Boolean) {

        when (characteristic.value[0]) {
            1.toByte() -> {
                updateConnectionState(true, isUpload)
                startTimer()
            }
            0.toByte() -> {
                updateConnectionState(false, isUpload)
                cancelTimer()
                updateSpeed(0)
                bitsCounted = 0
            }
            else -> { }
        }
    }

    private fun updateConnectionState(hasStarted: Boolean, isUpload: Boolean) {
        if (hasStarted) {
            if (isUpload) isUploadActive = true
            else          _isDownloadActive.postValue(true)
        }
        else {
            if (isUpload) isUploadActive = false
            else          _isDownloadActive.postValue(false)
        }

    }

    private fun startTimer() {
        if (timer == null) {
            timer = Timer()
            timerTask = PeriodicSpeedUpdate()
            timer?.scheduleAtFixedRate(timerTask, DISPLAY_REFRESH_PERIOD, DISPLAY_REFRESH_PERIOD)
        }
    }

    private fun cancelTimer() {
        bitsCounted = 0
        timerTask?.cancel()
        timer?.cancel()
        timer?.purge()
        timerTask = null
        timer = null
    }


    private fun updateSpeed(number: Int) {
        _throughputSpeed.postValue(number)
    }

    private fun updatePhyStatus(characteristic: BluetoothGattCharacteristic) {
        _phyStatus.postValue(Converter.getPhyStatus(characteristic.value[0]))
    }

    private fun updateConnectionInterval(characteristic: BluetoothGattCharacteristic) {
        _connectionInterval.postValue(Converter.getInterval(characteristic.value))
    }

    private fun updateSlaveLatency(characteristic: BluetoothGattCharacteristic) {
        _slaveLatency.postValue(Converter.getLatency(characteristic.value))
    }

    private fun updateSupervisionTimeout(characteristic: BluetoothGattCharacteristic) {
        _supervisionTimeout.postValue(Converter.getSupervisionTimeout(characteristic.value))
    }

    private fun updatePduSize(characteristic: BluetoothGattCharacteristic) {
        _pduSize.postValue(Converter.getPduValue(characteristic.value[0]))
    }

    private fun updateMtuSize(characteristic: BluetoothGattCharacteristic) {
        _mtuSize.postValue(Converter.getMtuValue(characteristic.value[0]))
    }

    private inner class PeriodicSpeedUpdate : TimerTask() {
        override fun run() {
            updateSpeed((bitsCounted * 1000 / DISPLAY_REFRESH_PERIOD).toInt())
            bitsCounted = 0
        }
    }

    companion object {
        private const val DISPLAY_REFRESH_PERIOD: Long = 200 // in miliseconds
    }
}